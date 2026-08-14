package com.altusix.slate.widgets.clock.analog

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.altusix.slate.core.model.SlateWidgetInfo
import com.altusix.slate.core.service.SlateClockTickerService
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.widgets.calendar.BaseCalendarReceiver

fun getClockAnalogWidgetsCatalog(): List<SlateWidgetInfo> {
    return listOf(
        SlateWidgetInfo("Analog Precision Dial", "2x2", "Clock – Analog", ClockAnalogPrecisionReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Bauhaus Geometric Dial", "2x2", "Clock – Analog", ClockBauhausReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Cyber Skeleton Ring Dial", "2x2", "Clock – Analog", ClockCyberSkeletonReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Sculpted Pill Minimal Dial", "2x2", "Clock – Analog", ClockSculptedPillReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Bold Typographic Cardinal Dial", "2x2", "Clock – Analog", ClockBoldTypographyReceiver::class.java, hasModeOption = false),
    )
}

fun updateAllClockAnalogWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val receivers = listOf(
        ClockAnalogPrecisionReceiver::class.java,
        ClockBauhausReceiver::class.java,
        ClockCyberSkeletonReceiver::class.java,
        ClockSculptedPillReceiver::class.java,
        ClockBoldTypographyReceiver::class.java
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

// 1. ANALOG PRECISION DIAL (2x2 Square)
class ClockAnalogPrecisionReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateAnalogPrecisionClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.stopService(Intent(context, SlateClockTickerService::class.java))
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }
}

// 2. BAUHAUS GEOMETRIC DIAL (2x2 Square)
class ClockBauhausReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateBauhausClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.stopService(Intent(context, SlateClockTickerService::class.java))
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }
}

// 3. CYBER SKELETON RING DIAL (2x2 Square / Circular Skeleton Face)
class ClockCyberSkeletonReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateCyberSkeletonClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.stopService(Intent(context, SlateClockTickerService::class.java))
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }
}

// 4. SCULPTED PILL MINIMAL DIAL (2x2 Square / Ultra-Minimal Capsule Face)
class ClockSculptedPillReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateSculptedPillClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.stopService(Intent(context, SlateClockTickerService::class.java))
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }
}

// 5. BOLD TYPOGRAPHIC CARDINAL DIAL (2x2 Square)
class ClockBoldTypographyReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateBoldTypographyClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.stopService(Intent(context, SlateClockTickerService::class.java))
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }
}