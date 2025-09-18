package com.surveyme.core

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import timber.log.Timber

class PermissionManager(private val context: Context) {

    companion object {
        const val REQUEST_CODE_LOCATION = 100
        const val REQUEST_CODE_BACKGROUND_LOCATION = 101

        val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val BACKGROUND_LOCATION_PERMISSION = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        } else {
            null
        }
    }

    fun hasLocationPermission(): Boolean {
        return LOCATION_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }.also { hasPermission ->
            Timber.d("Location permission status: $hasPermission")
        }
    }

    fun hasBackgroundLocationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not needed for Android 9 and below
        }.also { hasPermission ->
            Timber.d("Background location permission status: $hasPermission")
        }
    }

    fun shouldShowLocationPermissionRationale(activity: Activity): Boolean {
        return LOCATION_PERMISSIONS.any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
    }

    fun shouldShowBackgroundLocationRationale(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        } else {
            false
        }
    }

    fun isLocationServiceEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    fun showLocationPermissionRationale(
        activity: Activity,
        onPositive: () -> Unit
    ) {
        MaterialAlertDialogBuilder(activity)
            .setTitle("Location Permission Required")
            .setMessage("Survey Me needs location permission to:\n\n" +
                    "• Show your position on the map\n" +
                    "• Record your walking tracks\n" +
                    "• Notify you about nearby points of interest\n\n" +
                    "Your location data is only stored locally on your device.")
            .setPositiveButton("Grant Permission") { _, _ ->
                onPositive()
            }
            .setNegativeButton("Not Now") { dialog, _ ->
                dialog.dismiss()
                Timber.d("User declined location permission rationale")
            }
            .show()
    }

    fun showBackgroundLocationRationale(
        activity: Activity,
        onPositive: () -> Unit
    ) {
        MaterialAlertDialogBuilder(activity)
            .setTitle("Background Location Required")
            .setMessage("To continue tracking your path when the app is in the background, " +
                    "Survey Me needs 'Allow all the time' location permission.\n\n" +
                    "This ensures complete track recording during your surveys.")
            .setPositiveButton("Grant Permission") { _, _ ->
                onPositive()
            }
            .setNegativeButton("Skip") { dialog, _ ->
                dialog.dismiss()
                Timber.d("User declined background location permission")
            }
            .show()
    }

    fun showLocationServiceDialog(activity: Activity) {
        MaterialAlertDialogBuilder(activity)
            .setTitle("Enable Location Services")
            .setMessage("Location services are disabled. Please enable GPS to use map features.")
            .setPositiveButton("Settings") { _, _ ->
                activity.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    fun showPermissionDeniedDialog(activity: Activity) {
        MaterialAlertDialogBuilder(activity)
            .setTitle("Permission Denied")
            .setMessage("Location permission is required for core app functionality. " +
                    "You can grant permission in app settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                openAppSettings(activity)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun openAppSettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
        }
        activity.startActivity(intent)
    }

    fun requestLocationPermissions(fragment: Fragment, launcher: ActivityResultLauncher<Array<String>>) {
        Timber.d("Requesting location permissions")
        launcher.launch(LOCATION_PERMISSIONS)
    }

    fun requestBackgroundLocationPermission(fragment: Fragment, launcher: ActivityResultLauncher<String>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Timber.d("Requesting background location permission")
            launcher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }
}