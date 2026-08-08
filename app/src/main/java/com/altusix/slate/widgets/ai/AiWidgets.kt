package com.altusix.slate.widgets.ai

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import com.altusix.slate.core.receiver.BaseCanvasWidgetProvider
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.core.model.SlateWidgetInfo

fun getAiWidgetsCatalog(): List<SlateWidgetInfo> {
    return listOf(
        SlateWidgetInfo("Gemini", "2x2", "AI", GeminiTextReceiver::class.java),
        SlateWidgetInfo("ChatGPT Text", "2x2", "AI", ChatGptTextReceiver::class.java),
        SlateWidgetInfo("ChatGPT Voice", "2x2", "AI", ChatGptVoiceReceiver::class.java),
        SlateWidgetInfo("Claude", "2x2", "AI", ClaudeReceiver::class.java),
        SlateWidgetInfo("Grok", "2x2", "AI", GrokReceiver::class.java),
        SlateWidgetInfo("Perplexity", "2x2", "AI", PerplexityReceiver::class.java),
        SlateWidgetInfo("DeepSeek", "2x2", "AI", DeepSeekReceiver::class.java),
        SlateWidgetInfo("Copilot", "2x2", "AI", CopilotReceiver::class.java),
        SlateWidgetInfo("Meta AI", "2x2", "AI", MetaAiReceiver::class.java),
        SlateWidgetInfo("AI Primary Bar", "4x1", "AI", AiBarPrimaryReceiver::class.java),
        SlateWidgetInfo("AI Dock Bar", "4x1", "AI", AiBarDock5Receiver::class.java),
        SlateWidgetInfo("AI Capsule Bar", "4x1", "AI", AiBarCapsuleReceiver::class.java),
        SlateWidgetInfo("AI Dual Flagship Bar", "4x1", "AI", AiBarDualFlagshipReceiver::class.java),
        SlateWidgetInfo("AI Quad Folder", "2x2", "AI", AiFolder4ClassicReceiver::class.java),
        SlateWidgetInfo("AI Bento Folder", "4x2", "AI", AiFolder6BentoHeroReceiver::class.java),
        SlateWidgetInfo("AI Side Bento Folder", "4x2", "AI", AiFolder8BentoSideReceiver::class.java),
        SlateWidgetInfo("AI 3x3 Grid Folder", "2x2", "AI", AiFolder9GridReceiver::class.java),
        SlateWidgetInfo("AI Mega Folder", "4x2", "AI", AiFolder10MegaReceiver::class.java),
        SlateWidgetInfo("AI Asymmetric Bento", "3x2", "AI", AiFolder7AsymmetricReceiver::class.java)
    )
}

abstract class BaseSingleAiReceiver(private val target: AiTarget) : BaseCanvasWidgetProvider() {

    override fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val wPx = (wDp * density).toInt()
        val hPx = (hDp * density).toInt()
        val isLight = config.themeMode == "LIGHT"
        val bgColor = Color(config.backgroundColorHex).copy(alpha = config.opacity)
        val accentColor = Color(config.accentColorHex)

        return generateTileBitmap(
            context = context,
            target = target,
            bgColor = bgColor,
            accentColor = accentColor,
            isLight = isLight,
            shapeStyle = AiShapeStyle.SQUIRCLE,
            forceSquare = true, // Guarantees 1:1 square ratio for single 1x1 widgets
            widthPx = wPx,
            heightPx = hPx
        )
    }

    override fun getClickPendingIntent(context: Context, appWidgetId: Int): PendingIntent? {
        val intent = AiLauncherUtils.getLaunchIntent(context, target)
        return PendingIntent.getActivity(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

// 1. Gemini Text
class GeminiTextReceiver : BaseSingleAiReceiver(AiTarget.GEMINI_TEXT)

// 2. ChatGPT Text
class ChatGptTextReceiver : BaseSingleAiReceiver(AiTarget.CHATGPT_TEXT)

// 3. ChatGPT Voice
class ChatGptVoiceReceiver : BaseSingleAiReceiver(AiTarget.CHATGPT_VOICE)

// 4. Claude
class ClaudeReceiver : BaseSingleAiReceiver(AiTarget.CLAUDE)

// 5. Grok
class GrokReceiver : BaseSingleAiReceiver(AiTarget.GROK)

// 6. Perplexity
class PerplexityReceiver : BaseSingleAiReceiver(AiTarget.PERPLEXITY)

// 7. DeepSeek
class DeepSeekReceiver : BaseSingleAiReceiver(AiTarget.DEEPSEEK)

// 8. Copilot
class CopilotReceiver : BaseSingleAiReceiver(AiTarget.COPILOT)

// 9. Meta AI
class MetaAiReceiver : BaseSingleAiReceiver(AiTarget.META_AI)

fun updateAllAiWidgets(context: Context) {
    val receivers = listOf(
        GeminiTextReceiver::class.java,
        ChatGptTextReceiver::class.java,
        ChatGptVoiceReceiver::class.java,
        ClaudeReceiver::class.java,
        GrokReceiver::class.java,
        PerplexityReceiver::class.java,
        DeepSeekReceiver::class.java,
        CopilotReceiver::class.java,
        MetaAiReceiver::class.java
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