package com.surveyme.presentation.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.surveyme.R
import com.surveyme.core.PermissionManager
import com.surveyme.databinding.FragmentHomeBinding
import com.surveyme.presentation.base.BaseFragment
import com.surveyme.service.LocationTrackingService
import timber.log.Timber

class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    private lateinit var permissionManager: PermissionManager
    private var isTracking = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Notification permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notification permission denied. You won't receive POI alerts.", Toast.LENGTH_LONG).show()
        }
    }

    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("HomeFragment onViewCreated")

        permissionManager = PermissionManager(requireContext())

        setupTrackingControls()
        checkAndDisplayCrash()
        updateTrackingUI()
        checkNotificationPermission()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Request notification permission
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun checkAndDisplayCrash() {
        val prefs = requireContext().getSharedPreferences("crash_prefs", Context.MODE_PRIVATE)
        val lastCrash = prefs.getString("last_crash", null)

        if (lastCrash != null) {
            Timber.e("Displaying crash: $lastCrash")

            binding.cardCrash.visibility = View.VISIBLE
            binding.textCrashInfo.text = lastCrash

            binding.buttonClearCrash.setOnClickListener {
                prefs.edit().remove("last_crash").apply()
                binding.cardCrash.visibility = View.GONE
                Timber.d("Crash log cleared")
            }
        } else {
            binding.cardCrash.visibility = View.GONE
        }
    }

    private fun setupTrackingControls() {
        binding.buttonStartTracking.setOnClickListener {
            if (!permissionManager.hasLocationPermission()) {
                Toast.makeText(context, "Location permission required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isTracking) {
                startLocationTracking()
            } else {
                stopLocationTracking()
            }
        }
    }

    private fun startLocationTracking() {
        val intent = Intent(requireContext(), LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_START_TRACKING
        }
        ContextCompat.startForegroundService(requireContext(), intent)
        isTracking = true
        updateTrackingUI()
        Toast.makeText(context, "Tracking started", Toast.LENGTH_SHORT).show()
    }

    private fun stopLocationTracking() {
        val intent = Intent(requireContext(), LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_STOP_TRACKING
        }
        requireContext().startService(intent)
        isTracking = false
        updateTrackingUI()
        Toast.makeText(context, "Tracking stopped", Toast.LENGTH_SHORT).show()
    }

    private fun updateTrackingUI() {
        if (isTracking) {
            binding.buttonStartTracking.text = getString(R.string.stop_tracking)
            binding.textTrackingStatus.text = getString(R.string.tracking_active)
            binding.textTrackingStatus.setTextColor(
                ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
            )
        } else {
            binding.buttonStartTracking.text = getString(R.string.start_tracking)
            binding.textTrackingStatus.text = getString(R.string.tracking_inactive)
            binding.textTrackingStatus.setTextColor(
                ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
            )
        }
    }
}