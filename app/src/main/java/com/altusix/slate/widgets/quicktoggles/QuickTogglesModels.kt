package com.altusix.slate.widgets.quicktoggles

import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.location.LocationManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.location.LocationManagerCompat

// =========================================================================
// 1. DATA MODELS & ENUMS
// =========================================================================

enum class ToggleType {
    WIFI,
    BLUETOOTH,
    TORCH,
    RINGER,
    AUTOROTATE,
    HOTSPOT,
    AIRPLANE,
    DARK_MODE,
    LOCATION,
    TIMEOUT,
    BATTERY_SAVER,
    ALERT_SLIDER
}

enum class AlertSliderMode(val label: String) {
    SILENT("Silent"),
    VIBRATE("Vibrate"),
    RING("Ring")
}

data class ToggleItemState(
    val type: ToggleType,
    val isEnabled: Boolean,
    val label: String,
    val subtitle: String = "",
    val activeColorHex: Long = 0xFF30D158L
)

data class QuickTogglesState(
    val wifi: ToggleItemState = ToggleItemState(ToggleType.WIFI, true, "Wi-Fi", "Connected", 0xFF0A84FFL),
    val bluetooth: ToggleItemState = ToggleItemState(ToggleType.BLUETOOTH, true, "Bluetooth", "Active", 0xFF0A84FFL),
    val torch: ToggleItemState = ToggleItemState(ToggleType.TORCH, false, "Torch", "Off", 0xFFFFD60AL),
    val ringer: ToggleItemState = ToggleItemState(ToggleType.RINGER, true, "Sound", "Normal", 0xFF30D158L),
    val alertSlider: AlertSliderMode = AlertSliderMode.RING,
    val autoRotate: ToggleItemState = ToggleItemState(ToggleType.AUTOROTATE, false, "Rotate", "Portrait", 0xFFFF9F0AL),
    val hotspot: ToggleItemState = ToggleItemState(ToggleType.HOTSPOT, false, "Hotspot", "Off", 0xFFFF375FL),
    val airplane: ToggleItemState = ToggleItemState(ToggleType.AIRPLANE, false, "Airplane", "Off", 0xFFFF9F0AL),
    val darkMode: ToggleItemState = ToggleItemState(ToggleType.DARK_MODE, true, "Dark Mode", "On", 0xFFBF5AF2L),
    val location: ToggleItemState = ToggleItemState(ToggleType.LOCATION, true, "Location", "High Accuracy", 0xFF30D158L),
    val timeout: ToggleItemState = ToggleItemState(ToggleType.TIMEOUT, true, "Timeout", "1 min", 0xFF64D2FFL),
    val batterySaver: ToggleItemState = ToggleItemState(ToggleType.BATTERY_SAVER, false, "Battery Saver", "Off", 0xFFFFD60AL),
    val mediaVolumePercent: Int = 68
)

// =========================================================================
// 2. STATE MANAGER & HARDWARE CONTROLLER
// =========================================================================

object QuickTogglesStateManager {

    private const val PREFS_NAME = "slate_toggles_prefs"
    private const val KEY_TORCH_STATE = "torch_is_on"

    fun readCurrentState(context: Context): QuickTogglesState {
        // 1. Wi-Fi
        var isWifiOn = false
        var wifiSubtitle = "Off"
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = cm?.activeNetwork
            val caps = cm?.getNetworkCapabilities(activeNetwork)
            val isWifiConnected = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            isWifiOn = wm?.isWifiEnabled == true || isWifiConnected

            if (isWifiConnected) {
                val info = wm?.connectionInfo
                val rawSsid = info?.ssid?.replace("\"", "") ?: ""
                wifiSubtitle = if (rawSsid.isNotBlank() && rawSsid != "<unknown ssid>") rawSsid else "Connected"
            } else if (isWifiOn) {
                wifiSubtitle = "Available"
            }
        } catch (_: Exception) {
            wifiSubtitle = if (isWifiOn) "On" else "Off"
        }

        // 2. Bluetooth
        var isBtOn = false
        var btSubtitle = "Off"
        try {
            val btAdapter = BluetoothAdapter.getDefaultAdapter()
            isBtOn = btAdapter?.isEnabled == true
            if (isBtOn) {
                btSubtitle = "Ready"
            }
        } catch (_: Exception) {
            btSubtitle = "Off"
        }

