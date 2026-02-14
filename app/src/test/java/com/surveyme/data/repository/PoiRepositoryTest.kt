package com.surveyme.data.repository

import android.content.Context
import com.surveyme.data.database.PoiDao
import com.surveyme.data.database.SurveyMeDatabase
import com.surveyme.data.model.Poi
import com.surveyme.data.model.PoiCategory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PoiRepositoryTest {

    private lateinit var database: SurveyMeDatabase
    private lateinit var poiDao: PoiDao
    private lateinit var context: Context
    private lateinit var repository: PoiRepository

    @Before
    fun setup() {
        database = mockk()
        poiDao = mockk(relaxed = true)
        context = mockk(relaxed = true)

        every { database.poiDao() } returns poiDao

        repository = PoiRepository(database, context)
    }

    @Test
    fun `getAllActivePois returns flow from dao`() = runTest {
        val testPois = listOf(
            Poi(
                id = "1",
                name = "Point 1",
                description = "Desc 1",
                latitude = 1.0,
                longitude = 1.0,
                category = PoiCategory.UNKNOWN,
                source = "test"
            ),
            Poi(
                id = "2",
                name = "Point 2",
                description = "Desc 2",
                latitude = 2.0,
                longitude = 2.0,
                category = PoiCategory.UNKNOWN,
                source = "test"
            )
        )

        every { poiDao.getAllActivePois() } returns flowOf(testPois)

        val result = repository.getAllActivePois().toList()
        
        assertEquals(1, result.size)
        assertEquals(testPois, result[0])
        io.mockk.verify { poiDao.getAllActivePois() }
    }

    @Test
    fun `insertPoi delegates to dao`() = runTest {
        val poi = Poi(
            id = "1",
            name = "Point 1",
            description = "Desc 1",
            latitude = 1.0,
            longitude = 1.0,
            category = PoiCategory.UNKNOWN,
            source = "test"
        )

        repository.insertPoi(poi)

        coVerify { poiDao.insertPoi(poi) }
    }
}
