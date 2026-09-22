package com.altusix.slate.widgets.calendar

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.CalendarContract
import android.widget.RemoteViews
import com.altusix.slate.R
import com.altusix.slate.core.model.SlateWidgetInfo
import com.altusix.slate.core.service.SlateClockTickerService
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.data.local.loadSlateWidgetConfig
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
        SlateWidgetInfo("Big Date", "2x2", "Calendar", CalendarDate2x2Receiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Basic Calendar", "4x2", "Calendar", CalendarBasicReceiver::class.java, hasModeOption = true),
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

fun updateAllCalendarWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context) ?: return
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

    for (receiverClass in receivers) {
        val ids = manager.getAppWidgetIds(ComponentName(context, receiverClass))
        if (ids != null && ids.isNotEmpty()) {
            val receiver = receiverClass.getDeclaredConstructor().newInstance()
            for (id in ids) {
                receiver.updateSingleWidget(context, manager, id)
            }
        }
    }
}

abstract class BaseCalendarReceiver(
    open val targetAspect: Float = 1.0f
) : AppWidgetProvider() {

    abstract fun renderBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap

    fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap = renderBitmap(context, appWidgetId, config, isResponsive, wDp, hDp)

    fun renderBitmapForWidget(
        context: Context,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int,
        widgetId: Int
    ): Bitmap = renderBitmap(context, widgetId, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        updateAllCalendarWidgets(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val manager = AppWidgetManager.getInstance(context) ?: return
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

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateSingleWidget(context, appWidgetManager, appWidgetId)
    }

    fun updateSingleWidget(context: Context, manager: AppWidgetManager, id: Int) {
        try {
            val config = loadSlateWidgetConfig(context, id)
            val isResponsive = if (id == -1) true else parseAndLockIsResponsive(context, id)

            val options = manager.getAppWidgetOptions(id)
            val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val fallbackSize = if (targetAspect >= 1.8f) (if (targetAspect >= 2.5f) 280 to 70 else 280 to 140) else 150 to 150

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

            val bitmap = renderBitmap(context, id, config, isResponsive, effWDp, effHDp)
            val views = RemoteViews(context.packageName, R.layout.widget_base_single)
            views.setViewPadding(R.id.layout_grid_root, padH, padV, padH, padV)
            views.setImageViewBitmap(R.id.widget_image_view, bitmap)

            val calendarIntent = Intent(Intent.ACTION_VIEW).apply {
                data = CalendarContract.CONTENT_URI
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context, id, calendarIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.touch_slot_0, pendingIntent)

            manager.updateAppWidget(id, views)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// 1. CAPSULE PILL (2x1)
class CalendarPill2x1Receiver : BaseCalendarReceiver(targetAspect = 2.0f) {
    override fun renderBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap =
        generatePillCalendarBitmap(context, getCurrentCalendarPillState(), config, isResponsive, wDp, hDp)
}

// 2. BASIC CALENDAR (4x2)
class CalendarBasicReceiver : BaseCalendarReceiver(targetAspect = 2.0f) {
    override fun renderBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap =
        generateBasicCalendarBitmap(context, getCurrentCalendarDateState(), config, isResponsive, wDp, hDp)
}

// 3. BIG DATE (2x2 Fixed Receiver)
class CalendarDate2x2Receiver : BaseCalendarReceiver(targetAspect = 1.0f) {
    override fun renderBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap = generateBigDateBitmap(context, CalendarEngine.getDateState(), config, wDp, hDp)
}

// 4. MONTH OVERLAY CALENDAR (4x2)
class CalendarWatermarkReceiver : BaseCalendarReceiver(targetAspect = 2.0f) {
    override fun renderBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap =
        generateWatermarkCalendarBitmap(context, getCurrentCalendarDateState(), config, isResponsive, wDp, hDp)
}

// 5. CALENDAR PAGE (2x2)
class CalendarPage2x2Receiver : BaseCalendarReceiver(targetAspect = 1.0f) {
    override fun renderBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap =
        generateCalendarPageBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 6. INLINE HEADER DATE (2x2)
class CalendarInlineHeaderReceiver : BaseCalendarReceiver(targetAspect = 1.0f) {
    override fun renderBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap =
        generateInlineHeaderDateBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 7. FLIP CALENDAR (2x2)
class CalendarSplitFlapReceiver : BaseCalendarReceiver(targetAspect = 1.0f) {
    override fun renderBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap =
        generateSplitFlapCalendarBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 8. STACKED HEADER DATE (2x2)
class CalendarStackedHeaderReceiver : BaseCalendarReceiver(targetAspect = 1.0f) {
    override fun renderBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap =
        generateStackedHeaderDateBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 9. SIDEBAR MONTH DATE (2x2)
class CalendarSideBarReceiver : BaseCalendarReceiver(targetAspect = 1.0f) {
    override fun renderBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap =
        generateSideBarDateBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 10. QUADRANT GRID DATE (2x2)
class CalendarGridQuadrantReceiver : BaseCalendarReceiver(targetAspect = 1.0f) {
    override fun renderBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap =
        generateGridQuadrantCalendarBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 11. DIAGONAL SPLIT DATE (2x2)
class CalendarDiagonalSplitReceiver : BaseCalendarReceiver(targetAspect = 1.0f) {
    override fun renderBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap =
        generateDiagonalSplitDateBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 12. SPLIT DASHBOARD CALENDAR (4x2)
class CalendarDashboardReceiver : BaseCalendarReceiver(targetAspect = 2.0f) {
    override fun renderBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap =
        generateSplitDashboardCalendarBitmap(context, getCurrentCalendarDateState(), config, isResponsive, wDp, hDp)
}

// 13. FOCUS TIMELINE CALENDAR (4x2)
class CalendarFocusTimelineReceiver : BaseCalendarReceiver(targetAspect = 2.0f) {
    override fun renderBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap =
        generateFocusTimelineCalendarBitmap(context, getCurrentCalendarDateState(), config, isResponsive, wDp, hDp)
}

// 14. ANALOG TIMELINE HYBRID (4x2)
class CalendarAnalogTimelineReceiver : BaseCalendarReceiver(targetAspect = 2.0f) {
    override fun renderBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap =
        generateAnalogTimelineCalendarBitmap(context, getCurrentCalendarDateState(), config, isResponsive, wDp, hDp)
}

// 15. WEEK PROGRESS CALENDAR (4x2)
class CalendarWeekProgressReceiver : BaseCalendarReceiver(targetAspect = 2.0f) {
    override fun renderBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap =
        generateWeekProgressCalendarBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 16. MODULAR MATRIX CALENDAR (4x2)
class CalendarModularMatrixReceiver : BaseCalendarReceiver(targetAspect = 2.0f) {
    override fun renderBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap =
        generateModularMatrixCalendarBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 17. ELEGANT OVERVIEW CALENDAR (4x2)
class CalendarOverviewReceiver : BaseCalendarReceiver(targetAspect = 2.0f) {
    override fun renderBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap =
        generateOverviewCalendarBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 18. MINIMAL WEEK STRIP CALENDAR (4x2)
class CalendarMinimalWeekStripReceiver : BaseCalendarReceiver(targetAspect = 2.0f) {
    override fun renderBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap =
        generateMinimalWeekStripCalendarBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 19. VERTICAL TIME PILL WIDGET (4x2)
class CalendarVerticalTimePillReceiver : BaseCalendarReceiver(targetAspect = 2.0f) {
    override fun renderBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap =
        generateVerticalTimePillCalendarBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 20. TIMELINE PROGRESS CALENDAR (4x2)
class CalendarTimelineProgressReceiver : BaseCalendarReceiver(targetAspect = 2.0f) {
    override fun renderBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap =
        generateTimelineProgressCalendarBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 21. CORNER BADGE DATE (2x2)
class CalendarPageFlipReceiver : BaseCalendarReceiver(targetAspect = 1.0f) {
    override fun renderBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap =
        generatePageFlipDateBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 22. VERTICAL DATE WHEEL (2x2)
class CalendarVerticalWheelReceiver : BaseCalendarReceiver(targetAspect = 1.0f) {
    override fun renderBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap =
        generateVerticalDateWheelBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 23. MONTH PROGRESS CAPSULE (2x2)
class CalendarMonthProgressCapsuleReceiver : BaseCalendarReceiver(targetAspect = 1.0f) {
    override fun renderBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap =
        generateMonthProgressCapsuleBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 24. TIMELINE PILLARS DATE (2x2)
class CalendarTimelinePillarsReceiver : BaseCalendarReceiver(targetAspect = 1.0f) {
    override fun renderBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap =
        generateTimelinePillarsBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 25. TILTED BADGE FLIP DATE (2x2)
class CalendarTiltedBadgeFlipReceiver : BaseCalendarReceiver(targetAspect = 1.0f) {
    override fun renderBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap =
        generateTiltedBadgeFlipDateBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 26. SOLAR LANDSCAPE DATE (2x2)
class CalendarSolarLandscapeReceiver : BaseCalendarReceiver(targetAspect = 1.0f) {
    override fun renderBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap =
        generateSolarLandscapeDateBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 27. YEAR MATRIX PROGRESS (4x2)
class CalendarYearMatrixReceiver : BaseCalendarReceiver(targetAspect = 2.0f) {
    override fun renderBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap =
        generateYearMatrixProgressBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)
}

// 28. ANALOG MONTH DASHBOARD (4x2)
class CalendarAnalogCalendarHybridReceiver : BaseCalendarReceiver(targetAspect = 2.0f) {
    override fun renderBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap =
        generateAnalogCalendarHybridBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        try {
            context.startService(Intent(context, SlateClockTickerService::class.java))
        } catch (_: Exception) {}
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        try {
            context.stopService(Intent(context, SlateClockTickerService::class.java))
        } catch (_: Exception) {}
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        try {
            context.startService(Intent(context, SlateClockTickerService::class.java))
        } catch (_: Exception) {}
    }
}

// 29. ARCHITECTURAL ANALOG DASHBOARD (4x2)
class CalendarArchitecturalAnalogReceiver : BaseCalendarReceiver(targetAspect = 2.0f) {
    override fun renderBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap =
        generateArchitecturalAnalogReceiverBitmap(context, getCurrentCalendarDateState(), config, isResponsive, wDp, hDp)
}

// 30. RADIAL ARC ORBITAL DASHBOARD (4x2)
class CalendarRadialArcReceiver : BaseCalendarReceiver(targetAspect = 2.0f) {
    override fun renderBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap =
        generateRadialArcDashboardBitmap(context, CalendarEngine.getDateState(), config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        try {
            context.startService(Intent(context, SlateClockTickerService::class.java))
        } catch (_: Exception) {}
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        try {
            context.stopService(Intent(context, SlateClockTickerService::class.java))
        } catch (_: Exception) {}
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        try {
            context.startService(Intent(context, SlateClockTickerService::class.java))
        } catch (_: Exception) {}
    }
}