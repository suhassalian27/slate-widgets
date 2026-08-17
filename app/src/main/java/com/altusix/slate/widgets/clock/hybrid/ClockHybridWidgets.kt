package com.altusix.slate.widgets.clock.hybrid

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import com.altusix.slate.core.model.SlateWidgetInfo
import com.altusix.slate.core.service.SlateClockTickerService
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.widgets.clock.digital.BaseDigitalClockReceiver


// --- CATALOG REGISTRATION ---

fun getClockHybridWidgetsCatalog(): List<SlateWidgetInfo> {
    return listOf(
        SlateWidgetInfo(name = "Analog Digital Split", sizeText = "4x2", category = "Clock – Hybrid", receiverClass = ClockHybridAnalogDigitalSplitReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Bold Typographic Digital", "2x2", "Clock – Digital", ClockDigitalBoldTypographicReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("LCD Seven Segment Digital", "2x2", "Clock – Digital", ClockDigitalLcdSevenSegmentReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Asymmetric Slanted Digital", "2x2", "Clock – Digital", ClockDigitalAsymmetricSlantedReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Asymmetric Overlay Digital", "2x2", "Clock – Digital", ClockDigitalAsymmetricOverlayReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Gradient Tall Digital", "4x2", "Clock – Digital", ClockDigitalGradientTallReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Script Overlay Digital", "4x2", "Clock – Digital", ClockDigitalScriptOverlayReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Split Flap Digital", "4x2", "Clock – Digital", ClockDigitalSplitFlapReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Vertical Capsule Digital", "1x2", "Clock – Digital", ClockDigitalVerticalCapsuleReceiver::class.java, hasModeOption = true),
    )
}

fun updateAllClockHybridWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val receivers = listOf(
        ClockHybridAnalogDigitalSplitReceiver::class.java,
        ClockDigitalBoldTypographicReceiver::class.java,
        ClockDigitalLcdSevenSegmentReceiver::class.java,
        ClockDigitalAsymmetricSlantedReceiver::class.java,
        ClockDigitalAsymmetricOverlayReceiver::class.java,
        ClockDigitalGradientTallReceiver::class.java,
        ClockDigitalScriptOverlayReceiver::class.java,
        ClockDigitalSplitFlapReceiver::class.java,
        ClockDigitalVerticalCapsuleReceiver::class.java,
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

// --- WIDGET RECEIVERS ---

// 1. ANALOG DIGITAL SPLIT HYBRID (4x2 / Minimal Dial Left, Digital Time & Date Right)
class ClockHybridAnalogDigitalSplitReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(
        context: Context,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap = generateAnalogDigitalSplitHybridClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 2. BOLD TYPOGRAPHIC DIGITAL (2x2 Square / Stacked Giant Hour & Minute Display)
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

// 5. ASYMMETRIC OVERLAY DIGITAL (2x2 / Translucent Giant Right Time with Bottom-Left Date)
class ClockDigitalAsymmetricOverlayReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateAsymmetricOverlayDigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 6. GRADIENT TALL DIGITAL (4x2 / Responsive Tall Time with Gradient)
class ClockDigitalGradientTallReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateGradientTallDigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 7. SCRIPT OVERLAY DIGITAL (4x2 / Giant Pastel Time with Cursive Day & Uppercase Date)
class ClockDigitalScriptOverlayReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateScriptOverlayDigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 8. SPLIT FLAP DIGITAL (4x2 / Dual Flip Card Time with Centered Date)
class ClockDigitalSplitFlapReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateSplitFlapDigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 9. VERTICAL CAPSULE DIGITAL (1x2 / Pill Capsule Monolith with Center Date Badge)
class ClockDigitalVerticalCapsuleReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateVerticalCapsuleDigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}