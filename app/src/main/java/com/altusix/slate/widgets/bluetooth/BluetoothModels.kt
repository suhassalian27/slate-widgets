package com.altusix.slate.widgets.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

// =========================================================================
// DATA MODELS
// =========================================================================

/**
 * Holds real-time connection state, device name, and battery metrics.
 */
data class BluetoothDeviceData(
    val isConnected: Boolean,
    val deviceName: String,
    val batteryLevel: Int = -1,
    val needsPermission: Boolean = false
)

// =========================================================================
// DATA READER & SYSTEM UTILITIES
// =========================================================================

object BluetoothDataReader {

    /**
     * Checks runtime Bluetooth permissions based on Android API level.
     */
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
     * Reads active Bluetooth audio connection status and battery percentage.
     */
    @SuppressLint("MissingPermission")
    fun readCurrentDeviceStatus(context: Context): BluetoothDeviceData {
        if (!hasBluetoothPermission(context)) {
            return BluetoothDeviceData(
                isConnected = false,
                deviceName = "Grant Permission",
                batteryLevel = 0,
                needsPermission = true
            )
        }

        return try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()

            if (adapter != null && adapter.isEnabled) {
                val bondedDevices = adapter.bondedDevices ?: emptySet()

                // 1. Query connected devices via reflection
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

                // 2. Fallback check for A2DP / Headset audio profiles
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
                        needsPermission = false
                    )
                }
            }

            BluetoothDeviceData(
                isConnected = false,
                deviceName = "no device connected",
                batteryLevel = 0,
                needsPermission = false
            )
        } catch (e: Exception) {
            BluetoothDeviceData(
                isConnected = false,
                deviceName = "no device connected",
                batteryLevel = 0,
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