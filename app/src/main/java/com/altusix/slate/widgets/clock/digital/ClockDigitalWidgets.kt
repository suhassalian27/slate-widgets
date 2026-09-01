package com.altusix.slate.widgets.clock.digital

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.AlarmClock
import android.widget.RemoteViews
import com.altusix.slate.R
import com.altusix.slate.core.model.SlateWidgetInfo
import com.altusix.slate.core.service.SlateClockTickerService
import com.altusix.slate.core.theme.ThemePreferences
import com.altusix.slate.data.local.SlateWidgetConfig

// --- CATALOG REGISTRATION & BATCH UPDATES ---

fun getClockDigitalWidgetsCatalog(): List<SlateWidgetInfo> {
    return listOf(
        SlateWidgetInfo("Minimal Divider Digital", "2x2", "Clock – Digital", ClockDigitalMinimalDividerReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Compact Block Digital", "2x2", "Clock – Digital", ClockDigitalCompactBlockReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Typographic Word Digital", "2x2", "Clock – Digital", ClockDigitalTextWordReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Giant Hour Capsule Digital", "2x2", "Clock – Digital", ClockDigitalGiantHourCapsuleReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Modern 3D LED Digital", "4x2", "Clock – Digital", ClockDigitalModern3dLedReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Dual Pill Stack Digital", "1x2", "Clock – Digital", ClockDigitalDualPillStackReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Typeface 1", "4x2", "Clock – Digital", ClockDigitalTextFont1Receiver::class.java, hasModeOption = false, defaultOpacity = 0.0f),
        SlateWidgetInfo("Typeface 2", "4x2", "Clock – Digital", ClockDigitalTextFont2Receiver::class.java, hasModeOption = false, defaultOpacity = 0.0f),
        SlateWidgetInfo("Typeface 3", "4x2", "Clock – Digital", ClockDigitalTextFont3Receiver::class.java, hasModeOption = false, defaultOpacity = 0.0f),
        SlateWidgetInfo("Typeface 4", "4x2", "Clock – Digital", ClockDigitalTextFont4Receiver::class.java, hasModeOption = false, defaultOpacity = 0.0f),
        SlateWidgetInfo("Typeface 5", "4x2", "Clock – Digital", ClockDigitalTextFont5Receiver::class.java, hasModeOption = false, defaultOpacity = 0.0f),
        SlateWidgetInfo("Typeface 6", "4x2", "Clock – Digital", ClockDigitalTextFont6Receiver::class.java, hasModeOption = false, defaultOpacity = 0.0f),
        SlateWidgetInfo("Typeface 7", "4x2", "Clock – Digital", ClockDigitalTextFont7Receiver::class.java, hasModeOption = false, defaultOpacity = 0.0f),
        SlateWidgetInfo("Typeface 8", "4x2", "Clock – Digital", ClockDigitalTextFont8Receiver::class.java, hasModeOption = false, defaultOpacity = 0.0f),
        SlateWidgetInfo("Typeface 9", "4x2", "Clock – Digital", ClockDigitalTextFont9Receiver::class.java, hasModeOption = false, defaultOpacity = 0.0f),
        SlateWidgetInfo("Typeface 10", "4x2", "Clock – Digital", ClockDigitalTextFont10Receiver::class.java, hasModeOption = false, defaultOpacity = 0.0f),
        SlateWidgetInfo("Typeface 11", "4x2", "Clock – Digital", ClockDigitalTextFont11Receiver::class.java, hasModeOption = false, defaultOpacity = 0.0f),
        SlateWidgetInfo("Typeface 12", "4x2", "Clock – Digital", ClockDigitalTextFont12Receiver::class.java, hasModeOption = false, defaultOpacity = 0.0f),
        SlateWidgetInfo("Typeface 13", "4x2", "Clock – Digital", ClockDigitalTextFont13Receiver::class.java, hasModeOption = false, defaultOpacity = 0.0f),
        SlateWidgetInfo("Typeface 14", "4x2", "Clock – Digital", ClockDigitalTextFont14Receiver::class.java, hasModeOption = false, defaultOpacity = 0.0f),
        SlateWidgetInfo("Typeface 15", "4x2", "Clock – Digital", ClockDigitalTextFont15Receiver::class.java, hasModeOption = false, defaultOpacity = 0.0f),
        SlateWidgetInfo("Typeface 16", "4x2", "Clock – Digital", ClockDigitalTextFont16Receiver::class.java, hasModeOption = false, defaultOpacity = 0.0f),
        SlateWidgetInfo("Typeface 17", "4x2", "Clock – Digital", ClockDigitalTextFont17Receiver::class.java, hasModeOption = false, defaultOpacity = 0.0f),
        SlateWidgetInfo("Typeface 18", "4x2", "Clock – Digital", ClockDigitalTextFont18Receiver::class.java, hasModeOption = false, defaultOpacity = 0.0f),
        SlateWidgetInfo("Typeface 19", "4x2", "Clock – Digital", ClockDigitalTextFont19Receiver::class.java, hasModeOption = false, defaultOpacity = 0.0f),
        SlateWidgetInfo("Typeface 20", "4x2", "Clock – Digital", ClockDigitalTextFont20Receiver::class.java, hasModeOption = false, defaultOpacity = 0.0f),
        SlateWidgetInfo("Typeface 21", "4x2", "Clock – Digital", ClockDigitalTextFont21Receiver::class.java, hasModeOption = false, defaultOpacity = 0.0f),
        SlateWidgetInfo("Typeface 22", "4x2", "Clock – Digital", ClockDigitalTextFont22Receiver::class.java, hasModeOption = false, defaultOpacity = 0.0f),
        SlateWidgetInfo("Typeface 23", "4x2", "Clock – Digital", ClockDigitalTextFont23Receiver::class.java, hasModeOption = false, defaultOpacity = 0.0f),
        SlateWidgetInfo("Typeface 24", "4x2", "Clock – Digital", ClockDigitalTextFont24Receiver::class.java, hasModeOption = false, defaultOpacity = 0.0f),
        SlateWidgetInfo("Typeface 25", "4x2", "Clock – Digital", ClockDigitalTextFont25Receiver::class.java, hasModeOption = false, defaultOpacity = 0.0f),
        SlateWidgetInfo("Typeface 26", "4x2", "Clock – Digital", ClockDigitalTextFont26Receiver::class.java, hasModeOption = false, defaultOpacity = 0.0f),
        SlateWidgetInfo("Typeface 27", "4x2", "Clock – Digital", ClockDigitalTextFont27Receiver::class.java, hasModeOption = false, defaultOpacity = 0.0f),
        SlateWidgetInfo("Typeface 28", "4x2", "Clock – Digital", ClockDigitalTextFont28Receiver::class.java, hasModeOption = false, defaultOpacity = 0.0f),
        SlateWidgetInfo("Typeface 29", "4x2", "Clock – Digital", ClockDigitalTextFont29Receiver::class.java, hasModeOption = false, defaultOpacity = 0.0f),
        SlateWidgetInfo("Typeface 30", "4x2", "Clock – Digital", ClockDigitalTextFont30Receiver::class.java, hasModeOption = false, defaultOpacity = 0.0f),
        SlateWidgetInfo("Typeface 31", "4x2", "Clock – Digital", ClockDigitalTextFont31Receiver::class.java, hasModeOption = false, defaultOpacity = 0.0f),
        SlateWidgetInfo("Typeface 32", "4x2", "Clock – Digital", ClockDigitalTextFont32Receiver::class.java, hasModeOption = false, defaultOpacity = 0.0f),
        SlateWidgetInfo("Typeface 33", "4x2", "Clock – Digital", ClockDigitalTextFont33Receiver::class.java, hasModeOption = false, defaultOpacity = 0.0f),
        SlateWidgetInfo("Typeface 34", "4x2", "Clock – Digital", ClockDigitalTextFont34Receiver::class.java, hasModeOption = false, defaultOpacity = 0.0f)
    )
}

