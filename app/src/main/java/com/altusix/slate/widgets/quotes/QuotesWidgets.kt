package com.altusix.slate.widgets.quotes

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews
import com.altusix.slate.R
import com.altusix.slate.core.model.SlateWidgetInfo
import com.altusix.slate.core.theme.ThemePreferences
import com.altusix.slate.data.local.SlateWidgetConfig

// =========================================================================
// CATALOG & BATCH UPDATES
// =========================================================================

fun getQuotesWidgetsCatalog(): List<SlateWidgetInfo> {
    return listOf(
        SlateWidgetInfo("Editorial Pull Quote", "4x2", "Quotes", QuotesEditorialReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Radiant Emblem Mantra", "2x2", "Quotes", QuotesZenMantraReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Modern Accent Quotes", "4x2", "Quotes", QuotesTypewriterReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Punchy Horizon Banner", "4x1", "Quotes", QuotesKineticReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Golden Hour Card", "2x2", "Quotes", QuotesGoldenHourReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Two-Tone Insight", "2x2", "Quotes", QuotesPoetryReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Daily Insight Banner", "4x2", "Quotes", QuotesBentoReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Minimal Horizon Capsule", "2x1", "Quotes", QuotesAffirmationPillReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Mindful Smile Strip", "4x1", "Quotes", QuotesTerminalReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Mindful Smile Card", "2x2", "Quotes", QuotesBrutalistReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Bold Condensed Statement", "4x2", "Quotes", QuotesBoldCondensedReceiver::class.java, hasModeOption = true)
    )
}

fun updateAllQuotesWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context) ?: return
    val receivers: List<BaseQuotesReceiver> = listOf(
        QuotesEditorialReceiver(),
        QuotesZenMantraReceiver(),
        QuotesTypewriterReceiver(),
        QuotesKineticReceiver(),
        QuotesGoldenHourReceiver(),
        QuotesPoetryReceiver(),
        QuotesBentoReceiver(),
        QuotesAffirmationPillReceiver(),
        QuotesTerminalReceiver(),
        QuotesBrutalistReceiver(),
        QuotesBoldCondensedReceiver()
    )

    for (receiver in receivers) {
        val ids = manager.getAppWidgetIds(ComponentName(context, receiver::class.java)) ?: intArrayOf()
        for (id in ids) {
            receiver.updateSingleWidget(context, manager, id)
        }
    }
}

// -------------------------------------------------------------------------
// PREFERENCES & RESPONSIVE CONTEXT HELPERS
// -------------------------------------------------------------------------

private fun loadSlateWidgetConfig(context: Context, widgetId: Int): SlateWidgetConfig {
    val widgetPrefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
    val bgKey = "widget_${widgetId}_bg_color"

    if (!widgetPrefs.contains(bgKey) && widgetId != -1) {
        val globalSettings = ThemePreferences(context).getThemeSettings()
        val isLight = (((globalSettings.bgHex shr 16 and 0xFFL) * 0.2126f) +
                ((globalSettings.bgHex shr 8 and 0xFFL) * 0.7152f) +
                ((globalSettings.bgHex and 0xFFL) * 0.0722f)) / 255f > 0.5f

        widgetPrefs.edit()
            .putString("widget_${widgetId}_theme_mode", if (isLight) "LIGHT" else "DARK")
            .putLong("widget_${widgetId}_bg_color", globalSettings.bgHex)
            .putLong("widget_${widgetId}_accent_color", globalSettings.accentHex)
            .putFloat("widget_${widgetId}_opacity", globalSettings.opacity)
            .apply()
    }

    val globalSettings = ThemePreferences(context).getThemeSettings()
    val bgColor = widgetPrefs.getLong("widget_${widgetId}_bg_color", globalSettings.bgHex)
    val opacity = widgetPrefs.getFloat("widget_${widgetId}_opacity", globalSettings.opacity)
    val accentColor = widgetPrefs.getLong("widget_${widgetId}_accent_color", globalSettings.accentHex)

    val isLight = (((bgColor shr 16 and 0xFFL) * 0.2126f) +
            ((bgColor shr 8 and 0xFFL) * 0.7152f) +
            ((bgColor and 0xFFL) * 0.0722f)) / 255f > 0.5f
    val mode = widgetPrefs.getString("widget_${widgetId}_theme_mode", if (isLight) "LIGHT" else "DARK")
        ?: if (isLight) "LIGHT" else "DARK"

    return SlateWidgetConfig(
        themeMode = mode,
        backgroundColorHex = bgColor,
        opacity = opacity,
        accentColorHex = accentColor
    )
}

