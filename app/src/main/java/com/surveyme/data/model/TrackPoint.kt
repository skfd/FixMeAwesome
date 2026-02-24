package com.surveyme.data.model

data class TrackPoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val time: Long,
    val distanceFromStart: Float
)
