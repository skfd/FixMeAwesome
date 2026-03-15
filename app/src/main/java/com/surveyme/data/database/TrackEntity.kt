package com.surveyme.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey
    val id: String, // We'll use UUID strings
    val name: String,
    val totalDistance: Float, // in meters
    val startTime: Long, // timestamp
    val endTime: Long, // timestamp
    val duration: Long // in milliseconds
)
