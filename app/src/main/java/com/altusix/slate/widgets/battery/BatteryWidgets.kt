package com.altusix.slate.widgets.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import com.altusix.slate.data.local.SlateDataStore
import com.altusix.slate.data.local.SlateWidgetConfig
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first

data class DetailedBatteryData(
    val percentage: Int,
    val isCharging: Boolean,
    val healthText: String,
    val secondaryStatText: String,
    val tempText: String,
    val voltageText: String
)

private fun readDetailedBatteryStatus(context: Context): DetailedBatteryData {
    return try {
        val appCtx = context.applicationContext
        val batteryManager = appCtx.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager

        val pctRaw = try {
            batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        } catch (e: Exception) { -1 }

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val intent = appCtx.registerReceiver(null, filter)

        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val tempRaw = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val voltageRaw = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
        val healthRaw = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN) ?: 0

        val pct = when {
            pctRaw in 0..100 -> pctRaw
            level >= 0 && scale > 0 -> (level * 100) / scale
            else -> 80
        }

        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

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
            percentage = 80,
            isCharging = false,
            healthText = "Good health",
            secondaryStatText = "31.5°C temp",
            tempText = "31.5°C",
            voltageText = "4.1V"
        )
    }
}

// 1. Minimal 2x2 Tile
class MinimalBatteryWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = readDetailedBatteryStatus(context)
        val config = try {
            val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
            SlateDataStore(context).getWidgetConfig(appWidgetId)
                .catch { emit(SlateWidgetConfig()) }.first()
        } catch (e: Exception) {
            SlateWidgetConfig()
        }

        provideContent {
            MinimalBatteryTile(percentage = data.percentage, isCharging = data.isCharging, config = config)
        }
    }
}

class MinimalBatteryReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MinimalBatteryWidget()
}

// 2. Multi-Device Card (4x2)
class MultiDeviceBatteryWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = readDetailedBatteryStatus(context)
        val config = try {
            val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
            SlateDataStore(context).getWidgetConfig(appWidgetId)
                .catch { emit(SlateWidgetConfig()) }.first()
        } catch (e: Exception) {
            SlateWidgetConfig()
        }

        provideContent {
            MultiDeviceBatteryCard(
                phonePct = data.percentage,
                isCharging = data.isCharging,
                tempText = data.tempText,
                voltageText = data.voltageText,
                config = config
            )
        }
    }
}

class MultiDeviceBatteryReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MultiDeviceBatteryWidget()
}

// 3. Horizontal Strip (4x1)
class HorizontalBatteryWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = readDetailedBatteryStatus(context)
        val config = try {
            val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
            SlateDataStore(context).getWidgetConfig(appWidgetId)
                .catch { emit(SlateWidgetConfig()) }.first()
        } catch (e: Exception) {
            SlateWidgetConfig()
        }

        provideContent {
            HorizontalBatteryStrip(percentage = data.percentage, isCharging = data.isCharging, config = config)
        }
    }
}

class HorizontalBatteryReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HorizontalBatteryWidget()
}

// 4. Arc Gauge Tile (2x2)
class ArcGaugeBatteryWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = readDetailedBatteryStatus(context)
        val config = try {
            val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
            SlateDataStore(context).getWidgetConfig(appWidgetId)
                .catch { emit(SlateWidgetConfig()) }.first()
        } catch (e: Exception) {
            SlateWidgetConfig()
        }

        provideContent {
            ArcGaugeBatteryTile(percentage = data.percentage, isCharging = data.isCharging, config = config)
        }
    }
}

class ArcGaugeBatteryReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ArcGaugeBatteryWidget()
}

// 5. Editorial Stats Tile (2x2)
class EditorialStatsBatteryWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = readDetailedBatteryStatus(context)
        val config = try {
            val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
            SlateDataStore(context).getWidgetConfig(appWidgetId)
                .catch { emit(SlateWidgetConfig()) }.first()
        } catch (e: Exception) {
            SlateWidgetConfig()
        }

        provideContent {
            EditorialStatsBatteryTile(
                percentage = data.percentage,
                healthText = data.healthText,
                secondaryStatText = data.secondaryStatText,
                config = config
            )
        }
    }
}

class EditorialStatsBatteryReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EditorialStatsBatteryWidget()
}

// 6. Dot Matrix Battery LED (4x2 Wide)
class DotMatrixBatteryLEDWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = readDetailedBatteryStatus(context)
        val config = try {
            val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
            SlateDataStore(context).getWidgetConfig(appWidgetId)
                .catch { emit(SlateWidgetConfig()) }.first()
        } catch (e: Exception) {
            SlateWidgetConfig()
        }

        provideContent {
            DotMatrixBatteryLEDCard(percentage = data.percentage, isCharging = data.isCharging, config = config)
        }
    }
}

class DotMatrixBatteryLEDReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DotMatrixBatteryLEDWidget()
}

// 7. 100-Dot Level Meter Tile (2x2)
class DotLevelMeterWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = readDetailedBatteryStatus(context)
        val config = try {
            val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
            SlateDataStore(context).getWidgetConfig(appWidgetId)
                .catch { emit(SlateWidgetConfig()) }.first()
        } catch (e: Exception) {
            SlateWidgetConfig()
        }

        provideContent {
            DotLevelMeterTile(percentage = data.percentage, isCharging = data.isCharging, config = config)
        }
    }
}

class DotLevelMeterReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DotLevelMeterWidget()
}

// 8. 100-Dot Level Meter Card (4x2)
class DotLevelMeterWideWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = readDetailedBatteryStatus(context)
        val config = try {
            val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
            SlateDataStore(context).getWidgetConfig(appWidgetId)
                .catch { emit(SlateWidgetConfig()) }.first()
        } catch (e: Exception) {
            SlateWidgetConfig()
        }

        provideContent {
            DotLevelMeterCard(percentage = data.percentage, isCharging = data.isCharging, config = config)
        }
    }
}

class DotLevelMeterWideReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DotLevelMeterWideWidget()
}