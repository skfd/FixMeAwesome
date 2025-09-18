package com.surveyme.core

import android.location.Location
import com.surveyme.data.model.Poi
import timber.log.Timber
import kotlin.math.*

class ProximityDetector {

    companion object {
        private const val EARTH_RADIUS_METERS = 6371000.0
        private const val MIN_TIME_BETWEEN_NOTIFICATIONS_MS = 300000L // 5 minutes
    }

    private val lastNotificationTimes = mutableMapOf<String, Long>()
    private val visitedPois = mutableSetOf<String>()

    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    fun checkProximity(
        currentLat: Double,
        currentLon: Double,
        pois: List<Poi>,
        onProximityDetected: (Poi, Float) -> Unit
    ) {
        val currentTime = System.currentTimeMillis()
        Timber.d("Checking proximity for ${pois.size} POIs at location: $currentLat, $currentLon")

        pois.forEach { poi ->
            val distance = calculateDistance(currentLat, currentLon, poi.latitude, poi.longitude)
            Timber.v("Distance to ${poi.name}: ${distance}m (radius: ${poi.notificationRadius}m)")

            // Check if within notification radius
            if (distance <= poi.notificationRadius) {
                Timber.d("${poi.name} is within radius (${distance}m <= ${poi.notificationRadius}m)")

                val lastNotified = lastNotificationTimes[poi.id] ?: 0L
                val timeSinceLastNotification = currentTime - lastNotified
                val isVisited = visitedPois.contains(poi.id)

                Timber.d("POI ${poi.name}: visited=$isVisited, timeSinceNotification=${timeSinceLastNotification}ms")

                // Only notify if enough time has passed and POI hasn't been visited
                if (timeSinceLastNotification > MIN_TIME_BETWEEN_NOTIFICATIONS_MS &&
                    !isVisited) {

                    Timber.d("Proximity detected: ${poi.name} at ${distance}m - triggering notification")
                    lastNotificationTimes[poi.id] = currentTime
                    onProximityDetected(poi, distance)
                } else {
                    if (isVisited) {
                        Timber.d("Skipping notification for ${poi.name} - already visited")
                    } else {
                        Timber.d("Skipping notification for ${poi.name} - cooldown period (${MIN_TIME_BETWEEN_NOTIFICATIONS_MS - timeSinceLastNotification}ms remaining)")
                    }
                }
            }
        }
    }

    fun markPoiAsVisited(poiId: String) {
        visitedPois.add(poiId)
        Timber.d("POI marked as visited: $poiId")
    }

    fun resetVisitedPois() {
        visitedPois.clear()
        lastNotificationTimes.clear()
        Timber.d("Reset visited POIs and notification times")
    }

    fun getProximityPois(
        currentLat: Double,
        currentLon: Double,
        pois: List<Poi>,
        maxDistance: Float = 500f
    ): List<Pair<Poi, Float>> {
        return pois.mapNotNull { poi ->
            val distance = calculateDistance(currentLat, currentLon, poi.latitude, poi.longitude)
            if (distance <= maxDistance) {
                poi to distance
            } else {
                null
            }
        }.sortedBy { it.second }
    }
}