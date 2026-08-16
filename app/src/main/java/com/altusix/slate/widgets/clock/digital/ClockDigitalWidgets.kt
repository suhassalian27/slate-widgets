package com.altusix.slate.widgets.clock.digital

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.provider.AlarmClock
import android.widget.RemoteViews

import com.altusix.slate.R
import com.altusix.slate.core.model.SlateWidgetInfo
import com.altusix.slate.core.service.SlateClockTickerService
import com.altusix.slate.data.local.SlateWidgetConfig

// --- CATALOG REGISTRATION & BATCH UPDATES ---

fun getClockDigitalWidgetsCatalog(): List<SlateWidgetInfo> {
    return listOf(
        SlateWidgetInfo("Bold Typographic Digital", "2x2", "Clock – Digital", ClockDigitalBoldTypographicReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Minimal Divider Digital", "2x2", "Clock – Digital", ClockDigitalMinimalDividerReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("LCD Seven Segment Digital", "2x2", "Clock – Digital", ClockDigitalLcdSevenSegmentReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Asymmetric Slanted Digital", "2x2", "Clock – Digital", ClockDigitalAsymmetricSlantedReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Compact Block Digital", "2x2", "Clock – Digital", ClockDigitalCompactBlockReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Asymmetric Overlay Digital", "2x2", "Clock – Digital", ClockDigitalAsymmetricOverlayReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Typographic Word Digital", "2x2", "Clock – Digital", ClockDigitalTextWordReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Giant Hour Capsule Digital", "2x2", "Clock – Digital", ClockDigitalGiantHourCapsuleReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Modern 3D LED Digital", "4x2", "Clock – Digital", ClockDigitalModern3dLedReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Gradient Tall Digital", "4x2", "Clock – Digital", ClockDigitalGradientTallReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Script Overlay Digital", "4x2", "Clock – Digital", ClockDigitalScriptOverlayReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Split Flap Digital", "4x2", "Clock – Digital", ClockDigitalSplitFlapReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Vertical Capsule Digital", "1x2", "Clock – Digital", ClockDigitalVerticalCapsuleReceiver::class.java, hasModeOption = true),
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
        SlateWidgetInfo("Typeface 12", "4x2", "Clock – Digital", ClockDigitalTextFont12Receiver::class.java, hasModeOption = false, defaultOpacity = 0.0f)
    )
}

fun updateAllClockDigitalWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val receivers = listOf(
        ClockDigitalBoldTypographicReceiver::class.java,
        ClockDigitalMinimalDividerReceiver::class.java,
        ClockDigitalLcdSevenSegmentReceiver::class.java,
        ClockDigitalAsymmetricSlantedReceiver::class.java,
        ClockDigitalCompactBlockReceiver::class.java,
        ClockDigitalAsymmetricOverlayReceiver::class.java,
        ClockDigitalTextWordReceiver::class.java,
        ClockDigitalGiantHourCapsuleReceiver::class.java,
        ClockDigitalModern3dLedReceiver::class.java,
        ClockDigitalGradientTallReceiver::class.java,
        ClockDigitalScriptOverlayReceiver::class.java,
        ClockDigitalSplitFlapReceiver::class.java,
        ClockDigitalVerticalCapsuleReceiver::class.java,
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
        ClockDigitalTextFont12Receiver::class.java
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
        newOptions: android.os.Bundle?
    ) {
        updateSingleWidget(context, appWidgetManager, appWidgetId)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    private fun updateSingleWidget(context: Context, manager: AppWidgetManager, id: Int) {
        try {
            val widgetPrefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
            val appLauncherPrefs = context.getSharedPreferences("slate_app_launcher_prefs", Context.MODE_PRIVATE)

            val themeMode = widgetPrefs.getString("widget_${id}_theme_mode", "DARK") ?: "DARK"
            val defaultBg = if (themeMode == "LIGHT") 0xFFFFFFFFL else 0xFF161618L
            val defaultAccent = if (themeMode == "LIGHT") 0xFF000000L else 0xFFFFFFFFL

            val bgColor = widgetPrefs.getLong("widget_${id}_bg_color", defaultBg)
//            val opacity = widgetPrefs.getFloat("widget_${id}_opacity", 1.0f)
            val accentColor = widgetPrefs.getLong("widget_${id}_accent_color", defaultAccent)

            val widgetInfo = getClockDigitalWidgetsCatalog().find { it.receiverClass == javaClass }
            val defaultOpacity = widgetInfo?.defaultOpacity ?: 1.0f

            val opacity = widgetPrefs.getFloat("widget_${id}_opacity", defaultOpacity)

            val config = SlateWidgetConfig(themeMode, bgColor, opacity, accentColor)

            // --- PER-INSTANCE LOCK: Reads global default ONLY on first render, then locks to widget_${id} ---
            val isResponsive = parseAndLockIsResponsive(widgetPrefs, appLauncherPrefs, id)

            val options = manager.getAppWidgetOptions(id)
            val isPortrait = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT

            val rawW = if (isPortrait) {
                options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) ?: 0
            } else {
                options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH) ?: 0
            }
            val rawH = if (isPortrait) {
                options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT) ?: 0
            } else {
                options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) ?: 0
            }

            val wDp = rawW.coerceAtLeast(180)
            val hDp = rawH.coerceAtLeast(80)

            val rawBitmap = renderBitmap(context, config, isResponsive, wDp, hDp)
            val bitmap = scaleBitmapForIPC(rawBitmap, maxDimensionPx = 800)

            val views = RemoteViews(context.packageName, R.layout.widget_canvas_container)
            views.setImageViewBitmap(R.id.widget_canvas_image, bitmap)

            val clockIntent = getUniversalClockIntent(context)
            val pendingIntent = PendingIntent.getActivity(
                context,
                id,
                clockIntent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
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

        // 1. If this widget instance already has its own saved preference, use it!
        if (widgetPrefs.contains(keyWMode)) {
            val modeStr = widgetPrefs.getString(keyWMode, "RESPONSIVE")
            return modeStr.equals("RESPONSIVE", ignoreCase = true)
        }
        if (widgetPrefs.contains(keyWResponsive)) {
            return widgetPrefs.getBoolean(keyWResponsive, true)
        }

        // 2. First-time render for this ID! Read current pinning default from MainActivity...
        val defaultIsResponsive = appLauncherPrefs.getBoolean("default_is_responsive", true)

        // 3. ...and IMMEDIATELY lock it to this specific widget ID!
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

// 1. BOLD TYPOGRAPHIC DIGITAL (2x2 Square / Stacked Giant Hour & Minute Display)
class ClockDigitalBoldTypographicReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(
        context: Context,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap = generateBoldTypographicDigitalClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }
}

