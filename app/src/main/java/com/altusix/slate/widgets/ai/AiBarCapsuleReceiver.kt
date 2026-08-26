package com.altusix.slate.widgets.ai

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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

class AiBarPrimaryReceiver : BaseAiReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateAiBarHeroPrimaryBitmap(context, config, isResponsive, wDp, hDp, widgetId)

    override fun getClickPendingIntent(context: Context, widgetId: Int): PendingIntent? {
        return getAiAppLaunchPendingIntent(context, "com.google.android.apps.bard", widgetId)
            ?: getAiAppLaunchPendingIntent(context, "com.google.android.googlequicksearchbox", widgetId)
    }
}

class AiBarDock5Receiver : BaseAiReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateAiBarDock5Bitmap(context, config, isResponsive, wDp, hDp, widgetId)

    override fun getClickPendingIntent(context: Context, widgetId: Int): PendingIntent? {
        return getAiAppLaunchPendingIntent(context, "com.google.android.apps.bard", widgetId)
    }
}

class AiBarCapsuleReceiver : BaseAiReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateAiBarCapsuleBitmap(context, config, isResponsive, wDp, hDp, widgetId)

    override fun getClickPendingIntent(context: Context, widgetId: Int): PendingIntent? {
        return getAiAppLaunchPendingIntent(context, "com.openai.chatgpt", widgetId)
    }
}

class AiBarDualFlagshipReceiver : BaseAiReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateAiBarDualFlagshipBitmap(context, config, isResponsive, wDp, hDp, widgetId)

    override fun getClickPendingIntent(context: Context, widgetId: Int): PendingIntent? {
        return getAiAppLaunchPendingIntent(context, "com.openai.chatgpt", widgetId)
    }
}

// ============================================================================
// FOLDERS (2x2 / 4x2)
// ============================================================================

class AiFolder4ClassicReceiver : BaseAiReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateAiFolder4ClassicBitmap(context, config, isResponsive, wDp, hDp, widgetId)

    override fun getClickPendingIntent(context: Context, widgetId: Int): PendingIntent? {
        return getAiAppLaunchPendingIntent(context, "com.google.android.apps.bard", widgetId)
    }
}

class AiFolder6BentoHeroReceiver : BaseAiReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateAiFolder6BentoHeroBitmap(context, config, isResponsive, wDp, hDp, widgetId)

    override fun getClickPendingIntent(context: Context, widgetId: Int): PendingIntent? {
        return getAiAppLaunchPendingIntent(context, "com.google.android.apps.bard", widgetId)
    }
}

class AiFolder8BentoSideReceiver : BaseAiReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateAiFolder8BentoSideBitmap(context, config, isResponsive, wDp, hDp, widgetId)

    override fun getClickPendingIntent(context: Context, widgetId: Int): PendingIntent? {
        return getAiAppLaunchPendingIntent(context, "com.google.android.apps.bard", widgetId)
    }
}

class AiFolder9GridReceiver : BaseAiReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateAiFolder9GridBitmap(context, config, isResponsive, wDp, hDp, widgetId)

    override fun getClickPendingIntent(context: Context, widgetId: Int): PendingIntent? {
        return getAiAppLaunchPendingIntent(context, "com.google.android.apps.bard", widgetId)
    }
}

class AiFolder10MegaReceiver : BaseAiReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateAiFolder10MegaBitmap(context, config, isResponsive, wDp, hDp, widgetId)

    override fun getClickPendingIntent(context: Context, widgetId: Int): PendingIntent? {
        return getAiAppLaunchPendingIntent(context, "com.google.android.apps.bard", widgetId)
    }
}

class AiFolder7AsymmetricReceiver : BaseAiReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateAiFolder7AsymmetricBitmap(context, config, isResponsive, wDp, hDp, widgetId)

    override fun getClickPendingIntent(context: Context, widgetId: Int): PendingIntent? {
        return getAiAppLaunchPendingIntent(context, "com.openai.chatgpt", widgetId)
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