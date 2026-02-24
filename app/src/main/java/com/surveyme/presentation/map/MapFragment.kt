package com.surveyme.presentation.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.snackbar.Snackbar
import com.surveyme.R
import com.surveyme.core.Constants
import com.surveyme.core.PermissionManager
import com.surveyme.core.PreferencesManager
import com.surveyme.databinding.FragmentMapBinding
import com.surveyme.presentation.base.BaseFragment
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import timber.log.Timber
import java.io.File
import android.content.Intent
import android.graphics.Color
import androidx.lifecycle.lifecycleScope
import com.surveyme.data.PoiManager
import com.surveyme.data.model.Poi
import com.surveyme.data.model.PoiCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.take
import com.surveyme.service.LocationTrackingService
import com.surveyme.data.model.ActiveTrack
import kotlinx.coroutines.Job
import java.util.Locale

class MapFragment : BaseFragment<FragmentMapBinding>(), MapEventsReceiver {

    private var preferencesManager: PreferencesManager? = null
    private var permissionManager: PermissionManager? = null
    private var myLocationOverlay: MyLocationNewOverlay? = null
    private var compassOverlay: CompassOverlay? = null
    private var rotationGestureOverlay: RotationGestureOverlay? = null
    private val poiMarkers = mutableListOf<Marker>()
    private var activeTrackPolyline: Polyline? = null
    private val trackMarkers = mutableListOf<Marker>()
    
