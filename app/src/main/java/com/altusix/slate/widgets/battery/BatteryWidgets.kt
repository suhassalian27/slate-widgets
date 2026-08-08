package com.altusix.slate.widgets.battery

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.BatteryManager
import android.os.Build
import com.altusix.slate.core.receiver.BaseCanvasWidgetProvider
import com.altusix.slate.data.local.SlateWidgetConfig
import android.app.PendingIntent

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

fun updateAllBatteryWidgets(context: Context) {
    val receivers = listOf(
        DotLevelHeaderBatteryReceiver::class.java,
        DotLevelPureBatteryReceiver::class.java,
        MinimalLinearBatteryReceiver::class.java,
        MinimalRingBatteryReceiver::class.java,
        MultiDeviceBatteryReceiver::class.java,
        HorizontalBatteryReceiver::class.java,
        ArcGaugeBatteryReceiver::class.java,
        EditorialStatsBatteryReceiver::class.java,
        DotMatrixBatteryLEDReceiver::class.java,
        DotLevelMeterWideReceiver::class.java,
        SegmentedPillBatteryReceiver::class.java,
        PixelHeartBatteryReceiver::class.java,
        LightningBoltBatteryReceiver::class.java,
        CircularRingBatteryReceiver::class.java,
        VerticalBatteryPillReceiver::class.java,
        HorizontalBatteryPillReceiver::class.java
    )

    val manager = AppWidgetManager.getInstance(context)
    for (receiver in receivers) {
        val ids = manager.getAppWidgetIds(ComponentName(context, receiver))
        if (ids.isNotEmpty()) {
            val intent = Intent(context, receiver).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}

// Base Battery Receiver linking WorkManager scheduling and real-time charging triggers
// In BatteryWidgets.kt

abstract class BaseBatteryReceiver : BaseCanvasWidgetProvider() {
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        BatteryUpdateWorker.schedule(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        BatteryUpdateWorker.cancel(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == Intent.ACTION_POWER_CONNECTED ||
            action == Intent.ACTION_POWER_DISCONNECTED ||
            action == Intent.ACTION_BATTERY_LOW ||
            action == Intent.ACTION_BATTERY_OKAY
        ) {
            updateAllBatteryWidgets(context)
        }
    }

    override fun getClickPendingIntent(context: Context, appWidgetId: Int): PendingIntent? = null
}

// Minimal Linear Tile (2x2)
class MinimalLinearBatteryReceiver : BaseBatteryReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val data = readDetailedBatteryStatus(context)
        return generateBatteryMinimalLinearBitmap(context, data, config, wDp, hDp)
    }
}

// Minimal Ring Tile (2x2)
class MinimalRingBatteryReceiver : BaseBatteryReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val data = readDetailedBatteryStatus(context)
        return generateBatteryMinimalRingBitmap(context, data, config, wDp, hDp)
    }
}

// 2. Multi-Device Card (4x2)
class MultiDeviceBatteryReceiver : BaseBatteryReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val data = readDetailedBatteryStatus(context)
        return generateMultiDeviceBatteryBitmap(context, data, config, wDp, hDp)
    }
}

// 3. Horizontal Strip (4x1)
class HorizontalBatteryReceiver : BaseBatteryReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val data = readDetailedBatteryStatus(context)
        return generateHorizontalStripBitmap(context, data, config, wDp, hDp)
    }
}

// 4. Arc Gauge Tile (2x2)
class ArcGaugeBatteryReceiver : BaseBatteryReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val data = readDetailedBatteryStatus(context)
        return generateArcGaugeTileBitmap(context, data, config, wDp, hDp)
    }
}

// 5. Editorial Stats Tile (2x2)
class EditorialStatsBatteryReceiver : BaseBatteryReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val data = readDetailedBatteryStatus(context)
        return generateEditorialStatsBitmap(context, data, config, wDp, hDp)
    }
}

// 6. Dot Matrix LED (4x2)
class DotMatrixBatteryLEDReceiver : BaseBatteryReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val data = readDetailedBatteryStatus(context)
        val density = context.resources.displayMetrics.density
        val wPx = (wDp * density).toInt()
        val hPx = (hDp * density).toInt()
        val isLight = config.themeMode == "LIGHT"
        val activeColor = if (isLight) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.White
        val dimColor = if (isLight) androidx.compose.ui.graphics.Color(0x1F000000) else androidx.compose.ui.graphics.Color(0x1AFFFFFF)
        val bgColor = androidx.compose.ui.graphics.Color(config.backgroundColorHex).copy(alpha = config.opacity)
        return generateDotMatrixLEDBitmap("${data.percentage}%", activeColor, dimColor, bgColor, wPx, hPx)
    }
}

