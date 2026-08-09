package com.altusix.slate.widgets.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import androidx.core.content.ContextCompat

// =========================================================================
// DATA MODELS
// =========================================================================

/**
 * Holds real-time connection state, device name, battery metrics, and media volume level.
 */
data class BluetoothDeviceData(
    val isConnected: Boolean,
    val deviceName: String,
    val batteryLevel: Int = -1,
    val volumeLevel: Int = 50, // 0-100 media volume percentage
    val needsPermission: Boolean = false
)

// =========================================================================
// DATA READER & SYSTEM UTILITIES
// =========================================================================

object BluetoothDataReader {

    fun hasBluetoothPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Reads current media stream volume (0-100%).
     */
    fun getCurrentMediaVolume(context: Context): Int {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null) {
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                ((currentVol.toFloat() / maxVol.toFloat()) * 100).toInt().coerceIn(0, 100)
            } else {
                50
            }
        } catch (e: Exception) {
            50
        }
    }

    @SuppressLint("MissingPermission")
    fun readCurrentDeviceStatus(context: Context): BluetoothDeviceData {
        val currentVolume = getCurrentMediaVolume(context)

        if (!hasBluetoothPermission(context)) {
            return BluetoothDeviceData(
                isConnected = false,
                deviceName = "Grant Permission",
                batteryLevel = 0,
                volumeLevel = currentVolume,
                needsPermission = true
            )
        }

        return try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()

            if (adapter != null && adapter.isEnabled) {
                val bondedDevices = adapter.bondedDevices ?: emptySet()

                var connectedDevice: BluetoothDevice? = null
                for (device in bondedDevices) {
                    val isConnected = try {
                        val method = device.javaClass.getMethod("isConnected")
                        method.invoke(device) as Boolean
                    } catch (e: Exception) {
                        false
                    }
                    if (isConnected) {
                        connectedDevice = device
                        break
                    }
                }

                if (connectedDevice == null) {
                    val a2dpState = adapter.getProfileConnectionState(BluetoothProfile.A2DP)
                    val headsetState = adapter.getProfileConnectionState(BluetoothProfile.HEADSET)
                    if (a2dpState == BluetoothProfile.STATE_CONNECTED || headsetState == BluetoothProfile.STATE_CONNECTED) {
                        connectedDevice = bondedDevices.firstOrNull()
                    }
                }

                if (connectedDevice != null) {
                    val name = connectedDevice.name ?: "Bluetooth Device"
                    val battery = getDeviceBatteryLevel(connectedDevice)
                    return BluetoothDeviceData(
                        isConnected = true,
                        deviceName = name,
                        batteryLevel = if (battery in 0..100) battery else 85,
                        volumeLevel = currentVolume,
                        needsPermission = false
                    )
                }
            }

            BluetoothDeviceData(
                isConnected = false,
                deviceName = "no device connected",
                batteryLevel = 0,
                volumeLevel = currentVolume,
                needsPermission = false
            )
        } catch (e: Exception) {
            BluetoothDeviceData(
                isConnected = false,
                deviceName = "no device connected",
                batteryLevel = 0,
                volumeLevel = currentVolume,
                needsPermission = false
            )
        }
    }

    private fun getDeviceBatteryLevel(device: BluetoothDevice): Int {
        return try {
            val method = device.javaClass.getMethod("getBatteryLevel")
            val level = method.invoke(device) as Int
            if (level in 0..100) level else -1
        } catch (e: Exception) {
            -1
        }
    }
}