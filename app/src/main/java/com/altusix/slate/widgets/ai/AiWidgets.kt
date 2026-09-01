package com.altusix.slate.widgets.ai

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.RemoteViews
import com.altusix.slate.R
import com.altusix.slate.core.model.SlateWidgetInfo
import com.altusix.slate.core.theme.ThemePreferences
import com.altusix.slate.data.local.SlateWidgetConfig

fun getAiWidgetsCatalog(): List<SlateWidgetInfo> {
    return listOf(
        SlateWidgetInfo("Gemini", "2x2", "AI", GeminiTextReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("ChatGPT Text", "2x2", "AI", ChatGptTextReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("ChatGPT Voice", "2x2", "AI", ChatGptVoiceReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Claude", "2x2", "AI", ClaudeReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Grok", "2x2", "AI", GrokReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Perplexity", "2x2", "AI", PerplexityReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("DeepSeek", "2x2", "AI", DeepSeekReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Copilot", "2x2", "AI", CopilotReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Meta AI", "2x2", "AI", MetaAiReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("AI Primary Bar", "4x1", "AI", AiBarPrimaryReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("AI Dock Bar", "4x1", "AI", AiBarDock5Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo("AI Capsule Bar", "4x1", "AI", AiBarCapsuleReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("AI Dual Flagship Bar", "4x1", "AI", AiBarDualFlagshipReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("AI Quad Folder", "2x2", "AI", AiFolder4ClassicReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("AI Bento Folder", "4x2", "AI", AiFolder6BentoHeroReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("AI Side Bento Folder", "4x2", "AI", AiFolder8BentoSideReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("AI 3x3 Grid Folder", "2x2", "AI", AiFolder9GridReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("AI Mega Folder", "4x2", "AI", AiFolder10MegaReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("AI Asymmetric Bento", "3x2", "AI", AiFolder7AsymmetricReceiver::class.java, hasModeOption = true)
    )
}

abstract class BaseAiReceiver : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) updateWidget(context, appWidgetManager, widgetId)
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle?) {
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 200) ?: 200
        val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 200) ?: 200
        val wDp = if (wDpRaw <= 0) 200 else wDpRaw
        val hDp = if (hDpRaw <= 0) 200 else hDpRaw

        val isResponsive = parseAndLockIsResponsive(context, widgetId)
        val config = loadSlateWidgetConfig(context, widgetId)
        val bitmap = renderBitmapForWidget(context, config, isResponsive, wDp, hDp, widgetId)

        val views = RemoteViews(context.packageName, R.layout.widget_image_container)
        views.setImageViewBitmap(R.id.widget_image_view, bitmap)

        val pi = getClickPendingIntent(context, widgetId)
        if (pi != null) views.setOnClickPendingIntent(R.id.widget_image_view, pi)

        appWidgetManager.updateAppWidget(widgetId, views)
    }

    private fun loadSlateWidgetConfig(context: Context, widgetId: Int): SlateWidgetConfig {
        val widgetPrefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
        val bgKey = "widget_${widgetId}_bg_color"

        // Snapshot and lock current global theme when the widget is first created
        if (!widgetPrefs.contains(bgKey) && widgetId != -1) {
            val globalSettings = ThemePreferences(context).getThemeSettings()
            widgetPrefs.edit()
                .putLong("widget_${widgetId}_bg_color", globalSettings.bgHex)
                .putLong("widget_${widgetId}_accent_color", globalSettings.accentHex)
                .putFloat("widget_${widgetId}_opacity", globalSettings.opacity)
                .apply()
        }

        val globalSettings = ThemePreferences(context).getThemeSettings()
        val bgColor = widgetPrefs.getLong("widget_${widgetId}_bg_color", globalSettings.bgHex)
        val opacity = widgetPrefs.getFloat("widget_${widgetId}_opacity", globalSettings.opacity)
        val accentColor = widgetPrefs.getLong("widget_${widgetId}_accent_color", globalSettings.accentHex)

        val isLight = (((bgColor shr 16 and 0xFFL) * 0.2126f) + ((bgColor shr 8 and 0xFFL) * 0.7152f) + ((bgColor and 0xFFL) * 0.0722f)) / 255f > 0.5f

        return SlateWidgetConfig(
            themeMode = if (isLight) "LIGHT" else "DARK",
            backgroundColorHex = bgColor,
            opacity = opacity,
            accentColorHex = accentColor
        )
    }

    private fun parseAndLockIsResponsive(context: Context, widgetId: Int): Boolean {
        val widgetPrefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
        val modeKey = "widget_${widgetId}_mode"
        val isResponsiveKey = "widget_${widgetId}_is_responsive"
        if (widgetPrefs.contains(modeKey)) return widgetPrefs.getString(modeKey, "RESPONSIVE") == "RESPONSIVE"
        if (widgetPrefs.contains(isResponsiveKey)) return widgetPrefs.getBoolean(isResponsiveKey, true)

        val launcherPrefs = context.getSharedPreferences("slate_app_launcher_prefs", Context.MODE_PRIVATE)
        val defaultResponsive = launcherPrefs.getBoolean("default_is_responsive", true)
        widgetPrefs.edit().putBoolean(isResponsiveKey, defaultResponsive).apply()
        return defaultResponsive
    }

    abstract fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap
    open fun getClickPendingIntent(context: Context, widgetId: Int): PendingIntent? = null
}

abstract class BaseSingleAiReceiver(private val target: AiTarget) : BaseAiReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateSingleAiIconBitmap(context, target, config, isResponsive, wDp, hDp, widgetId)
    override fun getClickPendingIntent(context: Context, widgetId: Int): PendingIntent? =
        PendingIntent.getActivity(context, widgetId, AiLauncherUtils.getLaunchIntent(context, target), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

class GeminiTextReceiver : BaseSingleAiReceiver(AiTarget.GEMINI_TEXT)
class ChatGptTextReceiver : BaseSingleAiReceiver(AiTarget.CHATGPT_TEXT)
class ChatGptVoiceReceiver : BaseSingleAiReceiver(AiTarget.CHATGPT_VOICE)
class ClaudeReceiver : BaseSingleAiReceiver(AiTarget.CLAUDE)
class GrokReceiver : BaseSingleAiReceiver(AiTarget.GROK)
class PerplexityReceiver : BaseSingleAiReceiver(AiTarget.PERPLEXITY)
class DeepSeekReceiver : BaseSingleAiReceiver(AiTarget.DEEPSEEK)
class CopilotReceiver : BaseSingleAiReceiver(AiTarget.COPILOT)
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