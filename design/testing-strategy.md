# Survey Me - Testing Strategy Documentation

## 1. Overview

This document outlines the comprehensive testing strategy for the Survey Me Android application, covering all aspects of quality assurance from unit testing to user acceptance testing. It defines testing principles, methodologies, tools, and procedures to ensure the delivery of a robust, reliable, and high-quality application.

### 1.1 Testing Objectives

1. **Quality Assurance**: Ensure the application meets all functional and non-functional requirements
2. **Bug Prevention**: Identify and fix defects early in the development cycle
3. **Regression Prevention**: Ensure new changes don't break existing functionality
4. **Performance Validation**: Verify the app performs well under various conditions
5. **User Experience**: Ensure smooth, intuitive user interactions
6. **Device Compatibility**: Verify functionality across different devices and OS versions
7. **Security Validation**: Ensure data protection and privacy compliance
8. **Accessibility Compliance**: Verify WCAG 2.1 AA compliance

### 1.2 Testing Principles

- **Test Early, Test Often**: Continuous testing throughout development
- **Automation First**: Automate repetitive tests for efficiency
- **Risk-Based Testing**: Focus on critical functionality and high-risk areas
- **Test at All Levels**: Unit, integration, system, and acceptance testing
- **Real-World Conditions**: Test in conditions that mirror actual usage
- **Data-Driven Decisions**: Use metrics to guide testing efforts
- **Continuous Improvement**: Learn from defects and improve test coverage
- **Documentation**: Maintain clear test documentation and reports

## 2. Testing Levels and Types

### 2.1 Testing Pyramid

```
                    /\
                   /  \
                  / E2E \
                 /  Tests \
                /----------\
               / Integration\
              /    Tests     \
             /----------------\
            /   Unit Tests     \
           /____________________\
```

**Distribution:**
- Unit Tests: 70%
- Integration Tests: 20%
- End-to-End Tests: 10%

### 2.2 Unit Testing

#### 2.2.1 Scope
- Individual classes and functions
- Business logic validation
- Data transformations
- Utility functions
- ViewModels and Presenters

#### 2.2.2 Implementation

```kotlin
// Example Unit Test for LocationService
@RunWith(MockitoJUnitRunner::class)
class LocationServiceTest {

    @Mock
    private lateinit var locationRepository: LocationRepository

    @Mock
    private lateinit var gpsManager: GpsManager

    private lateinit var locationService: LocationService

    @Before
    fun setUp() {
        locationService = LocationService(locationRepository, gpsManager)
    }

    @Test
    fun `calculateDistance returns correct distance between two points`() {
        // Given
        val point1 = Location(52.520008, 13.404954) // Berlin
        val point2 = Location(48.856613, 2.352222)  // Paris

        // When
        val distance = locationService.calculateDistance(point1, point2)

        // Then
        assertEquals(877.46, distance, 0.01) // km with 10m precision
    }

    @Test
    fun `filterInaccurateLocations removes points with poor accuracy`() {
        // Given
        val locations = listOf(
            Location(0.0, 0.0, accuracy = 5.0f),
            Location(0.0, 0.0, accuracy = 50.0f),
            Location(0.0, 0.0, accuracy = 10.0f)
        )

        // When
        val filtered = locationService.filterInaccurateLocations(locations, 25.0f)

        // Then
        assertEquals(2, filtered.size)
        assertTrue(filtered.all { it.accuracy!! < 25.0f })
    }

    @Test
    fun `startTracking throws exception when permission denied`() {
        // Given
        whenever(gpsManager.hasLocationPermission()).thenReturn(false)

        // When/Then
        assertThrows<LocationPermissionException> {
            runBlocking {
                locationService.startTracking()
            }
        }
    }
}

// Example ViewModel Test
@ExperimentalCoroutinesApi
class TrackingViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Mock
    private lateinit var trackingUseCase: TrackingUseCase

    private lateinit var viewModel: TrackingViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        viewModel = TrackingViewModel(trackingUseCase)
    }

    @Test
    fun `startTracking updates state to active`() = runTest {
        // Given
        val expectedTrack = Track(id = 1, name = "Test Track")
        whenever(trackingUseCase.startTracking()).thenReturn(Result.success(expectedTrack))

        // When
        viewModel.startTracking()
        advanceUntilIdle()

        // Then
        assertEquals(TrackingState.Active, viewModel.trackingState.value)
        assertEquals(expectedTrack, viewModel.currentTrack.value)
    }

    @Test
    fun `pauseTracking maintains track data`() = runTest {
        // Given
        val track = Track(id = 1, name = "Test Track")
        viewModel.setCurrentTrack(track)

        // When
        viewModel.pauseTracking()

        // Then
        assertEquals(TrackingState.Paused, viewModel.trackingState.value)
        assertEquals(track, viewModel.currentTrack.value)
    }
}
```

