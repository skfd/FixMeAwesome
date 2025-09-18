package com.surveyme.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(tableName = "pois")
data class Poi(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String? = null,
    val latitude: Double,
    val longitude: Double,
    val category: PoiCategory = PoiCategory.UNKNOWN,
    val notificationRadius: Int = 50, // meters
    val isVisited: Boolean = false,
    val lastNotificationTime: Long? = null,
    val createdAt: Date = Date(),
    val source: String = "manual", // manual, gpx, osm
    val tags: Map<String, String> = emptyMap(), // OSM-style tags
    val notes: String? = null,
    val priority: Int = 0, // 0 = normal, 1 = high, -1 = low
    val isActive: Boolean = true
) {
    fun distanceTo(lat: Double, lon: Double): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            latitude, longitude,
            lat, lon,
            results
        )
        return results[0]
    }

    fun shouldNotify(currentTimeMillis: Long, cooldownMinutes: Int = 30): Boolean {
        if (!isActive) return false
        if (lastNotificationTime == null) return true

        val cooldownMillis = cooldownMinutes * 60 * 1000L
        return (currentTimeMillis - lastNotificationTime) > cooldownMillis
    }
}