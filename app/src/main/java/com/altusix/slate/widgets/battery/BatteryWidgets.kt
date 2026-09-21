package com.altusix.slate.widgets.battery

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Color
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.altusix.slate.R
import com.altusix.slate.core.model.SlateWidgetInfo
import com.altusix.slate.core.service.BatteryWidgetService
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.data.local.loadSlateWidgetConfig
import com.altusix.slate.utils.getSafeBgColor

fun getBatteryWidgetsCatalog(): List<SlateWidgetInfo> {
    return listOf(
        SlateWidgetInfo("Dot Level Header", "2x2", "Battery", DotLevelHeaderBatteryReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Dot Level Pure", "2x2", "Battery", DotLevelPureBatteryReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Lightning Bolt", "4x2", "Battery", LightningBoltBatteryReceiver::class.java, hasModeOption = true),

        SlateWidgetInfo("Minimal Linear", "2x2", "Battery", MinimalLinearBatteryReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Minimal Ring", "2x2", "Battery", MinimalRingBatteryReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Multi-Device", "4x2", "Battery", BatteryMultiDeviceStatsReceiver::class.java, hasModeOption = true),

        SlateWidgetInfo("Arc Battery", "2x2", "Battery", ArcGaugeBatteryReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Editorial", "2x2", "Battery", EditorialStatsBatteryReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Dot Matrix LED", "4x2", "Battery", DotMatrixBatteryLEDReceiver::class.java, hasModeOption = false),

        SlateWidgetInfo("5-Pill Gauge", "2x2", "Battery", SegmentedPillBatteryReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Pixel Heart", "2x2", "Battery", PixelHeartBatteryReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Dot Level Meter Wide", "4x2", "Battery", DotLevelMeterWideReceiver::class.java, hasModeOption = true),

        SlateWidgetInfo("Battery Strip", "4x1", "Battery", HorizontalBatteryReceiver::class.java, hasModeOption = true),

        SlateWidgetInfo("Circular Dial", "2x2", "Battery", CircularRingBatteryReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Vertical Pill", "1x2", "Battery", VerticalBatteryPillReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Horizontal Pill", "2x1", "Battery", HorizontalBatteryPillReceiver::class.java, hasModeOption = false)
    )
}

data class DetailedBatteryData(
    val percentage: Int,
    val isCharging: Boolean,
    val healthText: String,
    val secondaryStatText: String,
    val tempText: String,
    val voltageText: String
)

fun readDetailedBatteryStatus(context: Context): DetailedBatteryData {
    return try {
        val appCtx = context.applicationContext
        val batteryManager = appCtx.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager

        val pctRaw = try {
            batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        } catch (e: Exception) { -1 }

        val isChargingDirect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            batteryManager?.isCharging == true
        } else {
            val status = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) ?: -1
            status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        }

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val intent = appCtx.registerReceiver(null, filter)

        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val statusIntent = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val tempRaw = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val voltageRaw = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
        val healthRaw = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN) ?: 0

        val pct = when {
            pctRaw in 0..100 -> pctRaw
            level >= 0 && scale > 0 -> (level * 100) / scale
            else -> 100
        }

        val isCharging = isChargingDirect ||
                statusIntent == BatteryManager.BATTERY_STATUS_CHARGING ||
                statusIntent == BatteryManager.BATTERY_STATUS_FULL

        val healthText = when (healthRaw) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good health"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheating"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Low health"
            else -> if (isCharging) "Charging" else "Discharging"
        }

        val cycleCount = if (Build.VERSION.SDK_INT >= 34) {
            try { batteryManager?.getIntProperty(7) ?: -1 } catch (e: Exception) { -1 }
        } else -1

        val tempC = if (tempRaw > 0) tempRaw / 10.0f else 0.0f
        val tempStr = if (tempC > 0) "${tempC}°C" else "N/A"
        val voltageVolts = if (voltageRaw > 0) voltageRaw / 1000.0f else 0.0f
        val voltageStr = if (voltageVolts > 0) "${voltageVolts}V" else "N/A"

        val secondaryStatText = when {
            cycleCount > 0 -> "$cycleCount cycles"
            tempC > 0 -> "$tempStr temp"
            voltageVolts > 0 -> "$voltageStr voltage"
            else -> if (isCharging) "Power: USB" else "Power: Battery"
        }

        DetailedBatteryData(
            percentage = pct,
            isCharging = isCharging,
            healthText = healthText,
            secondaryStatText = secondaryStatText,
            tempText = tempStr,
            voltageText = voltageStr
        )
    } catch (e: Exception) {
        DetailedBatteryData(
            percentage = 100,
            isCharging = false,
            healthText = "Good health",
            secondaryStatText = "Healthy",
            tempText = "N/A",
            voltageText = "N/A"
        )
    }
}