### 2.3 Integration Testing

#### 2.3.1 Scope
- Database operations
- API communications
- Service interactions
- Repository layer
- File I/O operations

#### 2.3.2 Implementation

```kotlin
// Database Integration Test
@RunWith(AndroidJUnit4::class)
class TrackDaoTest {

    private lateinit var database: SurveyMeDatabase
    private lateinit var trackDao: TrackDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context, SurveyMeDatabase::class.java
        ).build()
        trackDao = database.trackDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndRetrieveTrack() = runTest {
        // Given
        val track = TrackEntity(
            name = "Morning Walk",
            startTime = System.currentTimeMillis(),
            status = "active"
        )

        // When
        val id = trackDao.insertTrack(track)
        val retrieved = trackDao.getTrackById(id)

        // Then
        assertNotNull(retrieved)
        assertEquals("Morning Walk", retrieved?.name)
        assertEquals("active", retrieved?.status)
    }

    @Test
    fun updateTrackStatistics() = runTest {
        // Given
        val track = TrackEntity(name = "Test", startTime = 0, status = "active")
        val id = trackDao.insertTrack(track)

        // When
        trackDao.updateTrackStatistics(
            trackId = id,
            distance = 1000.0,
            duration = 3600000,
            avgSpeed = 3.5,
            maxSpeed = 5.0,
            pointCount = 100
        )

        val updated = trackDao.getTrackById(id)

        // Then
        assertEquals(1000.0, updated?.totalDistance)
        assertEquals(100, updated?.pointCount)
    }
}

// API Integration Test
@RunWith(AndroidJUnit4::class)
class ApiIntegrationTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: SurveyMeApi

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(SurveyMeApi::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun getNearbyPois_success() = runTest {
        // Given
        val mockResponse = """
            {
                "pois": [
                    {
                        "id": "poi1",
                        "name": "Restaurant",
                        "lat": 52.52,
                        "lon": 13.40,
                        "category": "amenity"
                    }
                ],
                "total": 1
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setBody(mockResponse)
                .setResponseCode(200)
        )

        // When
        val response = apiService.getNearbyPois(52.52, 13.40, 1000)

        // Then
        assertTrue(response.isSuccessful)
        assertEquals(1, response.body()?.pois?.size)
        assertEquals("Restaurant", response.body()?.pois?.first()?.name)
    }
}
```

### 2.4 UI Testing

#### 2.4.1 Espresso Tests

```kotlin
@RunWith(AndroidJUnit4::class)
@LargeTest
class MapFragmentTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @get:Rule
    val permissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    @Before
    fun setUp() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
    }

    @Test
    fun startTrackingButton_startsTracking() {
        // Click FAB to start tracking
        onView(withId(R.id.fab_tracking))
            .perform(click())

        // Verify tracking started
        onView(withId(R.id.tracking_status))
            .check(matches(withText("Recording")))

        // Verify FAB changed to stop button
        onView(withId(R.id.fab_tracking))
            .check(matches(withContentDescription("Stop tracking")))
    }

    @Test
    fun mapInteraction_showsUserLocation() {
        // Wait for map to load
        onView(withId(R.id.map_view))
            .check(matches(isDisplayed()))

        // Click my location button
        onView(withId(R.id.btn_my_location))
            .perform(click())

        // Verify location marker is visible
        onView(withId(R.id.map_view))
            .check { view, _ ->
                val mapView = view as MapView
                assertTrue(mapView.overlays.any { it is MyLocationOverlay })
            }
    }

    @Test
    fun poiList_displaysCorrectly() {
        // Navigate to POI list
        onView(withId(R.id.bottom_nav))
            .perform(NavigationViewActions.navigateTo(R.id.navigation_pois))

        // Verify list is displayed
        onView(withId(R.id.poi_list))
            .check(matches(isDisplayed()))

        // Check first item
        onView(withId(R.id.poi_list))
            .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(0))
            .check(matches(hasDescendant(withId(R.id.poi_name))))
    }
}
```

