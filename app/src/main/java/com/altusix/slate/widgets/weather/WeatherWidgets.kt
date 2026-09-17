package com.altusix.slate.widgets.weather

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
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

private fun loadSlateWidgetConfig(context: Context, widgetId: Int): SlateWidgetConfig {
    val widgetPrefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
    val bgKey = "widget_${widgetId}_bg_color"

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

    if (widgetPrefs.contains("widget_-1_mode")) {
        val studioResponsive = widgetPrefs.getString("widget_-1_mode", "RESPONSIVE") == "RESPONSIVE"
        widgetPrefs.edit()
            .putBoolean(isResponsiveKey, studioResponsive)
            .putString(modeKey, if (studioResponsive) "RESPONSIVE" else "FIXED")
            .apply()
        return studioResponsive
    }

    val launcherPrefs = context.getSharedPreferences("slate_app_launcher_prefs", Context.MODE_PRIVATE)
    val defaultResponsive = launcherPrefs.getBoolean("default_is_responsive", true)
    widgetPrefs.edit()
        .putBoolean(isResponsiveKey, defaultResponsive)
        .putString(modeKey, if (defaultResponsive) "RESPONSIVE" else "FIXED")
        .apply()
    return defaultResponsive
}

abstract class BaseWeatherReceiver(
    open val targetAspect: Float = 2.0f,
    open val widgetTypeTag: String = "HORIZON"
) : AppWidgetProvider() {

    abstract fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap

    fun renderBitmapForWidget(
        context: Context,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int,
        widgetId: Int
    ): Bitmap = renderWidgetBitmap(context, widgetId, config, isResponsive, wDp, hDp)

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, this::class.java)) ?: intArrayOf()
            for (id in ids) {
                updateSingleWidget(context, manager, id)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            updateSingleWidget(context, appWidgetManager, id)
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle?) {
        updateSingleWidget(context, appWidgetManager, appWidgetId)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    fun updateSingleWidget(context: Context, manager: AppWidgetManager, id: Int) {
        try {
            val config = loadSlateWidgetConfig(context, id)
            val isResponsive = if (id == -1) true else parseAndLockIsResponsive(context, id)
            val options = manager.getAppWidgetOptions(id)
            val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

            val (fallbackW, fallbackH) = when {
                targetAspect >= 3.5f -> 300 to 75
                targetAspect in 2.5f..3.4f -> 240 to 80
                targetAspect in 1.8f..2.4f -> 300 to 150
                else -> 150 to 150
            }

            val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, fallbackW) ?: fallbackW else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, fallbackW) ?: fallbackW
            val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, fallbackH) ?: fallbackH else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, fallbackH) ?: fallbackH
            val wDp = if (wDpRaw <= 0) fallbackW else wDpRaw
            val hDp = if (hDpRaw <= 0) fallbackH else hDpRaw
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

            val bitmap = renderWidgetBitmap(context, id, config, isResponsive, effWDp, effHDp)
            val views = RemoteViews(context.packageName, R.layout.widget_base_single)

            try {
                views.setViewPadding(R.id.layout_grid_root, padH, padV, padH, padV)
            } catch (_: Exception) {}

            views.setImageViewBitmap(R.id.widget_image_view, bitmap)

            val clickIntent = WeatherRepository.createWeatherClickIntent(context, id)
            val pendingIntent = PendingIntent.getActivity(
                context,
                id,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.touch_slot_0, pendingIntent)

            manager.updateAppWidget(id, views)
        } catch (_: Exception) {}
    }
}

