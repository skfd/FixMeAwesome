package com.surveyme.data.model

data class ActiveTrack(
    val points: List<TrackPoint> = emptyList(),
    val totalDistance: Float = 0f,
    val startTime: Long = 0L,
    val duration: Long = 0L
)
