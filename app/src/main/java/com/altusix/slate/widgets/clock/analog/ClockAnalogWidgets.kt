package com.altusix.slate.widgets.clock.analog

import android.content.Context
import android.content.Intent
import com.altusix.slate.widgets.calendar.BaseCalendarReceiver
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.core.model.SlateWidgetInfo
import com.altusix.slate.core.service.SlateClockTickerService

fun getClockAnalogWidgetsCatalog(): List<SlateWidgetInfo> {
    return listOf(
        SlateWidgetInfo("Analog Precision Dial", "2x2", "Clock – Analog", ClockAnalogPrecisionReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Bauhaus Geometric Dial", "2x2", "Clock – Analog", ClockBauhausReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Chrono Architect Dual Dial", "4x2", "Clock – Analog", ClockChronoArchitectReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Cyber Skeleton Ring Dial", "2x2", "Clock – Analog", ClockCyberSkeletonReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Horizon Offset Analog", "4x2", "Clock – Analog", ClockHorizonOffsetReceiver::class.java, hasModeOption = true)
    )
}

// 1. ANALOG PRECISION DIAL (2x2 Square)
class ClockAnalogPrecisionReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateAnalogPrecisionClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) { super.onEnabled(context); context.startService(Intent(context, SlateClockTickerService::class.java)) }
    override fun onDisabled(context: Context) { super.onDisabled(context); context.stopService(Intent(context, SlateClockTickerService::class.java)) }
    override fun onUpdate(context: Context, appWidgetManager: android.appwidget.AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }
}

// 2. BAUHAUS GEOMETRIC DIAL (2x2 Square)
class ClockBauhausReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateBauhausClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) { super.onEnabled(context); context.startService(Intent(context, SlateClockTickerService::class.java)) }
    override fun onDisabled(context: Context) { super.onDisabled(context); context.stopService(Intent(context, SlateClockTickerService::class.java)) }
    override fun onUpdate(context: Context, appWidgetManager: android.appwidget.AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }
}

// 3. CHRONO ARCHITECT DUAL DIAL (4x2 Dual Dial)
class ClockChronoArchitectReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateChronoArchitectClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) { super.onEnabled(context); context.startService(Intent(context, SlateClockTickerService::class.java)) }
    override fun onDisabled(context: Context) { super.onDisabled(context); context.stopService(Intent(context, SlateClockTickerService::class.java)) }
    override fun onUpdate(context: Context, appWidgetManager: android.appwidget.AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }
}

// 4. CYBER SKELETON RING DIAL (2x2 Square)
class ClockCyberSkeletonReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateCyberSkeletonClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) { super.onEnabled(context); context.startService(Intent(context, SlateClockTickerService::class.java)) }
    override fun onDisabled(context: Context) { super.onDisabled(context); context.stopService(Intent(context, SlateClockTickerService::class.java)) }
    override fun onUpdate(context: Context, appWidgetManager: android.appwidget.AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }
}

// 5. HORIZON OFFSET ANALOG (4x2 Offset)
class ClockHorizonOffsetReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateHorizonOffsetClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) { super.onEnabled(context); context.startService(Intent(context, SlateClockTickerService::class.java)) }
    override fun onDisabled(context: Context) { super.onDisabled(context); context.stopService(Intent(context, SlateClockTickerService::class.java)) }
    override fun onUpdate(context: Context, appWidgetManager: android.appwidget.AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }
}
