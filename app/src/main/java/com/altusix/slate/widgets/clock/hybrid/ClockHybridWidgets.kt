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
        SlateWidgetInfo(name = "Horizontal Pill Hybrid", sizeText = "2x1", category = "Clock – Hybrid", receiverClass = ClockHybridHorizontalPillReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo(name = "Minimal Capsule Pill", sizeText = "2x1", category = "Clock – Hybrid", receiverClass = ClockHybridMinimalCapsulePillReceiver::class.java, hasModeOption = false)
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
        ClockHybridHorizontalPillReceiver::class.java,
        ClockHybridMinimalCapsulePillReceiver::class.java
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

// 1. ANALOG DIGITAL SPLIT HYBRID (4x2)
class ClockHybridAnalogDigitalSplitReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateAnalogDigitalSplitHybridClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        SlateClockTickerService.ensureServiceStarted(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        SlateClockTickerService.ensureServiceStarted(context)
    }
}

// 2. MINIMAL DIAL HYBRID (2x2)
class ClockHybridMinimalDialReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateMinimalDialHybridClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        SlateClockTickerService.ensureServiceStarted(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        SlateClockTickerService.ensureServiceStarted(context)
    }
}

// 3. BOLD TYPOGRAPHIC HYBRID (2x2)
class ClockHybridBoldTypographicReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateBoldTypographicDigitalClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        SlateClockTickerService.ensureServiceStarted(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        SlateClockTickerService.ensureServiceStarted(context)
    }
}

// 4. LCD SEVEN SEGMENT HYBRID (2x2)
class ClockHybridLcdSevenSegmentReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateLcdSevenSegmentDigitalClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        SlateClockTickerService.ensureServiceStarted(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        SlateClockTickerService.ensureServiceStarted(context)
    }
}

// 5. ASYMMETRIC SLANTED HYBRID (2x2)
class ClockHybridAsymmetricSlantedReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateAsymmetricSlantedDigitalClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        SlateClockTickerService.ensureServiceStarted(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        SlateClockTickerService.ensureServiceStarted(context)
    }
}

// 6. ASYMMETRIC OVERLAY HYBRID (2x2)
class ClockHybridAsymmetricOverlayReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateAsymmetricOverlayDigitalClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        SlateClockTickerService.ensureServiceStarted(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        SlateClockTickerService.ensureServiceStarted(context)
    }
}

// 7. GRADIENT TALL HYBRID (4x2)
class ClockHybridGradientTallReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateGradientTallDigitalClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        SlateClockTickerService.ensureServiceStarted(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        SlateClockTickerService.ensureServiceStarted(context)
    }
}

// 8. SCRIPT OVERLAY HYBRID (4x2)
class ClockHybridScriptOverlayReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateScriptOverlayDigitalClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        SlateClockTickerService.ensureServiceStarted(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        SlateClockTickerService.ensureServiceStarted(context)
    }
}

// 9. SPLIT FLAP HYBRID (4x2)
class ClockHybridSplitFlapReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateSplitFlapDigitalClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        SlateClockTickerService.ensureServiceStarted(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        SlateClockTickerService.ensureServiceStarted(context)
    }
}

// 10. VERTICAL CAPSULE HYBRID (1x2)
class ClockHybridVerticalCapsuleReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateVerticalCapsuleDigitalClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        SlateClockTickerService.ensureServiceStarted(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        SlateClockTickerService.ensureServiceStarted(context)
    }
}

// 11. PILL CAPSULE HYBRID (1x2)
class ClockHybridPillCapsuleReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generatePillCapsuleHybridClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        SlateClockTickerService.ensureServiceStarted(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        SlateClockTickerService.ensureServiceStarted(context)
    }
}

// 12. OVERLAPPING TYPOGRAPHY HYBRID (2x2)
class ClockHybridOverlappingTypographyReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateOverlappingTypographicHybridClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        SlateClockTickerService.ensureServiceStarted(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        SlateClockTickerService.ensureServiceStarted(context)
    }
}

// 13. GIANT HOUR TYPOGRAPHIC HYBRID (2x2)
class ClockHybridGiantHourReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateGiantHourTypographicHybridClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        SlateClockTickerService.ensureServiceStarted(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        SlateClockTickerService.ensureServiceStarted(context)
    }
}

// 14. SQUIRCLE PERIMETER TICK HYBRID (2x2)
class ClockHybridSquircleTickReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateSquircleTickDigitalClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        SlateClockTickerService.ensureServiceStarted(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        SlateClockTickerService.ensureServiceStarted(context)
    }
}

// 15. ARC DATE WEDGE HYBRID (2x2)
class ClockHybridArcDateWedgeReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateArcDateWedgeClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        SlateClockTickerService.ensureServiceStarted(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        SlateClockTickerService.ensureServiceStarted(context)
    }
}

// 16. HORIZONTAL PILL HYBRID (2x1)
class ClockHybridHorizontalPillReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateHorizontalPillHybridClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        SlateClockTickerService.ensureServiceStarted(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        SlateClockTickerService.ensureServiceStarted(context)
    }
}

// 17. MINIMAL CAPSULE PILL (2x1)
class ClockHybridMinimalCapsulePillReceiver : BaseDigitalClockReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateMinimalCapsulePillClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        SlateClockTickerService.ensureServiceStarted(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        SlateClockTickerService.ensureServiceStarted(context)
    }
}