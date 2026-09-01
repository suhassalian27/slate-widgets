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
import com.altusix.slate.core.theme.ThemePreferences
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
            val bgKey = "widget_${appWidgetId}_bg_color"

            // Snapshot and permanently lock the current global theme on initial placement
            if (!prefs.contains(bgKey) && appWidgetId != -1) {
                val globalSettings = ThemePreferences(context).getThemeSettings()
                val isLight = (((globalSettings.bgHex shr 16 and 0xFFL) * 0.2126f) +
                        ((globalSettings.bgHex shr 8 and 0xFFL) * 0.7152f) +
                        ((globalSettings.bgHex and 0xFFL) * 0.0722f)) / 255f > 0.5f

                prefs.edit()
                    .putString("widget_${appWidgetId}_theme_mode", if (isLight) "LIGHT" else "DARK")
                    .putLong("widget_${appWidgetId}_bg_color", globalSettings.bgHex)
                    .putLong("widget_${appWidgetId}_accent_color", globalSettings.accentHex)
                    .putFloat("widget_${appWidgetId}_opacity", globalSettings.opacity)
                    .apply()
            }

            val globalSettings = ThemePreferences(context).getThemeSettings()
            val bg = prefs.getLong("widget_${appWidgetId}_bg_color", globalSettings.bgHex)
            val opacity = prefs.getFloat("widget_${appWidgetId}_opacity", globalSettings.opacity)
            val accent = prefs.getLong("widget_${appWidgetId}_accent_color", globalSettings.accentHex)

            val isLight = (((bg shr 16 and 0xFFL) * 0.2126f) +
                    ((bg shr 8 and 0xFFL) * 0.7152f) +
                    ((bg and 0xFFL) * 0.0722f)) / 255f > 0.5f
            val mode = prefs.getString("widget_${appWidgetId}_theme_mode", if (isLight) "LIGHT" else "DARK")
                ?: if (isLight) "LIGHT" else "DARK"

            val config = SlateWidgetConfig(
                themeMode = mode,
                backgroundColorHex = bg,
                opacity = opacity,
                accentColorHex = accent
            )

            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 150) ?: 150 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 150) ?: 150
            val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 150) ?: 150 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 150) ?: 150

            val wDp = if (wDpRaw > 0) wDpRaw else 150
            val hDp = if (hDpRaw > 0) hDpRaw else 150

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