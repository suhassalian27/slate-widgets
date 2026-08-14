package com.altusix.slate.widgets.clock.analog

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.altusix.slate.core.model.SlateWidgetInfo
import com.altusix.slate.core.service.SlateClockTickerService
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.widgets.calendar.BaseCalendarReceiver
import com.altusix.slate.R
import androidx.core.content.res.ResourcesCompat

fun getClockAnalogWidgetsCatalog(): List<SlateWidgetInfo> {
    return listOf(
        SlateWidgetInfo("Analog Precision Dial", "2x2", "Clock – Analog", ClockAnalogPrecisionReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Bauhaus Geometric Dial", "2x2", "Clock – Analog", ClockBauhausReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Cyber Skeleton Ring Dial", "2x2", "Clock – Analog", ClockCyberSkeletonReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Sculpted Pill Minimal Dial", "2x2", "Clock – Analog", ClockSculptedPillReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Bold Typographic Cardinal Dial", "2x2", "Clock – Analog", ClockBoldTypographyReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Cyber Condensed Cardinal Dial", "2x2", "Clock – Analog", ClockCyberCondensedReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Capsule Skeleton Accent Dial", "2x2", "Clock – Analog", ClockCapsuleSkeletonReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Apex Arrowhead Cardinal Dial", "2x2", "Clock – Analog", ClockApexArrowheadReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Concentric Orbital Arc Dial", "2x2", "Clock – Analog", ClockConcentricOrbitalReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Triple Orbital Dots Dial", "2x2", "Clock – Analog", ClockTripleOrbitalDotsReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Sector Sweep Accent Dial", "2x2", "Clock – Analog", ClockSectorSweepReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Triple Rotating Ring Dial", "2x2", "Clock – Analog", ClockRotatingRingReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Hourglass Dynamic Accent Dial", "2x2", "Clock – Analog", ClockHourglassReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Minimal Dot Matrix Dial", "2x2", "Clock – Analog", ClockMinimalDotsReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Tactical Radar Scope Dial", "2x2", "Clock – Analog", ClockRadarScopeReceiver::class.java, hasModeOption = false)
    )
}

fun updateAllClockAnalogWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val receivers = listOf(
        ClockAnalogPrecisionReceiver::class.java,
        ClockBauhausReceiver::class.java,
        ClockCyberSkeletonReceiver::class.java,
        ClockSculptedPillReceiver::class.java,
        ClockBoldTypographyReceiver::class.java,
        ClockCyberCondensedReceiver::class.java,
        ClockCapsuleSkeletonReceiver::class.java,
        ClockApexArrowheadReceiver::class.java,
        ClockConcentricOrbitalReceiver::class.java,
        ClockTripleOrbitalDotsReceiver::class.java,
        ClockSectorSweepReceiver::class.java,
        ClockRotatingRingReceiver::class.java,
        ClockHourglassReceiver::class.java,
        ClockMinimalDotsReceiver::class.java,
        ClockRadarScopeReceiver::class.java
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

// 1. ANALOG PRECISION DIAL (2x2 Square)
class ClockAnalogPrecisionReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateAnalogPrecisionClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.stopService(Intent(context, SlateClockTickerService::class.java))
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }
}

// 2. BAUHAUS GEOMETRIC DIAL (2x2 Square)
class ClockBauhausReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateBauhausClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.stopService(Intent(context, SlateClockTickerService::class.java))
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }
}

// 3. CYBER SKELETON RING DIAL (2x2 Square / Circular Skeleton Face)
class ClockCyberSkeletonReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateCyberSkeletonClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.stopService(Intent(context, SlateClockTickerService::class.java))
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }
}

// 4. SCULPTED PILL MINIMAL DIAL (2x2 Square / Ultra-Minimal Capsule Face)
class ClockSculptedPillReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateSculptedPillClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.stopService(Intent(context, SlateClockTickerService::class.java))
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }
}

// 5. BOLD TYPOGRAPHIC CARDINAL DIAL (2x2 Square)
class ClockBoldTypographyReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateBoldTypographyClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.stopService(Intent(context, SlateClockTickerService::class.java))
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }
}

// 6. CYBER CONDENSED CARDINAL DIAL (2x2 Circular)
class ClockCyberCondensedReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateCyberCondensedClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.stopService(Intent(context, SlateClockTickerService::class.java))
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }
}

// 7. CAPSULE SKELETON ACCENT DIAL (2x2 Circular)
class ClockCapsuleSkeletonReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateCapsuleSkeletonClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.stopService(Intent(context, SlateClockTickerService::class.java))
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }
}

// 8. APEX ARROWHEAD CARDINAL DIAL (2x2 Circular)
class ClockApexArrowheadReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateApexArrowheadClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.stopService(Intent(context, SlateClockTickerService::class.java))
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }
}

// 9. CONCENTRIC ORBITAL ARC DIAL (2x2 Circular)
class ClockConcentricOrbitalReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateConcentricOrbitalClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.stopService(Intent(context, SlateClockTickerService::class.java))
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }
}

// 10. TRIPLE ORBITAL DOTS DIAL (2x2 Circular)
class ClockTripleOrbitalDotsReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateTripleOrbitalDotsClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.stopService(Intent(context, SlateClockTickerService::class.java))
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }
}

// 11. SECTOR SWEEP ACCENT DIAL (2x2 Circular)
class ClockSectorSweepReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateSectorSweepClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.stopService(Intent(context, SlateClockTickerService::class.java))
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }
}

// 12. TRIPLE ROTATING RING DIAL (2x2 Circular)
class ClockRotatingRingReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateRotatingRingClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.stopService(Intent(context, SlateClockTickerService::class.java))
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }
}

// 13. HOURGLASS DYNAMIC ACCENT DIAL (2x2 Square)
class ClockHourglassReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateHourglassClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.stopService(Intent(context, SlateClockTickerService::class.java))
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }
}

// 14. MINIMAL DOT MATRIX DIAL (2x2 Circular)
class ClockMinimalDotsReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateMinimalDotsClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.stopService(Intent(context, SlateClockTickerService::class.java))
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }
}

// 15. TACTICAL RADAR SCOPE DIAL (2x2 Circular)
class ClockRadarScopeReceiver : BaseCalendarReceiver() {
    override fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int) =
        generateRadarScopeClockBitmap(context, config, isResponsive, wDp, hDp)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.stopService(Intent(context, SlateClockTickerService::class.java))
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        context.startService(Intent(context, SlateClockTickerService::class.java))
    }
}