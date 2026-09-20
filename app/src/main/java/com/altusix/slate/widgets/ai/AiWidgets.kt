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
import com.altusix.slate.widgets.common.bindFolderTouchSlots

// ============================================================================
// AI WIDGETS CATALOG (19 Widgets)
// ============================================================================

fun getAiWidgetsCatalog(): List<SlateWidgetInfo> {
    return listOf(
        // Single Icon Widgets (2x2)
        SlateWidgetInfo("Gemini", "2x2", "AI", GeminiTextReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("ChatGPT Text", "2x2", "AI", ChatGptTextReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("ChatGPT Voice", "2x2", "AI", ChatGptVoiceReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Claude", "2x2", "AI", ClaudeReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Grok", "2x2", "AI", GrokReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Perplexity", "2x2", "AI", PerplexityReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("DeepSeek", "2x2", "AI", DeepSeekReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Copilot", "2x2", "AI", CopilotReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Meta AI", "2x2", "AI", MetaAiReceiver::class.java, hasModeOption = true),

        // Bar Widgets (4x1)
        SlateWidgetInfo("AI Primary Bar", "4x1", "AI", AiBarPrimaryReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("AI Dock Bar", "4x1", "AI", AiBarDock5Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo("AI Capsule Bar", "4x1", "AI", AiBarCapsuleReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("AI Dual Flagship Bar", "4x1", "AI", AiBarDualFlagshipReceiver::class.java, hasModeOption = true),

        // Folder & Bento Widgets (2x2, 4x2, 3x2)
        SlateWidgetInfo("AI Quad Folder", "2x2", "AI", AiFolder4ClassicReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("AI Bento Folder", "4x2", "AI", AiFolder6BentoHeroReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("AI Side Bento Folder", "4x2", "AI", AiFolder8BentoSideReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("AI 3x3 Grid Folder", "2x2", "AI", AiFolder9GridReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("AI Mega Folder", "4x2", "AI", AiFolder10MegaReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("AI Asymmetric Bento", "3x2", "AI", AiFolder7AsymmetricReceiver::class.java, hasModeOption = true)
    )
}

// ============================================================================
// HELPER: AI Launch Intent Factory
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
// BASE AI RECEIVER (Rendering, Geometry & State Management)
// ============================================================================

abstract class BaseAiReceiver(
    open val layoutResId: Int = R.layout.widget_base_single
) : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, this::class.java)) ?: intArrayOf()
            for (id in ids) {
                updateWidget(context, manager, id)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) updateWidget(context, appWidgetManager, widgetId)
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle?) {
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    open fun resolveLayoutResId(isResponsive: Boolean, wDp: Int, hDp: Int): Int = layoutResId

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

        val resolvedLayout = resolveLayoutResId(isResponsive, wDp, hDp)
        val views = RemoteViews(context.packageName, resolvedLayout)
        views.setImageViewBitmap(R.id.widget_image_view, bitmap)

        try {
            views.setViewPadding(R.id.layout_grid_root, 0, 0, 0, 0)
        } catch (_: Exception) {}

        val pi = getClickPendingIntent(context, widgetId)
        if (pi != null) {
            views.setOnClickPendingIntent(R.id.widget_image_view, pi)
            try {
                views.setOnClickPendingIntent(R.id.touch_slot_0, pi)
            } catch (_: Exception) {}
            try {
                views.setOnClickPendingIntent(R.id.slot_0, pi)
            } catch (_: Exception) {}
        }

        setupTouchTargets(context, views, widgetId)

        appWidgetManager.updateAppWidget(widgetId, views)
    }

    protected fun loadSlateWidgetConfig(context: Context, widgetId: Int): SlateWidgetConfig {
        val widgetPrefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
        val bgKey = "widget_${widgetId}_bg_color"

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

    protected fun parseAndLockIsResponsive(context: Context, widgetId: Int): Boolean {
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

    open fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap =
        renderBitmapForWidget(context, config, isResponsive, wDp, hDp, appWidgetId)

    open fun getClickPendingIntent(context: Context, widgetId: Int): PendingIntent? = null
    open fun setupTouchTargets(context: Context, views: RemoteViews, widgetId: Int) {}
}

// ============================================================================
// SINGLE AI WIDGET RECEIVERS (2x2)
// ============================================================================

abstract class BaseSingleAiReceiver(private val target: AiTarget) : BaseAiReceiver(R.layout.widget_base_single) {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateSingleAiIconBitmap(context, target, config, isResponsive, wDp, hDp, widgetId)

    override fun getClickPendingIntent(context: Context, widgetId: Int): PendingIntent? =
        createAiPendingIntent(context, target, widgetId, 0)
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

// ============================================================================
// MULTI-SLOT & FOLDER AI WIDGET RECEIVERS
// ============================================================================

abstract class BaseAiFolderReceiver(
    open val slotCount: Int,
    defaultLayoutResId: Int,
    open val targets: List<AiTarget>
) : BaseAiReceiver(defaultLayoutResId) {

    override fun setupTouchTargets(context: Context, views: RemoteViews, widgetId: Int) {
        val totalSlots = minOf(slotCount, targets.size)
        views.bindFolderTouchSlots(context, widgetId, totalSlots) { i ->
            AiLauncherUtils.getLaunchIntent(context, targets[i])
        }
    }
}

// ----------------------------------------------------------------------------
// BARS (4x1 - With Smart Horizontal / Vertical Responsive Pivoting)
// ----------------------------------------------------------------------------

class AiBarPrimaryReceiver : BaseAiFolderReceiver(
    slotCount = 4,
    defaultLayoutResId = R.layout.widget_base_bento_left_1_right_3,
    targets = listOf(AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT, AiTarget.CLAUDE, AiTarget.GROK)
) {
    override fun resolveLayoutResId(isResponsive: Boolean, wDp: Int, hDp: Int): Int {
        val aspectRatio = wDp.toFloat() / hDp.toFloat()
        return when {
            aspectRatio >= 1.75f -> R.layout.widget_base_bento_left_1_right_3
            aspectRatio >= 0.70f -> R.layout.widget_base_bento_top_1_bottom_3
            else -> R.layout.widget_base_col_4
        }
    }

    override fun renderBitmapForWidget(
        context: Context,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int,
        widgetId: Int
    ): Bitmap = generateAiBarHeroPrimaryBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

class AiBarDock5Receiver : BaseAiFolderReceiver(
    slotCount = 5,
    defaultLayoutResId = R.layout.widget_base_row_5,
    targets = listOf(AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT, AiTarget.CLAUDE, AiTarget.GROK, AiTarget.PERPLEXITY)
) {
    override fun resolveLayoutResId(isResponsive: Boolean, wDp: Int, hDp: Int): Int {
        val isVertical = isResponsive && (hDp > wDp)
        return if (isVertical) R.layout.widget_base_col_5 else R.layout.widget_base_row_5
    }

    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateAiBarDock5Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

class AiBarCapsuleReceiver : BaseAiFolderReceiver(
    slotCount = 4,
    defaultLayoutResId = R.layout.widget_base_row_4,
    targets = listOf(AiTarget.CHATGPT_VOICE, AiTarget.PERPLEXITY, AiTarget.CLAUDE, AiTarget.GEMINI_TEXT)
) {
    override fun resolveLayoutResId(isResponsive: Boolean, wDp: Int, hDp: Int): Int {
        val isVertical = isResponsive && (hDp > wDp)
        return if (isVertical) R.layout.widget_base_col_4 else R.layout.widget_base_row_4
    }

    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateAiBarCapsuleBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

class AiBarDualFlagshipReceiver : BaseAiFolderReceiver(
    slotCount = 2,
    defaultLayoutResId = R.layout.widget_base_row_2,
    targets = listOf(AiTarget.CHATGPT_TEXT, AiTarget.GEMINI_TEXT)
) {
    override fun resolveLayoutResId(isResponsive: Boolean, wDp: Int, hDp: Int): Int {
        val isVertical = isResponsive && (hDp > wDp)
        return if (isVertical) R.layout.widget_base_column_2 else R.layout.widget_base_row_2
    }

    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateAiBarDualFlagshipBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// ----------------------------------------------------------------------------
// FOLDERS (2x2 / 4x2 / 3x2 / 3x3)
// ----------------------------------------------------------------------------

class AiFolder4ClassicReceiver : BaseAiFolderReceiver(
    slotCount = 4,
    defaultLayoutResId = R.layout.widget_base_grid_2x2,
    targets = listOf(AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT, AiTarget.PERPLEXITY, AiTarget.CLAUDE)
) {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateAiFolder4ClassicBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

class AiFolder6BentoHeroReceiver : BaseAiFolderReceiver(
    slotCount = 6,
    defaultLayoutResId = R.layout.widget_base_bento_hero_6,
    targets = listOf(AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT, AiTarget.CLAUDE, AiTarget.GROK, AiTarget.DEEPSEEK, AiTarget.META_AI)
) {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateAiFolder6BentoHeroBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

class AiFolder8BentoSideReceiver : BaseAiFolderReceiver(
    slotCount = 8,
    defaultLayoutResId = R.layout.widget_base_bento_side_8,
    targets = listOf(
        AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT,
        AiTarget.CLAUDE, AiTarget.GROK,
        AiTarget.PERPLEXITY, AiTarget.COPILOT,
        AiTarget.DEEPSEEK, AiTarget.META_AI
    )
) {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateAiFolder8BentoSideBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

class AiFolder9GridReceiver : BaseAiFolderReceiver(
    slotCount = 9,
    defaultLayoutResId = R.layout.widget_base_grid_3x3_layout,
    targets = listOf(
        AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT, AiTarget.COPILOT,
        AiTarget.GROK, AiTarget.CLAUDE, AiTarget.DEEPSEEK,
        AiTarget.PERPLEXITY, AiTarget.META_AI, AiTarget.POE
    )
) {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateAiFolder9GridBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

class AiFolder10MegaReceiver : BaseAiFolderReceiver(
    slotCount = 10,
    defaultLayoutResId = R.layout.widget_base_grid_5x2,
    targets = listOf(
        AiTarget.GEMINI_TEXT, AiTarget.CHATGPT_TEXT, AiTarget.COPILOT, AiTarget.CLAUDE, AiTarget.GROK,
        AiTarget.PERPLEXITY, AiTarget.DEEPSEEK, AiTarget.META_AI, AiTarget.POE, AiTarget.MISTRAL
    )
) {
    override fun resolveLayoutResId(isResponsive: Boolean, wDp: Int, hDp: Int): Int {
        val isVertical = isResponsive && (hDp > wDp)
        return if (isVertical) R.layout.widget_base_grid_2x5 else R.layout.widget_base_grid_5x2
    }

    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateAiFolder10MegaBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

class AiFolder7AsymmetricReceiver : BaseAiFolderReceiver(
    slotCount = 7,
    defaultLayoutResId = R.layout.widget_base_bento_asymmetric_7,
    targets = listOf(
        AiTarget.CHATGPT_TEXT, AiTarget.GROK, AiTarget.COPILOT,
        AiTarget.GEMINI_TEXT, AiTarget.CLAUDE, AiTarget.PERPLEXITY, AiTarget.META_AI
    )
) {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateAiFolder7AsymmetricBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// ============================================================================
// UPDATE DISPATCHERS
// ============================================================================

fun updateAllAiWidgets(context: Context) {
    val receivers = listOf(
        GeminiTextReceiver(),
        ChatGptTextReceiver(),
        ChatGptVoiceReceiver(),
        ClaudeReceiver(),
        GrokReceiver(),
        PerplexityReceiver(),
        DeepSeekReceiver(),
        CopilotReceiver(),
        MetaAiReceiver(),
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

    val manager = AppWidgetManager.getInstance(context) ?: return
    for (receiver in receivers) {
        val ids = manager.getAppWidgetIds(ComponentName(context, receiver.javaClass)) ?: intArrayOf()
        for (id in ids) {
            try {
                receiver.updateWidget(context, manager, id)
            } catch (_: Exception) {}
        }
    }
}

// Backward compatibility alias for legacy folder updates
fun updateAllAiFolderWidgets(context: Context) = updateAllAiWidgets(context)