fun parseAndLockIsResponsive(context: Context, widgetId: Int): Boolean {
    val widgetPrefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
    val modeKey = "widget_${widgetId}_mode"
    val isResponsiveKey = "widget_${widgetId}_is_responsive"

    if (widgetPrefs.contains(modeKey)) {
        return widgetPrefs.getString(modeKey, "RESPONSIVE") == "RESPONSIVE"
    }
    if (widgetPrefs.contains(isResponsiveKey)) {
        return widgetPrefs.getBoolean(isResponsiveKey, true)
    }

    val launcherPrefs = context.getSharedPreferences("slate_app_launcher_prefs", Context.MODE_PRIVATE)
    val defaultResponsive = launcherPrefs.getBoolean("default_is_responsive", true)
    widgetPrefs.edit().putBoolean(isResponsiveKey, defaultResponsive).apply()
    return defaultResponsive
}

abstract class BaseBatteryReceiver(
    open val targetAspect: Float = 1.0f
) : AppWidgetProvider() {

    abstract fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap

    fun renderBitmapForWidget(
        context: Context,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int,
        widgetId: Int
    ): Bitmap = renderWidgetBitmap(context, widgetId, config, isResponsive, wDp, hDp)

    open fun getClickPendingIntent(context: Context, appWidgetId: Int): PendingIntent? {
        val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return if (intent.resolveActivity(context.packageManager) != null) {
            PendingIntent.getActivity(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            null
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        try {
            val serviceIntent = Intent(context, BatteryWidgetService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (_: Exception) {}
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        try {
            val serviceIntent = Intent(context, BatteryWidgetService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (_: Exception) {}
        for (appWidgetId in appWidgetIds) {
            updateSingleWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateSingleWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        try {
            val serviceIntent = Intent(context, BatteryWidgetService::class.java)
            context.stopService(serviceIntent)
        } catch (_: Exception) {}
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action

        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            try {
                val serviceIntent = Intent(context, BatteryWidgetService::class.java)
                ContextCompat.startForegroundService(context, serviceIntent)
            } catch (_: Exception) {}
        }

        if (action == Intent.ACTION_POWER_CONNECTED ||
            action == Intent.ACTION_POWER_DISCONNECTED ||
            action == Intent.ACTION_BATTERY_LOW ||
            action == Intent.ACTION_BATTERY_OKAY
        ) {
            updateAllBatteryWidgets(context)
        }
    }

    fun updateSingleWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        try {
            val config = loadSlateWidgetConfig(context, appWidgetId)
            val isResponsive = if (appWidgetId == -1) true else parseAndLockIsResponsive(context, appWidgetId)

            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val fallbackSize = when {
                targetAspect >= 3.5f -> 280 to 70
                targetAspect in 1.8f..2.4f -> 280 to 130
                targetAspect in 0.4f..0.6f -> 70 to 140
                else -> 150 to 150
            }

            val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, fallbackSize.first) ?: fallbackSize.first
            else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, fallbackSize.first) ?: fallbackSize.first
            val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, fallbackSize.second) ?: fallbackSize.second
            else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, fallbackSize.second) ?: fallbackSize.second

            val wDp = if (wDpRaw <= 0) fallbackSize.first else wDpRaw
            val hDp = if (hDpRaw <= 0) fallbackSize.second else hDpRaw
            val density = context.resources.displayMetrics.density

            val padH: Int
            val padV: Int
            val effWDp: Int
            val effHDp: Int

            if (!isResponsive && targetAspect > 0f) {
                val currentAspect = wDp.toFloat() / hDp.toFloat()
                if (currentAspect > targetAspect) {
                    val contentW = hDp * targetAspect
                    padH = (((wDp - contentW) / 2f) * density).toInt()
                    padV = 0
                    effWDp = maxOf(1, contentW.toInt())
                    effHDp = hDp
                } else {
                    val contentH = wDp / targetAspect
                    padH = 0
                    padV = (((hDp - contentH) / 2f) * density).toInt()
                    effWDp = wDp
                    effHDp = maxOf(1, contentH.toInt())
                }
            } else {
                padH = 0
                padV = 0
                effWDp = wDp
                effHDp = hDp
            }

            val bitmap = renderWidgetBitmap(context, appWidgetId, config, isResponsive, effWDp, effHDp)
            val views = RemoteViews(context.packageName, R.layout.widget_base_single)

            try {
                views.setViewPadding(R.id.layout_grid_root, padH, padV, padH, padV)
            } catch (_: Exception) {}

            views.setImageViewBitmap(R.id.widget_image_view, bitmap)

            val clickIntent = getClickPendingIntent(context, appWidgetId)
            if (clickIntent != null) {
                views.setOnClickPendingIntent(R.id.touch_slot_0, clickIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun updateAllBatteryWidgets(context: Context) {
    val receivers = listOf(
        DotLevelHeaderBatteryReceiver(),
        DotLevelPureBatteryReceiver(),
        MinimalLinearBatteryReceiver(),
        MinimalRingBatteryReceiver(),
        BatteryMultiDeviceStatsReceiver(),
        HorizontalBatteryReceiver(),
        ArcGaugeBatteryReceiver(),
        EditorialStatsBatteryReceiver(),
        DotMatrixBatteryLEDReceiver(),
        DotLevelMeterWideReceiver(),
        SegmentedPillBatteryReceiver(),
        PixelHeartBatteryReceiver(),
        LightningBoltBatteryReceiver(),
        CircularRingBatteryReceiver(),
        VerticalBatteryPillReceiver(),
        HorizontalBatteryPillReceiver()
    )

    val manager = AppWidgetManager.getInstance(context) ?: return
    for (receiver in receivers) {
        val ids = manager.getAppWidgetIds(ComponentName(context, receiver::class.java)) ?: intArrayOf()
        if (ids.isNotEmpty()) {
            for (id in ids) {
                receiver.updateSingleWidget(context, manager, id)
            }
            val intent = Intent(context, receiver::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}

// 1. Dot Level Header Tile (2x2)
class DotLevelHeaderBatteryReceiver : BaseBatteryReceiver(targetAspect = 1.0f) {
    override fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        val data = readDetailedBatteryStatus(context)
        return generateDotLevelMeterWithHeaderBitmap(
            context = context,
            data = data,
            config = config,
            isResponsive = false,
            wDp = wDp,
            hDp = hDp
        )
    }
}

// 2. Dot Level Pure Tile (2x2)
class DotLevelPureBatteryReceiver : BaseBatteryReceiver(targetAspect = 1.0f) {
    override fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        val data = readDetailedBatteryStatus(context)
        return generateDotLevelMeterPureBitmap(context, data, config, wDp, hDp)
    }
}

// 3. Minimal Linear Tile (2x2 / Responsive)
class MinimalLinearBatteryReceiver : BaseBatteryReceiver(targetAspect = 1.0f) {
    override fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        val data = readDetailedBatteryStatus(context)
        return generateBatteryMinimalLinearBitmap(context, data, config, isResponsive, wDp, hDp)
    }
}

// 4. Minimal Ring Tile (2x2)
class MinimalRingBatteryReceiver : BaseBatteryReceiver(targetAspect = 1.0f) {
    override fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        val data = readDetailedBatteryStatus(context)
        return generateBatteryMinimalRingBitmap(context, data, config, wDp, hDp)
    }
}

// 5. Arc Gauge Tile (2x2)
class ArcGaugeBatteryReceiver : BaseBatteryReceiver(targetAspect = 1.0f) {
    override fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        val data = readDetailedBatteryStatus(context)
        return generateArcGaugeTileBitmap(context, data, config, wDp, hDp)
    }
}

// 6. Editorial Stats Tile (2x2 / Responsive)
class EditorialStatsBatteryReceiver : BaseBatteryReceiver(targetAspect = 1.0f) {
    override fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        val data = readDetailedBatteryStatus(context)
        return generateEditorialStatsBitmap(context, data, config, isResponsive, wDp, hDp)
    }
}

// 7. Multi-Device Stats Bento (4x2 / Adaptive)
class BatteryMultiDeviceStatsReceiver : BaseBatteryReceiver(targetAspect = 2.0f) {
    override fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        val data = readDetailedBatteryStatus(context)
        return generateMultiDeviceBatteryBitmap(context, data, config, isResponsive, wDp, hDp, appWidgetId)
    }
}

// 8. Dot Matrix LED Receiver (4x2)
class DotMatrixBatteryLEDReceiver : BaseBatteryReceiver(targetAspect = 2.0f) {
    override fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        val data = readDetailedBatteryStatus(context)
        val isLight = config.themeMode == "LIGHT"

        // Use user-selected accent color for active lit-up LEDs
        val activeColor = config.accentColorHex.toInt() or 0xFF000000.toInt()
        val dimColor = if (isLight) 0x14000000 else 0x1AFFFFFF

        val bgColor = getSafeBgColor(config)
        val alphaInt = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
        val bgArgb = Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))

        return generateDotMatrixLEDBitmap(
            context = context,
            text = "${data.percentage}%",
            activeColorInt = activeColor,
            dimColorInt = dimColor,
            bgColorInt = bgArgb,
            wDp = wDp,
            hDp = hDp
        )
    }
}