private fun parseAndLockIsResponsive(context: Context, widgetId: Int): Boolean {
    val widgetPrefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
    val modeKey = "widget_${widgetId}_mode"
    val isResponsiveKey = "widget_${widgetId}_is_responsive"

    if (widgetPrefs.contains(modeKey)) {
        return widgetPrefs.getString(modeKey, "RESPONSIVE") == "RESPONSIVE"
    }
    if (widgetPrefs.contains(isResponsiveKey)) {
        return widgetPrefs.getBoolean(isResponsiveKey, true)
    }

    val launcherPrefs = context.getSharedPreferences("slate_app_launcher_prefs", Context.MODE_PRIVATE)
    val defaultResponsive = launcherPrefs.getBoolean("default_is_responsive", true)
    widgetPrefs.edit().putBoolean(isResponsiveKey, defaultResponsive).apply()
    return defaultResponsive
}

// =========================================================================
// BASE QUOTES RECEIVER
// =========================================================================

abstract class BaseQuotesReceiver(
    protected open val targetAspect: Float = 2.0f
) : AppWidgetProvider() {

    protected open val widgetTypeTag: String = "EDITORIAL"

    abstract fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, this::class.java)) ?: intArrayOf()
            for (id in ids) {
                updateSingleWidget(context, manager, id)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            updateSingleWidget(context, appWidgetManager, id)
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle?) {
        updateSingleWidget(context, appWidgetManager, appWidgetId)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    fun updateSingleWidget(context: Context, manager: AppWidgetManager, id: Int) {
        try {
            val config = loadSlateWidgetConfig(context, id)
            val isResponsive = if (id == -1) true else parseAndLockIsResponsive(context, id)
            val options = manager.getAppWidgetOptions(id)
            val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 160) ?: 160 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 160) ?: 160
            val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 160) ?: 160 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 160) ?: 160
            val wDp = if (wDpRaw <= 0) 160 else wDpRaw
            val hDp = if (hDpRaw <= 0) 160 else hDpRaw
            val density = context.resources.displayMetrics.density

            val padH: Int
            val padV: Int
            val effWDp: Int
            val effHDp: Int

            if (!isResponsive && targetAspect > 0f) {
                val currentAspect = wDp.toFloat() / hDp.toFloat()
                if (currentAspect > targetAspect) {
                    val contentW = hDp * targetAspect
                    padH = (((wDp - contentW) / 2f) * density).toInt()
                    padV = 0
                    effWDp = maxOf(1, contentW.toInt())
                    effHDp = hDp
                } else {
                    val contentH = wDp / targetAspect
                    padH = 0
                    padV = (((hDp - contentH) / 2f) * density).toInt()
                    effWDp = wDp
                    effHDp = maxOf(1, contentH.toInt())
                }
            } else {
                padH = 0
                padV = 0
                effWDp = wDp
                effHDp = hDp
            }

            val bitmap = renderWidgetBitmap(context, id, config, isResponsive, effWDp, effHDp)
            val views = RemoteViews(context.packageName, R.layout.widget_quotes_card_layout)

            try {
                views.setViewPadding(R.id.layout_quotes_root, padH, padV, padH, padV)
            } catch (_: Exception) {}

            views.setImageViewBitmap(R.id.widget_canvas_surface, bitmap)

            val studioIntent = Intent(context, QuotesConfigActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                data = Uri.parse("slate_quotes://$id")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val studioPi = PendingIntent.getActivity(
                context,
                id * 50 + 1,
                studioIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_quote_open, studioPi)

            manager.updateAppWidget(id, views)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// -----------------------------------------------------------------------------
// CONCRETE RECEIVERS
// -----------------------------------------------------------------------------

// 1. Editorial Pull Quote (4x2)
class QuotesEditorialReceiver : BaseQuotesReceiver(targetAspect = 2.0f) {
    override val widgetTypeTag: String = "EDITORIAL"
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val quote = QuotesStorageManager.getQuoteForWidget(context, appWidgetId, widgetTypeTag)
        return generateEditorialQuoteBitmap(context, quote, config, isResponsive, wDp, hDp)
    }
}

