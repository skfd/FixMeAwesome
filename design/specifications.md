# Survey Me - Technical Specifications Document

## Table of Contents

1. [Introduction](#1-introduction)
2. [Core Technical Specifications](#2-core-technical-specifications)
3. [Location and GPS Specifications](#3-location-and-gps-specifications)
4. [Map Rendering Specifications](#4-map-rendering-specifications)
5. [Data Format Specifications](#5-data-format-specifications)
6. [Notification System Specifications](#6-notification-system-specifications)
7. [Background Service Specifications](#7-background-service-specifications)
8. [Storage Specifications](#8-storage-specifications)
9. [Network Protocol Specifications](#9-network-protocol-specifications)
10. [Algorithm Specifications](#10-algorithm-specifications)
11. [Performance Specifications](#11-performance-specifications)
12. [Battery Management Specifications](#12-battery-management-specifications)

## 1. Introduction

This document provides detailed technical specifications for the Survey Me Android application. It defines precise implementation requirements, algorithms, data formats, protocols, and technical constraints that must be followed during development.

### 1.1 Document Purpose

This specification serves as the authoritative technical reference for:
- Development teams implementing features
- Quality assurance teams validating functionality
- System architects making design decisions
- Third-party integrators understanding interfaces

### 1.2 Specification Notation

- **MUST**: Absolute requirement
- **SHOULD**: Strong recommendation
- **MAY**: Optional feature
- **MUST NOT**: Absolute prohibition

## 2. Core Technical Specifications

### 2.1 Application Configuration

#### 2.1.1 Android Manifest Requirements

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />

<uses-feature android:name="android.hardware.location.gps" android:required="true" />
<uses-feature android:name="android.hardware.location.network" android:required="false" />
<uses-feature android:name="android.hardware.sensor.compass" android:required="false" />
<uses-feature android:name="android.hardware.sensor.accelerometer" android:required="false" />
```

#### 2.1.2 Build Requirements

| Parameter | Specification |
|-----------|--------------|
| Minimum SDK Version | 23 (Android 6.0) |
| Target SDK Version | 34 (Android 14) |
| Compile SDK Version | 34 |
| Java Version | 17 |
| Kotlin Version | 1.9.22+ |
| Gradle Version | 8.2+ |
| AndroidX | Required |

### 2.2 Application Lifecycle Specifications

#### 2.2.1 State Preservation

The application MUST preserve the following state across process death:
- Current map center coordinates and zoom level
- Active tracking session ID and status
- Loaded POI sets and visibility settings
- User preferences and configuration
- Partial track data not yet persisted

#### 2.2.2 Process Priority

```kotlin
class SurveyMeApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Request high priority
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(
                TRACKING_CHANNEL_ID,
                "Location Tracking",
                importance
            ).apply {
                description = "Persistent notification for location tracking"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        }
    }
}
```

## 3. Location and GPS Specifications

### 3.1 Location Provider Configuration

#### 3.1.1 Fused Location Provider Settings

```kotlin
data class LocationConfiguration(
    val updateInterval: Long,          // milliseconds
    val fastestInterval: Long,         // milliseconds
    val priority: Int,                 // LocationRequest priority
    val smallestDisplacement: Float,   // meters
    val maxWaitTime: Long,            // milliseconds for batched updates
    val numUpdates: Int?              // null for unlimited
)

object LocationProfiles {
    val HIGH_ACCURACY_WALKING = LocationConfiguration(
        updateInterval = 5000,
        fastestInterval = 2000,
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY,
        smallestDisplacement = 2.0f,
        maxWaitTime = 10000,
        numUpdates = null
    )

    val BALANCED_POWER_STATIONARY = LocationConfiguration(
        updateInterval = 30000,
        fastestInterval = 10000,
        priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY,
        smallestDisplacement = 10.0f,
        maxWaitTime = 60000,
        numUpdates = null
    )

    val HIGH_ACCURACY_VEHICLE = LocationConfiguration(
        updateInterval = 2000,
        fastestInterval = 1000,
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY,
        smallestDisplacement = 5.0f,
        maxWaitTime = 5000,
        numUpdates = null
    )
}
```

#### 3.1.2 GPS Signal Quality Metrics

| Metric | Poor | Fair | Good | Excellent |
|--------|------|------|------|-----------|
| Accuracy (meters) | >25 | 15-25 | 5-15 | <5 |
| Satellites Used | <4 | 4-6 | 7-9 | >9 |
| HDOP | >2.0 | 1.5-2.0 | 1.0-1.5 | <1.0 |
| Signal Strength (dBHz) | <30 | 30-35 | 35-40 | >40 |

### 3.2 Location Filtering Algorithm

```kotlin
class KalmanLocationFilter {
    private var lastLocation: Location? = null
    private var lastTimestamp: Long = 0
    private var variance = 1.0

    fun filterLocation(newLocation: Location): Location {
        if (lastLocation == null) {
            lastLocation = newLocation
            lastTimestamp = newLocation.time
            return newLocation
        }

        val timeDelta = (newLocation.time - lastTimestamp) / 1000.0
        lastTimestamp = newLocation.time

        // Predict variance
        variance += timeDelta * PROCESS_NOISE * PROCESS_NOISE

        // Calculate Kalman gain
        val gain = variance / (variance + newLocation.accuracy * newLocation.accuracy)

        // Update estimate
        val filteredLat = lastLocation!!.latitude +
            gain * (newLocation.latitude - lastLocation!!.latitude)
        val filteredLon = lastLocation!!.longitude +
            gain * (newLocation.longitude - lastLocation!!.longitude)

        // Update variance
        variance = (1 - gain) * variance

        return Location(newLocation).apply {
            latitude = filteredLat
            longitude = filteredLon
            accuracy = sqrt(variance).toFloat()
        }
    }

    companion object {
        private const val PROCESS_NOISE = 3.0 // meters per second
    }
}
```

### 3.3 Activity Recognition Integration

```kotlin
class ActivityTransitionManager(private val context: Context) {
    private val transitions = listOf(
        ActivityTransition.Builder()
            .setActivityType(DetectedActivity.WALKING)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
            .build(),
        ActivityTransition.Builder()
            .setActivityType(DetectedActivity.STILL)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
            .build(),
        ActivityTransition.Builder()
            .setActivityType(DetectedActivity.IN_VEHICLE)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
            .build()
    )

    fun registerActivityTransitions() {
        val request = ActivityTransitionRequest(transitions)

        ActivityRecognition.getClient(context)
            .requestActivityTransitionUpdates(request, getPendingIntent())
            .addOnSuccessListener {
                Log.d(TAG, "Activity transitions registered")
            }
    }
}
```

## 4. Map Rendering Specifications

### 4.1 Tile Server Configuration

#### 4.1.1 Primary Tile Sources

```kotlin
enum class TileSource(
    val name: String,
    val baseUrl: String,
    val zoomMin: Int,
    val zoomMax: Int,
    val tileSize: Int,
    val attribution: String
) {
    OSM_STANDARD(
        name = "OpenStreetMap",
        baseUrl = "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
        zoomMin = 0,
        zoomMax = 19,
        tileSize = 256,
        attribution = "© OpenStreetMap contributors"
    ),

    OSM_CYCLE(
        name = "OpenCycleMap",
        baseUrl = "https://tile.thunderforest.com/cycle/{z}/{x}/{y}.png",
        zoomMin = 0,
        zoomMax = 18,
        tileSize = 256,
        attribution = "© Thunderforest, © OpenStreetMap contributors"
    ),

    OSM_TRANSPORT(
        name = "Transport Map",
        baseUrl = "https://tile.thunderforest.com/transport/{z}/{x}/{y}.png",
        zoomMin = 0,
        zoomMax = 18,
        tileSize = 256,
        attribution = "© Thunderforest, © OpenStreetMap contributors"
    )
}
```

#### 4.1.2 Tile Caching Strategy

```kotlin
data class TileCacheConfig(
    val maxCacheSizeBytes: Long = 500 * 1024 * 1024, // 500MB
    val maxCacheAge: Long = 30 * 24 * 60 * 60 * 1000, // 30 days
    val trimCacheSizeBytes: Long = 450 * 1024 * 1024, // 450MB
    val minFreeSpaceBytes: Long = 100 * 1024 * 1024, // 100MB
    val preloadRadius: Int = 2, // tiles around viewport
    val compressionQuality: Int = 85 // JPEG quality
)

class TileCache(config: TileCacheConfig) {
    fun shouldCacheTile(zoom: Int): Boolean {
        return zoom >= 10 && zoom <= 17 // Cache only useful zoom levels
    }

    fun getTilePriority(tile: Tile): Int {
        return when (tile.zoom) {
            in 13..15 -> 1 // Highest priority for walking zoom levels
            in 11..12, 16..17 -> 2 // Medium priority
            else -> 3 // Low priority
        }
    }
}
```

### 4.2 Map Rendering Performance

#### 4.2.1 Frame Rate Requirements

| Interaction | Target FPS | Minimum FPS | Maximum Frame Time |
|------------|------------|-------------|-------------------|
| Idle | 1 | 1 | 1000ms |
| Panning | 60 | 30 | 33ms |
| Zooming | 60 | 30 | 33ms |
| Rotation | 60 | 30 | 33ms |
| Marker Animation | 30 | 24 | 42ms |

#### 4.2.2 Level of Detail (LOD) System

```kotlin
class MapLODManager(private val mapView: MapView) {
    fun getMarkerLOD(zoom: Float): MarkerLOD {
        return when (zoom) {
            in 0f..10f -> MarkerLOD.CLUSTERED
            in 10f..14f -> MarkerLOD.SIMPLE
            in 14f..17f -> MarkerLOD.DETAILED
            else -> MarkerLOD.FULL
        }
    }

    fun getPathLOD(zoom: Float, pointCount: Int): PathLOD {
        val simplificationRatio = when {
            zoom < 12 -> 0.1f
            zoom < 14 -> 0.3f
            zoom < 16 -> 0.6f
            else -> 1.0f
        }

        return if (pointCount * simplificationRatio > 1000) {
            PathLOD.DOUGLAS_PEUCKER
        } else {
            PathLOD.FULL
        }
    }
}
```

## 5. Data Format Specifications

### 5.1 GPX File Format

#### 5.1.1 Supported GPX Elements

```xml
<!-- GPX 1.1 Schema Support -->
<gpx version="1.1" creator="SurveyMe"
     xmlns="http://www.topografix.com/GPX/1/1"
     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
     xsi:schemaLocation="http://www.topografix.com/GPX/1/1
                         http://www.topografix.com/GPX/1/1/gpx.xsd">

    <!-- Metadata -->
    <metadata>
        <name>Survey Session Name</name>
        <desc>Description</desc>
        <author>
            <name>User Name</name>
        </author>
        <time>2024-01-01T12:00:00Z</time>
        <bounds minlat="-90.0" minlon="-180.0" maxlat="90.0" maxlon="180.0"/>
    </metadata>

    <!-- Waypoints (POIs) -->
    <wpt lat="52.5200" lon="13.4050">
        <ele>35.0</ele>
        <time>2024-01-01T12:00:00Z</time>
        <name>POI Name</name>
        <desc>Description</desc>
        <type>amenity:restaurant</type>
        <sym>Restaurant</sym>
        <extensions>
            <surveyme:category>food</surveyme:category>
            <surveyme:priority>high</surveyme:priority>
            <surveyme:radius>50</surveyme:radius>
        </extensions>
    </wpt>

    <!-- Tracks -->
    <trk>
        <name>Track Name</name>
        <desc>Track Description</desc>
        <type>walking</type>
        <trkseg>
            <trkpt lat="52.5200" lon="13.4050">
                <ele>35.0</ele>
                <time>2024-01-01T12:00:00Z</time>
                <hdop>1.2</hdop>
                <vdop>1.5</vdop>
                <pdop>1.9</pdop>
                <extensions>
                    <surveyme:accuracy>5.0</surveyme:accuracy>
                    <surveyme:speed>1.4</surveyme:speed>
                    <surveyme:bearing>45.0</surveyme:bearing>
                    <surveyme:satellites>8</surveyme:satellites>
                </extensions>
            </trkpt>
        </trkseg>
    </trk>
</gpx>
```

#### 5.1.2 GPX Parsing Specifications

```kotlin
class GpxParser {
    companion object {
        const val MAX_FILE_SIZE = 50 * 1024 * 1024 // 50MB
        const val MAX_WAYPOINTS = 100000
        const val MAX_TRACK_POINTS = 500000
        const val COORDINATE_PRECISION = 7 // decimal places
    }

    fun validateGpxFile(file: File): ValidationResult {
        if (file.length() > MAX_FILE_SIZE) {
            return ValidationResult.Error("File too large")
        }

        // Schema validation
        val schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
        val schema = schemaFactory.newSchema(gpxSchemaSource)
        val validator = schema.newValidator()

        return try {
            validator.validate(StreamSource(file))
            ValidationResult.Success
        } catch (e: SAXException) {
            ValidationResult.Error("Invalid GPX format: ${e.message}")
        }
    }
}
```

### 5.2 Internal Data Formats

#### 5.2.1 Track Point Data Structure

```kotlin
data class TrackPoint(
    val id: Long = 0,
    val trackId: Long,
    val latitude: Double,          // WGS84, decimal degrees
    val longitude: Double,         // WGS84, decimal degrees
    val altitude: Double?,         // meters above sea level
    val timestamp: Long,           // Unix timestamp milliseconds
    val accuracy: Float?,          // meters
    val speed: Float?,            // meters/second
    val bearing: Float?,          // degrees from north
    val satelliteCount: Int?,     // number of satellites
    val hdop: Float?,             // horizontal dilution of precision
    val vdop: Float?,             // vertical dilution of precision
    val provider: String,         // "gps", "network", "fused"
    val batteryLevel: Float?     // 0.0-1.0
)
```

#### 5.2.2 POI Data Structure

```kotlin
data class Poi(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String?,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val category: PoiCategory,
    val tags: Map<String, String>,
    val proximityRadius: Int = 50, // meters
    val priority: PoiPriority = PoiPriority.NORMAL,
    val sourceFile: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val lastNotified: Long? = null
)

enum class PoiCategory(val icon: Int, val color: Int) {
    AMENITY(R.drawable.ic_amenity, R.color.amenity),
    SHOP(R.drawable.ic_shop, R.color.shop),
    TOURISM(R.drawable.ic_tourism, R.color.tourism),
    TRANSPORT(R.drawable.ic_transport, R.color.transport),
    NATURAL(R.drawable.ic_natural, R.color.natural),
    HISTORIC(R.drawable.ic_historic, R.color.historic),
    LEISURE(R.drawable.ic_leisure, R.color.leisure),
    UNKNOWN(R.drawable.ic_unknown, R.color.unknown)
}
```

## 6. Notification System Specifications

### 6.1 Proximity Detection Algorithm

```kotlin
class ProximityDetector {
    companion object {
        const val EARTH_RADIUS_METERS = 6371000.0
    }

    /**
     * Haversine formula for distance calculation
     */
    fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) *
                cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS_METERS * c
    }

    /**
     * Efficient spatial indexing using geohash
     */
    fun getGeohash(lat: Double, lon: Double, precision: Int = 7): String {
        val base32 = "0123456789bcdefghjkmnpqrstuvwxyz"
        var minLat = -90.0
        var maxLat = 90.0
        var minLon = -180.0
        var maxLon = 180.0
        val bits = mutableListOf<Int>()

        var i = 0
        while (bits.size < precision * 5) {
            if (i % 2 == 0) { // longitude
                val mid = (minLon + maxLon) / 2
                if (lon > mid) {
                    bits.add(1)
                    minLon = mid
                } else {
                    bits.add(0)
                    maxLon = mid
                }
            } else { // latitude
                val mid = (minLat + maxLat) / 2
                if (lat > mid) {
                    bits.add(1)
                    minLat = mid
                } else {
                    bits.add(0)
                    maxLat = mid
                }
            }
            i++
        }

        return bits.chunked(5)
            .map { chunk ->
                chunk.fold(0) { acc, bit -> (acc shl 1) or bit }
            }
            .map { base32[it] }
            .joinToString("")
    }
}
```

### 6.2 Notification Configuration

```kotlin
data class NotificationConfig(
    val enabled: Boolean = true,
    val minInterval: Long = 60000, // minimum milliseconds between notifications
    val maxNotificationsPerHour: Int = 20,
    val quietHoursStart: LocalTime? = null,
    val quietHoursEnd: LocalTime? = null,
    val vibrationPattern: LongArray = longArrayOf(0, 200, 100, 200),
    val soundEnabled: Boolean = true,
    val ledColor: Int = Color.BLUE,
    val ledOnMs: Int = 300,
    val ledOffMs: Int = 3000,
    val priority: Int = NotificationCompat.PRIORITY_HIGH
)

class NotificationThrottler {
    private val recentNotifications = mutableListOf<Long>()

    fun shouldNotify(
        poiId: String,
        config: NotificationConfig
    ): Boolean {
        val now = System.currentTimeMillis()

        // Clean old notifications
        recentNotifications.removeAll {
            now - it > 3600000 // older than 1 hour
        }

        // Check rate limiting
        if (recentNotifications.size >= config.maxNotificationsPerHour) {
            return false
        }

        // Check minimum interval
        val lastNotification = recentNotifications.lastOrNull() ?: 0
        if (now - lastNotification < config.minInterval) {
            return false
        }

        // Check quiet hours
        if (config.quietHoursStart != null && config.quietHoursEnd != null) {
            val currentTime = LocalTime.now()
            if (currentTime.isAfter(config.quietHoursStart) &&
                currentTime.isBefore(config.quietHoursEnd)) {
                return false
            }
        }

        recentNotifications.add(now)
        return true
    }
}
```

## 7. Background Service Specifications

### 7.1 Foreground Service Implementation

```kotlin
class TrackingService : LifecycleService() {
    companion object {
        const val NOTIFICATION_ID = 1001
        const val WAKE_LOCK_TAG = "SurveyMe:TrackingWakeLock"
        const val WAKE_LOCK_TIMEOUT = 60 * 60 * 1000L // 1 hour
    }

    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var wifiLock: WifiManager.WifiLock

    override fun onCreate() {
        super.onCreate()

        // Acquire wake lock
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            WAKE_LOCK_TAG
        ).apply {
            setReferenceCounted(false)
        }

        // Acquire WiFi lock for network operations
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiLock = wifiManager.createWifiLock(
            WifiManager.WIFI_MODE_FULL_HIGH_PERF,
            "SurveyMe:WifiLock"
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_START_TRACKING -> startTracking()
            ACTION_STOP_TRACKING -> stopTracking()
            ACTION_PAUSE_TRACKING -> pauseTracking()
            ACTION_RESUME_TRACKING -> resumeTracking()
        }

        return START_STICKY // Restart if killed
    }

    private fun startTracking() {
        // Start foreground
        startForeground(NOTIFICATION_ID, createNotification())

        // Acquire locks
        if (!wakeLock.isHeld) {
            wakeLock.acquire(WAKE_LOCK_TIMEOUT)
        }
        if (!wifiLock.isHeld) {
            wifiLock.acquire()
        }

        // Start location updates
        startLocationUpdates()
    }

    private fun createNotification(): Notification {
        val channelId = "tracking_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when location is being tracked"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Survey Me - Tracking Active")
            .setContentText("Recording your location")
            .setSmallIcon(R.drawable.ic_tracking)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(createStopAction())
            .setContentIntent(createContentIntent())
            .build()
    }
}
```

### 7.2 Process Death Recovery

```kotlin
class ProcessRecoveryManager(private val context: Context) {
    companion object {
        const val PREFS_NAME = "process_recovery"
        const val KEY_TRACKING_ACTIVE = "tracking_active"
        const val KEY_TRACK_ID = "track_id"
        const val KEY_LAST_LOCATION = "last_location"
    }

    fun saveState(state: TrackingState) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_TRACKING_ACTIVE, state.isActive)
            .putLong(KEY_TRACK_ID, state.trackId ?: -1)
            .putString(KEY_LAST_LOCATION, gson.toJson(state.lastLocation))
            .apply()
    }

    fun recoverState(): TrackingState? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        if (!prefs.getBoolean(KEY_TRACKING_ACTIVE, false)) {
            return null
        }

        return TrackingState(
            isActive = true,
            trackId = prefs.getLong(KEY_TRACK_ID, -1).takeIf { it != -1L },
            lastLocation = prefs.getString(KEY_LAST_LOCATION, null)?.let {
                gson.fromJson(it, Location::class.java)
            }
        )
    }

    fun clearState() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
```

## 8. Storage Specifications

### 8.1 Database Schema

```sql
-- Database version: 1
PRAGMA foreign_keys = ON;
PRAGMA journal_mode = WAL; -- Write-Ahead Logging for performance

-- Tracks table with indexes
CREATE TABLE tracks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    uuid TEXT UNIQUE NOT NULL,
    name TEXT NOT NULL,
    description TEXT,
    start_time INTEGER NOT NULL,
    end_time INTEGER,
    total_distance REAL DEFAULT 0,
    total_duration INTEGER DEFAULT 0,
    average_speed REAL DEFAULT 0,
    max_speed REAL DEFAULT 0,
    min_altitude REAL,
    max_altitude REAL,
    status TEXT NOT NULL CHECK(status IN ('active', 'paused', 'completed')),
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE INDEX idx_tracks_status ON tracks(status);
CREATE INDEX idx_tracks_start_time ON tracks(start_time DESC);

-- Track points with spatial indexing
CREATE TABLE track_points (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    track_id INTEGER NOT NULL,
    sequence_number INTEGER NOT NULL,
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    altitude REAL,
    timestamp INTEGER NOT NULL,
    accuracy REAL,
    speed REAL,
    bearing REAL,
    satellite_count INTEGER,
    hdop REAL,
    vdop REAL,
    pdop REAL,
    provider TEXT,
    battery_level REAL,
    FOREIGN KEY (track_id) REFERENCES tracks(id) ON DELETE CASCADE,
    UNIQUE(track_id, sequence_number)
);

CREATE INDEX idx_track_points_track_id ON track_points(track_id);
CREATE INDEX idx_track_points_timestamp ON track_points(track_id, timestamp);
CREATE INDEX idx_track_points_location ON track_points(latitude, longitude);

-- POIs with geospatial indexing
CREATE TABLE pois (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    altitude REAL,
    geohash TEXT NOT NULL, -- For spatial queries
    category TEXT NOT NULL,
    tags TEXT, -- JSON string
    proximity_radius INTEGER DEFAULT 50,
    priority INTEGER DEFAULT 0,
    source_file TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    last_notified INTEGER,
    notification_count INTEGER DEFAULT 0
);

CREATE INDEX idx_pois_geohash ON pois(geohash);
CREATE INDEX idx_pois_category ON pois(category);
CREATE INDEX idx_pois_location ON pois(latitude, longitude);

-- Notification history
CREATE TABLE notifications (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    poi_id TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    user_lat REAL NOT NULL,
    user_lon REAL NOT NULL,
    distance REAL NOT NULL,
    acknowledged INTEGER DEFAULT 0,
    action TEXT, -- 'viewed', 'dismissed', 'surveyed'
    FOREIGN KEY (poi_id) REFERENCES pois(id) ON DELETE CASCADE
);

CREATE INDEX idx_notifications_poi_id ON notifications(poi_id);
CREATE INDEX idx_notifications_timestamp ON notifications(timestamp DESC);

-- Settings storage
CREATE TABLE settings (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL,
    updated_at INTEGER NOT NULL
);
```

### 8.2 File System Storage

```kotlin
object StorageManager {
    fun getStorageDirectories(context: Context): StorageDirectories {
        return StorageDirectories(
            internal = StorageLocation(
                root = context.filesDir,
                tracks = File(context.filesDir, "tracks"),
                pois = File(context.filesDir, "pois"),
                exports = File(context.filesDir, "exports"),
                cache = context.cacheDir
            ),
            external = if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                StorageLocation(
                    root = context.getExternalFilesDir(null),
                    tracks = context.getExternalFilesDir("tracks"),
                    pois = context.getExternalFilesDir("pois"),
                    exports = context.getExternalFilesDir("exports"),
                    cache = context.externalCacheDir
                )
            } else null
        )
    }

    fun calculateStorageUsage(context: Context): StorageUsage {
        val statsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
        val uuid = StorageManager.UUID_DEFAULT

        val appStats = statsManager.queryStatsForUid(uuid, Process.myUid())

        return StorageUsage(
            appSizeBytes = appStats.appBytes,
            dataSizeBytes = appStats.dataBytes,
            cacheSizeBytes = appStats.cacheBytes,
            totalSizeBytes = appStats.appBytes + appStats.dataBytes + appStats.cacheBytes
        )
    }
}
```

## 9. Network Protocol Specifications

### 9.1 Tile Server Communication

```kotlin
class TileDownloader {
    companion object {
        const val USER_AGENT = "SurveyMe/1.0 (Android; OSM contributor app)"
        const val MAX_CONCURRENT_DOWNLOADS = 4
        const val CONNECTION_TIMEOUT = 15000 // milliseconds
        const val READ_TIMEOUT = 30000 // milliseconds
        const val MAX_RETRIES = 3
        const val RETRY_DELAY = 1000 // milliseconds
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECTION_TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
        .readTimeout(READ_TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "image/png, image/jpeg")
                    .header("Accept-Encoding", "gzip, deflate")
                    .build()
            )
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.HEADERS
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        })
        .cache(Cache(cacheDir, 100 * 1024 * 1024)) // 100MB cache
        .build()
}
```

### 9.2 API Communication Protocols

```kotlin
interface SurveyMeApi {
    @GET("api/v1/tiles/{z}/{x}/{y}")
    suspend fun getTile(
        @Path("z") zoom: Int,
        @Path("x") x: Int,
        @Path("y") y: Int,
        @Query("style") style: String = "standard"
    ): ResponseBody

    @POST("api/v1/tracks")
    @Multipart
    suspend fun uploadTrack(
        @Part("metadata") metadata: RequestBody,
        @Part file: MultipartBody.Part
    ): TrackUploadResponse

    @GET("api/v1/pois/nearby")
    suspend fun getNearbyPois(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("radius") radiusMeters: Int,
        @Query("categories") categories: List<String>
    ): List<PoiResponse>
}

// Retry policy
class RetryInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var attempt = 0
        var response: Response? = null

        while (attempt < MAX_RETRIES) {
            try {
                response = chain.proceed(chain.request())
                if (response.isSuccessful) {
                    return response
                }
            } catch (e: IOException) {
                if (attempt >= MAX_RETRIES - 1) {
                    throw e
                }
            }

            attempt++
            Thread.sleep(RETRY_DELAY * attempt.toLong())
        }

        return response ?: throw IOException("Failed after $MAX_RETRIES attempts")
    }
}
```

## 10. Algorithm Specifications

### 10.1 Track Simplification Algorithm (Douglas-Peucker)

```kotlin
class DouglasPeuckerSimplifier {
    fun simplify(
        points: List<TrackPoint>,
        tolerance: Double = 5.0 // meters
    ): List<TrackPoint> {
        if (points.size <= 2) return points

        val simplified = mutableListOf<TrackPoint>()
        val stack = Stack<Pair<Int, Int>>()
        stack.push(0 to points.lastIndex)

        val keep = BooleanArray(points.size)
        keep[0] = true
        keep[points.lastIndex] = true

        while (stack.isNotEmpty()) {
            val (start, end) = stack.pop()

            var maxDistance = 0.0
            var maxIndex = 0

            for (i in start + 1 until end) {
                val distance = perpendicularDistance(
                    points[i],
                    points[start],
                    points[end]
                )

                if (distance > maxDistance) {
                    maxDistance = distance
                    maxIndex = i
                }
            }

            if (maxDistance > tolerance) {
                keep[maxIndex] = true
                stack.push(start to maxIndex)
                stack.push(maxIndex to end)
            }
        }

        return points.filterIndexed { index, _ -> keep[index] }
    }

