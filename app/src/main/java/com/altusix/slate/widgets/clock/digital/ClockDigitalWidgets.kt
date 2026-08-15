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
        SlateWidgetInfo("Modern 3D LED Digital", "4x2", "Clock – Digital", ClockDigitalModern3dLedReceiver::class.java, hasModeOption = false)
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
        ClockDigitalModern3dLedReceiver::class.java
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

            val themeMode = widgetPrefs.getString("widget_${id}_theme_mode", "DARK") ?: "DARK"
            val defaultBg = if (themeMode == "LIGHT") 0xFFFFFFFFL else 0xFF161618L
            val defaultAccent = if (themeMode == "LIGHT") 0xFF000000L else 0xFFFFFFFFL

            val bgColor = widgetPrefs.getLong("widget_${id}_bg_color", defaultBg)
            val opacity = widgetPrefs.getFloat("widget_${id}_opacity", 1.0f)
            val accentColor = widgetPrefs.getLong("widget_${id}_accent_color", defaultAccent)

            val config = SlateWidgetConfig(themeMode, bgColor, opacity, accentColor)

            val keyWMode = "widget_${id}_mode"
            val keyWResponsive = "widget_${id}_is_responsive"

            val isResponsive = when {
                widgetPrefs.contains(keyWMode) -> {
                    widgetPrefs.getString(keyWMode, "RESPONSIVE").equals("RESPONSIVE", ignoreCase = true)
                }
                widgetPrefs.contains(keyWResponsive) -> {
                    widgetPrefs.getBoolean(keyWResponsive, true)
                }
                else -> true
            }

            val options = manager.getAppWidgetOptions(id)
            val minW = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) ?: 0
            val minH = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) ?: 0
            val maxW = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH) ?: 0
            val maxH = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT) ?: 0

            val wDp = maxOf(minW, maxW, 140).coerceAtMost(220)
            val hDp = maxOf(minH, maxH, 60).coerceAtMost(220)

            val rawBitmap = renderBitmap(context, config, isResponsive, wDp, hDp)
            val bitmap = scaleBitmapForIPC(rawBitmap, maxDimensionPx = 600)

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