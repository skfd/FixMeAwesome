package com.surveyme.data.repository

import com.surveyme.data.database.LocationPointEntity
import com.surveyme.data.database.TrackDao
import com.surveyme.data.database.TrackEntity
import com.surveyme.data.model.ActiveTrack
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class TrackRepository(private val trackDao: TrackDao) {

    fun getAllTracksFlow(): Flow<List<TrackEntity>> {
        return trackDao.getAllTracksFlow()
    }

    suspend fun getTrackById(trackId: String): TrackEntity? {
        return trackDao.getTrackById(trackId)
    }

    suspend fun getPointsForTrack(trackId: String): List<LocationPointEntity> {
        return trackDao.getPointsForTrack(trackId)
    }

    suspend fun saveActiveTrack(activeTrack: ActiveTrack, name: String) {
        if (activeTrack.points.isEmpty()) return

        val trackId = UUID.randomUUID().toString()
        val endTime = activeTrack.points.last().time

        val trackEntity = TrackEntity(
            id = trackId,
            name = name,
            totalDistance = activeTrack.totalDistance,
            startTime = activeTrack.startTime,
            endTime = endTime,
            duration = activeTrack.duration
        )

        val pointEntities = activeTrack.points.map { point ->
            LocationPointEntity(
                trackId = trackId,
                latitude = point.latitude,
                longitude = point.longitude,
                altitude = point.altitude,
                time = point.time,
                distanceFromStart = point.distanceFromStart
            )
        }

        trackDao.insertTrackWithPoints(trackEntity, pointEntities)
    }

    suspend fun deleteTrack(trackEntity: TrackEntity) {
        trackDao.deleteTrack(trackEntity)
    }
}