### 2.5 End-to-End Testing

```kotlin
@RunWith(AndroidJUnit4::class)
@LargeTest
class SurveyWorkflowE2ETest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun completeSurveyWorkflow() {
        // 1. Import POIs
        onView(withId(R.id.bottom_nav))
            .perform(NavigationViewActions.navigateTo(R.id.navigation_pois))

        onView(withId(R.id.fab_import))
            .perform(click())

        // Select test GPX file
        selectTestGpxFile()

        // Verify POIs imported
        onView(withText("5 POIs imported"))
            .check(matches(isDisplayed()))

        // 2. Start tracking
        onView(withId(R.id.bottom_nav))
            .perform(NavigationViewActions.navigateTo(R.id.navigation_map))

        onView(withId(R.id.fab_tracking))
            .perform(click())

        // 3. Simulate movement near POI
        simulateLocationUpdate(52.52, 13.40)

        // 4. Verify notification appears
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.openNotification()

        val notification = device.findObject(
            UiSelector().textContains("POI Nearby")
        )
        assertTrue(notification.exists())

        // 5. Stop tracking
        device.pressBack()

        onView(withId(R.id.fab_tracking))
            .perform(click())

        // 6. Verify track saved
        onView(withId(R.id.bottom_nav))
            .perform(NavigationViewActions.navigateTo(R.id.navigation_tracks))

        onView(withId(R.id.track_list))
            .check(matches(hasDescendant(withText(containsString("Track")))))
    }
}
```

## 3. Performance Testing

### 3.1 Performance Test Cases

```kotlin
@RunWith(AndroidJUnit4::class)
class PerformanceTest {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun measureMapRenderingPerformance() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        scenario.onActivity { activity ->
            benchmarkRule.measureRepeated {
                runWithTimingDisabled {
                    // Setup: Move to specific location
                    activity.mapView.controller.setCenter(GeoPoint(52.52, 13.40))
                }

                // Measure: Pan the map
                activity.mapView.controller.animateTo(
                    GeoPoint(52.53, 13.41)
                )
            }
        }

        // Assert performance metrics
        assertTrue(benchmarkRule.getMetric("frameDurationMs").median < 16.0)
    }

    @Test
    fun measureDatabaseQueryPerformance() = runTest {
        val database = createTestDatabase()
        val dao = database.trackDao()

        // Insert test data
        repeat(1000) { i ->
            dao.insertTrack(
                TrackEntity(
                    name = "Track $i",
                    startTime = System.currentTimeMillis() - i * 1000,
                    status = "completed"
                )
            )
        }

        // Measure query performance
        val startTime = System.nanoTime()
        val tracks = dao.getAllTracksSync()
        val duration = (System.nanoTime() - startTime) / 1_000_000 // ms

        // Assert
        assertTrue("Query took ${duration}ms", duration < 100)
        assertEquals(1000, tracks.size)
    }

    @Test
    fun measureMemoryUsage() {
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()

        // Perform memory-intensive operation
        val pois = List(10000) { i ->
            PointOfInterest(
                id = "poi_$i",
                name = "POI $i",
                location = Location(0.0, 0.0),
                category = PoiCategory.AMENITY
            )
        }

        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryUsed = (finalMemory - initialMemory) / 1024 / 1024 // MB

        // Assert memory usage is reasonable
        assertTrue("Memory usage: ${memoryUsed}MB", memoryUsed < 50)
    }
}
```

### 3.2 Load Testing

