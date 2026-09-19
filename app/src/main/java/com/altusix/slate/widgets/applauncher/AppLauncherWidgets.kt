package com.altusix.slate.widgets.applauncher

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
import com.altusix.slate.ui.config.AppLauncherConfigActivity

abstract class BaseAppLauncherReceiver : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle?) {
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 150) ?: 150 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 150) ?: 150
        val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 150) ?: 150 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 150) ?: 150
        val wDp = if (wDpRaw <= 0) 150 else wDpRaw
        val hDp = if (hDpRaw <= 0) 150 else hDpRaw

        val isResponsive = parseAndLockIsResponsive(context, appWidgetId)
        val config = loadSlateWidgetConfig(context, appWidgetId)
        val bitmap = renderWidgetBitmap(context, appWidgetId, config, isResponsive, wDp, hDp)

        val views = RemoteViews(context.packageName, R.layout.widget_base_single)
        views.setImageViewBitmap(R.id.widget_image_view, bitmap)

        try {
            views.setViewPadding(R.id.layout_grid_root, 0, 0, 0, 0)
        } catch (_: Exception) {}

        val pi = getClickPendingIntent(context, appWidgetId)
        if (pi != null) {
            views.setOnClickPendingIntent(R.id.widget_image_view, pi)
            try {
                views.setOnClickPendingIntent(R.id.touch_slot_0, pi)
            } catch (_: Exception) {}
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    protected fun loadSlateWidgetConfig(context: Context, widgetId: Int): SlateWidgetConfig {
        val widgetPrefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
        val bgKey = "widget_${widgetId}_bg_color"

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

    protected fun parseAndLockIsResponsive(context: Context, widgetId: Int): Boolean {
        val widgetPrefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
        val modeKey = "widget_${widgetId}_mode"
        val isResponsiveKey = "widget_${widgetId}_is_responsive"
        if (widgetPrefs.contains(modeKey)) return widgetPrefs.getString(modeKey, "RESPONSIVE") == "RESPONSIVE"
        if (widgetPrefs.contains(isResponsiveKey)) return widgetPrefs.getBoolean(isResponsiveKey, true)

        val launcherPrefs = context.getSharedPreferences("slate_app_launcher_prefs", Context.MODE_PRIVATE)
        val defaultResponsive = launcherPrefs.getBoolean("default_is_responsive", true)
        widgetPrefs.edit().putBoolean(isResponsiveKey, defaultResponsive).apply()
        return defaultResponsive
    }

    abstract fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap

    open fun getClickPendingIntent(context: Context, appWidgetId: Int): PendingIntent? {
        val config = AppLauncherWidgetConfig.load(context, appWidgetId)

        if (config.packageName.isEmpty()) {
            val configIntent = Intent(context, AppLauncherConfigActivity::class.java).apply {
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

        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(config.packageName) ?: return null
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(
            context,
            appWidgetId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

fun getAppLauncherWidgetsCatalog(): List<SlateWidgetInfo> {
    return listOf(
        SlateWidgetInfo("App Launcher Adaptive", "1x1", "App Launcher", AdaptiveAppLauncherReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("App Launcher Rectangle", "2x1", "App Launcher", CustomTextAppLauncherReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("App Launcher Pill", "2x1", "App Launcher", PillAppLauncherReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("App Launcher - Squircle", "1x1", "App Launcher", SquircleLauncherReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("App Launcher - Heart", "1x1", "App Launcher", HeartLauncherReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("App Launcher - Triangle", "1x1", "App Launcher", TriangleLauncherReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("App Launcher - Soft Star", "1x1", "App Launcher", Star5LauncherReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("App Launcher - M3 Pentagon", "1x1", "App Launcher", PentagonLauncherReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("App Launcher - M3 Flower", "1x1", "App Launcher", FlowerLauncherReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("App Launcher - M3 Clover", "1x1", "App Launcher", CloverLauncherReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("App Launcher - M3 Diamond", "1x1", "App Launcher", DiamondLauncherReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("App Launcher - M3 Octagon", "1x1", "App Launcher", OctagonLauncherReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("App Launcher - Circle", "1x1", "App Launcher", CircleLauncherReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("App Launcher - Blob Bottom Right", "1x1", "App Launcher", BlobBottomRightLauncherReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("App Launcher - Blob Bottom Left", "1x1", "App Launcher", BlobBottomLeftLauncherReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("App Launcher - Blob Top Right", "1x1", "App Launcher", BlobTopRightLauncherReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("App Launcher - Blob Top Left", "1x1", "App Launcher", BlobTopLeftLauncherReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("AppLauncher - Pixel Star", "1x1", "App Launcher", PixelStarLauncherReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Cyber Glitch Launcher", "2x1", "App Launcher", GlitchTextLauncherReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Neon Halo Launcher", "1x1", "App Launcher", NeonRingLauncherReceiver::class.java, hasModeOption = true)
    )
}

class AdaptiveAppLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId).copy(isResponsive = isResponsive)
        return generateAdaptiveLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class CustomTextAppLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId).copy(isResponsive = isResponsive)
        return generateRectangleLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class PillAppLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId).copy(isResponsive = isResponsive)
        return generatePillLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class SquircleLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId).copy(isResponsive = isResponsive)
        return generateSquircleLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class HeartLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId).copy(isResponsive = isResponsive)
        return generateHeartLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class TriangleLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId).copy(isResponsive = isResponsive)
        return generateTriangleLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class Star5LauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId).copy(isResponsive = isResponsive)
        return generateStar5LauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class PentagonLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId).copy(isResponsive = isResponsive)
        return generatePentagonLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class FlowerLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId).copy(isResponsive = isResponsive)
        return generateFlowerLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class CloverLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId).copy(isResponsive = isResponsive)
        return generateCloverLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class DiamondLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId).copy(isResponsive = isResponsive)
        return generateDiamondLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class OctagonLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId).copy(isResponsive = isResponsive)
        return generateOctagonLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class CircleLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId).copy(isResponsive = isResponsive)
        return generateCircleLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class BlobBottomRightLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId).copy(isResponsive = isResponsive)
        return generateBlobBottomRightLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class BlobBottomLeftLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId).copy(isResponsive = isResponsive)
        return generateBlobBottomLeftLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class BlobTopRightLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId).copy(isResponsive = isResponsive)
        return generateBlobTopRightLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class BlobTopLeftLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId).copy(isResponsive = isResponsive)
        return generateBlobTopLeftLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class PixelStarLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId).copy(isResponsive = isResponsive)
        return generatePixelStarLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class GlitchTextLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId).copy(isResponsive = isResponsive)
        return generateGlitchTextLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class NeonRingLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId).copy(isResponsive = isResponsive)
        return generateNeonRingLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

fun updateAllAppLauncherWidgets(context: Context) {
    val receivers: List<BaseAppLauncherReceiver> = listOf(
        AdaptiveAppLauncherReceiver(),
        CustomTextAppLauncherReceiver(),
        PillAppLauncherReceiver(),
        SquircleLauncherReceiver(),
        HeartLauncherReceiver(),
        TriangleLauncherReceiver(),
        Star5LauncherReceiver(),
        PentagonLauncherReceiver(),
        FlowerLauncherReceiver(),
        CloverLauncherReceiver(),
        DiamondLauncherReceiver(),
        OctagonLauncherReceiver(),
        CircleLauncherReceiver(),
        BlobBottomRightLauncherReceiver(),
        BlobBottomLeftLauncherReceiver(),
        BlobTopRightLauncherReceiver(),
        BlobTopLeftLauncherReceiver(),
        PixelStarLauncherReceiver(),
        GlitchTextLauncherReceiver(),
        NeonRingLauncherReceiver()
    )

    val manager = AppWidgetManager.getInstance(context)
    for (receiver in receivers) {
        val ids = manager.getAppWidgetIds(ComponentName(context, receiver.javaClass)) ?: intArrayOf()
        for (id in ids) {
            try {
                receiver.updateWidget(context, manager, id)
            } catch (_: Exception) {}
        }
    }
}