        // 3. Torch
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isTorchOn = prefs.getBoolean(KEY_TORCH_STATE, false)

        // 4. Ringer & Alert Slider
        var alertMode = AlertSliderMode.RING
        var ringerSubtitle = "Normal"
        var isRingerActive = true
        var ringerLabel = "Sound"
        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            when (am?.ringerMode) {
                AudioManager.RINGER_MODE_SILENT -> {
                    alertMode = AlertSliderMode.SILENT
                    ringerSubtitle = "Silent"
                    ringerLabel = "Silent"
                    isRingerActive = false
                }
                AudioManager.RINGER_MODE_VIBRATE -> {
                    alertMode = AlertSliderMode.VIBRATE
                    ringerSubtitle = "Vibrate"
                    ringerLabel = "Vibrate"
                    isRingerActive = true
                }
                else -> {
                    alertMode = AlertSliderMode.RING
                    ringerSubtitle = "Ring"
                    ringerLabel = "Sound"
                    isRingerActive = true
                }
            }
        } catch (_: Exception) {}

        // 5. Auto-Rotate
        var isAutoRotateOn = false
        try {
            isAutoRotateOn = Settings.System.getInt(
                context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                0
            ) == 1
        } catch (_: Exception) {}

        // 6. Airplane Mode
        var isAirplaneOn = false
        try {
            isAirplaneOn = Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.AIRPLANE_MODE_ON,
                0
            ) != 0
        } catch (_: Exception) {}

        // 7. Dark Mode
        val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        // 8. Location
        var isLocationOn = false
        try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (lm != null) {
                isLocationOn = LocationManagerCompat.isLocationEnabled(lm)
            }
        } catch (_: Exception) {}

        // 9. Screen Timeout
        var timeoutSubtitle = "30s"
        try {
            val timeoutMs = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT,
                30000
            )
            timeoutSubtitle = when {
                timeoutMs <= 15000 -> "15s"
                timeoutMs <= 30000 -> "30s"
                timeoutMs <= 60000 -> "1m"
                timeoutMs <= 120000 -> "2m"
                timeoutMs <= 300000 -> "5m"
                timeoutMs <= 600000 -> "10m"
                else -> "${timeoutMs / 60000}m"
            }
        } catch (_: Exception) {}

        // 10. Battery Saver
        var isPowerSave = false
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            isPowerSave = pm?.isPowerSaveMode == true
        } catch (_: Exception) {}

        // 11. Media Volume
        var volumePercent = 60
        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (am != null) {
                val currentVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                volumePercent = ((currentVol.toFloat() / maxVol) * 100).toInt().coerceIn(0, 100)
            }
        } catch (_: Exception) {}

        return QuickTogglesState(
            wifi = ToggleItemState(ToggleType.WIFI, isWifiOn, "Wi-Fi", wifiSubtitle, 0xFF0A84FFL),
            bluetooth = ToggleItemState(ToggleType.BLUETOOTH, isBtOn, "Bluetooth", btSubtitle, 0xFF0A84FFL),
            torch = ToggleItemState(ToggleType.TORCH, isTorchOn, "Torch", if (isTorchOn) "On" else "Off", 0xFFFFD60AL),
            ringer = ToggleItemState(ToggleType.RINGER, isRingerActive, ringerLabel, ringerSubtitle, 0xFF30D158L),
            alertSlider = alertMode,
            autoRotate = ToggleItemState(ToggleType.AUTOROTATE, isAutoRotateOn, "Rotate", if (isAutoRotateOn) "Auto" else "Locked", 0xFFFF9F0AL),
            hotspot = ToggleItemState(ToggleType.HOTSPOT, false, "Hotspot", "Off", 0xFFFF375FL),
            airplane = ToggleItemState(ToggleType.AIRPLANE, isAirplaneOn, "Airplane", if (isAirplaneOn) "On" else "Off", 0xFFFF9F0AL),
            darkMode = ToggleItemState(ToggleType.DARK_MODE, isDark, "Dark Mode", if (isDark) "Dark" else "Light", 0xFFBF5AF2L),
            location = ToggleItemState(ToggleType.LOCATION, isLocationOn, "Location", if (isLocationOn) "On" else "Off", 0xFF30D158L),
            timeout = ToggleItemState(ToggleType.TIMEOUT, true, "Timeout", timeoutSubtitle, 0xFF64D2FFL),
            batterySaver = ToggleItemState(ToggleType.BATTERY_SAVER, isPowerSave, "Saver", if (isPowerSave) "Active" else "Off", 0xFFFFD60AL),
            mediaVolumePercent = volumePercent
        )
    }

    // -------------------------------------------------------------------------
    // DIRECT IN-PLACE ACTIONS
    // -------------------------------------------------------------------------

    fun toggleTorch(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val cameraId = cm?.cameraIdList?.firstOrNull { id ->
                val chars = cm.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: "0"

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val currentState = prefs.getBoolean(KEY_TORCH_STATE, false)
            val nextState = !currentState

            cm?.setTorchMode(cameraId, nextState)
            prefs.edit().putBoolean(KEY_TORCH_STATE, nextState).apply()
            nextState
        } catch (_: Exception) {
            false
        }
    }

    fun cycleSoundMode(context: Context): AlertSliderMode {
        return try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return AlertSliderMode.RING
            val current = am.ringerMode
            val nextMode: AlertSliderMode
            val nextRinger: Int

            when (current) {
                AudioManager.RINGER_MODE_NORMAL -> {
                    nextRinger = AudioManager.RINGER_MODE_VIBRATE
                    nextMode = AlertSliderMode.VIBRATE
                }
                AudioManager.RINGER_MODE_VIBRATE -> {
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    if (nm?.isNotificationPolicyAccessGranted == true) {
                        nextRinger = AudioManager.RINGER_MODE_SILENT
                        nextMode = AlertSliderMode.SILENT
                    } else {
                        nextRinger = AudioManager.RINGER_MODE_NORMAL
                        nextMode = AlertSliderMode.RING
                    }
                }
                else -> {
                    nextRinger = AudioManager.RINGER_MODE_NORMAL
                    nextMode = AlertSliderMode.RING
                }
            }

            am.ringerMode = nextRinger
            nextMode
        } catch (_: Exception) {
            AlertSliderMode.RING
        }
    }

    fun setAlertSliderMode(context: Context, targetMode: AlertSliderMode) {
        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            when (targetMode) {
                AlertSliderMode.SILENT -> {
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    if (nm?.isNotificationPolicyAccessGranted == true) {
                        am.ringerMode = AudioManager.RINGER_MODE_SILENT
                    } else {
                        am.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                    }
                }
                AlertSliderMode.VIBRATE -> am.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                AlertSliderMode.RING -> am.ringerMode = AudioManager.RINGER_MODE_NORMAL
            }
        } catch (_: Exception) {}
    }

    fun toggleAutoRotate(context: Context): Boolean {
        return try {
            if (Settings.System.canWrite(context)) {
                val current = Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)
                val next = if (current == 1) 0 else 1
                Settings.System.putInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, next)
                next == 1
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun cycleScreenTimeout(context: Context): String {
        val timeouts = listOf(15000, 30000, 60000, 120000, 300000)
        return try {
            if (Settings.System.canWrite(context)) {
                val current = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, 30000)
                val currentIdx = timeouts.indexOfFirst { it >= current }
                val nextIdx = (if (currentIdx >= 0) currentIdx + 1 else 0) % timeouts.size
                val nextVal = timeouts[nextIdx]
                Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, nextVal)
                when (nextVal) {
                    15000 -> "15s"
                    30000 -> "30s"
                    60000 -> "1m"
                    120000 -> "2m"
                    else -> "5m"
                }
            } else {
                "30s"
            }
        } catch (_: Exception) {
            "30s"
        }
    }

    // -------------------------------------------------------------------------
    // INTENT CREATORS (PANEL & SYSTEM SHORTCUTS)
    // -------------------------------------------------------------------------

    fun createWifiIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_WIFI).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
    }

    fun createBluetoothIntent(): Intent {
        return Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun createHotspotIntent(): Intent {
        return Intent().apply {
            action = "android.settings.TETHER_SETTINGS"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun createAirplaneIntent(): Intent {
        return Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun createLocationIntent(): Intent {
        return Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun createDisplaySettingsIntent(): Intent {
        return Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun createBatterySaverIntent(): Intent {
        return Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun createWriteSettingsIntent(): Intent {
        return Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun createNotificationPolicyIntent(): Intent {
        return Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}
