package com.altusix.slate.widgets.deviceinfo

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import com.altusix.slate.R
import com.altusix.slate.core.model.SlateWidgetInfo
import com.altusix.slate.data.local.SlateWidgetConfig

const val ACTION_REFRESH_DEVICE_INFO = "com.altusix.slate.ACTION_REFRESH_DEVICE_INFO"

private fun loadSlateWidgetConfig(context: Context, widgetId: Int): SlateWidgetConfig {
    val prefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
    val themeMode = prefs.getString("widget_${widgetId}_theme_mode", "DARK") ?: "DARK"
    val bgColor = prefs.getLong("widget_${widgetId}_bg_color", 0xFF161618L)
    val opacity = prefs.getFloat("widget_${widgetId}_opacity", 1.0f)
    val accentColor = prefs.getLong("widget_${widgetId}_accent_color", 0xFFFFFFFFL)
    return SlateWidgetConfig(themeMode = themeMode, backgroundColorHex = bgColor, opacity = opacity, accentColorHex = accentColor)
}

private fun parseAndLockIsResponsive(context: Context, widgetId: Int): Boolean {
    val widgetPrefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
    val modeKey = "widget_${widgetId}_mode"
    val isResponsiveKey = "widget_${widgetId}_is_responsive"
    if (widgetPrefs.contains(modeKey)) return widgetPrefs.getString(modeKey, "RESPONSIVE") == "RESPONSIVE"
    if (widgetPrefs.contains(isResponsiveKey)) return widgetPrefs.getBoolean(isResponsiveKey, true)

    val launcherPrefs = context.getSharedPreferences("slate_app_launcher_prefs", Context.MODE_PRIVATE)
    val defaultResponsive = launcherPrefs.getBoolean("default_is_responsive", true)
    widgetPrefs.edit().putBoolean(isResponsiveKey, defaultResponsive).apply()
    return defaultResponsive
}

abstract class BaseDeviceInfoReceiver : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_REFRESH_DEVICE_INFO) {
            val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val prefs = context.getSharedPreferences("slate_deviceinfo_prefs", Context.MODE_PRIVATE)
                prefs.edit().putLong("widget_${widgetId}_last_refresh", System.currentTimeMillis()).apply()

                val appWidgetManager = AppWidgetManager.getInstance(context)
                updateWidget(context, appWidgetManager, widgetId)

                Handler(Looper.getMainLooper()).postDelayed({
                    updateWidget(context, appWidgetManager, widgetId)
                }, 2200L)
            }
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) updateWidget(context, appWidgetManager, widgetId)
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle?) {
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 200) ?: 200
        val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 200) ?: 200
        val wDp = if (wDpRaw <= 0) 200 else wDpRaw
        val hDp = if (hDpRaw <= 0) 200 else hDpRaw

        val isResponsive = parseAndLockIsResponsive(context, widgetId)
        val config = loadSlateWidgetConfig(context, widgetId)
        val bitmap = renderBitmapForWidget(context, config, isResponsive, wDp, hDp, widgetId)

        val views = RemoteViews(context.packageName, R.layout.widget_image_container)
        views.setImageViewBitmap(R.id.widget_image_view, bitmap)

        val refreshIntent = Intent(context, this::class.java).apply {
            action = ACTION_REFRESH_DEVICE_INFO
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            widgetId,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_image_view, pi)

        appWidgetManager.updateAppWidget(widgetId, views)
    }

    abstract fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap
}

