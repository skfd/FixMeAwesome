package com.surveyme.data.repository

import android.content.Context
import com.surveyme.data.database.SurveyMeDatabase
import com.surveyme.data.geojson.GeoJsonParser
import com.surveyme.data.gpx.GpxParser
import com.surveyme.data.model.Poi
import com.surveyme.data.model.PoiCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.InputStream
class PoiRepository(
    private val database: SurveyMeDatabase,
    private val context: Context
) {
    private val poiDao = database.poiDao()
    private val gpxParser = GpxParser()
    private val geoJsonParser = GeoJsonParser()

    fun getAllActivePois(): Flow<List<Poi>> = poiDao.getAllActivePois()

    fun getPoisByCategory(category: PoiCategory): Flow<List<Poi>> =
        poiDao.getPoisByCategory(category)

    fun getUnvisitedPois(): Flow<List<Poi>> = poiDao.getUnvisitedPois()

    suspend fun getPoiById(id: String): Poi? = poiDao.getPoiById(id)

    suspend fun getPoisInBounds(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double
    ): List<Poi> = poiDao.getPoisInBounds(minLat, maxLat, minLon, maxLon)

    suspend fun insertPoi(poi: Poi) = poiDao.insertPoi(poi)

    suspend fun insertPois(pois: List<Poi>) = poiDao.insertPois(pois)

    suspend fun updatePoi(poi: Poi) = poiDao.updatePoi(poi)

    suspend fun markAsVisited(poiId: String) {
        poiDao.markAsVisited(poiId)
        Timber.d("POI marked as visited: $poiId")
    }

    suspend fun updateLastNotificationTime(poiId: String) {
        poiDao.updateLastNotificationTime(poiId, System.currentTimeMillis())
        Timber.d("Updated notification time for POI: $poiId")
    }

    suspend fun deletePoi(poi: Poi) = poiDao.deletePoi(poi)

    suspend fun deleteAllFromSource(source: String) = poiDao.deleteAllFromSource(source)

    suspend fun getStats(): Pair<Int, Int> {
        val active = poiDao.getActivePoiCount()
        val visited = poiDao.getVisitedPoiCount()
        return Pair(active, visited)
    }

    suspend fun importFromGpx(
        inputStream: InputStream,
        sourceName: String = "gpx_import"
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val waypoints = gpxParser.parse(inputStream)
            if (waypoints.isEmpty()) {
                return@withContext Result.failure(Exception("No waypoints found in GPX file"))
            }

            val pois = gpxParser.convertToPois(waypoints, sourceName)
            poiDao.insertPois(pois)

            Timber.d("Imported ${pois.size} POIs from GPX")
            Result.success(pois.size)
        } catch (e: Exception) {
            Timber.e(e, "Failed to import GPX")
            Result.failure(e)
        }
    }

    suspend fun loadBikeShareStations() = withContext(Dispatchers.IO) {
        try {
            // Try loading simplified JSON from assets
            try {
                Timber.d("Attempting to load toronto_bike_stations.json from assets")
                context.assets.open("toronto_bike_stations.json").use { inputStream ->
                    Timber.d("Opened bike stations JSON file from assets")
                    val jsonString = inputStream.bufferedReader().use { it.readText() }
                    val stations = parseBikeStations(jsonString)
                    Timber.d("Parsed ${stations.size} stations from JSON")
                    if (stations.isNotEmpty()) {
                        insertPois(stations)
                        Timber.d("Loaded ${stations.size} Toronto Bike Share stations from assets")
                        return@withContext
                    } else {
                        Timber.w("No stations found in JSON file")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Could not load JSON from assets")
            }

            // Fallback to hardcoded sample stations
            loadFallbackSamplePois()

        } catch (e: Exception) {
            Timber.e(e, "Failed to load bike share stations, using fallback")
            loadFallbackSamplePois()
        }
    }

    private suspend fun loadFallbackSamplePois() {
        // Key Toronto stations as fallback
        val fallbackPois = listOf(
            Poi(
                name = "Union Station",
                description = "Bike Share Toronto - Main Hub",
                latitude = 43.6457,
                longitude = -79.38017,
                category = PoiCategory.PUBLIC_TRANSPORT,
                notificationRadius = 75,
                priority = 2,
                tags = mapOf("operator" to "Bike Share Toronto", "fallback" to "true")
            ),
            Poi(
                name = "St. George Station",
                description = "Bike Share Toronto - University",
                latitude = 43.66719,
                longitude = -79.39956,
                category = PoiCategory.PUBLIC_TRANSPORT,
                notificationRadius = 50,
                priority = 1,
                tags = mapOf("operator" to "Bike Share Toronto", "fallback" to "true")
            ),
            Poi(
                name = "Dundas Square",
                description = "Bike Share Toronto - Downtown",
                latitude = 43.65615,
                longitude = -79.38143,
                category = PoiCategory.PUBLIC_TRANSPORT,
                notificationRadius = 50,
                priority = 1,
                tags = mapOf("operator" to "Bike Share Toronto", "fallback" to "true")
            )
        )

        insertPois(fallbackPois)
        Timber.d("Loaded ${fallbackPois.size} fallback bike share stations")
    }

    private fun parseBikeStations(jsonString: String): List<Poi> {
        val stations = mutableListOf<Poi>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val station = jsonArray.getJSONObject(i)
                val name = station.getString("name")
                val lat = station.getDouble("lat")
                val lon = station.getDouble("lon")
                val capacity = station.optInt("capacity", 20)

                // Determine priority based on capacity
                val priority = when {
                    capacity >= 40 -> 2
                    capacity >= 20 -> 1
                    else -> 0
                }

                // Set notification radius based on importance
                val notificationRadius = when (priority) {
                    2 -> 75
                    1 -> 50
                    else -> 40
                }

                val poi = Poi(
                    name = name,
                    description = "Bike Share Toronto - $capacity docks",
                    latitude = lat,
                    longitude = lon,
                    category = PoiCategory.PUBLIC_TRANSPORT,
                    notificationRadius = notificationRadius,
                    priority = priority,
                    tags = mapOf(
                        "capacity" to capacity.toString(),
                        "operator" to "Bike Share Toronto"
                    ),
                    source = "toronto_bike_json"
                )
                stations.add(poi)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse bike stations JSON")
        }
        return stations
    }

    suspend fun addSamplePois() {
        // This method now loads bike share stations instead of hardcoded POIs
        loadBikeShareStations()
    }
}