    private fun perpendicularDistance(
        point: TrackPoint,
        lineStart: TrackPoint,
        lineEnd: TrackPoint
    ): Double {
        // Calculate perpendicular distance from point to line
        val dx = lineEnd.longitude - lineStart.longitude
        val dy = lineEnd.latitude - lineStart.latitude

        val mag = sqrt(dx * dx + dy * dy)
        if (mag == 0.0) {
            return calculateDistance(
                point.latitude, point.longitude,
                lineStart.latitude, lineStart.longitude
            )
        }

        val u = ((point.longitude - lineStart.longitude) * dx +
                (point.latitude - lineStart.latitude) * dy) / (mag * mag)

        val closestPoint = when {
            u < 0 -> lineStart
            u > 1 -> lineEnd
            else -> TrackPoint(
                trackId = 0,
                latitude = lineStart.latitude + u * dy,
                longitude = lineStart.longitude + u * dx,
                timestamp = 0
            )
        }

        return calculateDistance(
            point.latitude, point.longitude,
            closestPoint.latitude, closestPoint.longitude
        )
    }
}
```

### 10.2 Clustering Algorithm

```kotlin
class PoiClusterer {
    fun cluster(
        pois: List<Poi>,
        zoom: Float,
        viewBounds: BoundingBox,
        maxClusterRadius: Int = 50 // pixels
    ): List<Cluster> {
        val clusters = mutableListOf<Cluster>()
        val processed = mutableSetOf<String>()

        val pixelRadius = maxClusterRadius / (2.0.pow(zoom.toDouble()))

        for (poi in pois) {
            if (poi.id in processed) continue

            val cluster = Cluster(center = poi)
            cluster.add(poi)
            processed.add(poi.id)

            // Find nearby POIs
            for (other in pois) {
                if (other.id in processed) continue

                val distance = calculateDistance(
                    poi.latitude, poi.longitude,
                    other.latitude, other.longitude
                )

                if (distance <= pixelRadius) {
                    cluster.add(other)
                    processed.add(other.id)
                }
            }

            clusters.add(cluster)
        }

        return clusters
    }
}

