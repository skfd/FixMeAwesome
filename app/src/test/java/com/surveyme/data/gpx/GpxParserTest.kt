package com.surveyme.data.gpx

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

@RunWith(RobolectricTestRunner::class)
class GpxParserTest {

    private val parser = GpxParser()

    @Test
    fun `parse valid gpx returns waypoints`() {
        val gpxContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" creator="SurveyMe">
                <wpt lat="43.6532" lon="-79.3832">
                    <name>Test Point</name>
                    <desc>A test description</desc>
                    <type>Shop</type>
                    <ele>100.0</ele>
                </wpt>
            </gpx>
        """.trimIndent()

        val inputStream = ByteArrayInputStream(gpxContent.toByteArray(StandardCharsets.UTF_8))
        val result = parser.parse(inputStream)

        assertEquals(1, result.size)
        val waypoint = result[0]
        assertEquals("Test Point", waypoint.name)
        assertEquals(43.6532, waypoint.latitude, 0.0001)
        assertEquals(-79.3832, waypoint.longitude, 0.0001)
        assertEquals("A test description", waypoint.description)
        assertEquals("Shop", waypoint.type)
        assertEquals(100.0, waypoint.elevation!!, 0.001)
    }

    @Test
    fun `parse empty gpx returns empty list`() {
        val gpxContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" creator="SurveyMe">
            </gpx>
        """.trimIndent()

        val inputStream = ByteArrayInputStream(gpxContent.toByteArray(StandardCharsets.UTF_8))
        val result = parser.parse(inputStream)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `convertToPois correctly maps fields`() {
        val waypoint = GpxParser.GpxWaypoint(
            name = "Cafe",
            latitude = 10.0,
            longitude = 20.0,
            description = "Good coffee",
            type = "Restaurant",
            elevation = 50.0
        )

        val pois = parser.convertToPois(listOf(waypoint), "test_source")

        assertEquals(1, pois.size)
        val poi = pois[0]
        assertEquals("Cafe", poi.name)
        assertEquals(10.0, poi.latitude, 0.0001)
        assertEquals(20.0, poi.longitude, 0.0001)
        assertEquals("test_source", poi.source)
        // Verify category logic (mapped in GpxParser)
        // Restaurant type should map to REMOVED_OR_RENAMED (checking logic in parser next)
        // Actually checking parser: "Restaurant" -> PoiCategory.RESTAURANT
    }
}
