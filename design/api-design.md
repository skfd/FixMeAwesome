# Survey Me - API and Service Design Documentation

## 1. Overview

This document defines all internal and external APIs, service interfaces, communication protocols, and integration points for the Survey Me Android application. It serves as the authoritative reference for all API contracts and service designs.

### 1.1 API Architecture Principles

1. **RESTful Design**: Follow REST principles for external APIs
2. **Versioning**: Explicit API versioning for backward compatibility
3. **Consistency**: Uniform naming conventions and response formats
4. **Error Handling**: Comprehensive error codes and messages
5. **Documentation**: Self-documenting APIs with OpenAPI/Swagger
6. **Security**: Authentication, authorization, and encryption
7. **Performance**: Pagination, caching, and rate limiting
8. **Monitoring**: Request tracking and performance metrics

### 1.2 API Categories

- **Internal Service APIs**: Communication between app components
- **External REST APIs**: Communication with backend services
- **Third-party Integrations**: OSM, tile servers, geocoding services
- **Platform APIs**: Android system services and Google Play Services

## 2. Internal Service APIs

### 2.1 Location Service API

```kotlin
interface LocationService {
    /**
     * Start location tracking with specified configuration
     */
    suspend fun startTracking(config: LocationConfig): Result<TrackingSession>

    /**
     * Stop current tracking session
     */
    suspend fun stopTracking(): Result<Unit>

    /**
     * Pause current tracking session
     */
    suspend fun pauseTracking(): Result<Unit>

    /**
     * Resume paused tracking session
     */
    suspend fun resumeTracking(): Result<Unit>

    /**
     * Get current location
     */
    suspend fun getCurrentLocation(): Result<Location>

    /**
     * Observe location updates
     */
    fun observeLocationUpdates(): Flow<Location>

    /**
     * Observe tracking state changes
     */
    fun observeTrackingState(): Flow<TrackingState>

    /**
     * Get GPS status information
     */
    fun getGpsStatus(): Flow<GpsStatus>
}

data class LocationConfig(
    val accuracy: LocationAccuracy = LocationAccuracy.HIGH,
    val interval: Long = 5000, // milliseconds
    val fastestInterval: Long = 2000,
    val minDisplacement: Float = 2.0f, // meters
    val enableBackgroundTracking: Boolean = true,
    val powerMode: PowerMode = PowerMode.BALANCED
)

enum class LocationAccuracy {
    HIGH,      // GPS + Network + Sensors
    BALANCED,  // Network + Sensors
    LOW,       // Network only
    PASSIVE    // Passive provider only
}

data class GpsStatus(
    val isEnabled: Boolean,
    val satellitesInView: Int,
    val satellitesUsed: Int,
    val accuracy: Float?, // meters
    val lastFixTime: Long?,
    val signalStrength: SignalStrength
)

enum class SignalStrength {
    EXCELLENT, // < 5m accuracy
    GOOD,      // 5-10m
    FAIR,      // 10-25m
    POOR,      // > 25m
    NO_SIGNAL
}

sealed class TrackingState {
    object Idle : TrackingState()
    data class Active(
        val sessionId: String,
        val startTime: Long,
        val pointCount: Int,
        val distance: Double
    ) : TrackingState()
    data class Paused(
        val sessionId: String,
        val pausedAt: Long
    ) : TrackingState()
    object Stopped : TrackingState()
}
```

### 2.2 Map Service API