    private var trackingJob: Job? = null
    private var activeTrackJob: Job? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Timber.d("Location permissions granted")
            onLocationPermissionGranted()
        } else {
            Timber.d("Location permissions denied")
            activity?.let { activity ->
                permissionManager?.let { pm ->
                    if (pm.shouldShowLocationPermissionRationale(activity)) {
                        showPermissionSnackbar()
                    } else {
                        pm.showPermissionDeniedDialog(activity)
                    }
                }
            }
        }
    }

    private val backgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Timber.d("Background location permission granted")
            view?.let { view ->
                Snackbar.make(view, "Background tracking enabled", Snackbar.LENGTH_SHORT).show()
            }
        } else {
            Timber.d("Background location permission denied")
            view?.let { view ->
                Snackbar.make(view, "Background tracking not available", Snackbar.LENGTH_LONG)
                    .setAction("Settings") {
                        activity?.let { activity ->
                            permissionManager?.showPermissionDeniedDialog(activity)
                        }
                    }.show()
            }
        }
    }

    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentMapBinding {
        return FragmentMapBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configure OSMDroid
        Configuration.getInstance().apply {
            userAgentValue = requireContext().packageName
            // Set cache location
            osmdroidBasePath = File(requireContext().filesDir, "osmdroid")
            osmdroidTileCache = File(requireContext().cacheDir, "osmdroid_tiles")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("MapFragment onViewCreated")

        preferencesManager = PreferencesManager(requireContext())
        permissionManager = PermissionManager(requireContext())

        setupMap()
        setupClickListeners()

        // Check and request location permissions
        checkLocationPermissions()

        // Restore last position if available
        restoreMapPosition()

        // Load and display POIs
        loadPois()
        
        // Observe Tracking State
        observeTrackingState()
    }

    private fun checkLocationPermissions() {
        val pm = permissionManager ?: return
        activity?.let { activity ->
            when {
                pm.hasLocationPermission() -> {
                    Timber.d("Location permissions already granted")
                    onLocationPermissionGranted()
                }
                pm.shouldShowLocationPermissionRationale(activity) -> {
                    pm.showLocationPermissionRationale(activity) {
                        requestLocationPermissions()
                    }
                }
                else -> {
                    requestLocationPermissions()
                }
            }
        } ?: Timber.w("Activity not attached, cannot check permissions")
    }

    private fun requestLocationPermissions() {
        permissionManager?.requestLocationPermissions(this, locationPermissionLauncher)
    }

    private fun onLocationPermissionGranted() {
        setupMapOverlays()
        checkLocationServices()

        // Request background location permission if needed
        permissionManager?.let { pm ->
            if (!pm.hasBackgroundLocationPermission()) {
                view?.let { view ->
                    Snackbar.make(view, "Enable background location for continuous tracking", Snackbar.LENGTH_LONG)
                        .setAction("Enable") {
                            requestBackgroundLocationPermission()
                        }.show()
                }
            }
        }
    }

    private fun requestBackgroundLocationPermission() {
        val pm = permissionManager ?: return
        activity?.let { activity ->
            if (pm.shouldShowBackgroundLocationRationale(activity)) {
                pm.showBackgroundLocationRationale(activity) {
                    pm.requestBackgroundLocationPermission(this, backgroundLocationLauncher)
                }
            } else {
                pm.requestBackgroundLocationPermission(this, backgroundLocationLauncher)
            }
        }
    }

    private fun checkLocationServices() {
        val pm = permissionManager ?: return
        activity?.let { activity ->
            if (!pm.isLocationServiceEnabled()) {
                pm.showLocationServiceDialog(activity)
            }
        }
    }

    private fun showPermissionSnackbar() {
        view?.let { view ->
            Snackbar.make(view, "Location permission required for map features", Snackbar.LENGTH_LONG)
                .setAction("Grant") {
                    requestLocationPermissions()
                }.show()
        }
    }

    private fun setupMap() {
        binding.mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

            // Set default position (center of the world)
            controller.setZoom(Constants.DEFAULT_MAP_ZOOM)
            val startPoint = GeoPoint(Constants.DEFAULT_MAP_CENTER_LAT, Constants.DEFAULT_MAP_CENTER_LON)
            controller.setCenter(startPoint)

            Timber.d("Map initialized at: $startPoint, zoom: ${Constants.DEFAULT_MAP_ZOOM}")
        }
    }

    private fun setupMapOverlays() {
        val pm = permissionManager ?: return
        if (!pm.hasLocationPermission()) {
            Timber.w("Cannot setup overlays without location permission")
            return
        }

        // Add map click events
        val mapEventsOverlay = MapEventsOverlay(this)
        binding.mapView.overlays.add(0, mapEventsOverlay)

        // Add my location overlay
        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), binding.mapView).apply {
            enableMyLocation()
            enableFollowLocation()
        }
        binding.mapView.overlays.add(myLocationOverlay)

        // Add compass overlay
        compassOverlay = CompassOverlay(requireContext(), InternalCompassOrientationProvider(requireContext()), binding.mapView).apply {
            enableCompass()
        }
        binding.mapView.overlays.add(compassOverlay)

        // Add rotation gesture overlay
        rotationGestureOverlay = RotationGestureOverlay(binding.mapView).apply {
            isEnabled = true
        }
        binding.mapView.overlays.add(rotationGestureOverlay)

        Timber.d("Map overlays configured with location services")
    }

    private fun setupClickListeners() {
        binding.fabMyLocation.setOnClickListener {
            myLocationOverlay?.myLocation?.let { location ->
                binding.mapView.controller.animateTo(location)
                binding.mapView.controller.setZoom(18.0)
                Timber.d("Centered on location: $location")
            } ?: run {
                Toast.makeText(requireContext(), "Location not available", Toast.LENGTH_SHORT).show()
            }
        }

        binding.fabZoomIn.setOnClickListener {
            binding.mapView.controller.zoomIn()
            Timber.d("Zoomed in to: ${binding.mapView.zoomLevelDouble}")
        }

        binding.fabZoomOut.setOnClickListener {
            binding.mapView.controller.zoomOut()
            Timber.d("Zoomed out to: ${binding.mapView.zoomLevelDouble}")
        }
    }

    private fun observeTrackingState() {
        trackingJob = viewLifecycleOwner.lifecycleScope.launch {
            LocationTrackingService.isTrackingFlow.collectLatest { isTracking ->
                if (!isTracking) {
                    clearActiveTrack()
                }
            }
        }

        activeTrackJob = viewLifecycleOwner.lifecycleScope.launch {
            LocationTrackingService.activeTrackFlow.collectLatest { track ->
                updateActiveTrackOnMap(track)
            }
        }
    }

    private fun updateActiveTrackOnMap(track: ActiveTrack) {
        if (track.points.isEmpty()) return
        
        // Only draw if drawing setting is enabled
        if (preferencesManager?.isDrawTrackEnabled != true) {
            clearActiveTrack()
            return
        }

        // Draw Polyline
        if (activeTrackPolyline == null) {
            activeTrackPolyline = Polyline(binding.mapView).apply {
                outlinePaint.color = Color.BLUE
                outlinePaint.strokeWidth = 10f
            }
            binding.mapView.overlays.add(activeTrackPolyline)
        }

        val geoPoints = track.points.map { GeoPoint(it.latitude, it.longitude) }
        activeTrackPolyline?.setPoints(geoPoints)

        // Draw basic markers every 20 points
        if (track.points.size % 20 == 0 && track.points.isNotEmpty()) {
            val point = track.points.last()
            val marker = Marker(binding.mapView).apply {
                position = GeoPoint(point.latitude, point.longitude)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = resources.getDrawable(android.R.drawable.presence_online, null)
            }
            trackMarkers.add(marker)
            binding.mapView.overlays.add(marker)
        }

        binding.mapView.invalidate()
    }

    private fun clearActiveTrack() {
        activeTrackPolyline?.let {
            binding.mapView.overlays.remove(it)
        }
        activeTrackPolyline = null

        trackMarkers.forEach {
            binding.mapView.overlays.remove(it)
        }
        trackMarkers.clear()

        binding.mapView.invalidate()
    }

    private fun restoreMapPosition() {
        preferencesManager?.getLastMapPosition()?.let { (lat, lon, zoom) ->
            val position = GeoPoint(lat, lon)
            binding.mapView.controller.setCenter(position)
            binding.mapView.controller.setZoom(zoom.toDouble())
            Timber.d("Restored map position: $position, zoom: $zoom")
        }
    }

    private fun saveMapPosition() {
        try {
            val center = binding.mapView.mapCenter as? GeoPoint ?: return
            val zoom = binding.mapView.zoomLevelDouble.toFloat()
            preferencesManager?.saveLastMapPosition(center.latitude, center.longitude, zoom)
            Timber.d("Saved map position: ${center.latitude}, ${center.longitude}, zoom: $zoom")
        } catch (e: Exception) {
            Timber.e(e, "Failed to save map position")
        }
    }

    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
        p?.let {
            val message = getString(R.string.map_click, it.latitude, it.longitude)
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            Timber.d("Map tapped at: ${it.latitude}, ${it.longitude}")
        }
        return true
    }

    override fun longPressHelper(p: GeoPoint?): Boolean {
        // Handle long press if needed
        return false
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
        myLocationOverlay?.enableMyLocation()
        compassOverlay?.enableCompass()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
        myLocationOverlay?.disableMyLocation()
        compassOverlay?.disableCompass()
        saveMapPosition()
    }

    private fun loadPois() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = PoiManager.getRepository(requireContext())

                // Check if POIs exist, if not add sample POIs for testing
                repository.getAllActivePois().take(1).collect { pois ->
                    if (pois.isEmpty()) {
                        Timber.d("No POIs found, adding sample POIs")
                        repository.addSamplePois()
                    }
                }

                // Now load and display POIs
                repository.getAllActivePois().collectLatest { pois ->
                    Timber.d("Loading ${pois.size} POIs onto map")
                    withContext(Dispatchers.Main) {
                        displayPoisOnMap(pois)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load POIs")
            }
        }
    }

    private fun displayPoisOnMap(pois: List<Poi>) {
        // Clear existing POI markers
        poiMarkers.forEach { marker ->
            binding.mapView.overlays.remove(marker)
        }
        poiMarkers.clear()

        // Add new POI markers
        pois.forEach { poi ->
            val marker = Marker(binding.mapView).apply {
                position = GeoPoint(poi.latitude, poi.longitude)
                title = poi.name
                snippet = poi.description
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                // Set icon based on category
                icon = when (poi.category) {
                    PoiCategory.TOURIST_ATTRACTION -> resources.getDrawable(android.R.drawable.star_on, null)
                    PoiCategory.RESTAURANT -> resources.getDrawable(android.R.drawable.ic_menu_myplaces, null)
                    PoiCategory.SHOP -> resources.getDrawable(android.R.drawable.ic_menu_manage, null)
                    PoiCategory.PUBLIC_TRANSPORT -> resources.getDrawable(android.R.drawable.ic_menu_directions, null)
                    PoiCategory.AMENITY -> resources.getDrawable(android.R.drawable.ic_menu_recent_history, null)
                    PoiCategory.HISTORIC -> resources.getDrawable(android.R.drawable.ic_menu_gallery, null)
                    PoiCategory.NATURAL -> resources.getDrawable(android.R.drawable.ic_menu_mapmode, null)
                    PoiCategory.INFRASTRUCTURE -> resources.getDrawable(android.R.drawable.ic_menu_report_image, null)
                    PoiCategory.UNKNOWN -> resources.getDrawable(android.R.drawable.ic_menu_info_details, null)
                }

                // Set click listener for info window
                setOnMarkerClickListener { marker, mapView ->
                    if (!marker.isInfoWindowOpen) {
                        marker.showInfoWindow()
                    } else {
                        marker.closeInfoWindow()
                    }
                    mapView.controller.animateTo(marker.position)
                    true
                }
            }

            binding.mapView.overlays.add(marker)
            poiMarkers.add(marker)
        }

        // Refresh the map
        binding.mapView.invalidate()

        Timber.d("Displayed ${pois.size} POIs on map")
    }

    override fun onDestroyView() {
        trackingJob?.cancel()
        activeTrackJob?.cancel()
        binding.mapView.onDetach()
        super.onDestroyView()
    }
}