fun updateAllClockDigitalWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val receivers = listOf(
        ClockDigitalMinimalDividerReceiver::class.java,
        ClockDigitalCompactBlockReceiver::class.java,
        ClockDigitalTextWordReceiver::class.java,
        ClockDigitalGiantHourCapsuleReceiver::class.java,
        ClockDigitalModern3dLedReceiver::class.java,
        ClockDigitalDualPillStackReceiver::class.java,
        ClockDigitalTextFont1Receiver::class.java,
        ClockDigitalTextFont2Receiver::class.java,
        ClockDigitalTextFont3Receiver::class.java,
        ClockDigitalTextFont4Receiver::class.java,
        ClockDigitalTextFont5Receiver::class.java,
        ClockDigitalTextFont6Receiver::class.java,
        ClockDigitalTextFont7Receiver::class.java,
        ClockDigitalTextFont8Receiver::class.java,
        ClockDigitalTextFont9Receiver::class.java,
        ClockDigitalTextFont10Receiver::class.java,
        ClockDigitalTextFont11Receiver::class.java,
        ClockDigitalTextFont12Receiver::class.java,
        ClockDigitalTextFont13Receiver::class.java,
        ClockDigitalTextFont14Receiver::class.java,
        ClockDigitalTextFont15Receiver::class.java,
        ClockDigitalTextFont16Receiver::class.java,
        ClockDigitalTextFont17Receiver::class.java,
        ClockDigitalTextFont18Receiver::class.java,
        ClockDigitalTextFont19Receiver::class.java,
        ClockDigitalTextFont20Receiver::class.java,
        ClockDigitalTextFont21Receiver::class.java,
        ClockDigitalTextFont22Receiver::class.java,
        ClockDigitalTextFont23Receiver::class.java,
        ClockDigitalTextFont24Receiver::class.java,
        ClockDigitalTextFont25Receiver::class.java,
        ClockDigitalTextFont26Receiver::class.java,
        ClockDigitalTextFont27Receiver::class.java,
        ClockDigitalTextFont28Receiver::class.java,
        ClockDigitalTextFont29Receiver::class.java,
        ClockDigitalTextFont30Receiver::class.java,
        ClockDigitalTextFont31Receiver::class.java,
        ClockDigitalTextFont32Receiver::class.java,
        ClockDigitalTextFont33Receiver::class.java,
        ClockDigitalTextFont34Receiver::class.java
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

// --- BASE DIGITAL CLOCK RECEIVER ---

abstract class BaseDigitalClockReceiver : AppWidgetProvider() {

    abstract fun renderBitmap(
        context: Context,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, javaClass))
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
        updateSingleWidget(context, appWidgetManager, appWidgetId)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    private fun loadSlateWidgetConfig(context: Context, widgetId: Int, defaultOpacity: Float): SlateWidgetConfig {
        val widgetPrefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
        val bgKey = "widget_${widgetId}_bg_color"

        // Snapshot and permanently lock the current global theme on initial placement
        if (!widgetPrefs.contains(bgKey) && widgetId != -1) {
            val globalSettings = ThemePreferences(context).getThemeSettings()
            val isLight = (((globalSettings.bgHex shr 16 and 0xFFL) * 0.2126f) +
                    ((globalSettings.bgHex shr 8 and 0xFFL) * 0.7152f) +
                    ((globalSettings.bgHex and 0xFFL) * 0.0722f)) / 255f > 0.5f

            val initialOpacity = if (defaultOpacity == 0.0f) 0.0f else globalSettings.opacity

            widgetPrefs.edit()
                .putString("widget_${widgetId}_theme_mode", if (isLight) "LIGHT" else "DARK")
                .putLong("widget_${widgetId}_bg_color", globalSettings.bgHex)
                .putLong("widget_${widgetId}_accent_color", globalSettings.accentHex)
                .putFloat("widget_${widgetId}_opacity", initialOpacity)
                .apply()
        }

        val globalSettings = ThemePreferences(context).getThemeSettings()
        val bgColor = widgetPrefs.getLong("widget_${widgetId}_bg_color", globalSettings.bgHex)
        val fallbackOpacity = if (defaultOpacity == 0.0f) 0.0f else globalSettings.opacity
        val opacity = widgetPrefs.getFloat("widget_${widgetId}_opacity", fallbackOpacity)
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

    private fun updateSingleWidget(context: Context, manager: AppWidgetManager, id: Int) {
        try {
            val widgetPrefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
            val appLauncherPrefs = context.getSharedPreferences("slate_app_launcher_prefs", Context.MODE_PRIVATE)

            val widgetInfo = getClockDigitalWidgetsCatalog().find { it.receiverClass == javaClass }
            val defaultOpacity = widgetInfo?.defaultOpacity ?: 1.0f

            val config = loadSlateWidgetConfig(context, id, defaultOpacity)
            val isResponsive = parseAndLockIsResponsive(widgetPrefs, appLauncherPrefs, id)

            val options = manager.getAppWidgetOptions(id)
            val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

            val rawW = if (isLandscape) {
                options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 180) ?: 180
            } else {
                options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180) ?: 180
            }
            val rawH = if (isLandscape) {
                options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80) ?: 80
            } else {
                options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 80) ?: 80
            }

            val wDp = (if (rawW <= 0) 180 else rawW).coerceAtLeast(140)
            val hDp = (if (rawH <= 0) 80 else rawH).coerceAtLeast(60)

            val rawBitmap = renderBitmap(context, config, isResponsive, wDp, hDp)
            val bitmap = scaleBitmapForIPC(rawBitmap, maxDimensionPx = 800)

            val views = RemoteViews(context.packageName, R.layout.widget_canvas_container)
            views.setImageViewBitmap(R.id.widget_canvas_image, bitmap)

            val clockIntent = getUniversalClockIntent(context)
            val pendingIntent = PendingIntent.getActivity(
                context,
                id,
                clockIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_canvas_image, pendingIntent)

            manager.updateAppWidget(id, views)

            if (rawBitmap != bitmap) {
                rawBitmap.recycle()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseAndLockIsResponsive(
        widgetPrefs: android.content.SharedPreferences,
        appLauncherPrefs: android.content.SharedPreferences,
        id: Int
    ): Boolean {
        val keyWMode = "widget_${id}_mode"
        val keyWResponsive = "widget_${id}_is_responsive"

        if (widgetPrefs.contains(keyWMode)) {
            val modeStr = widgetPrefs.getString(keyWMode, "RESPONSIVE")
            return modeStr.equals("RESPONSIVE", ignoreCase = true)
        }
        if (widgetPrefs.contains(keyWResponsive)) {
            return widgetPrefs.getBoolean(keyWResponsive, true)
        }

        val defaultIsResponsive = appLauncherPrefs.getBoolean("default_is_responsive", true)

        widgetPrefs.edit()
            .putBoolean(keyWResponsive, defaultIsResponsive)
            .putString(keyWMode, if (defaultIsResponsive) "RESPONSIVE" else "FIXED")
            .apply()

        return defaultIsResponsive
    }

    private fun scaleBitmapForIPC(src: Bitmap, maxDimensionPx: Int): Bitmap {
        val width = src.width
        val height = src.height
        if (width <= maxDimensionPx && height <= maxDimensionPx) return src

        val maxDim = maxOf(width, height).toFloat()
        val scale = maxDimensionPx.toFloat() / maxDim
        val targetW = (width * scale).toInt().coerceAtLeast(1)
        val targetH = (height * scale).toInt().coerceAtLeast(1)

        return Bitmap.createScaledBitmap(src, targetW, targetH, true)
    }

    private fun getUniversalClockIntent(context: Context): Intent {
        val pm = context.packageManager
        val categoryClockIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory("android.intent.category.APP_CLOCK")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        if (categoryClockIntent.resolveActivity(pm) != null) {
            return categoryClockIntent
        }

        val oemClockPackages = listOf(
            "com.google.android.deskclock",
            "com.sec.android.app.clockpackage",
            "com.oneplus.deskclock",
            "com.coloros.alarm",
            "com.vivo.alarm",
            "com.xiaomi.deskclock",
            "com.android.deskclock"
        )

        for (pkg in oemClockPackages) {
            val launchIntent = pm.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                return launchIntent
            }
        }

        return Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }
}