```kotlin
interface MapService {
    /**
     * Load map tiles for specified region
     */
    suspend fun loadMapTiles(
        bounds: BoundingBox,
        zoomRange: IntRange
    ): Result<TileLoadResult>

    /**
     * Get tile from cache or network
     */
    suspend fun getTile(
        x: Int,
        y: Int,
        zoom: Int,
        source: TileSource = TileSource.DEFAULT
    ): Result<Bitmap>

    /**
     * Pre-cache tiles for offline use
     */
    suspend fun downloadOfflineRegion(
        region: OfflineRegion,
        progressCallback: (DownloadProgress) -> Unit
    ): Result<Unit>

    /**
     * Delete offline region
     */
    suspend fun deleteOfflineRegion(regionId: String): Result<Unit>

    /**
     * Get list of offline regions
     */
    suspend fun getOfflineRegions(): Result<List<OfflineRegion>>

    /**
     * Clear tile cache
     */
    suspend fun clearCache(): Result<ClearCacheResult>

    /**
     * Get cache statistics
     */
    suspend fun getCacheStats(): Result<CacheStats>
}

data class BoundingBox(
    val north: Double,
    val south: Double,
    val east: Double,
    val west: Double
) {
    fun contains(lat: Double, lon: Double): Boolean {
        return lat in south..north && lon in west..east
    }

    fun expand(factor: Double): BoundingBox {
        val latExpand = (north - south) * factor / 2
        val lonExpand = (east - west) * factor / 2
        return BoundingBox(
            north = (north + latExpand).coerceAtMost(90.0),
            south = (south - latExpand).coerceAtLeast(-90.0),
            east = (east + lonExpand).coerceAtMost(180.0),
            west = (west - lonExpand).coerceAtLeast(-180.0)
        )
    }
}

data class TileLoadResult(
    val tilesLoaded: Int,
    val tilesFailed: Int,
    val fromCache: Int,
    val fromNetwork: Int,
    val totalSizeBytes: Long
)

data class DownloadProgress(
    val regionId: String,
    val tilesDownloaded: Int,
    val totalTiles: Int,
    val bytesDownloaded: Long,
    val estimatedBytesTotal: Long,
    val percentComplete: Float
)

data class CacheStats(
    val totalSizeBytes: Long,
    val tileCount: Int,
    val oldestTile: Long,
    val newestTile: Long,
    val hitRate: Float
)
```

### 2.3 POI Service API

```kotlin
interface PoiService {
    /**
     * Import POIs from GPX file
     */
    suspend fun importFromGpx(
        fileUri: Uri,
        options: ImportOptions = ImportOptions()
    ): Result<ImportResult>

    /**
     * Export POIs to GPX file
     */
    suspend fun exportToGpx(
        pois: List<PointOfInterest>,
        outputUri: Uri
    ): Result<ExportResult>

    /**
     * Get POIs within radius of location
     */
    suspend fun getNearbyPois(
        location: Location,
        radiusMeters: Int,
        categories: Set<PoiCategory>? = null
    ): Result<List<PointOfInterest>>

    /**
     * Get POIs within bounding box
     */
    suspend fun getPoisInBounds(
        bounds: BoundingBox,
        categories: Set<PoiCategory>? = null
    ): Result<List<PointOfInterest>>

    /**
     * Search POIs by name or tags
     */
    suspend fun searchPois(
        query: String,
        limit: Int = 50
    ): Result<List<PointOfInterest>>

    /**
     * Register geofences for POI proximity alerts
     */
    suspend fun registerGeofences(
        pois: List<PointOfInterest>
    ): Result<GeofenceRegistrationResult>

    /**
     * Handle geofence transition event
     */
    suspend fun handleGeofenceTransition(
        event: GeofenceEvent
    ): Result<Unit>

    /**
     * Mark POI as visited
     */
    suspend fun markAsVisited(
        poiId: String,
        notes: String? = null
    ): Result<Unit>
}

data class ImportOptions(
    val overwriteExisting: Boolean = false,
    val validateCoordinates: Boolean = true,
    val defaultCategory: PoiCategory = PoiCategory.CUSTOM,
    val defaultProximityRadius: Int = 50
)

data class ImportResult(
    val imported: Int,
    val skipped: Int,
    val failed: Int,
    val errors: List<ImportError>
)

data class ImportError(
    val line: Int,
    val reason: String,
    val data: String?
)

data class ExportResult(
    val exported: Int,
    val fileSizeBytes: Long,
    val filePath: String
)

data class GeofenceRegistrationResult(
    val registered: Int,
    val failed: Int,
    val activeGeofences: Int
)

data class GeofenceEvent(
    val poiId: String,
    val transition: GeofenceTransition,
    val location: Location,
    val timestamp: Long
)

enum class GeofenceTransition {
    ENTER,
    EXIT,
    DWELL
}
```

