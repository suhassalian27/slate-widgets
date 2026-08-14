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
import com.altusix.slate.data.local.SlateWidgetConfig

fun getCalendarWidgetsCatalog(): List<SlateWidgetInfo> {
    return listOf(
        SlateWidgetInfo("Capsule Calendar", "2x1", "Calendar", CalendarPillReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Basic Calendar", "4x2", "Calendar", CalendarBasicReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Big Date", "2x2", "Calendar", CalendarDate2x2Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Overlay Calendar", "4x2", "Calendar", CalendarWatermarkReceiver::class.java, hasModeOption = false),
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

fun updateAllCalendarWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val receivers = listOf(
        CalendarPillReceiver::class.java,
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
            val widgetPrefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
            val calPrefs = context.getSharedPreferences("slate_calendar_prefs", Context.MODE_PRIVATE)

            val themeMode = widgetPrefs.getString("widget_${id}_theme_mode", "DARK") ?: "DARK"
            val defaultBg = if (themeMode == "LIGHT") 0xFFFFFFFFL else 0xFF161618L
            val defaultAccent = if (themeMode == "LIGHT") 0xFF000000L else 0xFFFFFFFFL

            val bgColor = widgetPrefs.getLong("widget_${id}_bg_color", defaultBg)
            val opacity = widgetPrefs.getFloat("widget_${id}_opacity", 1.0f)
            val accentColor = widgetPrefs.getLong("widget_${id}_accent_color", defaultAccent)

            val config = SlateWidgetConfig(themeMode, bgColor, opacity, accentColor)

            // STRICT PER-INSTANCE ISOLATION BY WIDGET ID
            val keyCalResponsive = "calendar_${id}_is_responsive"
            val keyWMode = "widget_${id}_mode"
            val keyWResponsive = "widget_${id}_is_responsive"

            val isResponsive = when {
                widgetPrefs.contains(keyWMode) -> {
                    widgetPrefs.getString(keyWMode, "RESPONSIVE").equals("RESPONSIVE", ignoreCase = true)
                }
                calPrefs.contains(keyCalResponsive) -> {
                    calPrefs.getBoolean(keyCalResponsive, true)
                }
                widgetPrefs.contains(keyWResponsive) -> {
                    widgetPrefs.getBoolean(keyWResponsive, true)
                }
                else -> {
                    // Fallback to launcher default ONLY on initial creation and permanently lock it
                    val defaultResponsive = context.getSharedPreferences("slate_app_launcher_prefs", Context.MODE_PRIVATE)
                        .getBoolean("default_is_responsive", true)
                    calPrefs.edit().putBoolean(keyCalResponsive, defaultResponsive).apply()
                    defaultResponsive
                }
            }

            val options = manager.getAppWidgetOptions(id)
            val minW = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) ?: 0
            val minH = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) ?: 0
            val maxW = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH) ?: 0
            val maxH = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT) ?: 0

            val wDp = maxOf(minW, maxW, 140)
            val hDp = maxOf(minH, maxH, 60)

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

class CalendarPillReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generatePillCalendarBitmap(context, CalendarEngine.getPillState(), config, wDp, hDp)
}

class CalendarBasicReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateBasicCalendarBitmap(context, CalendarEngine.getDateState(), config, wDp, hDp)
}

class CalendarDate2x2Receiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateBigDateBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

class CalendarWatermarkReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateWatermarkCalendarBitmap(context, CalendarEngine.getDateState(), config, wDp, hDp)
}

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

// 12. SPLIT DASHBOARD CALENDAR (4x2 / Split Date & Month Grid)
class CalendarDashboardReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateSplitDashboardCalendarBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 13. FOCUS TIMELINE CALENDAR (4x2 / Split Dual-Block Timeline)
class CalendarFocusTimelineReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateFocusTimelineCalendarBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 14. ANALOG TIMELINE HYBRID (4x2 / Clock & Date Pill Strip)
class CalendarAnalogTimelineReceiver : BaseCalendarReceiver() {

    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateAnalogTimelineCalendarBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)

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
        // Ensure ticker service is alive when widgets update
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }
}

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
        // Ensure ticker service is alive when widgets update
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }
}

// 29. ARCHITECTURAL ANALOG DASHBOARD (4x2 / Sculpted Dial & Day Progress)
class CalendarArchitecturalAnalogReceiver : BaseCalendarReceiver() {

    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateArchitecturalAnalogDashboardBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)

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