fun getDeviceInfoWidgetsCatalog(): List<SlateWidgetInfo> {
    return listOf(
        SlateWidgetInfo(name = "Device Info Mini Bento", sizeText = "2x2", category = "Device Info", receiverClass = DeviceInfoMiniBento2x2Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Storage & RAM Bars", sizeText = "2x1", category = "Device Info", receiverClass = DeviceInfoStorageBarCapsule2x1Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Device Info Dashboard", sizeText = "4x2", category = "Device Info", receiverClass = DeviceInfoDashboard4x2Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Battery Temp Pill", sizeText = "2x1", category = "Device Info", receiverClass = DeviceInfoBatteryTemp2x1Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "RAM Load Pill", sizeText = "2x1", category = "Device Info", receiverClass = DeviceInfoRamLoad2x1Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Free Storage Pill", sizeText = "2x1", category = "Device Info", receiverClass = DeviceInfoFreeStorage2x1Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "System Uptime Pill", sizeText = "2x1", category = "Device Info", receiverClass = DeviceInfoUptime2x1Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Device IP Pill", sizeText = "2x1", category = "Device Info", receiverClass = DeviceInfoDeviceIp2x1Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Network Status Pill", sizeText = "2x1", category = "Device Info", receiverClass = DeviceInfoNetwork2x1Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Refresh Rate Pill", sizeText = "2x1", category = "Device Info", receiverClass = DeviceInfoRefreshRate2x1Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Resolution Pill", sizeText = "2x1", category = "Device Info", receiverClass = DeviceInfoResolution2x1Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Android Version Pill", sizeText = "2x1", category = "Device Info", receiverClass = DeviceInfoAndroidVersion2x1Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Device Name Pill", sizeText = "2x1", category = "Device Info", receiverClass = DeviceInfoDeviceName2x1Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Battery Voltage Pill", sizeText = "2x1", category = "Device Info", receiverClass = DeviceInfoBatteryVoltage2x1Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Time To Charge Pill", sizeText = "2x1", category = "Device Info", receiverClass = DeviceInfoTimeToCharge2x1Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Data Usage Pill", sizeText = "2x1", category = "Device Info", receiverClass = DeviceInfoDataUsage2x1Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Wi-Fi Usage Pill", sizeText = "2x1", category = "Device Info", receiverClass = DeviceInfoWifiUsage2x1Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Network Speed Pill", sizeText = "2x1", category = "Device Info", receiverClass = DeviceInfoSpeedTest2x1Receiver::class.java, hasModeOption = true)
    )
}

fun updateAllDeviceInfoWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val receivers = listOf(
        DeviceInfoMiniBento2x2Receiver::class.java,
        DeviceInfoStorageBarCapsule2x1Receiver::class.java,
        DeviceInfoDashboard4x2Receiver::class.java,
        DeviceInfoBatteryTemp2x1Receiver::class.java,
        DeviceInfoRamLoad2x1Receiver::class.java,
        DeviceInfoFreeStorage2x1Receiver::class.java,
        DeviceInfoUptime2x1Receiver::class.java,
        DeviceInfoDeviceIp2x1Receiver::class.java,
        DeviceInfoNetwork2x1Receiver::class.java,
        DeviceInfoRefreshRate2x1Receiver::class.java,
        DeviceInfoResolution2x1Receiver::class.java,
        DeviceInfoAndroidVersion2x1Receiver::class.java,
        DeviceInfoDeviceName2x1Receiver::class.java,
        DeviceInfoBatteryVoltage2x1Receiver::class.java,
        DeviceInfoTimeToCharge2x1Receiver::class.java,
        DeviceInfoDataUsage2x1Receiver::class.java,
        DeviceInfoWifiUsage2x1Receiver::class.java,
        DeviceInfoSpeedTest2x1Receiver::class.java
    )
    for (receiverClass in receivers) {
        val ids = manager.getAppWidgetIds(ComponentName(context, receiverClass)) ?: intArrayOf()
        if (ids.isNotEmpty()) {
            val intent = Intent(context, receiverClass).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}

// 1. DEVICE INFO MINI BENTO (2x2)
class DeviceInfoMiniBento2x2Receiver : BaseDeviceInfoReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateDeviceInfoMiniBento2x2Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 2. STORAGE & RAM BARS (2x1)
class DeviceInfoStorageBarCapsule2x1Receiver : BaseDeviceInfoReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateStorageBarCapsule2x1Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 3. DEVICE INFO DASHBOARD (4x2)
class DeviceInfoDashboard4x2Receiver : BaseDeviceInfoReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateDeviceInfoDashboard4x2Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 4. BATTERY TEMP PILL (2x1)
class DeviceInfoBatteryTemp2x1Receiver : BaseDeviceInfoReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateBatteryTempPill2x1Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 5. RAM LOAD PILL (2x1)
class DeviceInfoRamLoad2x1Receiver : BaseDeviceInfoReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateRamLoadPill2x1Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 6. FREE STORAGE PILL (2x1)
class DeviceInfoFreeStorage2x1Receiver : BaseDeviceInfoReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateFreeStoragePill2x1Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 7. SYSTEM UPTIME PILL (2x1)
class DeviceInfoUptime2x1Receiver : BaseDeviceInfoReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateUptimePill2x1Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 8. DEVICE IP PILL (2x1)
class DeviceInfoDeviceIp2x1Receiver : BaseDeviceInfoReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateDeviceIpPill2x1Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 9. NETWORK STATUS PILL (2x1)
class DeviceInfoNetwork2x1Receiver : BaseDeviceInfoReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateNetworkPill2x1Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 10. REFRESH RATE PILL (2x1)
class DeviceInfoRefreshRate2x1Receiver : BaseDeviceInfoReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateRefreshRatePill2x1Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 11. RESOLUTION PILL (2x1)
class DeviceInfoResolution2x1Receiver : BaseDeviceInfoReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateResolutionPill2x1Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 12. ANDROID VERSION PILL (2x1)
class DeviceInfoAndroidVersion2x1Receiver : BaseDeviceInfoReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateAndroidVersionPill2x1Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 13. DEVICE NAME PILL (2x1)
class DeviceInfoDeviceName2x1Receiver : BaseDeviceInfoReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateDeviceNamePill2x1Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 14. BATTERY VOLTAGE PILL (2x1)
class DeviceInfoBatteryVoltage2x1Receiver : BaseDeviceInfoReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateBatteryVoltagePill2x1Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 15. TIME TO CHARGE PILL (2x1)
class DeviceInfoTimeToCharge2x1Receiver : BaseDeviceInfoReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateTimeToChargePill2x1Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 16. DATA USAGE PILL (2x1)
class DeviceInfoDataUsage2x1Receiver : BaseDeviceInfoReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateDataUsagePill2x1Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 17. WI-FI USAGE PILL (2x1)
class DeviceInfoWifiUsage2x1Receiver : BaseDeviceInfoReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateWifiUsagePill2x1Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 18. INTERNET SPEED TEST PILL (2x1)
class DeviceInfoSpeedTest2x1Receiver : BaseDeviceInfoReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateSpeedTestPill2x1Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}