### 2.4 Notification Service API

```kotlin
interface NotificationService {
    /**
     * Show POI proximity notification
     */
    fun showProximityNotification(
        poi: PointOfInterest,
        distance: Float
    )

    /**
     * Show tracking status notification
     */
    fun showTrackingNotification(
        state: TrackingState,
        stats: TrackingStats?
    )

    /**
     * Update tracking notification
     */
    fun updateTrackingNotification(
        stats: TrackingStats
    )

    /**
     * Cancel specific notification
     */
    fun cancelNotification(notificationId: Int)

    /**
     * Cancel all notifications
     */
    fun cancelAllNotifications()

    /**
     * Configure notification settings
     */
    fun configureNotificationSettings(
        settings: NotificationSettings
    )

    /**
     * Check if notifications are enabled
     */
    fun areNotificationsEnabled(): Boolean

    /**
     * Request notification permission (Android 13+)
     */
    suspend fun requestNotificationPermission(): Boolean
}

data class TrackingStats(
    val duration: Long,
    val distance: Double,
    val avgSpeed: Double,
    val currentSpeed: Double?,
    val pointCount: Int,
    val batteryLevel: Float?
)

data class NotificationSettings(
    val channelId: String,
    val importance: Int,
    val soundEnabled: Boolean,
    val vibrationEnabled: Boolean,
    val vibrationPattern: LongArray?,
    val ledEnabled: Boolean,
    val ledColor: Int?
)
```

## 3. External REST APIs

### 3.1 Backend API Specification

