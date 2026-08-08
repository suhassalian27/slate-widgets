package com.altusix.slate.widgets.ai

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import com.altusix.slate.core.receiver.BaseCanvasWidgetProvider
import com.altusix.slate.data.local.SlateWidgetConfig

// ============================================================================
// HELPER: AI App Launch Pending Intent
// ============================================================================

fun getAiAppLaunchPendingIntent(context: Context, packageName: String, requestCode: Int): PendingIntent? {
    val pm = context.packageManager
    val launchIntent = pm.getLaunchIntentForPackage(packageName) ?: return null
    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return PendingIntent.getActivity(
        context,
        requestCode,
        launchIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

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

    override fun getClickPendingIntent(context: Context, appWidgetId: Int): PendingIntent? {
        return getAiAppLaunchPendingIntent(context, "com.google.android.apps.bard", appWidgetId)
            ?: getAiAppLaunchPendingIntent(context, "com.google.android.googlequicksearchbox", appWidgetId)
    }
}

class AiBarDock5Receiver : BaseCanvasWidgetProvider() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val wPx = (wDp * density).toInt()
        val hPx = (hDp * density).toInt()
        return generateAiBarDock5Bitmap(context, config, wPx, hPx)
    }

    override fun getClickPendingIntent(context: Context, appWidgetId: Int): PendingIntent? {
        return getAiAppLaunchPendingIntent(context, "com.google.android.apps.bard", appWidgetId)
    }
}

class AiBarCapsuleReceiver : BaseCanvasWidgetProvider() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val wPx = (wDp * density).toInt()
        val hPx = (hDp * density).toInt()
        return generateAiBarCapsuleBitmap(context, config, wPx, hPx)
    }

    override fun getClickPendingIntent(context: Context, appWidgetId: Int): PendingIntent? {
        return getAiAppLaunchPendingIntent(context, "com.openai.chatgpt", appWidgetId)
    }
}

class AiBarDualFlagshipReceiver : BaseCanvasWidgetProvider() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val wPx = (wDp * density).toInt()
        val hPx = (hDp * density).toInt()
        return generateAiBarDualFlagshipBitmap(context, config, wPx, hPx)
    }

    override fun getClickPendingIntent(context: Context, appWidgetId: Int): PendingIntent? {
        return getAiAppLaunchPendingIntent(context, "com.openai.chatgpt", appWidgetId)
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

    override fun getClickPendingIntent(context: Context, appWidgetId: Int): PendingIntent? {
        return getAiAppLaunchPendingIntent(context, "com.google.android.apps.bard", appWidgetId)
    }
}

class AiFolder6BentoHeroReceiver : BaseCanvasWidgetProvider() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val wPx = (wDp * density).toInt()
        val hPx = (hDp * density).toInt()
        return generateAiFolder6BentoHeroBitmap(context, config, wPx, hPx)
    }

    override fun getClickPendingIntent(context: Context, appWidgetId: Int): PendingIntent? {
        return getAiAppLaunchPendingIntent(context, "com.google.android.apps.bard", appWidgetId)
    }
}

class AiFolder8BentoSideReceiver : BaseCanvasWidgetProvider() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val wPx = (wDp * density).toInt()
        val hPx = (hDp * density).toInt()
        return generateAiFolder8BentoSideBitmap(context, config, wPx, hPx)
    }

    override fun getClickPendingIntent(context: Context, appWidgetId: Int): PendingIntent? {
        return getAiAppLaunchPendingIntent(context, "com.google.android.apps.bard", appWidgetId)
    }
}

class AiFolder9GridReceiver : BaseCanvasWidgetProvider() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val wPx = (wDp * density).toInt()
        val hPx = (hDp * density).toInt()
        return generateAiFolder9GridBitmap(context, config, wPx, hPx)
    }

    override fun getClickPendingIntent(context: Context, appWidgetId: Int): PendingIntent? {
        return getAiAppLaunchPendingIntent(context, "com.google.android.apps.bard", appWidgetId)
    }
}

class AiFolder10MegaReceiver : BaseCanvasWidgetProvider() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val wPx = (wDp * density).toInt()
        val hPx = (hDp * density).toInt()
        return generateAiFolder10MegaBitmap(context, config, wPx, hPx)
    }

    override fun getClickPendingIntent(context: Context, appWidgetId: Int): PendingIntent? {
        return getAiAppLaunchPendingIntent(context, "com.google.android.apps.bard", appWidgetId)
    }
}

class AiFolder7AsymmetricReceiver : BaseCanvasWidgetProvider() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val wPx = (wDp * density).toInt()
        val hPx = (hDp * density).toInt()
        return generateAiFolder7AsymmetricBitmap(context, config, wPx, hPx)
    }

    override fun getClickPendingIntent(context: Context, appWidgetId: Int): PendingIntent? {
        return getAiAppLaunchPendingIntent(context, "com.openai.chatgpt", appWidgetId)
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