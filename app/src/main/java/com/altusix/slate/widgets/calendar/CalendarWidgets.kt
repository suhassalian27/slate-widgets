package com.altusix.slate.widgets.calendar

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.provider.CalendarContract
import android.widget.RemoteViews
import com.altusix.slate.R
import com.altusix.slate.core.model.SlateWidgetInfo
import com.altusix.slate.core.service.SlateClockTickerService
import com.altusix.slate.core.theme.ThemePreferences
import com.altusix.slate.data.local.SlateWidgetConfig
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun getCurrentCalendarPillState(): CalendarPillState {
    val cal = Calendar.getInstance()
    val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH).toString()
    val monthShort = SimpleDateFormat("MMM", Locale.ENGLISH).format(cal.time)
    val dayOfWeekFull = SimpleDateFormat("EEEE", Locale.ENGLISH).format(cal.time)
    return CalendarPillState(dayOfMonth, monthShort, dayOfWeekFull)
}

fun getCurrentCalendarDateState(): CalendarDateState {
    val cal = Calendar.getInstance()
    val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH).toString()
    val monthShort = SimpleDateFormat("MMM", Locale.ENGLISH).format(cal.time)
    val dayOfWeekShort = SimpleDateFormat("EEE", Locale.ENGLISH).format(cal.time)
    val year = cal.get(Calendar.YEAR).toString()
    return CalendarDateState(dayOfMonth, monthShort, dayOfWeekShort, year)
}