```yaml
openapi: 3.0.0
info:
  title: Survey Me Backend API
  version: 1.0.0
  description: Backend services for Survey Me app

servers:
  - url: https://api.surveyme.app/v1
    description: Production server
  - url: https://staging-api.surveyme.app/v1
    description: Staging server

paths:
  /auth/login:
    post:
      summary: User login
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required:
                - email
                - password
              properties:
                email:
                  type: string
                  format: email
                password:
                  type: string
                  minLength: 8
      responses:
        '200':
          description: Successful login
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AuthResponse'
        '401':
          $ref: '#/components/responses/Unauthorized'

  /tracks:
    get:
      summary: Get user's tracks
      security:
        - bearerAuth: []
      parameters:
        - name: page
          in: query
          schema:
            type: integer
            default: 1
        - name: limit
          in: query
          schema:
            type: integer
            default: 20
            maximum: 100
      responses:
        '200':
          description: List of tracks
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/TrackList'

    post:
      summary: Upload new track
      security:
        - bearerAuth: []
      requestBody:
        required: true
        content:
          multipart/form-data:
            schema:
              type: object
              required:
                - gpxFile
                - metadata
              properties:
                gpxFile:
                  type: string
                  format: binary
                metadata:
                  $ref: '#/components/schemas/TrackMetadata'
      responses:
        '201':
          description: Track created
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Track'

  /tracks/{trackId}:
    get:
      summary: Get track details
      security:
        - bearerAuth: []
      parameters:
        - name: trackId
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: Track details
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/TrackDetail'
        '404':
          $ref: '#/components/responses/NotFound'

  /pois/nearby:
    get:
      summary: Get POIs near location
      parameters:
        - name: lat
          in: query
          required: true
          schema:
            type: number
            format: double
            minimum: -90
            maximum: 90
        - name: lon
          in: query
          required: true
          schema:
            type: number
            format: double
            minimum: -180
            maximum: 180
        - name: radius
          in: query
          schema:
            type: integer
            default: 1000
            minimum: 10
            maximum: 50000
        - name: categories
          in: query
          style: form
          explode: false
          schema:
            type: array
            items:
              type: string
      responses:
        '200':
          description: List of nearby POIs
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PoiList'

  /pois/import:
    post:
      summary: Bulk import POIs
      security:
        - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/PoiImportRequest'
      responses:
        '202':
          description: Import accepted
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ImportJob'

components:
  schemas:
    AuthResponse:
      type: object
      properties:
        accessToken:
          type: string
        refreshToken:
          type: string
        expiresIn:
          type: integer
        user:
          $ref: '#/components/schemas/User'

    User:
      type: object
      properties:
        id:
          type: string
          format: uuid
        email:
          type: string
          format: email
        username:
          type: string
        createdAt:
          type: string
          format: date-time

    Track:
      type: object
      properties:
        id:
          type: string
          format: uuid
        name:
          type: string
        startTime:
          type: string
          format: date-time
        endTime:
          type: string
          format: date-time
        distance:
          type: number
        duration:
          type: integer
        pointCount:
          type: integer

    TrackDetail:
      allOf:
        - $ref: '#/components/schemas/Track'
        - type: object
          properties:
            points:
              type: array
              items:
                $ref: '#/components/schemas/TrackPoint'
            statistics:
              $ref: '#/components/schemas/TrackStatistics'

    TrackPoint:
      type: object
      properties:
        lat:
          type: number
        lon:
          type: number
        ele:
          type: number
        time:
          type: string
          format: date-time
        accuracy:
          type: number

    PointOfInterest:
      type: object
      properties:
        id:
          type: string
        name:
          type: string
        description:
          type: string
        lat:
          type: number
        lon:
          type: number
        category:
          type: string
        tags:
          type: object
          additionalProperties:
            type: string

  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT

  responses:
    Unauthorized:
      description: Authentication required
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/Error'

    NotFound:
      description: Resource not found
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/Error'

    Error:
      type: object
      properties:
        code:
          type: string
        message:
          type: string
        details:
          type: object
```

### 3.2 API Client Implementation

```kotlin
// Retrofit API interface
interface SurveyMeApi {
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): Response<AuthResponse>

    @GET("tracks")
    suspend fun getTracks(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Header("Authorization") token: String
    ): Response<TrackListResponse>

    @Multipart
    @POST("tracks")
    suspend fun uploadTrack(
        @Part file: MultipartBody.Part,
        @Part("metadata") metadata: RequestBody,
        @Header("Authorization") token: String
    ): Response<TrackResponse>

    @GET("tracks/{trackId}")
    suspend fun getTrackDetail(
        @Path("trackId") trackId: String,
        @Header("Authorization") token: String
    ): Response<TrackDetailResponse>

    @GET("pois/nearby")
    suspend fun getNearbyPois(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("radius") radius: Int = 1000,
        @Query("categories") categories: String? = null
    ): Response<PoiListResponse>

    @POST("pois/import")
    suspend fun importPois(
        @Body pois: PoiImportRequest,
        @Header("Authorization") token: String
    ): Response<ImportJobResponse>
}

// API client with error handling
class ApiClient(
    private val api: SurveyMeApi,
    private val authManager: AuthManager
) {
    suspend fun <T> safeApiCall(
        call: suspend () -> Response<T>
    ): Result<T> {
        return try {
            val response = call()
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(EmptyResponseException())
            } else {
                when (response.code()) {
                    401 -> {
                        // Try to refresh token
                        if (authManager.refreshToken()) {
                            // Retry the call
                            safeApiCall(call)
                        } else {
                            Result.failure(UnauthorizedException())
                        }
                    }
                    404 -> Result.failure(NotFoundException())
                    429 -> Result.failure(RateLimitException())
                    500 -> Result.failure(ServerErrorException())
                    else -> Result.failure(ApiException(response.code(), response.message()))
                }
            }
        } catch (e: IOException) {
            Result.failure(NetworkException(e))
        } catch (e: Exception) {
            Result.failure(UnknownException(e))
        }
    }

    suspend fun uploadTrack(track: Track, points: List<TrackPoint>): Result<TrackResponse> {
        val gpxContent = GpxExporter().exportTrack(track, points)
        val file = gpxContent.toRequestBody("application/gpx+xml".toMediaType())
        val filePart = MultipartBody.Part.createFormData(
            "gpxFile",
            "${track.name}.gpx",
            file
        )

        val metadata = TrackMetadata(
            name = track.name,
            description = track.description,
            activity = "survey",
            isPublic = false
        )
        val metadataBody = Json.encodeToString(metadata)
            .toRequestBody("application/json".toMediaType())

        return safeApiCall {
            api.uploadTrack(
                file = filePart,
                metadata = metadataBody,
                token = authManager.getAuthToken()
            )
        }
    }
}
```