```kotlin
class LoadTest {

    @Test
    fun testHighVolumePoiLoading() = runTest {
        val repository = PoiRepository()

        // Generate large dataset
        val pois = List(100_000) { generateRandomPoi() }

        // Measure import time
        val startTime = System.currentTimeMillis()
        repository.importPois(pois)
        val duration = System.currentTimeMillis() - startTime

        // Assert performance
        assertTrue("Import took ${duration}ms", duration < 30_000)

        // Test query performance with large dataset
        val queryStart = System.currentTimeMillis()
        val nearby = repository.getNearbyPois(
            Location(52.52, 13.40),
            radius = 1000
        )
        val queryDuration = System.currentTimeMillis() - queryStart

        assertTrue("Query took ${queryDuration}ms", queryDuration < 500)
    }

    @Test
    fun testContinuousTrackingLoad() = runTest {
        val trackingService = TrackingService()
        val locations = mutableListOf<Location>()

        // Simulate 4 hours of continuous tracking
        val duration = 4 * 60 * 60 // seconds
        val interval = 5 // seconds

        repeat(duration / interval) { i ->
            val location = Location(
                latitude = 52.52 + i * 0.0001,
                longitude = 13.40 + i * 0.0001,
                time = System.currentTimeMillis() + i * interval * 1000
            )

            trackingService.addLocation(location)
            locations.add(location)

            // Check memory every 100 points
            if (i % 100 == 0) {
                val memoryUsage = getMemoryUsage()
                assertTrue("Memory usage at point $i: ${memoryUsage}MB", memoryUsage < 150)
            }
        }

        // Verify all points recorded
        assertEquals(duration / interval, locations.size)
    }
}
```

## 4. Security Testing

### 4.1 Security Test Cases

```kotlin
@RunWith(AndroidJUnit4::class)
class SecurityTest {

    @Test
    fun testDataEncryption() {
        val secureStorage = SecureStorage(context)
        val sensitiveData = "user_token_12345"

        // Store encrypted data
        secureStorage.saveSecurely("token", sensitiveData)

        // Verify data is encrypted in storage
        val prefs = context.getSharedPreferences("secure_prefs", Context.MODE_PRIVATE)
        val storedValue = prefs.getString("token", null)

        assertNotNull(storedValue)
        assertNotEquals(sensitiveData, storedValue) // Should be encrypted

        // Verify decryption works
        val decrypted = secureStorage.getSecurely("token")
        assertEquals(sensitiveData, decrypted)
    }

    @Test
    fun testCertificatePinning() {
        val client = createPinnedHttpClient()

        // Test with correct certificate
        val validRequest = Request.Builder()
            .url("https://api.surveyme.app/health")
            .build()

        val validResponse = client.newCall(validRequest).execute()
        assertTrue(validResponse.isSuccessful)

        // Test with wrong certificate (should fail)
        val invalidClient = createUnpinnedHttpClient()
        val invalidRequest = Request.Builder()
            .url("https://malicious.example.com/api")
            .build()

        assertThrows<SSLPeerUnverifiedException> {
            invalidClient.newCall(invalidRequest).execute()
        }
    }

    @Test
    fun testPermissionHandling() {
        // Revoke location permission
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("pm revoke ${context.packageName} android.permission.ACCESS_FINE_LOCATION")

        // Try to start tracking
        val trackingService = TrackingService(context)
        val result = trackingService.startTracking()

        // Should fail gracefully
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is LocationPermissionException)
    }

    @Test
    fun testSqlInjectionPrevention() {
        val database = createTestDatabase()
        val maliciousInput = "'; DROP TABLE tracks; --"

        // Try SQL injection
        val dao = database.trackDao()
        val tracks = dao.searchTracks(maliciousInput)

        // Should return empty list, not crash
        assertTrue(tracks.isEmpty())

        // Verify table still exists
        val allTracks = dao.getAllTracksSync()
        assertNotNull(allTracks)
    }
}
```

## 5. Accessibility Testing

### 5.1 Accessibility Test Implementation

```kotlin
@RunWith(AndroidJUnit4::class)
class AccessibilityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testScreenReaderSupport() {
        // Enable TalkBack simulation
        AccessibilityChecks.enable()
            .setRunChecksFromRootView(true)
            .setSuppressingResultMatcher(
                allOf(
                    matchesCheckNames(`is`("TouchTargetSizeCheck")),
                    matchesViews(withId(R.id.small_button))
                )
            )

        // Navigate through app
        onView(withId(R.id.fab_tracking))
            .check(matches(withContentDescription(notNullValue())))
            .perform(click())

        // Verify all interactive elements have content descriptions
        onView(isRoot())
            .check { view, _ ->
                checkAccessibility(view)
            }
    }

    @Test
    fun testMinimumTouchTargets() {
        onView(isRoot())
            .check { view, _ ->
                val minSize = 48.dp
                checkAllTouchTargets(view, minSize)
            }
    }

    @Test
    fun testColorContrast() {
        val colors = mapOf(
            R.color.primary_text to R.color.background,
            R.color.secondary_text to R.color.background,
            R.color.button_text to R.color.button_background
        )

        colors.forEach { (foreground, background) ->
            val contrast = calculateContrast(
                context.getColor(foreground),
                context.getColor(background)
            )

            // WCAG AA requires 4.5:1 for normal text
            assertTrue("Contrast ratio: $contrast", contrast >= 4.5)
        }
    }

    @Test
    fun testKeyboardNavigation() {
        // Test tab navigation
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // Press tab multiple times
        repeat(10) {
            device.pressKeyCode(KeyEvent.KEYCODE_TAB)
            Thread.sleep(100)
        }

        // Should cycle through all focusable elements
        onView(withId(R.id.fab_tracking))
            .check(matches(hasFocus()))
    }
}
```

