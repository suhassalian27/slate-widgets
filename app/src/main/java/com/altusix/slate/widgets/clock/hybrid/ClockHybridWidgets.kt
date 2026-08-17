package com.altusix.slate.widgets.clock.hybrid

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import com.altusix.slate.core.model.SlateWidgetInfo
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.widgets.clock.digital.BaseDigitalClockReceiver

// --- CATALOG REGISTRATION ---

fun getClockHybridWidgetsCatalog(): List<SlateWidgetInfo> {
    return listOf(
        SlateWidgetInfo(name = "Analog Digital Split", sizeText = "4x2", category = "Clock – Hybrid", receiverClass = ClockHybridAnalogDigitalSplitReceiver::class.java, hasModeOption = true)
    )
}

fun updateAllClockHybridWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val receivers = listOf(
        ClockHybridAnalogDigitalSplitReceiver::class.java
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