## 4. Third-Party Service Integrations

### 4.1 OpenStreetMap Tile Servers

```kotlin
interface TileServerProvider {
    fun getTileUrl(x: Int, y: Int, zoom: Int): String
    fun getAttribution(): String
    fun getMaxZoom(): Int
    fun getMinZoom(): Int
    fun getTileSize(): Int
}

class OsmTileProvider : TileServerProvider {
    override fun getTileUrl(x: Int, y: Int, zoom: Int): String {
        val server = listOf("a", "b", "c").random()
        return "https://$server.tile.openstreetmap.org/$zoom/$x/$y.png"
    }

    override fun getAttribution() = "© OpenStreetMap contributors"
    override fun getMaxZoom() = 19
    override fun getMinZoom() = 0
    override fun getTileSize() = 256
}

class MapboxTileProvider(private val accessToken: String) : TileServerProvider {
    override fun getTileUrl(x: Int, y: Int, zoom: Int): String {
        return "https://api.mapbox.com/styles/v1/mapbox/streets-v11/tiles/" +
               "$zoom/$x/$y@2x?access_token=$accessToken"
    }

    override fun getAttribution() = "© Mapbox © OpenStreetMap"
    override fun getMaxZoom() = 22
    override fun getMinZoom() = 0
    override fun getTileSize() = 512
}
```

### 4.2 Nominatim Geocoding Service

```kotlin
interface GeocodingService {
    suspend fun geocode(address: String): Result<List<GeocodingResult>>
    suspend fun reverseGeocode(lat: Double, lon: Double): Result<ReverseGeocodingResult>
}

class NominatimService(
    private val httpClient: OkHttpClient
) : GeocodingService {
    private val baseUrl = "https://nominatim.openstreetmap.org"

    override suspend fun geocode(address: String): Result<List<GeocodingResult>> {
        val request = Request.Builder()
            .url("$baseUrl/search?q=${address.urlEncode()}&format=json&limit=10")
            .header("User-Agent", "SurveyMe/1.0")
            .build()

        return withContext(Dispatchers.IO) {
            try {
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val json = response.body?.string()
                    val results = Json.decodeFromString<List<NominatimResult>>(json ?: "[]")
                    Result.success(results.map { it.toGeocodingResult() })
                } else {
                    Result.failure(GeocodingException("Geocoding failed: ${response.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun reverseGeocode(lat: Double, lon: Double): Result<ReverseGeocodingResult> {
        val request = Request.Builder()
            .url("$baseUrl/reverse?lat=$lat&lon=$lon&format=json")
            .header("User-Agent", "SurveyMe/1.0")
            .build()

        return withContext(Dispatchers.IO) {
            try {
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val json = response.body?.string()
                    val result = Json.decodeFromString<NominatimReverseResult>(json ?: "{}")
                    Result.success(result.toReverseGeocodingResult())
                } else {
                    Result.failure(GeocodingException("Reverse geocoding failed: ${response.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

@Serializable
data class NominatimResult(
    val place_id: Long,
    val licence: String,
    val osm_type: String,
    val osm_id: Long,
    val lat: String,
    val lon: String,
    val display_name: String,
    val address: Map<String, String>? = null,
    val boundingbox: List<String>
)

data class GeocodingResult(
    val displayName: String,
    val latitude: Double,
    val longitude: Double,
    val boundingBox: BoundingBox?,
    val osmId: Long?,
    val address: Address?
)

data class Address(
    val houseNumber: String?,
    val street: String?,
    val city: String?,
    val state: String?,
    val country: String?,
    val postalCode: String?
)
```