fun getWeatherWidgetsCatalog(): List<SlateWidgetInfo> {
    return listOf(
        SlateWidgetInfo(name = "Weather Horizon", sizeText = "4x2", category = "Weather", receiverClass = WeatherHorizonReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Weather Bento Glance", sizeText = "4x2", category = "Weather", receiverClass = WeatherBentoGlanceReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Weather Daylight Arc", sizeText = "2x2", category = "Weather", receiverClass = WeatherDaylightArcReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo(name = "Weather Pill Dock", sizeText = "4x1", category = "Weather", receiverClass = WeatherPillDockReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Weather Editorial", sizeText = "2x2", category = "Weather", receiverClass = WeatherEditorialReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo(name = "Weather Hourly Ribbon", sizeText = "4x1", category = "Weather", receiverClass = WeatherHourlyRibbonReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Weather Minimalist Dual", sizeText = "2x2", category = "Weather", receiverClass = WeatherMinimalistDualReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo(name = "Weather Compact Dial", sizeText = "2x2", category = "Weather", receiverClass = WeatherCompactDialReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo(name = "Weather Metro Trio", sizeText = "3x1", category = "Weather", receiverClass = WeatherMetroTrioReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Micro Weather", sizeText = "1x1", category = "Weather", receiverClass = WeatherMicroReceiver::class.java, hasModeOption = true)
    )
}

fun updateAllWeatherWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context) ?: return
    val receivers = listOf(
        WeatherHorizonReceiver(),
        WeatherBentoGlanceReceiver(),
        WeatherDaylightArcReceiver(),
        WeatherPillDockReceiver(),
        WeatherEditorialReceiver(),
        WeatherHourlyRibbonReceiver(),
        WeatherMinimalistDualReceiver(),
        WeatherCompactDialReceiver(),
        WeatherMetroTrioReceiver(),
        WeatherMicroReceiver()
    )
    for (receiver in receivers) {
        val ids = manager.getAppWidgetIds(ComponentName(context, receiver::class.java)) ?: intArrayOf()
        for (id in ids) {
            receiver.updateSingleWidget(context, manager, id)
        }
    }
}

// Concrete Receivers

// 1. Weather Horizon (4x2 / Editorial Forecast
class WeatherHorizonReceiver : BaseWeatherReceiver(targetAspect = 2.0f, widgetTypeTag = "HORIZON") {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap =
        generateWeatherHorizonBitmap(context, config, isResponsive, wDp, hDp, appWidgetId)
}

// 2. Weather Bento Glance (4x2)
class WeatherBentoGlanceReceiver : BaseWeatherReceiver(targetAspect = 2.0f, widgetTypeTag = "BENTO") {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap =
        generateWeatherBentoGlanceBitmap(context, config, isResponsive, wDp, hDp, appWidgetId)
}

// 3. Weather Daylight Arc (2x2 / Sun Daylight Progress Arc)
class WeatherDaylightArcReceiver : BaseWeatherReceiver(targetAspect = 1.0f, widgetTypeTag = "ARC") {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap =
        generateWeatherDaylightArcBitmap(context, config, isResponsive, wDp, hDp, appWidgetId)
}

// 4. Weather Pill Dock (4x1)
class WeatherPillDockReceiver : BaseWeatherReceiver(targetAspect = 4.0f, widgetTypeTag = "DOCK") {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap =
        generateWeatherPillDockBitmap(context, config, isResponsive, wDp, hDp, appWidgetId)
}

// 5. Weather Editorial Capsule (2x2)
class WeatherEditorialReceiver : BaseWeatherReceiver(targetAspect = 1.0f, widgetTypeTag = "EDITORIAL") {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap =
        generateWeatherEditorialBitmap(context, config, isResponsive, wDp, hDp, appWidgetId)
}

// 6. Weather Hourly Ribbon (4x1)
class WeatherHourlyRibbonReceiver : BaseWeatherReceiver(targetAspect = 4.0f, widgetTypeTag = "RIBBON") {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap =
        generateWeatherHourlyRibbonBitmap(context, config, isResponsive, wDp, hDp, appWidgetId)
}

// 7. Weather Minimalist Dual (2x2)
class WeatherMinimalistDualReceiver : BaseWeatherReceiver(targetAspect = 1.0f, widgetTypeTag = "DUAL") {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap =
        generateWeatherMinimalistDualBitmap(context, config, isResponsive, wDp, hDp, appWidgetId)
}

// 8. Weather Compact Dial (2x2)
class WeatherCompactDialReceiver : BaseWeatherReceiver(targetAspect = 1.0f, widgetTypeTag = "DIAL") {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap =
        generateWeatherCompactDialBitmap(context, config, isResponsive, wDp, hDp, appWidgetId)
}

// 9. Weather Metro Trio (3x1)
class WeatherMetroTrioReceiver : BaseWeatherReceiver(targetAspect = 3.0f, widgetTypeTag = "TRIO") {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap =
        generateWeatherMetroTrioBitmap(context, config, isResponsive, wDp, hDp, appWidgetId)
}

// 10. Micro Weather (1x1)
class WeatherMicroReceiver : BaseWeatherReceiver(targetAspect = 1.0f, widgetTypeTag = "MICRO") {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap =
        generateWeatherMicroBitmap(context, config, isResponsive, wDp, hDp, appWidgetId)
}