// 9. Dot Level Meter Wide (4x2)
class DotLevelMeterWideReceiver : BaseBatteryReceiver(targetAspect = 2.0f) {
    override fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        val data = readDetailedBatteryStatus(context)
        return generateCenteredLevelBitmap(context, data.percentage, config, isResponsive, wDp, hDp)
    }
}

// 10. Horizontal Strip (4x1)
class HorizontalBatteryReceiver : BaseBatteryReceiver(targetAspect = 4.0f) {
    override fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        val data = readDetailedBatteryStatus(context)
        return generateHorizontalStripBitmap(context, data, config, isResponsive, wDp, hDp)
    }
}

// 11. Segmented Pill Tile Receiver (2x2)
class SegmentedPillBatteryReceiver : BaseBatteryReceiver(targetAspect = 1.0f) {
    override fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        val data = readDetailedBatteryStatus(context)
        return generateSegmentedPillTileBitmap(context, data, config, wDp, hDp)
    }
}

// 12. Pixel Heart Tile Receiver (2x2)
class PixelHeartBatteryReceiver : BaseBatteryReceiver(targetAspect = 1.0f) {
    override fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        val data = readDetailedBatteryStatus(context)
        return generatePixelHeartBitmap(context, data, config, wDp, hDp)
    }
}

