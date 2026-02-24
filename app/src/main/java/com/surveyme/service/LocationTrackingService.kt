package com.surveyme.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.surveyme.R
import com.surveyme.core.ProximityDetector
import com.surveyme.data.PoiManager
import com.surveyme.data.model.Poi
import com.surveyme.presentation.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import com.surveyme.data.model.ActiveTrack
import com.surveyme.data.model.TrackPoint

class LocationTrackingService : Service() {

    companion object {
        const val CHANNEL_ID = "location_tracking_channel"
        const val NOTIFICATION_ID = 1001
        const val POI_NOTIFICATION_CHANNEL_ID = "poi_proximity_channel"
        const val ACTION_START_TRACKING = "com.surveyme.START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.surveyme.STOP_TRACKING"

        private const val LOCATION_INTERVAL = 5000L // 5 seconds
        private const val FASTEST_LOCATION_INTERVAL = 2000L // 2 seconds

        private val _isTrackingFlow = MutableStateFlow(false)
        val isTrackingFlow: StateFlow<Boolean> = _isTrackingFlow.asStateFlow()

        private val _activeTrackFlow = MutableStateFlow(ActiveTrack())
        val activeTrackFlow: StateFlow<ActiveTrack> = _activeTrackFlow.asStateFlow()
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var notificationManager: NotificationManager
    private lateinit var proximityDetector: ProximityDetector

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val poisCache = mutableListOf<Poi>()
    private var isTracking = false

    override fun onCreate() {
        super.onCreate()
        Timber.d("LocationTrackingService onCreate")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        proximityDetector = ProximityDetector()

        createNotificationChannels()
        setupLocationCallback()
        loadPois()
    }

    private fun createNotificationChannels() {
        // Main tracking channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val trackingChannel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when the app is tracking your location"
                setShowBadge(false)
            }

            // POI proximity channel
            val poiChannel = NotificationChannel(
                POI_NOTIFICATION_CHANNEL_ID,
                "POI Proximity Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when you're near a point of interest"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
            }

            notificationManager.createNotificationChannel(trackingChannel)
            notificationManager.createNotificationChannel(poiChannel)
        }
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    handleLocationUpdate(location)
                }
            }
        }
    }

    private fun handleLocationUpdate(location: Location) {
        Timber.d("Location update: ${location.latitude}, ${location.longitude}")
        // Create a snapshot of the POIs to avoid ConcurrentModificationException
        // if the list is updated on a background thread while iterating
        val currentPois = synchronized(poisCache) {
            poisCache.toList()
        }

        Timber.d("POI cache size: ${currentPois.size}")

        // Check proximity to POIs
        if (currentPois.isNotEmpty()) {
            Timber.d("Checking proximity to ${currentPois.size} POIs...")

            // Log distances to all POIs for debugging
            currentPois.forEach { poi ->
                val distance = poi.distanceTo(location.latitude, location.longitude)
                Timber.d("Distance to ${poi.name}: ${distance}m (radius: ${poi.notificationRadius}m)")
            }

            proximityDetector.checkProximity(
                location.latitude,
                location.longitude,
                currentPois
            ) { poi, distance ->
                Timber.d("Proximity notification triggered for ${poi.name} at ${distance}m")
                showPoiNotification(poi, distance)
                markPoiAsVisited(poi.id)
            }
        } else {
            Timber.w("No POIs in cache for proximity detection")
        }

        // --- Track Recording Logic ---
        val currentTrack = _activeTrackFlow.value
        val lastPoint = currentTrack.points.lastOrNull()

        var incrementalDistance = 0f
        if (lastPoint != null) {
            val results = FloatArray(1)
            Location.distanceBetween(
                lastPoint.latitude, lastPoint.longitude,
                location.latitude, location.longitude,
                results
            )
            incrementalDistance = results[0]
        }

        val totalDistance = currentTrack.totalDistance + incrementalDistance
        val startTime = if (currentTrack.startTime == 0L) System.currentTimeMillis() else currentTrack.startTime
        val duration = System.currentTimeMillis() - startTime

        val newPoint = TrackPoint(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            time = location.time,
            distanceFromStart = totalDistance
        )

        _activeTrackFlow.value = currentTrack.copy(
            points = currentTrack.points + newPoint,
            totalDistance = totalDistance,
            startTime = startTime,
            duration = duration
        )
        // ------------------------------

        // Update the ongoing notification with current location
        updateNotification(location)
    }

    private fun loadPois() {
        serviceScope.launch {
            try {
                val repository = PoiManager.getRepository(this@LocationTrackingService)
                repository.getAllActivePois().collectLatest { pois ->
                    synchronized(poisCache) {
                        poisCache.clear()
                        poisCache.addAll(pois)
                    }
                    Timber.d("Loaded ${pois.size} POIs for proximity detection")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load POIs")
            }
        }
    }

    private fun markPoiAsVisited(poiId: String) {
        serviceScope.launch {
            try {
                val repository = PoiManager.getRepository(this@LocationTrackingService)
                repository.markAsVisited(poiId)
                repository.updateLastNotificationTime(poiId)
                proximityDetector.markPoiAsVisited(poiId)
            } catch (e: Exception) {
                Timber.e(e, "Failed to mark POI as visited")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TRACKING -> startLocationTracking()
            ACTION_STOP_TRACKING -> stopLocationTracking()
        }
        return START_STICKY
    }

    private fun startLocationTracking() {
        if (isTracking) return

        Timber.d("Starting location tracking")
        isTracking = true
        _isTrackingFlow.value = true
        _activeTrackFlow.value = ActiveTrack() // Reset track

        // Start as foreground service
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Request location updates
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val locationRequest = LocationRequest.create().apply {
                interval = LOCATION_INTERVAL
                fastestInterval = FASTEST_LOCATION_INTERVAL
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    private fun stopLocationTracking() {
        if (!isTracking) return

        Timber.d("Stopping location tracking")
        isTracking = false
        _isTrackingFlow.value = false

        fusedLocationClient.removeLocationUpdates(locationCallback)
        stopForeground(true)
        stopSelf()
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, LocationTrackingService::class.java).apply {
                action = ACTION_STOP_TRACKING
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Survey Me - Tracking")
            .setContentText("Recording your survey route")
            .setSmallIcon(R.drawable.ic_home)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_home, "Stop", stopIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(location: Location) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Survey Me - Tracking")
            .setContentText("Location: ${location.latitude.format(5)}, ${location.longitude.format(5)}")
            .setSmallIcon(R.drawable.ic_home)
            .setOngoing(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showPoiNotification(poi: Poi, distance: Float) {
        val pendingIntent = PendingIntent.getActivity(
            this,
            poi.id.hashCode(),
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, POI_NOTIFICATION_CHANNEL_ID)
            .setContentTitle("POI Nearby: ${poi.name}")
            .setContentText("${poi.description} - ${distance.toInt()}m away")
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .build()

        notificationManager.notify(poi.id.hashCode() + 2000, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("LocationTrackingService onDestroy")
        serviceScope.cancel()
        if (isTracking) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun Double.format(digits: Int) = "%.${digits}f".format(this)
}