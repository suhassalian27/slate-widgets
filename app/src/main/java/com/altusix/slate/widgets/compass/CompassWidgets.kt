package com.altusix.slate.widgets.compass

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.RemoteViews
import com.altusix.slate.R
import com.altusix.slate.core.model.SlateWidgetInfo
import com.altusix.slate.core.theme.ThemePreferences
import com.altusix.slate.data.local.SlateWidgetConfig

const val ACTION_TOGGLE_COMPASS = "com.altusix.slate.ACTION_TOGGLE_COMPASS"

fun getCompassWidgetsCatalog(): List<SlateWidgetInfo> {
    return listOf(
        SlateWidgetInfo(name = "Orbital Pulse Compass", sizeText = "2x2", category = "Compass", receiverClass = CompassDotMatrixReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Tactical Radar Compass", sizeText = "2x2", category = "Compass", receiverClass = CompassTacticalRadarReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Minimalist Bezel Compass", sizeText = "2x2", category = "Compass", receiverClass = CompassMinimalistBezelReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Horizontal Pill Compass", sizeText = "2x1", category = "Compass", receiverClass = CompassHorizontalPillReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Pure Circle Compass", sizeText = "2x2", category = "Compass", receiverClass = CompassPureCircleReceiver::class.java, hasModeOption = false)
    )
}

private fun parseAndLockIsResponsive(context: Context, widgetId: Int): Boolean {
    val widgetPrefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
    val key = "widget_${widgetId}_is_responsive"
    if (widgetPrefs.contains(key)) {
        return widgetPrefs.getBoolean(key, true)
    }
    val launcherPrefs = context.getSharedPreferences("slate_app_launcher_prefs", Context.MODE_PRIVATE)
    val defaultResp = launcherPrefs.getBoolean("default_is_responsive", true)
    widgetPrefs.edit().putBoolean(key, defaultResp).apply()
    return defaultResp
}

private fun loadSlateWidgetConfig(context: Context, widgetId: Int): SlateWidgetConfig {
    val widgetPrefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
    val bgKey = "widget_${widgetId}_bg_color"

    // Snapshot and lock current global theme when the widget is first created
    if (!widgetPrefs.contains(bgKey) && widgetId != -1) {
        val globalSettings = ThemePreferences(context).getThemeSettings()
        widgetPrefs.edit()
            .putLong("widget_${widgetId}_bg_color", globalSettings.bgHex)
            .putLong("widget_${widgetId}_accent_color", globalSettings.accentHex)
            .putFloat("widget_${widgetId}_opacity", globalSettings.opacity)
            .apply()
    }

    val globalSettings = ThemePreferences(context).getThemeSettings()
    val bgColor = widgetPrefs.getLong("widget_${widgetId}_bg_color", globalSettings.bgHex)
    val opacity = widgetPrefs.getFloat("widget_${widgetId}_opacity", globalSettings.opacity)
    val accentColor = widgetPrefs.getLong("widget_${widgetId}_accent_color", globalSettings.accentHex)

    val isLight = (((bgColor shr 16 and 0xFFL) * 0.2126f) + ((bgColor shr 8 and 0xFFL) * 0.7152f) + ((bgColor and 0xFFL) * 0.0722f)) / 255f > 0.5f

    return SlateWidgetConfig(
        themeMode = if (isLight) "LIGHT" else "DARK",
        backgroundColorHex = bgColor,
        opacity = opacity,
        accentColorHex = accentColor
    )
}

abstract class BaseCompassReceiver : android.appwidget.AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_TOGGLE_COMPASS) {
            val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val prefs = context.getSharedPreferences("slate_compass_prefs", Context.MODE_PRIVATE)
                val activeUntil = prefs.getLong("widget_${widgetId}_active_until", 0L)

                if (System.currentTimeMillis() < activeUntil) {
                    prefs.edit().putLong("widget_${widgetId}_active_until", 0L).commit()
                } else {
                    prefs.edit().putLong("widget_${widgetId}_active_until", System.currentTimeMillis() + 60_000L).commit()
                    SlateCompassSensorService.ensureStarted(context)
                }
                updateAllCompassWidgets(context)
            }
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int, newOptions: Bundle?) {
        updateWidget(context, appWidgetManager, widgetId)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, widgetId, newOptions)
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 200) ?: 200
        val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 200) ?: 200
        val wDp = if (wDpRaw <= 0) 200 else wDpRaw
        val hDp = if (hDpRaw <= 0) 200 else hDpRaw

        val isResponsive = parseAndLockIsResponsive(context, widgetId)
        val config = loadSlateWidgetConfig(context, widgetId)

        val bitmap = renderBitmapForWidget(context, config, isResponsive, wDp, hDp, widgetId)

        val views = RemoteViews(context.packageName, R.layout.widget_image_container)
        views.setImageViewBitmap(R.id.widget_image_view, bitmap)

        val toggleIntent = Intent(context, this::class.java).apply {
            action = ACTION_TOGGLE_COMPASS
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, widgetId, toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_image_view, pendingIntent)

        appWidgetManager.updateAppWidget(widgetId, views)
    }

    abstract fun renderBitmapForWidget(
        context: Context,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int,
        widgetId: Int
    ): Bitmap
}

fun updateAllCompassWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val receivers = listOf(
        CompassDotMatrixReceiver::class.java,
        CompassTacticalRadarReceiver::class.java,
        CompassMinimalistBezelReceiver::class.java,
        CompassHorizontalPillReceiver::class.java,
        CompassPureCircleReceiver::class.java
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

// 1. ORBITAL PULSE COMPASS (2x2 / Minimal Floating Node Compass)
class CompassDotMatrixReceiver : BaseCompassReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateDotMatrixCompassBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 2. TACTICAL RADAR COMPASS (2x2 / Tap-Activated Tactical Radar)
class CompassTacticalRadarReceiver : BaseCompassReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateTacticalRadarCompassBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 3. MINIMALIST BEZEL COMPASS (2x2 / Tap-Activated Ring Compass)
class CompassMinimalistBezelReceiver : BaseCompassReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateMinimalistBezelCompassBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 4. HORIZONTAL PILL COMPASS (2x1 / Tap-Activated Precision Tape Strip)
class CompassHorizontalPillReceiver : BaseCompassReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateHorizontalPillCompassBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 5. PURE CIRCLE COMPASS (2x2 / Minimal Circular Dial with North Triangle & Center Heading)
class CompassPureCircleReceiver : BaseCompassReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generatePureCircleCompassBitmap(context, config, false, wDp, hDp, widgetId)
}