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
        SlateWidgetInfo(name = "Minimal Dial Hybrid", sizeText = "2x2", category = "Clock – Hybrid", receiverClass = ClockHybridMinimalDialReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Bold Typographic Hybrid", "2x2", "Clock – Hybrid", ClockHybridBoldTypographicReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("LCD Seven Segment Hybrid", "2x2", "Clock – Hybrid", ClockHybridLcdSevenSegmentReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Asymmetric Slanted Hybrid", "2x2", "Clock – Hybrid", ClockHybridAsymmetricSlantedReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Asymmetric Overlay Hybrid", "2x2", "Clock – Hybrid", ClockHybridAsymmetricOverlayReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Gradient Tall Hybrid", "4x2", "Clock – Hybrid", ClockHybridGradientTallReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Script Overlay Hybrid", "4x2", "Clock – Hybrid", ClockHybridScriptOverlayReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Split Flap Hybrid", "4x2", "Clock – Hybrid", ClockHybridSplitFlapReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Vertical Capsule Hybrid", "1x2", "Clock – Hybrid", ClockHybridVerticalCapsuleReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Pill Capsule Hybrid", sizeText = "1x2", category = "Clock – Hybrid", receiverClass = ClockHybridPillCapsuleReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo(name = "Overlapping Typography Hybrid", sizeText = "2x2", category = "Clock – Hybrid", receiverClass = ClockHybridOverlappingTypographyReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo(name = "Giant Hour Typographic Hybrid", sizeText = "2x2", category = "Clock – Hybrid", receiverClass = ClockHybridGiantHourReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo(name = "Squircle Perimeter Tick Hybrid", sizeText = "2x2", category = "Clock – Hybrid", receiverClass = ClockHybridSquircleTickReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo(name = "Arc Date Wedge Hybrid", sizeText = "2x2", category = "Clock – Hybrid", receiverClass = ClockHybridArcDateWedgeReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo(name = "Horizontal Pill Hybrid", sizeText = "2x1", category = "Clock – Hybrid", receiverClass = ClockHybridHorizontalPillReceiver::class.java, hasModeOption = false)
    )
}

fun updateAllClockHybridWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val receivers = listOf(
        ClockHybridAnalogDigitalSplitReceiver::class.java,
        ClockHybridMinimalDialReceiver::class.java,
        ClockHybridBoldTypographicReceiver::class.java,
        ClockHybridLcdSevenSegmentReceiver::class.java,
        ClockHybridAsymmetricSlantedReceiver::class.java,
        ClockHybridAsymmetricOverlayReceiver::class.java,
        ClockHybridGradientTallReceiver::class.java,
        ClockHybridScriptOverlayReceiver::class.java,
        ClockHybridSplitFlapReceiver::class.java,
        ClockHybridVerticalCapsuleReceiver::class.java,
        ClockHybridPillCapsuleReceiver::class.java,
        ClockHybridOverlappingTypographyReceiver::class.java,
        ClockHybridGiantHourReceiver::class.java,
        ClockHybridSquircleTickReceiver::class.java,
        ClockHybridArcDateWedgeReceiver::class.java,
        ClockHybridHorizontalPillReceiver::class.java
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

// 2. MINIMAL DIAL HYBRID (2x2 / Clean Analog Center, Stacked HH/MM Top-Right, Accent Day)
class ClockHybridMinimalDialReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(
        context: Context,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap = generateMinimalDialHybridClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 3. BOLD TYPOGRAPHIC HYBRID (2x2 Square / Stacked Giant Hour & Minute Display)
class ClockHybridBoldTypographicReceiver : BaseDigitalClockReceiver() {
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

// 4. LCD SEVEN SEGMENT HYBRID (2x2 / Stacked 7-Segment LCD with Vertical Date)
class ClockHybridLcdSevenSegmentReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateLcdSevenSegmentDigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 5. ASYMMETRIC SLANTED HYBRID (2x2 / Minimal Bottom-Weighted Layout)
class ClockHybridAsymmetricSlantedReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateAsymmetricSlantedDigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 6. ASYMMETRIC OVERLAY HYBRID (2x2 / Translucent Giant Right Time with Bottom-Left Date)
class ClockHybridAsymmetricOverlayReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateAsymmetricOverlayDigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 7. GRADIENT TALL HYBRID (4x2 / Responsive Tall Time with Gradient)
class ClockHybridGradientTallReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateGradientTallDigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 8. SCRIPT OVERLAY HYBRID (4x2 / Giant Pastel Time with Cursive Day & Uppercase Date)
class ClockHybridScriptOverlayReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateScriptOverlayDigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 9. SPLIT FLAP HYBRID (4x2 / Dual Flip Card Time with Centered Date)
class ClockHybridSplitFlapReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateSplitFlapDigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 10. VERTICAL CAPSULE HYBRID (1x2 / Pill Capsule Monolith with Center Date Badge)
class ClockHybridVerticalCapsuleReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateVerticalCapsuleDigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 11. PILL CAPSULE HYBRID (1x2 / Top Digital Time, Bottom Analog Dial & Accent Date Badge)
class ClockHybridPillCapsuleReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(
        context: Context,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap = generatePillCapsuleHybridClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 12. OVERLAPPING TYPOGRAPHY HYBRID (2x2 / Dual-Tone Giant Numbers & Skeleton Hands)
class ClockHybridOverlappingTypographyReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(
        context: Context,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap = generateOverlappingTypographicHybridClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 13. GIANT HOUR TYPOGRAPHIC HYBRID (2x2 / Circular Watch Face with Bottom Giant Hour & Mid-Right Stack)
class ClockHybridGiantHourReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(
        context: Context,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap = generateGiantHourTypographicHybridClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 14. SQUIRCLE PERIMETER TICK HYBRID (2x2 / Contour Ticks & Bold Time)
class ClockHybridSquircleTickReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(
        context: Context,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap = generateSquircleTickDigitalClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 15. ARC DATE WEDGE HYBRID (2x2 / Arc Date & Triangular Wedge Hour Hand)
class ClockHybridArcDateWedgeReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(
        context: Context,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap = generateArcDateWedgeClockBitmap(context, config, isResponsive, wDp, hDp)
}

// 16. HORIZONTAL PILL HYBRID (2x1 / Left Dial & Right Digital Time)
class ClockHybridHorizontalPillReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(
        context: Context,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap = generateHorizontalPillHybridClockBitmap(context, config, isResponsive, wDp, hDp)
}