package com.surveyme.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {

    @Insert
    suspend fun insertTrack(track: TrackEntity)

    @Update
    suspend fun updateTrack(track: TrackEntity)

    @Delete
    suspend fun deleteTrack(track: TrackEntity)

    @Query("SELECT * FROM tracks ORDER BY startTime DESC")
    fun getAllTracksFlow(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE id = :trackId LIMIT 1")
    suspend fun getTrackById(trackId: String): TrackEntity?

    @Insert
    suspend fun insertPoints(points: List<LocationPointEntity>)

    @Query("SELECT * FROM location_points WHERE trackId = :trackId ORDER BY time ASC")
    fun getPointsForTrackFlow(trackId: String): Flow<List<LocationPointEntity>>

    @Query("SELECT * FROM location_points WHERE trackId = :trackId ORDER BY time ASC")
    suspend fun getPointsForTrack(trackId: String): List<LocationPointEntity>

    @Transaction
    suspend fun insertTrackWithPoints(track: TrackEntity, points: List<LocationPointEntity>) {
        insertTrack(track)
        insertPoints(points)
    }
}
