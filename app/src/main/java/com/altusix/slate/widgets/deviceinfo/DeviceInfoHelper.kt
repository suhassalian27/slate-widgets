package com.altusix.slate.widgets.deviceinfo

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import java.io.File
import java.util.concurrent.TimeUnit

data class DeviceMetrics(
    val deviceName: String,
    val androidVersion: String,
    val ramUsedGb: Float,
    val ramTotalGb: Float,
    val ramPercent: Int,
    val storageUsedGb: Float,
    val storageTotalGb: Float,
    val storagePercent: Int,
    val batteryLevel: Int,
    val isCharging: Boolean,
    val uptimeFormatted: String
)

fun fetchDeviceMetrics(context: Context): DeviceMetrics {
    // RAM Metrics
    val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memInfo = ActivityManager.MemoryInfo()
    actManager.getMemoryInfo(memInfo)
    val ramTotal = memInfo.totalMem / (1024f * 1024f * 1024f)
    val ramAvail = memInfo.availMem / (1024f * 1024f * 1024f)
    val ramUsed = ramTotal - ramAvail
    val ramPercent = ((ramUsed / ramTotal) * 100).toInt().coerceIn(0, 100)

    // Storage Metrics
    val path: File = Environment.getDataDirectory()
    val stat = StatFs(path.path)
    val blockSize = stat.blockSizeLong
    val totalBlocks = stat.blockCountLong
    val availBlocks = stat.availableBlocksLong
    val storageTotal = (totalBlocks * blockSize) / (1024f * 1024f * 1024f)
    val storageAvail = (availBlocks * blockSize) / (1024f * 1024f * 1024f)
    val storageUsed = storageTotal - storageAvail
    val storagePercent = ((storageUsed / storageTotal) * 100).toInt().coerceIn(0, 100)

    // Battery Metrics
    val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
        context.registerReceiver(null, filter)
    }
    val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val batteryPct = if (level >= 0 && scale > 0) ((level / scale.toFloat()) * 100).toInt() else 0
    val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

    // Uptime
    val uptimeMs = SystemClock.elapsedRealtime()
    val hours = TimeUnit.MILLISECONDS.toHours(uptimeMs)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(uptimeMs) % 60
    val uptimeFormatted = "${hours}h ${minutes}m"

    // Device Model
    val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
    val model = Build.MODEL
    val deviceName = if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"

    return DeviceMetrics(
        deviceName = deviceName,
        androidVersion = "Android ${Build.VERSION.RELEASE}",
        ramUsedGb = ramUsed,
        ramTotalGb = ramTotal,
        ramPercent = ramPercent,
        storageUsedGb = storageUsed,
        storageTotalGb = storageTotal,
        storagePercent = storagePercent,
        batteryLevel = batteryPct,
        isCharging = isCharging,
        uptimeFormatted = uptimeFormatted
    )
}