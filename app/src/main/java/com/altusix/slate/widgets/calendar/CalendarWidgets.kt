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
        SlateWidgetInfo("Stacked Header Date", "2x2", "Calendar", CalendarStackedHeaderReceiver::class.java, hasModeOption = true)
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
        CalendarStackedHeaderReceiver::class.java
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