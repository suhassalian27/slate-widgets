package com.altusix.slate.widgets.ai

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri

enum class AiTarget(
    val title: String,
    val packageName: String,
    val drawableResName: String,
    val isVoice: Boolean = false
) {
    GEMINI_TEXT("Gemini", "com.google.android.apps.bard", "ic_gemini"),
    CHATGPT_TEXT("ChatGPT", "com.openai.chatgpt", "ic_chatgpt"),
    CHATGPT_VOICE("ChatGPT Voice", "com.openai.chatgpt", "ic_chatgpt", isVoice = true),
    CLAUDE("Claude", "com.anthropic.claude", "ic_claude"),
    GROK("Grok", "ai.x.grok", "ic_grok"),
    PERPLEXITY("Perplexity", "ai.perplexity.app.android", "ic_perplexity"),
    DEEPSEEK("DeepSeek", "com.deepseek.chat", "ic_deepseek"),
    COPILOT("Copilot", "com.microsoft.copilot", "ic_copilot"),
    META_AI("Meta AI", "com.facebook.stella", "ic_meta_ai"),
    POE("Poe", "com.poe.android", "ic_poe"),
    PI("Pi AI", "ai.inflection.pi", "ic_pi"),
    CHARACTER_AI("Character.AI", "ai.character.app", "ic_character")
}

object AiLauncherUtils {
    fun getLaunchIntent(context: Context, target: AiTarget): Intent {
        val pm = context.packageManager

        // 1. ChatGPT Native Voice Overlay (Explicit Assistant Activity)
        if (target == AiTarget.CHATGPT_VOICE) {
            val assistantOverlayIntent = Intent().apply {
                component = ComponentName(
                    "com.openai.chatgpt",
                    "com.openai.voice.assistant.AssistantActivity"
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            if (assistantOverlayIntent.resolveActivity(pm) != null) {
                return assistantOverlayIntent
            }
        }

        // 2. Standard App Launch
        val appIntent = pm.getLaunchIntentForPackage(target.packageName)
        if (appIntent != null) {
            appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            return appIntent
        }

        // 3. Play Store Fallback
        val playStoreUri = "market://details?id=${target.packageName}"
        val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse(playStoreUri)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return if (playStoreIntent.resolveActivity(pm) != null) {
            playStoreIntent
        } else {
            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${target.packageName}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }
}