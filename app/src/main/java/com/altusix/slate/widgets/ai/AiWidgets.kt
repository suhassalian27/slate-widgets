package com.altusix.slate.widgets.ai

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import com.altusix.slate.data.local.SlateDataStore
import com.altusix.slate.data.local.SlateWidgetConfig
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first

abstract class BaseAiWidget(private val target: AiTarget) : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val config = try {
            val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
            SlateDataStore(context).getWidgetConfig(appWidgetId)
                .catch { emit(SlateWidgetConfig()) }.first()
        } catch (e: Exception) { SlateWidgetConfig() }

        provideContent {
            AiShortcutTile(target = target, config = config)
        }
    }
}

// 1. Gemini Text
class GeminiTextWidget : BaseAiWidget(AiTarget.GEMINI_TEXT)
class GeminiTextReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = GeminiTextWidget() }

// 2. ChatGPT Text
class ChatGptTextWidget : BaseAiWidget(AiTarget.CHATGPT_TEXT)
class ChatGptTextReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = ChatGptTextWidget() }

// 3. ChatGPT Voice
class ChatGptVoiceWidget : BaseAiWidget(AiTarget.CHATGPT_VOICE)
class ChatGptVoiceReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = ChatGptVoiceWidget() }

// 4. Claude
class ClaudeWidget : BaseAiWidget(AiTarget.CLAUDE)
class ClaudeReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = ClaudeWidget() }

// 5. Grok
class GrokWidget : BaseAiWidget(AiTarget.GROK)
class GrokReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = GrokWidget() }

// 6. Perplexity
class PerplexityWidget : BaseAiWidget(AiTarget.PERPLEXITY)
class PerplexityReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = PerplexityWidget() }

// 7. DeepSeek
class DeepSeekWidget : BaseAiWidget(AiTarget.DEEPSEEK)
class DeepSeekReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = DeepSeekWidget() }

// 8. Copilot
class CopilotWidget : BaseAiWidget(AiTarget.COPILOT)
class CopilotReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = CopilotWidget() }

// 9. Meta AI
class MetaAiWidget : BaseAiWidget(AiTarget.META_AI)
class MetaAiReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = MetaAiWidget() }

suspend fun updateAllAiWidgets(context: Context) {
    val manager = GlanceAppWidgetManager(context)
    if (manager.getGlanceIds(GeminiTextWidget::class.java).isNotEmpty()) GeminiTextWidget().updateAll(context)
    if (manager.getGlanceIds(ChatGptTextWidget::class.java).isNotEmpty()) ChatGptTextWidget().updateAll(context)
    if (manager.getGlanceIds(ChatGptVoiceWidget::class.java).isNotEmpty()) ChatGptVoiceWidget().updateAll(context)
    if (manager.getGlanceIds(ClaudeWidget::class.java).isNotEmpty()) ClaudeWidget().updateAll(context)
    if (manager.getGlanceIds(GrokWidget::class.java).isNotEmpty()) GrokWidget().updateAll(context)
    if (manager.getGlanceIds(PerplexityWidget::class.java).isNotEmpty()) PerplexityWidget().updateAll(context)
    if (manager.getGlanceIds(DeepSeekWidget::class.java).isNotEmpty()) DeepSeekWidget().updateAll(context)
    if (manager.getGlanceIds(CopilotWidget::class.java).isNotEmpty()) CopilotWidget().updateAll(context)
    if (manager.getGlanceIds(MetaAiWidget::class.java).isNotEmpty()) MetaAiWidget().updateAll(context)
}