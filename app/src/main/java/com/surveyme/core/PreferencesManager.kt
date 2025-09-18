package com.surveyme.core

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import timber.log.Timber

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        Constants.PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_MAP_STYLE = "map_style"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_TRACKING_ACTIVE = "tracking_active"
        private const val KEY_LAST_LATITUDE = "last_latitude"
        private const val KEY_LAST_LONGITUDE = "last_longitude"
        private const val KEY_LAST_ZOOM = "last_zoom"
    }

    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) {
            prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()
            applyDarkMode(value)
            Timber.d("Dark mode set to: $value")
        }

    var areNotificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()
            Timber.d("Notifications enabled: $value")
        }

    var mapStyle: String
        get() = prefs.getString(KEY_MAP_STYLE, "standard") ?: "standard"
        set(value) {
            prefs.edit().putString(KEY_MAP_STYLE, value).apply()
            Timber.d("Map style set to: $value")
        }

    var isFirstLaunch: Boolean
        get() = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
        set(value) {
            prefs.edit().putBoolean(KEY_FIRST_LAUNCH, value).apply()
        }

    var isTrackingActive: Boolean
        get() = prefs.getBoolean(KEY_TRACKING_ACTIVE, false)
        set(value) {
            prefs.edit().putBoolean(KEY_TRACKING_ACTIVE, value).apply()
            Timber.d("Tracking active: $value")
        }

    fun saveLastMapPosition(latitude: Double, longitude: Double, zoom: Float) {
        prefs.edit().apply {
            putFloat(KEY_LAST_LATITUDE, latitude.toFloat())
            putFloat(KEY_LAST_LONGITUDE, longitude.toFloat())
            putFloat(KEY_LAST_ZOOM, zoom)
            apply()
        }
    }

    fun getLastMapPosition(): Triple<Double, Double, Float>? {
        if (!prefs.contains(KEY_LAST_LATITUDE)) return null

        val lat = prefs.getFloat(KEY_LAST_LATITUDE, 0f).toDouble()
        val lon = prefs.getFloat(KEY_LAST_LONGITUDE, 0f).toDouble()
        val zoom = prefs.getFloat(KEY_LAST_ZOOM, Constants.DEFAULT_MAP_ZOOM.toFloat())

        return Triple(lat, lon, zoom)
    }

    fun clear() {
        prefs.edit().clear().apply()
        Timber.d("Preferences cleared")
    }

    private fun applyDarkMode(enabled: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (enabled) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
        )
    }
}