// 2. MINIMAL DIVIDER DIGITAL (2x2 / Stacked Time with Accent Line Divider)
class ClockDigitalMinimalDividerReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateMinimalDividerDigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 3. LCD SEVEN SEGMENT DIGITAL (2x2 / Stacked 7-Segment LCD with Vertical Date)
class ClockDigitalLcdSevenSegmentReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateLcdSevenSegmentDigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 4. ASYMMETRIC SLANTED DIGITAL (2x2 / Minimal Bottom-Weighted Layout)
class ClockDigitalAsymmetricSlantedReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateAsymmetricSlantedDigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 5. COMPACT BLOCK DIGITAL (2x2 / 4-Digit Block Time in Condensed Font)
class ClockDigitalCompactBlockReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateCompactBlockDigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 6. ASYMMETRIC OVERLAY DIGITAL (2x2 / Translucent Giant Right Time with Bottom-Left Date)
class ClockDigitalAsymmetricOverlayReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateAsymmetricOverlayDigitalClockBitmap(context, config, isResponsive, wDp, hDp)
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

// 10. GRADIENT TALL DIGITAL (4x2 / Responsive Tall Time with Gradient)
class ClockDigitalGradientTallReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateGradientTallDigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 11. SCRIPT OVERLAY DIGITAL (4x2 / Giant Pastel Time with Cursive Day & Uppercase Date)
class ClockDigitalScriptOverlayReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateScriptOverlayDigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 12. SPLIT FLAP DIGITAL (4x2 / Dual Flip Card Time with Centered Date)
class ClockDigitalSplitFlapReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateSplitFlapDigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 13. VERTICAL CAPSULE DIGITAL (1x2 / Pill Capsule Monolith with Center Date Badge)
class ClockDigitalVerticalCapsuleReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateVerticalCapsuleDigitalClockBitmap(context, config, isResponsive, wDp, hDp)
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