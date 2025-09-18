package com.surveyme.data.geojson

import com.surveyme.data.model.Poi
import com.surveyme.data.model.PoiCategory
import org.json.JSONObject
import timber.log.Timber
import java.io.InputStream

class GeoJsonParser {

    fun parseBikeShareStations(inputStream: InputStream): List<Poi> {
        val pois = mutableListOf<Poi>()

        try {
            val jsonString = inputStream.bufferedReader().use { it.readText() }.trim()

            // Check if file was read properly
            if (jsonString.isEmpty()) {
                Timber.e("GeoJSON file is empty")
                return emptyList()
            }

            Timber.d("GeoJSON file size: ${jsonString.length} characters")

            val geoJson = JSONObject(jsonString)

            val features = geoJson.getJSONArray("features")

            for (i in 0 until features.length()) {
                val feature = features.getJSONObject(i)
                val properties = feature.optJSONObject("properties") ?: continue
                val geometry = feature.optJSONObject("geometry") ?: continue

                // Skip if not a Point geometry
                if (geometry.getString("type") != "Point") continue

                val coordinates = geometry.getJSONArray("coordinates")
                val longitude = coordinates.getDouble(0)
                val latitude = coordinates.getDouble(1)

                // Extract bike share specific properties
                val name = properties.optString("name", "Bike Station #${i + 1}")
                val capacity = properties.optString("capacity", "")
                val operator = properties.optString("operator", "Bike Share Toronto")
                val network = properties.optString("network", "")

                // Build description
                val description = buildString {
                    append(operator)
                    if (capacity.isNotEmpty()) {
                        append(" - $capacity docks")
                    }
                    if (network.isNotEmpty()) {
                        append(" ($network)")
                    }
                }

                // Determine priority based on capacity
                val priority = when {
                    capacity.toIntOrNull()?.let { it >= 40 } == true -> 2
                    capacity.toIntOrNull()?.let { it >= 20 } == true -> 1
                    else -> 0
                }

                // Set notification radius based on station importance
                val notificationRadius = when (priority) {
                    2 -> 75
                    1 -> 50
                    else -> 40
                }

                val poi = Poi(
                    name = name,
                    description = description.ifEmpty { "Bike Share Station" },
                    latitude = latitude,
                    longitude = longitude,
                    category = PoiCategory.PUBLIC_TRANSPORT,
                    notificationRadius = notificationRadius,
                    priority = priority,
                    tags = buildMap {
                        if (capacity.isNotEmpty()) put("capacity", capacity)
                        put("operator", operator)
                        if (network.isNotEmpty()) put("network", network)

                        // Add additional properties if they exist
                        properties.optString("amenity", "").let {
                            if (it.isNotEmpty()) put("amenity", it)
                        }
                        properties.optString("bicycle_parking", "").let {
                            if (it.isNotEmpty()) put("bicycle_parking", it)
                        }
                    },
                    source = "bikeshare_geojson"
                )

                pois.add(poi)
            }

            Timber.d("Parsed ${pois.size} bike share stations from GeoJSON")

        } catch (e: Exception) {
            Timber.e(e, "Failed to parse GeoJSON file")
        }

        return pois
    }
}