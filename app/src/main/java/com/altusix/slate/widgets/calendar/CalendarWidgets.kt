package com.altusix.slate.widgets.calendar

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.RemoteViews
import com.altusix.slate.R
import com.altusix.slate.core.model.SlateWidgetInfo
import com.altusix.slate.data.local.SlateWidgetConfig

fun getCalendarWidgetsCatalog(): List<SlateWidgetInfo> {
    return listOf(
        SlateWidgetInfo("Capsule Calendar", "2x1", "Calendar", CalendarPillReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Month Grid", "2x2", "Calendar", CalendarMonth2x2Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Big Date", "2x2", "Calendar", CalendarDate2x2Receiver::class.java, hasModeOption = true)
    )
}

fun updateAllCalendarWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val receivers = listOf(
        CalendarPillReceiver::class.java,
        CalendarMonth2x2Receiver::class.java,
        CalendarDate2x2Receiver::class.java
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

    abstract fun renderBitmap(context: Context, config: SlateWidgetConfig, wDp: Int, hDp: Int): android.graphics.Bitmap

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
            val themeMode = widgetPrefs.getString("widget_${id}_theme_mode", "DARK") ?: "DARK"

            val defaultBg = if (themeMode == "LIGHT") 0xFFFFFFFFL else 0xFF161618L
            val defaultAccent = if (themeMode == "LIGHT") 0xFF000000L else 0xFFFFFFFFL

            val bgColor = widgetPrefs.getLong("widget_${id}_bg_color", defaultBg)
            val opacity = widgetPrefs.getFloat("widget_${id}_opacity", 1.0f)
            val accentColor = widgetPrefs.getLong("widget_${id}_accent_color", defaultAccent)

            val config = SlateWidgetConfig(themeMode, bgColor, opacity, accentColor)

            val options = manager.getAppWidgetOptions(id)
            val minW = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) ?: 0
            val minH = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) ?: 0
            val maxW = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH) ?: 0
            val maxH = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT) ?: 0

            val wDp = maxOf(minW, maxW, 140)
            val hDp = maxOf(minH, maxH, 60)

            val bitmap = renderBitmap(context, config, wDp, hDp)
            val views = RemoteViews(context.packageName, R.layout.widget_canvas_container)

            // Fixed: Matched ID with widget_canvas_container.xml (widget_canvas_image)
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
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, wDp: Int, hDp: Int) =
        generatePillCalendarBitmap(context, CalendarEngine.getPillState(), config, wDp, hDp)
}

class CalendarMonth2x2Receiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, wDp: Int, hDp: Int) =
        generateMonthGrid2x2Bitmap(context, CalendarEngine.getMonthGridState(), config, wDp, hDp)
}

class CalendarDate2x2Receiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, wDp: Int, hDp: Int) =
        generateDate2x2Bitmap(context, CalendarEngine.getDateState(), config, wDp, hDp)
}