// 13. Lightning Bolt Tile (4x2 Bento Receiver)
class LightningBoltBatteryReceiver : BaseBatteryReceiver(targetAspect = 2.0f) {
    override fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        val data = readDetailedBatteryStatus(context)
        return generateWavyLightningBoltBitmap(
            context = context,
            data = data,
            config = config,
            isResponsive = isResponsive,
            wDp = wDp,
            hDp = hDp
        )
    }
}

// 14. Circular Ring Dial (2x2)
class CircularRingBatteryReceiver : BaseBatteryReceiver(targetAspect = 1.0f) {
    override fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        val data = readDetailedBatteryStatus(context)
        return generateCircularGaugeBitmap(context, data.percentage, data.isCharging, config, wDp, hDp)
    }
}

// 15. Vertical Pill (1x2)
class VerticalBatteryPillReceiver : BaseBatteryReceiver(targetAspect = 0.5f) {
    override fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        val data = readDetailedBatteryStatus(context)
        return generateVerticalPillBitmap(context, data.percentage, data.isCharging, config, wDp, hDp)
    }
}

// 16. Horizontal Pill (2x1)
class HorizontalBatteryPillReceiver : BaseBatteryReceiver(targetAspect = 2.0f) {
    override fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        val data = readDetailedBatteryStatus(context)
        return generateHorizontalPillBitmap(context, data.percentage, data.isCharging, config, wDp, hDp)
    }
}
