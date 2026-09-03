package com.altusix.slate.widgets.ai

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews
import com.altusix.slate.R
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

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            val options = appWidgetManager.getAppWidgetOptions(widgetId)
            renderAndApplyWidget(context, appWidgetManager, widgetId, options)
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle?) {
        renderAndApplyWidget(context, appWidgetManager, appWidgetId, newOptions)
    }

    override fun renderBitmapForWidget(
        context: Context,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int,
        widgetId: Int
    ): Bitmap = generateAiFolder9GridBitmap(context, config, isResponsive, wDp, hDp, widgetId)

    fun renderAndApplyWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int,
        options: Bundle?
    ) {
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 160) ?: 160 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 160) ?: 160
        val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 160) ?: 160 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 160) ?: 160
        val wDp = if (wDpRaw <= 0) 160 else wDpRaw
        val hDp = if (hDpRaw <= 0) 160 else hDpRaw

        val isResponsive = parseAndLockIsResponsive(context, widgetId)
        val config = loadSlateWidgetConfig(context, widgetId)

        val views = RemoteViews(context.packageName, R.layout.widget_base_grid_3x3_layout)
        val bitmap = generateAiFolder9GridBitmap(context, config, isResponsive, wDp, hDp, widgetId)
        views.setImageViewBitmap(R.id.widget_image_view, bitmap)

        val pm = context.packageManager
        fun safeAiIntent(pkg: String, webUrl: String): Intent {
            return pm.getLaunchIntentForPackage(pkg)?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            } ?: Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }

        // 9 distinct AI launch targets matching the grid order
        val intents = listOf(
            // Row 0
            safeAiIntent("com.google.android.apps.bard", "https://gemini.google.com"),
            safeAiIntent("com.openai.chatgpt", "https://chatgpt.com"),
            safeAiIntent("com.microsoft.copilot", "https://copilot.microsoft.com"),
            // Row 1
            safeAiIntent("com.x.android", "https://x.com/i/grok"),
            safeAiIntent("com.anthropic.claude", "https://claude.ai"),
            safeAiIntent("com.deepseek.chat", "https://chat.deepseek.com"),
            // Row 2
            safeAiIntent("ai.perplexity.app.android", "https://www.perplexity.ai"),
            safeAiIntent("com.facebook.katana", "https://www.meta.ai"),
            safeAiIntent("com.quora.poe.android", "https://poe.com")
        )

        val slotIds = intArrayOf(
            R.id.slot_0, R.id.slot_1, R.id.slot_2,
            R.id.slot_3, R.id.slot_4, R.id.slot_5,
            R.id.slot_6, R.id.slot_7, R.id.slot_8
        )

        for (i in 0..8) {
            views.setOnClickPendingIntent(
                slotIds[i],
                PendingIntent.getActivity(
                    context,
                    widgetId * 100 + i,
                    intents[i],
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        }

        appWidgetManager.updateAppWidget(widgetId, views)
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