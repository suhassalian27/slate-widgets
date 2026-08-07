package com.altusix.slate.widgets.ai

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import com.altusix.slate.core.receiver.BaseCanvasWidgetProvider
import com.altusix.slate.data.local.SlateWidgetConfig

// ============================================================================
// BARS (4x1)
// ============================================================================

class AiBarPrimaryReceiver : BaseCanvasWidgetProvider() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val wPx = (wDp * density).toInt()
        val hPx = (hDp * density).toInt()
        return generateAiBarHeroPrimaryBitmap(context, config, wPx, hPx)
    }
}

class AiBarDock5Receiver : BaseCanvasWidgetProvider() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val wPx = (wDp * density).toInt()
        val hPx = (hDp * density).toInt()
        return generateAiBarDock5Bitmap(context, config, wPx, hPx)
    }
}

class AiBarCapsuleReceiver : BaseCanvasWidgetProvider() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val wPx = (wDp * density).toInt()
        val hPx = (hDp * density).toInt()
        return generateAiBarCapsuleBitmap(context, config, wPx, hPx)
    }
}

class AiBarDualFlagshipReceiver : BaseCanvasWidgetProvider() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val wPx = (wDp * density).toInt()
        val hPx = (hDp * density).toInt()
        return generateAiBarDualFlagshipBitmap(context, config, wPx, hPx)
    }
}

// ============================================================================
// FOLDERS (2x2 / 4x2)
// ============================================================================

class AiFolder4ClassicReceiver : BaseCanvasWidgetProvider() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val wPx = (wDp * density).toInt()
        val hPx = (hDp * density).toInt()
        return generateAiFolder4ClassicBitmap(context, config, wPx, hPx)
    }
}

class AiFolder6BentoHeroReceiver : BaseCanvasWidgetProvider() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val wPx = (wDp * density).toInt()
        val hPx = (hDp * density).toInt()
        return generateAiFolder6BentoHeroBitmap(context, config, wPx, hPx)
    }
}

class AiFolder8BentoSideReceiver : BaseCanvasWidgetProvider() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val wPx = (wDp * density).toInt()
        val hPx = (hDp * density).toInt()
        return generateAiFolder8BentoSideBitmap(context, config, wPx, hPx)
    }
}

class AiFolder9GridReceiver : BaseCanvasWidgetProvider() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val wPx = (wDp * density).toInt()
        val hPx = (hDp * density).toInt()
        return generateAiFolder9GridBitmap(context, config, wPx, hPx)
    }
}

class AiFolder10MegaReceiver : BaseCanvasWidgetProvider() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val wPx = (wDp * density).toInt()
        val hPx = (hDp * density).toInt()
        return generateAiFolder10MegaBitmap(context, config, wPx, hPx)
    }
}

class AiFolder7AsymmetricReceiver : BaseCanvasWidgetProvider() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val wPx = (wDp * density).toInt()
        val hPx = (hDp * density).toInt()
        return generateAiFolder7AsymmetricBitmap(context, config, wPx, hPx)
    }
}

fun updateAllAiFolderWidgets(context: Context) {
    val receivers = listOf(
        AiBarPrimaryReceiver::class.java,
        AiBarDock5Receiver::class.java,
        AiBarCapsuleReceiver::class.java,
        AiBarDualFlagshipReceiver::class.java,
        AiFolder4ClassicReceiver::class.java,
        AiFolder6BentoHeroReceiver::class.java,
        AiFolder8BentoSideReceiver::class.java,
        AiFolder9GridReceiver::class.java,
        AiFolder10MegaReceiver::class.java,
        AiFolder7AsymmetricReceiver::class.java
    )

    val manager = AppWidgetManager.getInstance(context)
    for (receiver in receivers) {
        val ids = manager.getAppWidgetIds(ComponentName(context, receiver))
        if (ids.isNotEmpty()) {
            val intent = Intent(context, receiver).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}