// 7. Dot Level Header Tile (2x2)
class DotLevelHeaderBatteryReceiver : BaseBatteryReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val data = readDetailedBatteryStatus(context)
        return generateDotLevelMeterWithHeaderBitmap(context, data, config, wDp, hDp)
    }
}

// 8. Dot Level Pure Tile (2x2)
class DotLevelPureBatteryReceiver : BaseBatteryReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val data = readDetailedBatteryStatus(context)
        return generateDotLevelMeterPureBitmap(context, data, config, wDp, hDp)
    }
}

// 9. Dot Level Meter Wide (4x2)
class DotLevelMeterWideReceiver : BaseBatteryReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val data = readDetailedBatteryStatus(context)
        val density = context.resources.displayMetrics.density
        val wPx = (wDp * density).toInt()
        val hPx = (hDp * density).toInt()
        val isLight = config.themeMode == "LIGHT"
        val activeColor = androidx.compose.ui.graphics.Color(config.accentColorHex)
        val dimColor = if (isLight) androidx.compose.ui.graphics.Color(0x1F000000) else androidx.compose.ui.graphics.Color(0x1AFFFFFF)
        val bgColor = androidx.compose.ui.graphics.Color(config.backgroundColorHex).copy(alpha = config.opacity)
        return generateCenteredLevelBitmap(data.percentage, activeColor, dimColor, bgColor, wPx, hPx)
    }
}

// 10. Segmented Pill Tile (2x2)
class SegmentedPillBatteryReceiver : BaseBatteryReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val data = readDetailedBatteryStatus(context)
        return generateSegmentedPillTileBitmap(context, data, config, wDp, hDp)
    }
}

// 11. Pixel Heart Tile (2x2)
class PixelHeartBatteryReceiver : BaseBatteryReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val data = readDetailedBatteryStatus(context)
        return generatePixelHeartBitmap(context, data, config, wDp, hDp)
    }
}

// 12. Lightning Bolt Tile (2x2 / 4x2)
class LightningBoltBatteryReceiver : BaseBatteryReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val data = readDetailedBatteryStatus(context)
        val isWide = wDp >= 200
        return generateWavyLightningBoltBitmap(context, data, config, wDp, hDp, isWide)
    }
}

// 13. Circular Ring Dial (2x2)
class CircularRingBatteryReceiver : BaseBatteryReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val data = readDetailedBatteryStatus(context)
        val density = context.resources.displayMetrics.density
        val wPx = (wDp * density).toInt()
        val hPx = (hDp * density).toInt()
        val isLight = config.themeMode == "LIGHT"
        val accentColor = androidx.compose.ui.graphics.Color(config.accentColorHex)
        val dimColor = if (isLight) androidx.compose.ui.graphics.Color(0x1F000000) else androidx.compose.ui.graphics.Color(0x2BFFFFFF)
        val iconColor = if (isLight) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.White
        val bgColor = androidx.compose.ui.graphics.Color(config.backgroundColorHex).copy(alpha = config.opacity)
        return generateCircularGaugeBitmap(data.percentage, data.isCharging, accentColor, dimColor, iconColor, bgColor, isLight, 1.0f, wPx, hPx)
    }
}

// 14. Vertical Pill (1x2)
class VerticalBatteryPillReceiver : BaseBatteryReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val data = readDetailedBatteryStatus(context)
        val density = context.resources.displayMetrics.density
        val wPx = (wDp * density).toInt()
        val hPx = (hDp * density).toInt()
        return generateVerticalPillBitmap(data.percentage, data.isCharging, config, wPx, hPx)
    }
}

// 15. Horizontal Pill (2x1)
class HorizontalBatteryPillReceiver : BaseBatteryReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val data = readDetailedBatteryStatus(context)
        val density = context.resources.displayMetrics.density
        val wPx = (wDp * density).toInt()
        val hPx = (hDp * density).toInt()
        return generateHorizontalPillBitmap(data.percentage, data.isCharging, config, wPx, hPx)
    }
}