// 2. Radiant Emblem Mantra (2x2)
class QuotesZenMantraReceiver : BaseQuotesReceiver(targetAspect = 1.0f) {
    override val widgetTypeTag: String = "ZEN"
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val quote = QuotesStorageManager.getQuoteForWidget(context, appWidgetId, widgetTypeTag)
        return generateRadiantMantraBitmap(context, quote, config, isResponsive, wDp, hDp)
    }
}

// 3. Modern Accent Quotes (4x2)
class QuotesTypewriterReceiver : BaseQuotesReceiver(targetAspect = 2.0f) {
    override val widgetTypeTag: String = "TYPEWRITER_MODERN"
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val quote = QuotesStorageManager.getQuoteForWidget(context, appWidgetId, widgetTypeTag)
        return generateModernQuotesSpreadBitmap(context, quote, config, isResponsive, wDp, hDp)
    }
}

// 4. Punchy Horizon Banner (4x1)
class QuotesKineticReceiver : BaseQuotesReceiver(targetAspect = 4.0f) {
    override val widgetTypeTag: String = "KINETIC"
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val quote = QuotesStorageManager.getQuoteForWidget(context, appWidgetId, widgetTypeTag)
        return generatePunchyHorizonBitmap(context, quote, config, isResponsive, wDp, hDp)
    }
}

// 5. Golden Hour Card (2x2)
class QuotesGoldenHourReceiver : BaseQuotesReceiver(targetAspect = 1.0f) {
    override val widgetTypeTag: String = "GOLDEN"
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val quote = QuotesStorageManager.getQuoteForWidget(context, appWidgetId, widgetTypeTag)
        return generateGoldenHourCardBitmap(context, quote, config, isResponsive, wDp, hDp)
    }
}

// 6. Two-Tone Insight (2x2)
class QuotesPoetryReceiver : BaseQuotesReceiver(targetAspect = 1.0f) {
    override val widgetTypeTag: String = "POETRY_TWOTONE"
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val quote = QuotesStorageManager.getQuoteForWidget(context, appWidgetId, widgetTypeTag)
        return generateTwoToneInsightBitmap(context, quote, config, isResponsive, wDp, hDp)
    }
}

// 7. Daily Insight Banner (4x2)
class QuotesBentoReceiver : BaseQuotesReceiver(targetAspect = 2.0f) {
    override val widgetTypeTag: String = "BENTO_INSIGHT"
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val quote = QuotesStorageManager.getQuoteForWidget(context, appWidgetId, widgetTypeTag)
        return generateDailyInsightBitmap(context, quote, config, isResponsive, wDp, hDp)
    }
}

// 8. Minimal Horizon Capsule (2x1)
class QuotesAffirmationPillReceiver : BaseQuotesReceiver(targetAspect = 2.0f) {
    override val widgetTypeTag: String = "PILL"
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val quote = QuotesStorageManager.getQuoteForWidget(context, appWidgetId, widgetTypeTag)
        return generateMinimalCapsuleBitmap(context, quote, config, isResponsive, wDp, hDp)
    }
}

// 9. Mindful Smile Strip (4x1)
class QuotesTerminalReceiver : BaseQuotesReceiver(targetAspect = 4.0f) {
    override val widgetTypeTag: String = "TERMINAL_SMILE"
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val quote = QuotesStorageManager.getQuoteForWidget(context, appWidgetId, widgetTypeTag)
        return generatePunchyHorizonBitmap(context, quote, config, isResponsive, wDp, hDp)
    }
}

// 10. Mindful Smile Card (2x2)
class QuotesBrutalistReceiver : BaseQuotesReceiver(targetAspect = 1.0f) {
    override val widgetTypeTag: String = "BRUTALIST_SMILE_CARD"
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val quote = QuotesStorageManager.getQuoteForWidget(context, appWidgetId, widgetTypeTag)
        return generateMindfulSmileBitmap(context, quote, config, isResponsive, wDp, hDp)
    }
}

// 11. Bold Condensed Statement (4x2)
class QuotesBoldCondensedReceiver : BaseQuotesReceiver(targetAspect = 2.0f) {
    override val widgetTypeTag: String = "BOLD"
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val quote = QuotesStorageManager.getQuoteForWidget(context, appWidgetId, widgetTypeTag)
        return generateBoldCondensedBitmap(context, quote, config, isResponsive, wDp, hDp)
    }
}