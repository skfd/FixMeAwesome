package com.surveyme.data.database

import androidx.room.*
import com.surveyme.data.model.Poi
import com.surveyme.data.model.PoiCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface PoiDao {
    @Query("SELECT * FROM pois WHERE isActive = 1 ORDER BY priority DESC, createdAt DESC")
    fun getAllActivePois(): Flow<List<Poi>>

    @Query("SELECT * FROM pois WHERE isActive = 1 AND category = :category ORDER BY priority DESC, createdAt DESC")
    fun getPoisByCategory(category: PoiCategory): Flow<List<Poi>>

    @Query("SELECT * FROM pois WHERE id = :id LIMIT 1")
    suspend fun getPoiById(id: String): Poi?

    @Query("SELECT * FROM pois WHERE isVisited = 0 AND isActive = 1")
    fun getUnvisitedPois(): Flow<List<Poi>>

    @Query("SELECT * FROM pois WHERE " +
            "latitude BETWEEN :minLat AND :maxLat AND " +
            "longitude BETWEEN :minLon AND :maxLon AND " +
            "isActive = 1")
    suspend fun getPoisInBounds(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double
    ): List<Poi>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoi(poi: Poi)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPois(pois: List<Poi>)

    @Update
    suspend fun updatePoi(poi: Poi)

    @Query("UPDATE pois SET isVisited = 1 WHERE id = :poiId")
    suspend fun markAsVisited(poiId: String)

    @Query("UPDATE pois SET lastNotificationTime = :timestamp WHERE id = :poiId")
    suspend fun updateLastNotificationTime(poiId: String, timestamp: Long)

    @Delete
    suspend fun deletePoi(poi: Poi)

    @Query("DELETE FROM pois WHERE source = :source")
    suspend fun deleteAllFromSource(source: String)

    @Query("DELETE FROM pois")
    suspend fun deleteAllPois()

    @Query("SELECT COUNT(*) FROM pois WHERE isActive = 1")
    suspend fun getActivePoiCount(): Int

    @Query("SELECT COUNT(*) FROM pois WHERE isVisited = 1")
    suspend fun getVisitedPoiCount(): Int
}