### 4.3 Overpass API Integration

```kotlin
interface OverpassApi {
    suspend fun queryPois(
        bounds: BoundingBox,
        tags: Map<String, String>
    ): Result<List<OsmElement>>
}

class OverpassApiClient(
    private val httpClient: OkHttpClient
) : OverpassApi {
    private val baseUrl = "https://overpass-api.de/api/interpreter"

    override suspend fun queryPois(
        bounds: BoundingBox,
        tags: Map<String, String>
    ): Result<List<OsmElement>> {
        val query = buildOverpassQuery(bounds, tags)

        val request = Request.Builder()
            .url(baseUrl)
            .post(query.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
            .build()

        return withContext(Dispatchers.IO) {
            try {
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val json = response.body?.string()
                    val result = Json.decodeFromString<OverpassResponse>(json ?: "{}")
                    Result.success(result.elements)
                } else {
                    Result.failure(OverpassException("Query failed: ${response.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun buildOverpassQuery(bounds: BoundingBox, tags: Map<String, String>): String {
        val bbox = "${bounds.south},${bounds.west},${bounds.north},${bounds.east}"
        val tagFilters = tags.map { (k, v) -> "[\"$k\"=\"$v\"]" }.joinToString("")

        return """
            [out:json][timeout:25];
            (
                node$tagFilters($bbox);
                way$tagFilters($bbox);
                relation$tagFilters($bbox);
            );
            out body;
            >;
            out skel qt;
        """.trimIndent()
    }
}

@Serializable
data class OverpassResponse(
    val version: Double,
    val generator: String,
    val elements: List<OsmElement>
)

@Serializable
data class OsmElement(
    val type: String,
    val id: Long,
    val lat: Double? = null,
    val lon: Double? = null,
    val tags: Map<String, String>? = null
)
```

## 5. Platform Service APIs

### 5.1 Android Location Services

```kotlin
class AndroidLocationService(
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient
) : LocationService {
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.forEach { location ->
                _locationUpdates.tryEmit(location.toDomainModel())
            }
        }

        override fun onLocationAvailability(availability: LocationAvailability) {
            _gpsStatus.tryEmit(
                GpsStatus(
                    isEnabled = availability.isLocationAvailable,
                    satellitesInView = 0,
                    satellitesUsed = 0,
                    accuracy = null,
                    lastFixTime = null,
                    signalStrength = if (availability.isLocationAvailable) {
                        SignalStrength.GOOD
                    } else {
                        SignalStrength.NO_SIGNAL
                    }
                )
            )
        }
    }

    private val _locationUpdates = MutableSharedFlow<Location>()
    private val _trackingState = MutableStateFlow<TrackingState>(TrackingState.Idle)
    private val _gpsStatus = MutableSharedFlow<GpsStatus>()

    override suspend fun startTracking(config: LocationConfig): Result<TrackingSession> {
        return try {
            val locationRequest = buildLocationRequest(config)

            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return Result.failure(PermissionDeniedException("Location permission not granted"))
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )

            val session = TrackingSession(
                id = UUID.randomUUID().toString(),
                startTime = System.currentTimeMillis()
            )

            _trackingState.emit(
                TrackingState.Active(
                    sessionId = session.id,
                    startTime = session.startTime,
                    pointCount = 0,
                    distance = 0.0
                )
            )

            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildLocationRequest(config: LocationConfig): LocationRequest {
        return LocationRequest.create().apply {
            interval = config.interval
            fastestInterval = config.fastestInterval
            smallestDisplacement = config.minDisplacement

            priority = when (config.accuracy) {
                LocationAccuracy.HIGH -> LocationRequest.PRIORITY_HIGH_ACCURACY
                LocationAccuracy.BALANCED -> LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
                LocationAccuracy.LOW -> LocationRequest.PRIORITY_LOW_POWER
                LocationAccuracy.PASSIVE -> LocationRequest.PRIORITY_NO_POWER
            }
        }
    }

    override fun observeLocationUpdates(): Flow<Location> = _locationUpdates.asSharedFlow()
    override fun observeTrackingState(): Flow<TrackingState> = _trackingState.asStateFlow()
    override fun getGpsStatus(): Flow<GpsStatus> = _gpsStatus.asSharedFlow()
}
```