class Cluster(var center: Poi) {
    private val items = mutableListOf<Poi>()

    fun add(poi: Poi) {
        items.add(poi)
        // Recalculate center
        center = Poi(
            id = "cluster_${items.size}",
            name = "${items.size} POIs",
            description = null,
            latitude = items.map { it.latitude }.average(),
            longitude = items.map { it.longitude }.average(),
            altitude = null,
            category = PoiCategory.UNKNOWN,
            tags = emptyMap()
        )
    }
}
```

## 11. Performance Specifications

### 11.1 Memory Constraints

| Component | Maximum Memory | Warning Threshold | Critical Threshold |
|-----------|---------------|------------------|-------------------|
| Map Tiles Cache | 100 MB | 80 MB | 95 MB |
| Track Points Buffer | 20 MB | 15 MB | 18 MB |
| POI Database | 50 MB | 40 MB | 45 MB |
| Image Cache | 30 MB | 25 MB | 28 MB |
| Total App Memory | 200 MB | 150 MB | 180 MB |

### 11.2 Response Time Requirements

| Operation | Target | Maximum | Measurement Point |
|-----------|--------|---------|------------------|
| App Cold Start | 2s | 3s | Splash to Map |
| Map Pan/Zoom | 16ms | 33ms | Per Frame |
| POI Load (1000 items) | 500ms | 1s | Query to Display |
| Location Update | 100ms | 200ms | GPS to UI |
| Notification | 50ms | 100ms | Trigger to Show |
| Track Save | 200ms | 500ms | Per 100 points |

## 12. Battery Management Specifications

### 12.1 Power Optimization Strategies

```kotlin
enum class PowerMode {
    HIGH_PERFORMANCE {
        override fun getLocationInterval() = 2000L
        override fun getMapRefreshRate() = 60
        override fun getWakeLockLevel() = PowerManager.PARTIAL_WAKE_LOCK
    },
    BALANCED {
        override fun getLocationInterval() = 5000L
        override fun getMapRefreshRate() = 30
        override fun getWakeLockLevel() = PowerManager.PARTIAL_WAKE_LOCK
    },
    POWER_SAVER {
        override fun getLocationInterval() = 10000L
        override fun getMapRefreshRate() = 15
        override fun getWakeLockLevel() = 0 // No wake lock
    };