## 6. Test Data Management

### 6.1 Test Data Factories

```kotlin
object TestDataFactory {

    fun createTrack(
        id: Long = 0,
        name: String = "Test Track ${Random.nextInt()}",
        status: TrackStatus = TrackStatus.ACTIVE
    ): Track {
        return Track(
            id = id,
            name = name,
            startTime = System.currentTimeMillis(),
            status = status
        )
    }

    fun createPoi(
        name: String = "Test POI ${Random.nextInt()}",
        lat: Double = Random.nextDouble() * 180 - 90,
        lon: Double = Random.nextDouble() * 360 - 180
    ): PointOfInterest {
        return PointOfInterest(
            name = name,
            location = Location(lat, lon),
            category = PoiCategory.values().random()
        )
    }

    fun createGpxFile(poiCount: Int = 10): File {
        val gpxContent = buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<gpx version="1.1">""")

            repeat(poiCount) { i ->
                val poi = createPoi("POI $i")
                appendLine("""
                    <wpt lat="${poi.location.latitude}" lon="${poi.location.longitude}">
                        <name>${poi.name}</name>
                        <desc>Test POI</desc>
                    </wpt>
                """.trimIndent())
            }

            appendLine("</gpx>")
        }

        val file = File.createTempFile("test", ".gpx")
        file.writeText(gpxContent)
        return file
    }

    fun createMockLocation(
        lat: Double = 52.52,
        lon: Double = 13.40,
        accuracy: Float = 10.0f
    ): android.location.Location {
        return android.location.Location("test").apply {
            latitude = lat
            longitude = lon
            this.accuracy = accuracy
            time = System.currentTimeMillis()
        }
    }
}

// Test data builders
class TrackBuilder {
    private var id: Long = 0
    private var name: String = "Track"
    private var points: MutableList<TrackPoint> = mutableListOf()

    fun withId(id: Long) = apply { this.id = id }
    fun withName(name: String) = apply { this.name = name }
    fun withPoints(count: Int) = apply {
        points = List(count) { i ->
            TrackPoint(
                trackId = id,
                latitude = 52.52 + i * 0.001,
                longitude = 13.40 + i * 0.001,
                timestamp = System.currentTimeMillis() + i * 1000
            )
        }.toMutableList()
    }

    fun build(): Pair<Track, List<TrackPoint>> {
        val track = Track(id = id, name = name)
        return track to points
    }
}
```

## 7. Test Automation Framework

### 7.1 Custom Test Rules

```kotlin
class LocationPermissionRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                // Grant location permission before test
                val instrumentation = InstrumentationRegistry.getInstrumentation()
                instrumentation.uiAutomation.executeShellCommand(
                    "pm grant ${instrumentation.targetContext.packageName} " +
                    "android.permission.ACCESS_FINE_LOCATION"
                )

                try {
                    base.evaluate()
                } finally {
                    // Clean up if needed
                }
            }
        }
    }
}

class DatabaseRule : TestWatcher() {
    lateinit var database: SurveyMeDatabase
        private set

    override fun starting(description: Description) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            SurveyMeDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
    }

    override fun finished(description: Description) {
        database.close()
    }
}

class MockLocationRule(
    private val lat: Double = 52.52,
    private val lon: Double = 13.40
) : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                // Set mock location
                val locationManager = InstrumentationRegistry.getInstrumentation()
                    .targetContext
                    .getSystemService(Context.LOCATION_SERVICE) as LocationManager

                locationManager.addTestProvider(
                    LocationManager.GPS_PROVIDER,
                    false, false, false, false,
                    true, true, true,
                    Criteria.POWER_LOW,
                    Criteria.ACCURACY_FINE
                )

                locationManager.setTestProviderEnabled(
                    LocationManager.GPS_PROVIDER,
                    true
                )

                val location = Location(LocationManager.GPS_PROVIDER).apply {
                    latitude = lat
                    longitude = lon
                    accuracy = 5.0f
                    time = System.currentTimeMillis()
                }

                locationManager.setTestProviderLocation(
                    LocationManager.GPS_PROVIDER,
                    location
                )

                try {
                    base.evaluate()
                } finally {
                    locationManager.removeTestProvider(LocationManager.GPS_PROVIDER)
                }
            }
        }
    }
}
```