### 5.2 Google Play Services Integration

```kotlin
class PlayServicesManager(
    private val context: Context
) {
    private val googleApiAvailability = GoogleApiAvailability.getInstance()

    fun checkPlayServicesAvailability(): PlayServicesStatus {
        return when (val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)) {
            ConnectionResult.SUCCESS -> PlayServicesStatus.Available
            ConnectionResult.SERVICE_MISSING -> PlayServicesStatus.Missing
            ConnectionResult.SERVICE_UPDATING -> PlayServicesStatus.Updating
            ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> PlayServicesStatus.UpdateRequired
            ConnectionResult.SERVICE_DISABLED -> PlayServicesStatus.Disabled
            ConnectionResult.SERVICE_INVALID -> PlayServicesStatus.Invalid
            else -> PlayServicesStatus.Unknown(resultCode)
        }
    }

    fun showErrorDialogIfNecessary(
        activity: Activity,
        errorCode: Int,
        requestCode: Int
    ): Boolean {
        return if (googleApiAvailability.isUserResolvableError(errorCode)) {
            googleApiAvailability.getErrorDialog(
                activity,
                errorCode,
                requestCode
            )?.show()
            true
        } else {
            false
        }
    }

    suspend fun ensurePlayServicesAvailable(): Result<Unit> {
        return when (val status = checkPlayServicesAvailability()) {
            PlayServicesStatus.Available -> Result.success(Unit)
            is PlayServicesStatus.UpdateRequired -> {
                Result.failure(PlayServicesUpdateRequiredException())
            }
            else -> Result.failure(PlayServicesNotAvailableException(status))
        }
    }
}

sealed class PlayServicesStatus {
    object Available : PlayServicesStatus()
    object Missing : PlayServicesStatus()
    object Updating : PlayServicesStatus()
    object UpdateRequired : PlayServicesStatus()
    object Disabled : PlayServicesStatus()
    object Invalid : PlayServicesStatus()
    data class Unknown(val code: Int) : PlayServicesStatus()
}
```

## 6. Error Handling and Status Codes

### 6.1 Error Response Format

```kotlin
@Serializable
data class ApiError(
    val code: String,
    val message: String,
    val details: Map<String, Any>? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ErrorCode(val value: String, val httpStatus: Int) {
    // Client errors (4xx)
    BAD_REQUEST("BAD_REQUEST", 400),
    UNAUTHORIZED("UNAUTHORIZED", 401),
    FORBIDDEN("FORBIDDEN", 403),
    NOT_FOUND("NOT_FOUND", 404),
    CONFLICT("CONFLICT", 409),
    VALIDATION_ERROR("VALIDATION_ERROR", 422),
    RATE_LIMIT_EXCEEDED("RATE_LIMIT_EXCEEDED", 429),

    // Server errors (5xx)
    INTERNAL_ERROR("INTERNAL_ERROR", 500),
    NOT_IMPLEMENTED("NOT_IMPLEMENTED", 501),
    SERVICE_UNAVAILABLE("SERVICE_UNAVAILABLE", 503),

    // Business logic errors
    TRACK_ALREADY_EXISTS("TRACK_ALREADY_EXISTS", 409),
    POI_DUPLICATE("POI_DUPLICATE", 409),
    INVALID_GPX_FORMAT("INVALID_GPX_FORMAT", 422),
    QUOTA_EXCEEDED("QUOTA_EXCEEDED", 403),
    INVALID_COORDINATES("INVALID_COORDINATES", 422)
}
```