fun getCalendarWidgetsCatalog(): List<SlateWidgetInfo> {
    return listOf(
        SlateWidgetInfo("Capsule Calendar", "2x1", "Calendar", CalendarPill2x1Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Basic Calendar", "4x2", "Calendar", CalendarBasicReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Big Date", "2x2", "Calendar", CalendarDate2x2Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Overlay Calendar", "4x2", "Calendar", CalendarWatermarkReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Calendar Page", "2x2", "Calendar", CalendarPage2x2Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Inline Header Date", "2x2", "Calendar", CalendarInlineHeaderReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Flip Calendar", "2x2", "Calendar", CalendarSplitFlapReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Stacked Header Date", "2x2", "Calendar", CalendarStackedHeaderReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Sidebar Month Date", "2x2", "Calendar", CalendarSideBarReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Quadrant Grid Date", "2x2", "Calendar", CalendarGridQuadrantReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Diagonal Split Date", "2x2", "Calendar", CalendarDiagonalSplitReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Dashboard Calendar", "4x2", "Calendar", CalendarDashboardReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Focus Timeline Calendar", "4x2", "Calendar", CalendarFocusTimelineReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Analog Timeline Hybrid", "4x2", "Calendar", CalendarAnalogTimelineReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Week Progress Calendar", "4x2", "Calendar", CalendarWeekProgressReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Modular Matrix Calendar", "4x2", "Calendar", CalendarModularMatrixReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Overview Calendar", "4x2", "Calendar", CalendarOverviewReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Minimal Week Strip Calendar", "4x2", "Calendar", CalendarMinimalWeekStripReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Vertical Time Pill Calendar", "4x2", "Calendar", CalendarVerticalTimePillReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Timeline Progress Calendar", "4x2", "Calendar", CalendarTimelineProgressReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Page Flip Date", "2x2", "Calendar", CalendarPageFlipReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Vertical Date Wheel", "2x2", "Calendar", CalendarVerticalWheelReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Month Progress Capsule", "2x2", "Calendar", CalendarMonthProgressCapsuleReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Timeline Pillars Date", "2x2", "Calendar", CalendarTimelinePillarsReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Tilted Badge Flip Date", "2x2", "Calendar", CalendarTiltedBadgeFlipReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Solar Landscape Date", "2x2", "Calendar", CalendarSolarLandscapeReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Year Matrix Progress", "4x2", "Calendar", CalendarYearMatrixReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Analog Month Dashboard", "4x2", "Calendar", CalendarAnalogCalendarHybridReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Architectural Analog Dashboard", "4x2", "Calendar", CalendarArchitecturalAnalogReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Radial Arc Orbital Dashboard", "4x2", "Calendar", CalendarRadialArcReceiver::class.java, hasModeOption = true)
    )
}

private fun loadSlateWidgetConfig(context: Context, widgetId: Int): SlateWidgetConfig {
    val widgetPrefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
    val bgKey = "widget_${widgetId}_bg_color"

    // Snapshot and permanently lock the current global theme on initial placement
    if (!widgetPrefs.contains(bgKey) && widgetId != -1) {
        val globalSettings = ThemePreferences(context).getThemeSettings()
        val isLight = (((globalSettings.bgHex shr 16 and 0xFFL) * 0.2126f) +
                ((globalSettings.bgHex shr 8 and 0xFFL) * 0.7152f) +
                ((globalSettings.bgHex and 0xFFL) * 0.0722f)) / 255f > 0.5f

        widgetPrefs.edit()
            .putString("widget_${widgetId}_theme_mode", if (isLight) "LIGHT" else "DARK")
            .putLong("widget_${widgetId}_bg_color", globalSettings.bgHex)
            .putLong("widget_${widgetId}_accent_color", globalSettings.accentHex)
            .putFloat("widget_${widgetId}_opacity", globalSettings.opacity)
            .apply()
    }

    val globalSettings = ThemePreferences(context).getThemeSettings()
    val bgColor = widgetPrefs.getLong("widget_${widgetId}_bg_color", globalSettings.bgHex)
    val opacity = widgetPrefs.getFloat("widget_${widgetId}_opacity", globalSettings.opacity)
    val accentColor = widgetPrefs.getLong("widget_${widgetId}_accent_color", globalSettings.accentHex)

    val isLight = (((bgColor shr 16 and 0xFFL) * 0.2126f) +
            ((bgColor shr 8 and 0xFFL) * 0.7152f) +
            ((bgColor and 0xFFL) * 0.0722f)) / 255f > 0.5f
    val mode = widgetPrefs.getString("widget_${widgetId}_theme_mode", if (isLight) "LIGHT" else "DARK")
        ?: if (isLight) "LIGHT" else "DARK"

    return SlateWidgetConfig(
        themeMode = mode,
        backgroundColorHex = bgColor,
        opacity = opacity,
        accentColorHex = accentColor
    )
}

private fun parseAndLockIsResponsive(context: Context, widgetId: Int): Boolean {
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

fun updateAllCalendarWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val receivers = listOf(
        CalendarPill2x1Receiver::class.java,
        CalendarBasicReceiver::class.java,
        CalendarDate2x2Receiver::class.java,
        CalendarWatermarkReceiver::class.java,
        CalendarPage2x2Receiver::class.java,
        CalendarInlineHeaderReceiver::class.java,
        CalendarSplitFlapReceiver::class.java,
        CalendarStackedHeaderReceiver::class.java,
        CalendarSideBarReceiver::class.java,
        CalendarGridQuadrantReceiver::class.java,
        CalendarDiagonalSplitReceiver::class.java,
        CalendarDashboardReceiver::class.java,
        CalendarFocusTimelineReceiver::class.java,
        CalendarAnalogTimelineReceiver::class.java,
        CalendarWeekProgressReceiver::class.java,
        CalendarModularMatrixReceiver::class.java,
        CalendarOverviewReceiver::class.java,
        CalendarMinimalWeekStripReceiver::class.java,
        CalendarVerticalTimePillReceiver::class.java,
        CalendarTimelineProgressReceiver::class.java,
        CalendarPageFlipReceiver::class.java,
        CalendarVerticalWheelReceiver::class.java,
        CalendarMonthProgressCapsuleReceiver::class.java,
        CalendarTimelinePillarsReceiver::class.java,
        CalendarTiltedBadgeFlipReceiver::class.java,
        CalendarSolarLandscapeReceiver::class.java,
        CalendarYearMatrixReceiver::class.java,
        CalendarAnalogCalendarHybridReceiver::class.java,
        CalendarArchitecturalAnalogReceiver::class.java,
        CalendarRadialArcReceiver::class.java
    )

    for (receiver in receivers) {
        val ids = manager.getAppWidgetIds(ComponentName(context, receiver))
        if (ids != null && ids.isNotEmpty()) {
            val intent = Intent(context, receiver).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}

abstract class BaseCalendarReceiver : AppWidgetProvider() {

    abstract fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        updateAllCalendarWidgets(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, this::class.java))
        if (ids != null && ids.isNotEmpty()) {
            onUpdate(context, manager, ids)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            updateSingleWidget(context, appWidgetManager, id)
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: android.os.Bundle?) {
        updateSingleWidget(context, appWidgetManager, appWidgetId)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    private fun updateSingleWidget(context: Context, manager: AppWidgetManager, id: Int) {
        try {
            val config = loadSlateWidgetConfig(context, id)
            val isResponsive = parseAndLockIsResponsive(context, id)

            val options = manager.getAppWidgetOptions(id)
            val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 140) ?: 140 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 140) ?: 140
            val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 60) ?: 60 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 60) ?: 60

            val wDp = if (wDpRaw <= 0) 140 else wDpRaw
            val hDp = if (hDpRaw <= 0) 60 else hDpRaw

            val bitmap = renderBitmap(context, config, isResponsive, wDp, hDp)
            val views = RemoteViews(context.packageName, R.layout.widget_canvas_container)

            views.setImageViewBitmap(R.id.widget_canvas_image, bitmap)

            val calendarIntent = Intent(Intent.ACTION_VIEW).apply {
                data = CalendarContract.CONTENT_URI
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context, id, calendarIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_canvas_image, pendingIntent)

            manager.updateAppWidget(id, views)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// 1. CAPSULE PILL (2x1)
class CalendarPill2x1Receiver : BaseCalendarReceiver() { override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap = generatePillCalendarBitmap(context, getCurrentCalendarPillState(), config, isResponsive, wDp, hDp) }

// 2. BASIC CALENDAR (4x2)
class CalendarBasicReceiver : BaseCalendarReceiver() { override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap = generateBasicCalendarBitmap(context, getCurrentCalendarDateState(), config, isResponsive, wDp, hDp) }

class CalendarDate2x2Receiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateBigDateBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 4. MONTH OVERLAY CALENDAR (4x2)
class CalendarWatermarkReceiver : BaseCalendarReceiver() { override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap = generateWatermarkCalendarBitmap(context, getCurrentCalendarDateState(), config, isResponsive, wDp, hDp) }

class CalendarPage2x2Receiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateCalendarPageBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 6. INLINE HEADER DATE (2x2 Square / Responsive Single Card)
class CalendarInlineHeaderReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateInlineHeaderDateBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 7. FLIP CALENDAR (2x2 Square / Responsive Single Card)
class CalendarSplitFlapReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateSplitFlapCalendarBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 8. STACKED HEADER DATE (2x2 Square / Responsive Single Card)
class CalendarStackedHeaderReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateStackedHeaderDateBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 9. SIDEBAR MONTH DATE (2x2 Square / Responsive Single Card)
class CalendarSideBarReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateSideBarDateBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 10. QUADRANT GRID DATE (2x2 Square / Responsive Single Card)
class CalendarGridQuadrantReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateGridQuadrantCalendarBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 11. DIAGONAL SPLIT DATE (2x2 Square / Responsive Single Card)
class CalendarDiagonalSplitReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateDiagonalSplitDateBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 12. SPLIT DASHBOARD CALENDAR (4x2)
class CalendarDashboardReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap = generateSplitDashboardCalendarBitmap(context, getCurrentCalendarDateState(), config, isResponsive, wDp, hDp) }

// 13. FOCUS TIMELINE CALENDAR (4x2)
class CalendarFocusTimelineReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap = generateFocusTimelineCalendarBitmap(context, getCurrentCalendarDateState(), config, isResponsive, wDp, hDp) }