    abstract fun getLocationInterval(): Long
    abstract fun getMapRefreshRate(): Int
    abstract fun getWakeLockLevel(): Int
}

class BatteryManager(private val context: Context) {
    fun getCurrentPowerMode(): PowerMode {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        return when {
            batteryLevel > 50 -> PowerMode.HIGH_PERFORMANCE
            batteryLevel > 20 -> PowerMode.BALANCED
            else -> PowerMode.POWER_SAVER
        }
    }

    fun registerBatteryMonitor() {
        context.registerReceiver(
            BatteryReceiver(),
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
    }
}
```

### 12.2 Doze Mode Compatibility

```kotlin
class DozeManager(private val context: Context) {
    @RequiresApi(Build.VERSION_CODES.M)
    fun requestBatteryOptimizationExemption() {
        val packageName = context.packageName
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager

        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            context.startActivity(intent)
        }
    }

    fun scheduleDozeCompatibleWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val syncWork = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES // Minimum for doze compatibility
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                1, TimeUnit.MINUTES
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "sync_work",
                ExistingPeriodicWorkPolicy.KEEP,
                syncWork
            )
    }
}
```

---

*This technical specifications document defines the precise implementation requirements for Survey Me. All development must adhere to these specifications to ensure consistency, reliability, and performance.*