// --- WIDGET RECEIVERS ---

// 2. MINIMAL DIVIDER DIGITAL (2x2 / Stacked Time with Accent Line Divider)
class ClockDigitalMinimalDividerReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateMinimalDividerDigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 5. COMPACT BLOCK DIGITAL (2x2 / 4-Digit Block Time in Condensed Font)
class ClockDigitalCompactBlockReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateCompactBlockDigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 7. TYPOGRAPHIC WORD DIGITAL (2x2 / Stacked Natural Language Word Time)
class ClockDigitalTextWordReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateTextWordClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 8. GIANT HOUR CAPSULE DIGITAL (2x2 / Giant Hour with Accent Minute Pill)
class ClockDigitalGiantHourCapsuleReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateGiantHourCapsuleDigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 9. MODERN 3D LED HORIZONTAL DIGITAL (4x2 / Contoured 3D LED Clock)
class ClockDigitalModern3dLedReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateModern3dLedHorizontalDigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 14. DUAL PILL STACK DIGITAL (1x2 / Dual Nested Pill Tiles for Time & Accent Date)
class ClockDigitalDualPillStackReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateMinimalStackedDigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 15. TEXT DIGITAL FONT 1 (4x2 / Pure Typographic Giant 4-Digit Time)
class ClockDigitalTextFont1Receiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateTextFont1DigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 16. TEXT DIGITAL FONT 2 (4x2 / Pure Typographic Giant 4-Digit Time)
class ClockDigitalTextFont2Receiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateTextFont2DigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 17. TEXT DIGITAL FONT 3 (4x2 / Pure Typographic Giant 4-Digit Time)
class ClockDigitalTextFont3Receiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateTextFont3DigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 18. TEXT DIGITAL FONT 4 (4x2 / Pure Typographic Giant 4-Digit Time)
class ClockDigitalTextFont4Receiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateTextFont4DigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 19. TEXT DIGITAL FONT 5 (4x2 / Pure Typographic Giant 4-Digit Time)
class ClockDigitalTextFont5Receiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateTextFont5DigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 20. TEXT DIGITAL FONT 6 (4x2 / Pure Typographic Giant 4-Digit Time)
class ClockDigitalTextFont6Receiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateTextFont6DigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 21. TEXT DIGITAL FONT 7 (4x2 / Pure Typographic Giant 4-Digit Time)
class ClockDigitalTextFont7Receiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateTextFont7DigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 22. TEXT DIGITAL FONT 8 (4x2 / Pure Typographic Giant 4-Digit Time)
class ClockDigitalTextFont8Receiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateTextFont8DigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 23. TEXT DIGITAL FONT 9 (4x2 / Pure Typographic Giant 4-Digit Time)
class ClockDigitalTextFont9Receiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateTextFont9DigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 24. TEXT DIGITAL FONT 10 (4x2 / Pure Typographic Giant 4-Digit Time)
class ClockDigitalTextFont10Receiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateTextFont10DigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 25. TEXT DIGITAL FONT 11 (4x2 / Pure Typographic Giant 4-Digit Time)
class ClockDigitalTextFont11Receiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateTextFont11DigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 26. TEXT DIGITAL FONT 12 (4x2 / Pure Typographic Giant 4-Digit Time)
class ClockDigitalTextFont12Receiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateTextFont12DigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 27. TEXT DIGITAL FONT 13 (4x2 / Pure Typographic Giant 4-Digit Time)
class ClockDigitalTextFont13Receiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateTextFont13DigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 28. TEXT DIGITAL FONT 14 (4x2 / Pure Typographic Giant 4-Digit Time)
class ClockDigitalTextFont14Receiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateTextFont14DigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 29. TEXT DIGITAL FONT 15 (4x2 / Pure Typographic Giant 4-Digit Time)
class ClockDigitalTextFont15Receiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateTextFont15DigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 30. TEXT DIGITAL FONT 16 (4x2 / Pure Typographic Giant 4-Digit Time)
class ClockDigitalTextFont16Receiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateTextFont16DigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 31. TEXT DIGITAL FONT 17 (4x2 / Pure Typographic Giant 4-Digit Time)
class ClockDigitalTextFont17Receiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateTextFont17DigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 32. TEXT DIGITAL FONT 18 (4x2 / Pure Typographic Giant 4-Digit Time)
class ClockDigitalTextFont18Receiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateTextFont18DigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 33. TEXT DIGITAL FONT 19 (4x2 / Pure Typographic Giant 4-Digit Time)
class ClockDigitalTextFont19Receiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateTextFont19DigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 34. TEXT DIGITAL FONT 20 (4x2 / Pure Typographic Giant 4-Digit Time)
class ClockDigitalTextFont20Receiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateTextFont20DigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 35. TEXT DIGITAL FONT 21 (4x2 / Pure Typographic Giant 4-Digit Time)
class ClockDigitalTextFont21Receiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateTextFont21DigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 36. TEXT DIGITAL FONT 22 (4x2 / Pure Typographic Giant 4-Digit Time)
class ClockDigitalTextFont22Receiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateTextFont22DigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 37. TEXT DIGITAL FONT 23 (4x2 / Pure Typographic Giant 4-Digit Time)
class ClockDigitalTextFont23Receiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateTextFont23DigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 38. TEXT DIGITAL FONT 24 (4x2 / Pure Typographic Giant 4-Digit Time)
class ClockDigitalTextFont24Receiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateTextFont24DigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 39. TEXT DIGITAL FONT 25 (4x2 / Pure Typographic Giant 4-Digit Time)
class ClockDigitalTextFont25Receiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateTextFont25DigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 40. TEXT DIGITAL FONT 26 (4x2 / Pure Typographic Giant 4-Digit Time)
class ClockDigitalTextFont26Receiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateTextFont26DigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 41. TEXT DIGITAL FONT 27 (4x2 / Pure Typographic Giant 4-Digit Time)
class ClockDigitalTextFont27Receiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateTextFont27DigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 42. TEXT DIGITAL FONT 28 (4x2 / Pure Typographic Giant 4-Digit Time)
class ClockDigitalTextFont28Receiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateTextFont28DigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 43. TEXT DIGITAL FONT 29 (4x2 / Pure Typographic Giant 4-Digit Time)
class ClockDigitalTextFont29Receiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateTextFont29DigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 44. TEXT DIGITAL FONT 30 (4x2 / Pure Typographic Giant 4-Digit Time)
class ClockDigitalTextFont30Receiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateTextFont30DigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 45. TEXT DIGITAL FONT 31 (4x2 / Pure Typographic Giant 4-Digit Time)
class ClockDigitalTextFont31Receiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateTextFont31DigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 46. TEXT DIGITAL FONT 32 (4x2 / Pure Typographic Giant 4-Digit Time)
class ClockDigitalTextFont32Receiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateTextFont32DigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 47. TEXT DIGITAL FONT 33 (4x2 / Pure Typographic Giant 4-Digit Time)
class ClockDigitalTextFont33Receiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateTextFont33DigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 48. TEXT DIGITAL FONT 34 (4x2 / Pure Typographic Giant 4-Digit Time)
class ClockDigitalTextFont34Receiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateTextFont34DigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}