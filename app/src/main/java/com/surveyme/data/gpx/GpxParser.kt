package com.surveyme.data.gpx

import com.surveyme.data.model.Poi
import com.surveyme.data.model.PoiCategory
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import timber.log.Timber
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class GpxParser {

    data class GpxWaypoint(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val description: String? = null,
        val type: String? = null,
        val time: Date? = null,
        val elevation: Double? = null
    )

    fun parse(inputStream: InputStream): List<GpxWaypoint> {
        val waypoints = mutableListOf<GpxWaypoint>()

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(inputStream, null)

            var eventType = parser.eventType
            var currentWaypoint: GpxWaypoint? = null
            var currentName: String? = null
            var currentDesc: String? = null
            var currentType: String? = null
            var currentTime: Date? = null
            var currentElevation: Double? = null
            var inWaypoint = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name.lowercase()) {
                            "wpt" -> {
                                inWaypoint = true
                                val lat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                                val lon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()

                                if (lat != null && lon != null) {
                                    currentWaypoint = GpxWaypoint(
                                        name = "",
                                        latitude = lat,
                                        longitude = lon
                                    )
                                }
                            }
                            "name" -> {
                                if (inWaypoint && parser.next() == XmlPullParser.TEXT) {
                                    currentName = parser.text
                                }
                            }
                            "desc" -> {
                                if (inWaypoint && parser.next() == XmlPullParser.TEXT) {
                                    currentDesc = parser.text
                                }
                            }
                            "type" -> {
                                if (inWaypoint && parser.next() == XmlPullParser.TEXT) {
                                    currentType = parser.text
                                }
                            }
                            "time" -> {
                                if (inWaypoint && parser.next() == XmlPullParser.TEXT) {
                                    currentTime = parseTime(parser.text)
                                }
                            }
                            "ele" -> {
                                if (inWaypoint && parser.next() == XmlPullParser.TEXT) {
                                    currentElevation = parser.text.toDoubleOrNull()
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name.lowercase() == "wpt" && inWaypoint) {
                            currentWaypoint?.let { wp ->
                                waypoints.add(
                                    wp.copy(
                                        name = currentName ?: "Waypoint ${waypoints.size + 1}",
                                        description = currentDesc,
                                        type = currentType,
                                        time = currentTime,
                                        elevation = currentElevation
                                    )
                                )
                            }
                            // Reset for next waypoint
                            inWaypoint = false
                            currentWaypoint = null
                            currentName = null
                            currentDesc = null
                            currentType = null
                            currentTime = null
                            currentElevation = null
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing GPX file")
        }

        return waypoints
    }

    fun convertToPois(waypoints: List<GpxWaypoint>, source: String = "gpx"): List<Poi> {
        return waypoints.map { waypoint ->
            Poi(
                name = waypoint.name,
                description = waypoint.description,
                latitude = waypoint.latitude,
                longitude = waypoint.longitude,
                category = categorizeWaypoint(waypoint),
                source = source,
                createdAt = waypoint.time ?: Date(),
                tags = buildMap {
                    waypoint.type?.let { put("type", it) }
                    waypoint.elevation?.let { put("elevation", it.toString()) }
                }
            )
        }
    }

    private fun categorizeWaypoint(waypoint: GpxWaypoint): PoiCategory {
        val type = waypoint.type?.lowercase() ?: ""
        val name = waypoint.name.lowercase()

        return when {
            type.contains("shop") || name.contains("shop") || name.contains("store") -> PoiCategory.SHOP
            type.contains("restaurant") || name.contains("restaurant") || name.contains("cafe") -> PoiCategory.RESTAURANT
            type.contains("tourist") || type.contains("attraction") || name.contains("monument") -> PoiCategory.TOURIST_ATTRACTION
            type.contains("transport") || name.contains("station") || name.contains("stop") -> PoiCategory.PUBLIC_TRANSPORT
            type.contains("amenity") || name.contains("toilet") || name.contains("parking") -> PoiCategory.AMENITY
            type.contains("historic") || name.contains("castle") || name.contains("church") -> PoiCategory.HISTORIC
            type.contains("natural") || name.contains("park") || name.contains("peak") -> PoiCategory.NATURAL
            type.contains("infrastructure") || name.contains("bridge") || name.contains("tower") -> PoiCategory.INFRASTRUCTURE
            else -> PoiCategory.UNKNOWN
        }
    }

    private fun parseTime(timeString: String): Date? {
        val formats = arrayOf(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ssZ"
        )

        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                return sdf.parse(timeString)
            } catch (e: Exception) {
                // Try next format
            }
        }
        return null
    }
}