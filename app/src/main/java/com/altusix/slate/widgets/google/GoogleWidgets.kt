package com.altusix.slate.widgets.google

import android.app.PendingIntent
import android.app.SearchManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.altusix.slate.R
import com.altusix.slate.core.model.SlateWidgetInfo
import com.altusix.slate.core.receiver.BaseCanvasWidgetProvider
import com.altusix.slate.core.theme.ThemePreferences
import com.altusix.slate.data.local.SlateWidgetConfig

fun getGoogleWidgetsCatalog(): List<SlateWidgetInfo> {
    return listOf(
        SlateWidgetInfo("Search Capsule", "4x1", "Google", GoogleSearchCapsuleReceiver::class.java, hasModeOption = false)
    )
}

fun updateAllGoogleWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val receivers = listOf(GoogleSearchCapsuleReceiver::class.java)
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

abstract class BaseGoogleReceiver : BaseCanvasWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            renderAndApplyWidget(context, appWidgetManager, appWidgetId, options)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            val options = appWidgetManager.getAppWidgetOptions(id)
            renderAndApplyWidget(context, appWidgetManager, id, options)
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle?) {
        renderAndApplyWidget(context, appWidgetManager, appWidgetId, newOptions)
    }

    protected fun loadSlateWidgetConfig(context: Context, widgetId: Int, defaultOpacity: Float = 1.0f): SlateWidgetConfig {
        val widgetPrefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
        val bgKey = "widget_${widgetId}_bg_color"

        if (!widgetPrefs.contains(bgKey) && widgetId != -1) {
            val globalSettings = ThemePreferences(context).getThemeSettings()
            val isLight = (((globalSettings.bgHex shr 16 and 0xFFL) * 0.2126f) +
                    ((globalSettings.bgHex shr 8 and 0xFFL) * 0.7152f) +
                    ((globalSettings.bgHex and 0xFFL) * 0.0722f)) / 255f > 0.5f

            val initialOpacity = if (defaultOpacity == 0.0f) 0.0f else globalSettings.opacity

            widgetPrefs.edit()
                .putString("widget_${widgetId}_theme_mode", if (isLight) "LIGHT" else "DARK")
                .putLong("widget_${widgetId}_bg_color", globalSettings.bgHex)
                .putLong("widget_${widgetId}_accent_color", globalSettings.accentHex)
                .putFloat("widget_${widgetId}_opacity", initialOpacity)
                .apply()
        }

        val globalSettings = ThemePreferences(context).getThemeSettings()
        val bgColor = widgetPrefs.getLong("widget_${widgetId}_bg_color", globalSettings.bgHex)
        val fallbackOpacity = if (defaultOpacity == 0.0f) 0.0f else globalSettings.opacity
        val opacity = widgetPrefs.getFloat("widget_${widgetId}_opacity", fallbackOpacity)
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

    abstract fun renderAndApplyWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int, options: Bundle?)
}

// 1. GOOGLE SEARCH CAPSULE (4x1)
class GoogleSearchCapsuleReceiver : BaseGoogleReceiver() {

    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        return generateGoogleSearchCapsuleBitmap(context, config, true, wDp, hDp, appWidgetId)
    }

    override fun renderAndApplyWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int, options: Bundle?) {
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 260) ?: 260 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 260) ?: 260
        val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 56) ?: 56 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 56) ?: 56
        val wDp = if (wDpRaw <= 0) 260 else wDpRaw
        val hDp = if (hDpRaw <= 0) 56 else hDpRaw

        val config = loadSlateWidgetConfig(context, widgetId)
        val views = RemoteViews(context.packageName, R.layout.widget_google_search_capsule)
        val bitmap = generateGoogleSearchCapsuleBitmap(context, config, true, wDp, hDp, widgetId)
        views.setImageViewBitmap(R.id.widget_canvas_image, bitmap)

        // Threshold-based degradation
        val showLens = wDp >= 140
        val showGemini = wDp >= 210
        val showMic = wDp >= 280

        views.setViewVisibility(R.id.btn_google_lens, if (showLens) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.btn_google_gemini, if (showGemini) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.btn_google_mic, if (showMic) View.VISIBLE else View.GONE)

        // 1. Google Web Search (Base Touch Target)
        val directSearchIntent = Intent().apply {
            setClassName("com.google.android.googlequicksearchbox", "com.google.android.googlequicksearchbox.SearchActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val fallbackSearchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, "")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val safeSearchIntent = if (directSearchIntent.resolveActivity(context.packageManager) != null) directSearchIntent else fallbackSearchIntent
        views.setOnClickPendingIntent(
            R.id.btn_google_search,
            PendingIntent.getActivity(context, widgetId * 100 + 1, safeSearchIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        )

        // 2. Gemini / "Hey Google" Assistant Voice Session
        if (showMic) {
            val voiceAssistIntent = Intent("android.intent.action.VOICE_ASSIST").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val assistIntent = Intent(Intent.ACTION_ASSIST).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val googleOpaVoiceIntent = Intent("com.google.android.googlequicksearchbox.action.OPA_VOICE_SEARCH").apply {
                setPackage("com.google.android.googlequicksearchbox")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val safeLiveIntent = when {
                voiceAssistIntent.resolveActivity(context.packageManager) != null -> voiceAssistIntent
                assistIntent.resolveActivity(context.packageManager) != null -> assistIntent
                googleOpaVoiceIntent.resolveActivity(context.packageManager) != null -> googleOpaVoiceIntent
                else -> voiceAssistIntent
            }
            views.setOnClickPendingIntent(
                R.id.btn_google_mic,
                PendingIntent.getActivity(context, widgetId * 100 + 4, safeLiveIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )
        }

        // 3. Google Gemini Main App
        if (showGemini) {
            val geminiPkgIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.bard")
            val geminiWebIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://gemini.google.com")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val safeGeminiIntent = geminiPkgIntent ?: geminiWebIntent
            views.setOnClickPendingIntent(
                R.id.btn_google_gemini,
                PendingIntent.getActivity(context, widgetId * 100 + 2, safeGeminiIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )
        }

        // 4. Google Lens
        if (showLens) {
            val lensPkgIntent = context.packageManager.getLaunchIntentForPackage("com.google.ar.lens")
            val lensActivityIntent = Intent().apply {
                component = ComponentName("com.google.android.googlequicksearchbox", "com.google.android.apps.gsa.staticplugins.lens.LensActivity")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val lensDeepLink = Intent(Intent.ACTION_VIEW, Uri.parse("googleapp://lens")).apply {
                setPackage("com.google.android.googlequicksearchbox")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.ar.lens")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val webStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.ar.lens")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val safeLensIntent = when {
                lensPkgIntent != null -> lensPkgIntent
                lensActivityIntent.resolveActivity(context.packageManager) != null -> lensActivityIntent
                lensDeepLink.resolveActivity(context.packageManager) != null -> lensDeepLink
                playStoreIntent.resolveActivity(context.packageManager) != null -> playStoreIntent
                else -> webStoreIntent
            }
            views.setOnClickPendingIntent(
                R.id.btn_google_lens,
                PendingIntent.getActivity(context, widgetId * 100 + 3, safeLensIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )
        }

        appWidgetManager.updateAppWidget(widgetId, views)
    }
}