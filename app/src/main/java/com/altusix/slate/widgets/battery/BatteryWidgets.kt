package com.altusix.slate.widgets.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import com.altusix.slate.data.local.SlateDataStore
import com.altusix.slate.data.local.SlateWidgetConfig
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first

private fun readBatteryStatus(context: Context): Pair<Int, Boolean> {
    return try {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 0
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

        val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else 0
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        Pair(pct, isCharging)
    } catch (e: Exception) {
        Pair(85, false)
    }
}

// 1. Minimal 2x2 Tile
class MinimalBatteryWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (batteryPct, isCharging) = readBatteryStatus(context)
        val config = try {
            val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
            SlateDataStore(context).getWidgetConfig(appWidgetId)
                .catch { emit(SlateWidgetConfig()) }.first()
        } catch (e: Exception) {
            SlateWidgetConfig()
        }

        provideContent {
            MinimalBatteryTile(percentage = batteryPct, isCharging = isCharging, config = config)
        }
    }
}

class MinimalBatteryReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MinimalBatteryWidget()
}

// 2. Multi-Device Card (4x2)
class MultiDeviceBatteryWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (batteryPct, _) = readBatteryStatus(context)
        val config = try {
            val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
            SlateDataStore(context).getWidgetConfig(appWidgetId)
                .catch { emit(SlateWidgetConfig()) }.first()
        } catch (e: Exception) {
            SlateWidgetConfig()
        }

        provideContent {
            MultiDeviceBatteryCard(phonePct = batteryPct, config = config)
        }
    }
}

class MultiDeviceBatteryReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MultiDeviceBatteryWidget()
}

// 3. Horizontal Strip (4x1)
class HorizontalBatteryWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (batteryPct, isCharging) = readBatteryStatus(context)
        val config = try {
            val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
            SlateDataStore(context).getWidgetConfig(appWidgetId)
                .catch { emit(SlateWidgetConfig()) }.first()
        } catch (e: Exception) {
            SlateWidgetConfig()
        }

        provideContent {
            HorizontalBatteryStrip(percentage = batteryPct, isCharging = isCharging, config = config)
        }
    }
}

class HorizontalBatteryReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HorizontalBatteryWidget()
}

// 4. Arc Gauge Tile (2x2)
class ArcGaugeBatteryWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (batteryPct, isCharging) = readBatteryStatus(context)
        val config = try {
            val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
            SlateDataStore(context).getWidgetConfig(appWidgetId)
                .catch { emit(SlateWidgetConfig()) }.first()
        } catch (e: Exception) {
            SlateWidgetConfig()
        }

        provideContent {
            ArcGaugeBatteryTile(percentage = batteryPct, isCharging = isCharging, config = config)
        }
    }
}

class ArcGaugeBatteryReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ArcGaugeBatteryWidget()
}