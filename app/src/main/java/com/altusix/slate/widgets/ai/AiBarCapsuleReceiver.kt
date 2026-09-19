package com.altusix.slate.widgets.ai

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.widget.RemoteViews
import com.altusix.slate.R
import com.altusix.slate.data.local.SlateWidgetConfig

// ============================================================================
// HELPER: AI App Launch Pending Intent
// ============================================================================

fun createAiPendingIntent(context: Context, target: AiTarget, widgetId: Int, slotIndex: Int): PendingIntent {
    val intent = AiLauncherUtils.getLaunchIntent(context, target)
    return PendingIntent.getActivity(
        context,
        widgetId * 100 + slotIndex,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

// ============================================================================
// BARS (4x1)
// ============================================================================

class AiBarPrimaryReceiver : BaseAiReceiver(R.layout.widget_base_row_4) {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateAiBarHeroPrimaryBitmap(context, config, isResponsive, wDp, hDp, widgetId)

    override fun setupTouchTargets(context: Context, views: RemoteViews, widgetId: Int) {
        views.setOnClickPendingIntent(R.id.touch_slot_0, createAiPendingIntent(context, AiTarget.GEMINI_TEXT, widgetId, 0))
        views.setOnClickPendingIntent(R.id.touch_slot_1, createAiPendingIntent(context, AiTarget.CHATGPT_TEXT, widgetId, 1))
        views.setOnClickPendingIntent(R.id.touch_slot_2, createAiPendingIntent(context, AiTarget.CLAUDE, widgetId, 2))
        views.setOnClickPendingIntent(R.id.touch_slot_3, createAiPendingIntent(context, AiTarget.GROK, widgetId, 3))
    }
}

class AiBarDock5Receiver : BaseAiReceiver(R.layout.widget_base_row_5) {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateAiBarDock5Bitmap(context, config, isResponsive, wDp, hDp, widgetId)

    override fun setupTouchTargets(context: Context, views: RemoteViews, widgetId: Int) {
        views.setOnClickPendingIntent(R.id.touch_slot_0, createAiPendingIntent(context, AiTarget.GEMINI_TEXT, widgetId, 0))
        views.setOnClickPendingIntent(R.id.touch_slot_1, createAiPendingIntent(context, AiTarget.CHATGPT_TEXT, widgetId, 1))
        views.setOnClickPendingIntent(R.id.touch_slot_2, createAiPendingIntent(context, AiTarget.CLAUDE, widgetId, 2))
        views.setOnClickPendingIntent(R.id.touch_slot_3, createAiPendingIntent(context, AiTarget.GROK, widgetId, 3))
        views.setOnClickPendingIntent(R.id.touch_slot_4, createAiPendingIntent(context, AiTarget.PERPLEXITY, widgetId, 4))
    }
}

class AiBarCapsuleReceiver : BaseAiReceiver(R.layout.widget_base_row_4) {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateAiBarCapsuleBitmap(context, config, isResponsive, wDp, hDp, widgetId)

    override fun setupTouchTargets(context: Context, views: RemoteViews, widgetId: Int) {
        views.setOnClickPendingIntent(R.id.touch_slot_0, createAiPendingIntent(context, AiTarget.CHATGPT_VOICE, widgetId, 0))
        views.setOnClickPendingIntent(R.id.touch_slot_1, createAiPendingIntent(context, AiTarget.PERPLEXITY, widgetId, 1))
        views.setOnClickPendingIntent(R.id.touch_slot_2, createAiPendingIntent(context, AiTarget.CLAUDE, widgetId, 2))
        views.setOnClickPendingIntent(R.id.touch_slot_3, createAiPendingIntent(context, AiTarget.GEMINI_TEXT, widgetId, 3))
    }
}

class AiBarDualFlagshipReceiver : BaseAiReceiver(R.layout.widget_base_row_2) {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateAiBarDualFlagshipBitmap(context, config, isResponsive, wDp, hDp, widgetId)

    override fun setupTouchTargets(context: Context, views: RemoteViews, widgetId: Int) {
        views.setOnClickPendingIntent(R.id.touch_slot_0, createAiPendingIntent(context, AiTarget.CHATGPT_TEXT, widgetId, 0))
        views.setOnClickPendingIntent(R.id.touch_slot_1, createAiPendingIntent(context, AiTarget.GEMINI_TEXT, widgetId, 1))
    }
}

// ============================================================================
// FOLDERS (2x2 / 4x2 / 3x2 / 3x3)
// ============================================================================

class AiFolder4ClassicReceiver : BaseAiReceiver(R.layout.widget_base_grid_2x2) {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateAiFolder4ClassicBitmap(context, config, isResponsive, wDp, hDp, widgetId)

    override fun setupTouchTargets(context: Context, views: RemoteViews, widgetId: Int) {
        views.setOnClickPendingIntent(R.id.slot_0, createAiPendingIntent(context, AiTarget.GEMINI_TEXT, widgetId, 0))
        views.setOnClickPendingIntent(R.id.slot_1, createAiPendingIntent(context, AiTarget.CHATGPT_TEXT, widgetId, 1))
        views.setOnClickPendingIntent(R.id.slot_2, createAiPendingIntent(context, AiTarget.PERPLEXITY, widgetId, 2))
        views.setOnClickPendingIntent(R.id.slot_3, createAiPendingIntent(context, AiTarget.CLAUDE, widgetId, 3))
    }
}

class AiFolder6BentoHeroReceiver : BaseAiReceiver(R.layout.widget_base_bento_hero_6) {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateAiFolder6BentoHeroBitmap(context, config, isResponsive, wDp, hDp, widgetId)

    override fun setupTouchTargets(context: Context, views: RemoteViews, widgetId: Int) {
        views.setOnClickPendingIntent(R.id.touch_slot_0, createAiPendingIntent(context, AiTarget.GEMINI_TEXT, widgetId, 0))
        views.setOnClickPendingIntent(R.id.touch_slot_1, createAiPendingIntent(context, AiTarget.CHATGPT_TEXT, widgetId, 1))
        views.setOnClickPendingIntent(R.id.touch_slot_2, createAiPendingIntent(context, AiTarget.CLAUDE, widgetId, 2))
        views.setOnClickPendingIntent(R.id.touch_slot_3, createAiPendingIntent(context, AiTarget.GROK, widgetId, 3))
        views.setOnClickPendingIntent(R.id.touch_slot_4, createAiPendingIntent(context, AiTarget.DEEPSEEK, widgetId, 4))
        views.setOnClickPendingIntent(R.id.touch_slot_5, createAiPendingIntent(context, AiTarget.META_AI, widgetId, 5))
    }
}

class AiFolder8BentoSideReceiver : BaseAiReceiver(R.layout.widget_base_bento_side_8) {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateAiFolder8BentoSideBitmap(context, config, isResponsive, wDp, hDp, widgetId)

    override fun setupTouchTargets(context: Context, views: RemoteViews, widgetId: Int) {
        views.setOnClickPendingIntent(R.id.touch_slot_0, createAiPendingIntent(context, AiTarget.GEMINI_TEXT, widgetId, 0))
        views.setOnClickPendingIntent(R.id.touch_slot_1, createAiPendingIntent(context, AiTarget.CHATGPT_TEXT, widgetId, 1))
        views.setOnClickPendingIntent(R.id.touch_slot_2, createAiPendingIntent(context, AiTarget.CLAUDE, widgetId, 2))
        views.setOnClickPendingIntent(R.id.touch_slot_3, createAiPendingIntent(context, AiTarget.GROK, widgetId, 3))
        views.setOnClickPendingIntent(R.id.touch_slot_4, createAiPendingIntent(context, AiTarget.PERPLEXITY, widgetId, 4))
        views.setOnClickPendingIntent(R.id.touch_slot_5, createAiPendingIntent(context, AiTarget.COPILOT, widgetId, 5))
        views.setOnClickPendingIntent(R.id.touch_slot_6, createAiPendingIntent(context, AiTarget.DEEPSEEK, widgetId, 6))
        views.setOnClickPendingIntent(R.id.touch_slot_7, createAiPendingIntent(context, AiTarget.META_AI, widgetId, 7))
    }
}

class AiFolder9GridReceiver : BaseAiReceiver(R.layout.widget_base_grid_3x3_layout) {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateAiFolder9GridBitmap(context, config, isResponsive, wDp, hDp, widgetId)

    override fun setupTouchTargets(context: Context, views: RemoteViews, widgetId: Int) {
        val targets = listOf(
            AiTarget.GEMINI_TEXT,
            AiTarget.CHATGPT_TEXT,
            AiTarget.COPILOT,
            AiTarget.GROK,
            AiTarget.CLAUDE,
            AiTarget.DEEPSEEK,
            AiTarget.PERPLEXITY,
            AiTarget.META_AI,
            AiTarget.POE
        )
        val slotIds = intArrayOf(
            R.id.slot_0, R.id.slot_1, R.id.slot_2,
            R.id.slot_3, R.id.slot_4, R.id.slot_5,
            R.id.slot_6, R.id.slot_7, R.id.slot_8
        )
        for (i in targets.indices) {
            views.setOnClickPendingIntent(slotIds[i], createAiPendingIntent(context, targets[i], widgetId, i))
        }
    }
}

class AiFolder10MegaReceiver : BaseAiReceiver(R.layout.widget_base_grid_5x2) {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateAiFolder10MegaBitmap(context, config, isResponsive, wDp, hDp, widgetId)

    override fun setupTouchTargets(context: Context, views: RemoteViews, widgetId: Int) {
        val targets = listOf(
            AiTarget.GEMINI_TEXT,
            AiTarget.CHATGPT_TEXT,
            AiTarget.COPILOT,
            AiTarget.CLAUDE,
            AiTarget.GROK,
            AiTarget.PERPLEXITY,
            AiTarget.DEEPSEEK,
            AiTarget.META_AI,
            AiTarget.POE,
            AiTarget.PI
        )
        val slotIds = intArrayOf(
            R.id.touch_slot_0, R.id.touch_slot_1, R.id.touch_slot_2, R.id.touch_slot_3, R.id.touch_slot_4,
            R.id.touch_slot_5, R.id.touch_slot_6, R.id.touch_slot_7, R.id.touch_slot_8, R.id.touch_slot_9
        )
        for (i in targets.indices) {
            views.setOnClickPendingIntent(slotIds[i], createAiPendingIntent(context, targets[i], widgetId, i))
        }
    }
}

class AiFolder7AsymmetricReceiver : BaseAiReceiver(R.layout.widget_base_bento_asymmetric_7) {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateAiFolder7AsymmetricBitmap(context, config, isResponsive, wDp, hDp, widgetId)

    override fun setupTouchTargets(context: Context, views: RemoteViews, widgetId: Int) {
        val targets = listOf(
            AiTarget.GEMINI_TEXT,
            AiTarget.CHATGPT_TEXT,
            AiTarget.COPILOT,
            AiTarget.CLAUDE,
            AiTarget.GROK,
            AiTarget.PERPLEXITY,
            AiTarget.DEEPSEEK
        )
        val slotIds = intArrayOf(
            R.id.touch_slot_0, R.id.touch_slot_1, R.id.touch_slot_2,
            R.id.touch_slot_3, R.id.touch_slot_4, R.id.touch_slot_5, R.id.touch_slot_6
        )
        for (i in targets.indices) {
            views.setOnClickPendingIntent(slotIds[i], createAiPendingIntent(context, targets[i], widgetId, i))
        }
    }
}

fun updateAllAiFolderWidgets(context: Context) {
    val receivers: List<BaseAiReceiver> = listOf(
        AiBarPrimaryReceiver(),
        AiBarDock5Receiver(),
        AiBarCapsuleReceiver(),
        AiBarDualFlagshipReceiver(),
        AiFolder4ClassicReceiver(),
        AiFolder6BentoHeroReceiver(),
        AiFolder8BentoSideReceiver(),
        AiFolder9GridReceiver(),
        AiFolder10MegaReceiver(),
        AiFolder7AsymmetricReceiver()
    )

    val manager = AppWidgetManager.getInstance(context)
    for (receiver in receivers) {
        val ids = manager.getAppWidgetIds(ComponentName(context, receiver.javaClass)) ?: intArrayOf()
        for (id in ids) {
            try {
                receiver.updateWidget(context, manager, id)
            } catch (_: Exception) {}
        }
    }
}