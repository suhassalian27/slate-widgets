package com.altusix.slate.core.receiver

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.RemoteViews
import com.altusix.slate.R
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.ui.config.WidgetConfigActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class BaseCanvasWidgetProvider : AppWidgetProvider() {

    abstract fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        wDp: Int,
        hDp: Int
    ): Bitmap

    open fun getClickPendingIntent(context: Context, appWidgetId: Int): PendingIntent? {
        val configIntent = Intent(context, WidgetConfigActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            appWidgetId,
            configIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
            val mode = prefs.getString("widget_${appWidgetId}_theme_mode", "DARK") ?: "DARK"

            // Dynamic defaults based on theme mode
            val defaultBg = if (mode == "LIGHT") 0xFFFFFFFFL else 0xFF161618L
            val defaultAccent = if (mode == "LIGHT") 0xFF000000L else 0xFFFFFFFFL

            val bg = prefs.getLong("widget_${appWidgetId}_bg_color", defaultBg)
            val opacity = prefs.getFloat("widget_${appWidgetId}_opacity", 1.0f)
            val accent = prefs.getLong("widget_${appWidgetId}_accent_color", defaultAccent)

            val config = SlateWidgetConfig(
                themeMode = mode,
                backgroundColorHex = bg,
                opacity = opacity,
                accentColorHex = accent
            )

            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) ?: 0
            val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) ?: 0

            val wDp = if (minWidth > 0) minWidth else 150
            val hDp = if (minHeight > 0) minHeight else 150

            val bitmap = renderWidgetBitmap(context, appWidgetId, config, wDp, hDp)

            val views = RemoteViews(context.packageName, R.layout.widget_canvas_container)
            views.setImageViewBitmap(R.id.widget_canvas_image, bitmap)

            val pi = getClickPendingIntent(context, appWidgetId)
            if (pi != null) {
                views.setOnClickPendingIntent(R.id.widget_canvas_image, pi)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}