// 14. ANALOG TIMELINE HYBRID (4x2)
class CalendarAnalogTimelineReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap = generateAnalogTimelineCalendarBitmap(context, getCurrentCalendarDateState(), config, isResponsive, wDp, hDp) }

// 15. WEEK PROGRESS CALENDAR (4x2 / Capsule Progress Tracker)
class CalendarWeekProgressReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateWeekProgressCalendarBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 16. MODULAR MATRIX CALENDAR (4x2 / Bento Day Grid)
class CalendarModularMatrixReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateModularMatrixCalendarBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 17. ELEGANT OVERVIEW CALENDAR (4x2 / Giant Date & Month Grid)
class CalendarOverviewReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateOverviewCalendarBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 18. MINIMAL WEEK STRIP CALENDAR (4x2 / Date & Underlined Day Strip)
class CalendarMinimalWeekStripReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateMinimalWeekStripCalendarBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 19. VERTICAL TIME PILL WIDGET (4x2 / Rotated Clock & Dual Capsule Stack)
class CalendarVerticalTimePillReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateVerticalTimePillCalendarBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 20. TIMELINE PROGRESS CALENDAR (4x2 / Minimal Horizontal Axis)
class CalendarTimelineProgressReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateTimelineProgressCalendarBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 21. CORNER BADGE DATE (2x2 Square / Responsive Single Card)
class CalendarPageFlipReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generatePageFlipDateBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 22. VERTICAL DATE WHEEL (2x2 Square / Responsive Single Card)
class CalendarVerticalWheelReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateVerticalDateWheelBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 23. MONTH PROGRESS CAPSULE (2x2 Square / Responsive Single Card)
class CalendarMonthProgressCapsuleReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateMonthProgressCapsuleBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 24. TIMELINE PILLARS DATE (2x2 Square / Responsive Single Card)
class CalendarTimelinePillarsReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateTimelinePillarsBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 25. TILTED BADGE FLIP DATE (2x2 Square / Responsive Single Card)
class CalendarTiltedBadgeFlipReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateTiltedBadgeFlipDateBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 26. SOLAR LANDSCAPE DATE (2x2 Square / Responsive Single Card)
class CalendarSolarLandscapeReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateSolarLandscapeDateBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 27. YEAR MATRIX PROGRESS (4x2 / Year Dot Matrix & Status Header)
class CalendarYearMatrixReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateYearMatrixProgressBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 28. ANALOG MONTH DASHBOARD (4x2 / Precision Clock & Calendar Grid)
class CalendarAnalogCalendarHybridReceiver : BaseCalendarReceiver() {

    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateAnalogCalendarHybridBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)

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

// 29. ARCHITECTURAL ANALOG DASHBOARD (4x2)
class CalendarArchitecturalAnalogReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap = generateArchitecturalAnalogReceiverBitmap(context, getCurrentCalendarDateState(), config, isResponsive, wDp, hDp) }

// 30. RADIAL ARC ORBITAL DASHBOARD (4x2 / Concentric Time Arcs & Life Progress)
class CalendarRadialArcReceiver : BaseCalendarReceiver() {

    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateRadialArcDashboardBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)

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