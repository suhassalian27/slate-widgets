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
const val ACTION_RUN_SPEED_TEST = "com.altusix.slate.ACTION_RUN_SPEED_TEST"

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

    open fun getClickAction(): String = ACTION_REFRESH_DEVICE_INFO

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

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int, newOptions: Bundle?) {
        updateWidget(context, appWidgetManager, widgetId)
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
            action = getClickAction()
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
        // Bentos & Overview
        SlateWidgetInfo(name = "Device Info Mini Bento", sizeText = "2x2", category = "Device Info", receiverClass = DeviceInfoMiniBento2x2Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Storage & RAM Bars", sizeText = "2x1", category = "Device Info", receiverClass = DeviceInfoStorageBarCapsule2x1Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Device Info Dashboard", sizeText = "4x2", category = "Device Info", receiverClass = DeviceInfoDashboard4x2Receiver::class.java, hasModeOption = true),

        // Widgets 4–18: Minimal Capsule Pills
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
        SlateWidgetInfo(name = "Network Speed Pill", sizeText = "2x1", category = "Device Info", receiverClass = DeviceInfoSpeedTest2x1Receiver::class.java, hasModeOption = true),

        // Widgets 19–33: Detailed Info Cards
        SlateWidgetInfo(name = "Battery Temp Card", sizeText = "2x1", category = "Device Info", receiverClass = DeviceInfoBatteryTempCard2x1Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "RAM Load Card", sizeText = "2x1", category = "Device Info", receiverClass = DeviceInfoRamLoadCard2x1Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Free Storage Card", sizeText = "2x1", category = "Device Info", receiverClass = DeviceInfoFreeStorageCard2x1Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "System Uptime Card", sizeText = "2x1", category = "Device Info", receiverClass = DeviceInfoUptimeCard2x1Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Device IP Card", sizeText = "2x1", category = "Device Info", receiverClass = DeviceInfoDeviceIpCard2x1Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Network Status Card", sizeText = "2x1", category = "Device Info", receiverClass = DeviceInfoNetworkCard2x1Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Refresh Rate Card", sizeText = "2x1", category = "Device Info", receiverClass = DeviceInfoRefreshRateCard2x1Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Resolution Card", sizeText = "2x1", category = "Device Info", receiverClass = DeviceInfoResolutionCard2x1Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Android Version Card", sizeText = "2x1", category = "Device Info", receiverClass = DeviceInfoAndroidVersionCard2x1Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Device Name Card", sizeText = "2x1", category = "Device Info", receiverClass = DeviceInfoDeviceNameCard2x1Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Battery Voltage Card", sizeText = "2x1", category = "Device Info", receiverClass = DeviceInfoBatteryVoltageCard2x1Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Time To Charge Card", sizeText = "2x1", category = "Device Info", receiverClass = DeviceInfoTimeToChargeCard2x1Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Data Usage Card", sizeText = "2x1", category = "Device Info", receiverClass = DeviceInfoDataUsageCard2x1Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Wi-Fi Usage Card", sizeText = "2x1", category = "Device Info", receiverClass = DeviceInfoWifiUsageCard2x1Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Network Speed Card", sizeText = "2x1", category = "Device Info", receiverClass = DeviceInfoSpeedTestCard2x1Receiver::class.java, hasModeOption = true)
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

abstract class BaseSpeedTestReceiver : BaseDeviceInfoReceiver() {

    override fun getClickAction(): String = ACTION_RUN_SPEED_TEST

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_RUN_SPEED_TEST) {
            val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val prefs = context.getSharedPreferences("slate_deviceinfo_prefs", Context.MODE_PRIVATE)

                // 1. Show immediate "Testing..." state
                prefs.edit().putString("widget_${widgetId}_speed_state", "Testing...").apply()
                val appWidgetManager = AppWidgetManager.getInstance(context)
                updateWidget(context, appWidgetManager, widgetId)

                // 2. Run benchmark on background thread
                Thread {
                    val measuredSpeed = runQuickSpeedBenchmark(context)
                    prefs.edit().putString("widget_${widgetId}_speed_state", measuredSpeed).apply()

                    Handler(Looper.getMainLooper()).post {
                        // Display measured speed result
                        updateWidget(context, appWidgetManager, widgetId)

                        // 3. Revert back to "Speed Test" after 3 seconds
                        Handler(Looper.getMainLooper()).postDelayed({
                            prefs.edit().putString("widget_${widgetId}_speed_state", "Speed Test").apply()
                            updateWidget(context, appWidgetManager, widgetId)
                        }, 3000L)
                    }
                }.start()
                return
            }
        }
        super.onReceive(context, intent)
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
class DeviceInfoSpeedTest2x1Receiver : BaseSpeedTestReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateSpeedTestPill2x1Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 19. BATTERY TEMP CARD (2x1)
class DeviceInfoBatteryTempCard2x1Receiver : BaseDeviceInfoReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateBatteryTempCard2x1Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 20. RAM LOAD CARD (2x1)
class DeviceInfoRamLoadCard2x1Receiver : BaseDeviceInfoReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateRamLoadCard2x1Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 21. FREE STORAGE CARD (2x1)
class DeviceInfoFreeStorageCard2x1Receiver : BaseDeviceInfoReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateFreeStorageCard2x1Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 22. SYSTEM UPTIME CARD (2x1)
class DeviceInfoUptimeCard2x1Receiver : BaseDeviceInfoReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateUptimeCard2x1Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 23. DEVICE IP CARD (2x1)
class DeviceInfoDeviceIpCard2x1Receiver : BaseDeviceInfoReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateDeviceIpCard2x1Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 24. NETWORK STATUS CARD (2x1)
class DeviceInfoNetworkCard2x1Receiver : BaseDeviceInfoReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateNetworkCard2x1Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 25. REFRESH RATE CARD (2x1)
class DeviceInfoRefreshRateCard2x1Receiver : BaseDeviceInfoReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateRefreshRateCard2x1Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 26. RESOLUTION CARD (2x1)
class DeviceInfoResolutionCard2x1Receiver : BaseDeviceInfoReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateResolutionCard2x1Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 27. ANDROID VERSION CARD (2x1)
class DeviceInfoAndroidVersionCard2x1Receiver : BaseDeviceInfoReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateAndroidVersionCard2x1Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 28. DEVICE NAME CARD (2x1)
class DeviceInfoDeviceNameCard2x1Receiver : BaseDeviceInfoReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateDeviceNameCard2x1Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 29. BATTERY VOLTAGE CARD (2x1)
class DeviceInfoBatteryVoltageCard2x1Receiver : BaseDeviceInfoReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateBatteryVoltageCard2x1Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 30. TIME TO CHARGE CARD (2x1)
class DeviceInfoTimeToChargeCard2x1Receiver : BaseDeviceInfoReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateTimeToChargeCard2x1Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 31. DATA USAGE CARD (2x1)
class DeviceInfoDataUsageCard2x1Receiver : BaseDeviceInfoReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateDataUsageCard2x1Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 32. WI-FI USAGE CARD (2x1)
class DeviceInfoWifiUsageCard2x1Receiver : BaseDeviceInfoReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateWifiUsageCard2x1Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 33. INTERNET SPEED TEST CARD (2x1)
class DeviceInfoSpeedTestCard2x1Receiver : BaseSpeedTestReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateSpeedTestCard2x1Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}