## 8. Continuous Testing

### 8.1 Test Execution Strategy

```yaml
# test-execution.yml
testing:
  unit_tests:
    trigger: on_commit
    timeout: 10m
    parallel: true
    coverage_threshold: 80

  integration_tests:
    trigger: on_pull_request
    timeout: 20m
    parallel: false
    retry_failed: 2

  ui_tests:
    trigger: on_merge_to_develop
    timeout: 30m
    devices:
      - api: 26
        device: Pixel 2
      - api: 30
        device: Pixel 4
      - api: 33
        device: Pixel 6

  performance_tests:
    trigger: nightly
    timeout: 60m
    baseline: previous_release
    metrics:
      - app_startup_time
      - frame_render_time
      - memory_usage
      - battery_consumption

  security_tests:
    trigger: weekly
    timeout: 45m
    scans:
      - static_analysis
      - dependency_check
      - penetration_test
```

## 9. Test Reporting

### 9.1 Test Report Generation

```kotlin
class TestReporter {

    fun generateHtmlReport(results: TestResults): String {
        return buildString {
            appendLine("<!DOCTYPE html>")
            appendLine("<html>")
            appendLine("<head><title>Test Report</title></head>")
            appendLine("<body>")
            appendLine("<h1>Survey Me Test Report</h1>")

            appendLine("<h2>Summary</h2>")
            appendLine("<table>")
            appendLine("<tr><td>Total Tests:</td><td>${results.total}</td></tr>")
            appendLine("<tr><td>Passed:</td><td>${results.passed}</td></tr>")
            appendLine("<tr><td>Failed:</td><td>${results.failed}</td></tr>")
            appendLine("<tr><td>Skipped:</td><td>${results.skipped}</td></tr>")
            appendLine("<tr><td>Duration:</td><td>${results.duration}ms</td></tr>")
            appendLine("</table>")

            if (results.failed > 0) {
                appendLine("<h2>Failed Tests</h2>")
                appendLine("<ul>")
                results.failures.forEach { failure ->
                    appendLine("<li>${failure.testName}: ${failure.message}</li>")
                }
                appendLine("</ul>")
            }

            appendLine("<h2>Coverage</h2>")
            appendLine("<p>Line Coverage: ${results.coverage.lineCoverage}%</p>")
            appendLine("<p>Branch Coverage: ${results.coverage.branchCoverage}%</p>")

            appendLine("</body>")
            appendLine("</html>")
        }
    }

    fun generateJsonReport(results: TestResults): String {
        return Json.encodeToString(results)
    }
}

@Serializable
data class TestResults(
    val total: Int,
    val passed: Int,
    val failed: Int,
    val skipped: Int,
    val duration: Long,
    val failures: List<TestFailure>,
    val coverage: Coverage
)

@Serializable
data class TestFailure(
    val testName: String,
    val message: String,
    val stackTrace: String
)

@Serializable
data class Coverage(
    val lineCoverage: Float,
    val branchCoverage: Float,
    val methodCoverage: Float
)
```

## 10. Test Metrics and KPIs

### 10.1 Key Testing Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Test Coverage | > 80% | Line coverage |
| Test Execution Time | < 30 min | CI/CD pipeline |
| Defect Detection Rate | > 90% | Bugs found in testing vs production |
| Test Automation Rate | > 85% | Automated vs manual tests |
| Test Reliability | > 95% | Non-flaky test rate |
| Mean Time to Detect | < 1 hour | Bug discovery time |
| Mean Time to Fix | < 4 hours | Bug resolution time |
| Regression Rate | < 5% | Reintroduced bugs |

---

*This testing strategy document defines comprehensive quality assurance procedures for the Survey Me application, ensuring reliable, high-quality software delivery.*