## 7. Rate Limiting and Throttling

```kotlin
class RateLimiter(
    private val maxRequests: Int = 100,
    private val windowMs: Long = 60000 // 1 minute
) {
    private val requests = mutableListOf<Long>()

    @Synchronized
    fun tryAcquire(): Boolean {
        val now = System.currentTimeMillis()

        // Remove old requests outside window
        requests.removeAll { it < now - windowMs }

        return if (requests.size < maxRequests) {
            requests.add(now)
            true
        } else {
            false
        }
    }

    fun getResetTime(): Long {
        return if (requests.isEmpty()) {
            0
        } else {
            requests.first() + windowMs
        }
    }
}

// Retrofit interceptor for rate limiting
class RateLimitInterceptor(
    private val rateLimiter: RateLimiter
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        if (!rateLimiter.tryAcquire()) {
            throw RateLimitException(
                "Rate limit exceeded",
                resetTime = rateLimiter.getResetTime()
            )
        }

        val response = chain.proceed(chain.request())

        // Check for server rate limit headers
        response.header("X-RateLimit-Remaining")?.toIntOrNull()?.let { remaining ->
            if (remaining == 0) {
                val resetTime = response.header("X-RateLimit-Reset")?.toLongOrNull() ?: 0
                throw RateLimitException(
                    "Server rate limit exceeded",
                    resetTime = resetTime
                )
            }
        }

        return response
    }
}
```

## 8. WebSocket Communication

```kotlin
interface RealtimeService {
    fun connect(url: String)
    fun disconnect()
    fun observeConnectionState(): Flow<ConnectionState>
    fun observeMessages(): Flow<RealtimeMessage>
    fun send(message: RealtimeMessage)
}

class WebSocketRealtimeService(
    private val okHttpClient: OkHttpClient
) : RealtimeService {
    private var webSocket: WebSocket? = null
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    private val _messages = MutableSharedFlow<RealtimeMessage>()

    private val webSocketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            _connectionState.tryEmit(ConnectionState.Connected)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            try {
                val message = Json.decodeFromString<RealtimeMessage>(text)
                _messages.tryEmit(message)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse message", e)
            }
        }

        override fun onFailure(
            webSocket: WebSocket,
            t: Throwable,
            response: Response?
        ) {
            _connectionState.tryEmit(ConnectionState.Error(t))
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            _connectionState.tryEmit(ConnectionState.Disconnected)
        }
    }

    override fun connect(url: String) {
        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = okHttpClient.newWebSocket(request, webSocketListener)
        _connectionState.tryEmit(ConnectionState.Connecting)
    }

    override fun send(message: RealtimeMessage) {
        val json = Json.encodeToString(message)
        webSocket?.send(json)
    }
}

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    data class Error(val throwable: Throwable) : ConnectionState()
}

@Serializable
sealed class RealtimeMessage {
    @Serializable
    data class LocationUpdate(
        val userId: String,
        val location: Location,
        val timestamp: Long
    ) : RealtimeMessage()

    @Serializable
    data class PoiDiscovered(
        val poi: PointOfInterest,
        val discoveredBy: String,
        val timestamp: Long
    ) : RealtimeMessage()
}
```

---

*This API and Service Design documentation defines all interfaces, protocols, and integration points for the Survey Me application. It should be kept synchronized with implementation changes.*