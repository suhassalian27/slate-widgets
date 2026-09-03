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
import android.widget.RemoteViews
import com.altusix.slate.R
import com.altusix.slate.core.model.SlateWidgetInfo
import com.altusix.slate.core.receiver.BaseCanvasWidgetProvider
import com.altusix.slate.core.theme.ThemePreferences
import com.altusix.slate.data.local.SlateWidgetConfig

fun getGoogleWidgetsCatalog(): List<SlateWidgetInfo> {
    return listOf(
        SlateWidgetInfo("Search Capsule", "4x1", "Google", GoogleSearchCapsuleReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Workspace Hub", "2x2", "Google", GoogleWorkspaceQuadReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Google Trio", "2x2", "Google", GoogleTrioReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Google Mega Folder", "4x2", "Google", GoogleMegaFolderReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Media Discovery Capsule", "3x1", "Google", GoogleMediaCapsuleReceiver::class.java, hasModeOption = true)
    )
}

fun updateAllGoogleWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val receivers = listOf(
        GoogleSearchCapsuleReceiver::class.java,
        GoogleWorkspaceQuadReceiver::class.java,
        GoogleTrioReceiver::class.java,
        GoogleMegaFolderReceiver::class.java,
        GoogleMediaCapsuleReceiver::class.java
    )
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

        val showLens = wDp >= 140
        val showGemini = wDp >= 210
        val showMic = wDp >= 280

        views.setViewVisibility(R.id.btn_google_lens, if (showLens) android.view.View.VISIBLE else android.view.View.GONE)
        views.setViewVisibility(R.id.btn_google_gemini, if (showGemini) android.view.View.VISIBLE else android.view.View.GONE)
        views.setViewVisibility(R.id.btn_google_mic, if (showMic) android.view.View.VISIBLE else android.view.View.GONE)

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

        if (showMic) {
            val voiceAssistIntent = Intent("android.intent.action.VOICE_ASSIST").apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            val assistIntent = Intent(Intent.ACTION_ASSIST).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
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

        if (showGemini) {
            val geminiPkgIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.bard")
            val geminiWebIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://gemini.google.com")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            val safeGeminiIntent = geminiPkgIntent ?: geminiWebIntent
            views.setOnClickPendingIntent(
                R.id.btn_google_gemini,
                PendingIntent.getActivity(context, widgetId * 100 + 2, safeGeminiIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )
        }

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
            val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.ar.lens")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            val webStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.ar.lens")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }

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

// 2. GOOGLE WORKSPACE QUAD (2x2)
class GoogleWorkspaceQuadReceiver : BaseGoogleReceiver() {

    private fun parseAndLockIsResponsive(context: Context, widgetId: Int): Boolean {
        val widgetPrefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
        val modeKey = "widget_${widgetId}_mode"
        if (widgetPrefs.contains(modeKey)) {
            return widgetPrefs.getString(modeKey, "RESPONSIVE") == "RESPONSIVE"
        }
        val respKey = "widget_${widgetId}_is_responsive"
        if (widgetPrefs.contains(respKey)) {
            return widgetPrefs.getBoolean(respKey, true)
        }
        val launcherPrefs = context.getSharedPreferences("slate_app_launcher_prefs", Context.MODE_PRIVATE)
        val defaultResp = launcherPrefs.getBoolean("default_is_responsive", true)
        widgetPrefs.edit().putBoolean(respKey, defaultResp).apply()
        return defaultResp
    }

    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        // Preview Isolation: Strictly locked to Fixed 1:1 mode
        return generateGoogleWorkspaceQuadBitmap(context, config, false, wDp, hDp, appWidgetId)
    }

    override fun renderAndApplyWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int, options: Bundle?) {
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 160) ?: 160 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 160) ?: 160
        val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 160) ?: 160 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 160) ?: 160
        val wDp = if (wDpRaw <= 0) 160 else wDpRaw
        val hDp = if (hDpRaw <= 0) 160 else hDpRaw

        val isResponsive = parseAndLockIsResponsive(context, widgetId)
        val config = loadSlateWidgetConfig(context, widgetId)

        val views = RemoteViews(context.packageName, R.layout.widget_appfolder_grid4_layout)
        val bitmap = generateGoogleWorkspaceQuadBitmap(context, config, isResponsive, wDp, hDp, widgetId)
        views.setImageViewBitmap(R.id.widget_image_view, bitmap)

        val intents = listOf(
            Intent(Intent.ACTION_WEB_SEARCH).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK },
            context.packageManager.getLaunchIntentForPackage("com.google.android.youtube") ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK },
            context.packageManager.getLaunchIntentForPackage("com.google.android.gm") ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://mail.google.com")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK },
            context.packageManager.getLaunchIntentForPackage("com.google.android.apps.docs") ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://drive.google.com")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        )

        val slotViewIds = intArrayOf(
            R.id.touch_slot_0,
            R.id.touch_slot_1,
            R.id.touch_slot_2,
            R.id.touch_slot_3
        )

        for (i in 0..3) {
            views.setOnClickPendingIntent(
                slotViewIds[i],
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

// 3. GOOGLE TRIO BENTO (2x2)
class GoogleTrioReceiver : BaseGoogleReceiver() {

    private fun parseAndLockIsResponsive(context: Context, widgetId: Int): Boolean {
        val widgetPrefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
        val modeKey = "widget_${widgetId}_mode"
        if (widgetPrefs.contains(modeKey)) {
            return widgetPrefs.getString(modeKey, "RESPONSIVE") == "RESPONSIVE"
        }
        val respKey = "widget_${widgetId}_is_responsive"
        if (widgetPrefs.contains(respKey)) {
            return widgetPrefs.getBoolean(respKey, true)
        }
        val launcherPrefs = context.getSharedPreferences("slate_app_launcher_prefs", Context.MODE_PRIVATE)
        val defaultResp = launcherPrefs.getBoolean("default_is_responsive", true)
        widgetPrefs.edit().putBoolean(respKey, defaultResp).apply()
        return defaultResp
    }

    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        return generateGoogleTrioBitmap(context, config, false, wDp, hDp, appWidgetId)
    }

    override fun renderAndApplyWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int, options: Bundle?) {
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 160) ?: 160 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 160) ?: 160
        val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 160) ?: 160 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 160) ?: 160
        val wDp = if (wDpRaw <= 0) 160 else wDpRaw
        val hDp = if (hDpRaw <= 0) 160 else hDpRaw

        val isResponsive = parseAndLockIsResponsive(context, widgetId)
        val config = loadSlateWidgetConfig(context, widgetId)

        val aspectRatio = wDp.toFloat() / hDp.toFloat()

        // Adaptive layout selection matching canvas reflow
        val layoutResId = when {
            isResponsive && aspectRatio < 0.72f -> {
                val vLayout = context.resources.getIdentifier("widget_appfolder_grid3v_layout", "layout", context.packageName)
                if (vLayout != 0) vLayout else R.layout.widget_bento_trio_layout
            }
            isResponsive && aspectRatio > 1.65f -> {
                val hLayout = context.resources.getIdentifier("widget_appfolder_grid3_layout", "layout", context.packageName)
                if (hLayout != 0) hLayout else R.layout.widget_bento_trio_layout
            }
            else -> R.layout.widget_bento_trio_layout
        }

        val views = RemoteViews(context.packageName, layoutResId)
        val bitmap = generateGoogleTrioBitmap(context, config, isResponsive, wDp, hDp, widgetId)
        views.setImageViewBitmap(R.id.widget_image_view, bitmap)

        val directSearchIntent = Intent().apply {
            setClassName("com.google.android.googlequicksearchbox", "com.google.android.googlequicksearchbox.SearchActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val fallbackSearchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, "")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val safeSearchIntent = if (directSearchIntent.resolveActivity(context.packageManager) != null) directSearchIntent else fallbackSearchIntent

        val youtubeIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
            ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }

        val photosIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.photos")
            ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://photos.google.com")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }

        // slot_0: Google (top banner / left tile / top column)
        // slot_1: YouTube (bottom-left / center tile / middle column)
        // slot_2: Photos (bottom-right / right tile / bottom column)
        val intents = listOf(safeSearchIntent, youtubeIntent, photosIntent)
        val slotIds = intArrayOf(R.id.touch_slot_0, R.id.touch_slot_1, R.id.touch_slot_2)

        for (i in 0..2) {
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

// 4. GOOGLE MEGA FOLDER (4x2 / 10 Apps)
class GoogleMegaFolderReceiver : BaseGoogleReceiver() {

    private fun parseAndLockIsResponsive(context: Context, widgetId: Int): Boolean {
        val widgetPrefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
        val modeKey = "widget_${widgetId}_mode"
        if (widgetPrefs.contains(modeKey)) {
            return widgetPrefs.getString(modeKey, "RESPONSIVE") == "RESPONSIVE"
        }
        val respKey = "widget_${widgetId}_is_responsive"
        if (widgetPrefs.contains(respKey)) {
            return widgetPrefs.getBoolean(respKey, true)
        }
        val launcherPrefs = context.getSharedPreferences("slate_app_launcher_prefs", Context.MODE_PRIVATE)
        val defaultResp = launcherPrefs.getBoolean("default_is_responsive", true)
        widgetPrefs.edit().putBoolean(respKey, defaultResp).apply()
        return defaultResp
    }

    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        return generateGoogleMegaFolder10Bitmap(context, config, false, wDp, hDp, appWidgetId)
    }

    override fun renderAndApplyWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int, options: Bundle?) {
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 260) ?: 260 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 260) ?: 260
        val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 130) ?: 130 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 130) ?: 130
        val wDp = if (wDpRaw <= 0) 260 else wDpRaw
        val hDp = if (hDpRaw <= 0) 130 else hDpRaw

        val isResponsive = parseAndLockIsResponsive(context, widgetId)
        val config = loadSlateWidgetConfig(context, widgetId)

        val aspectRatio = wDp.toFloat() / hDp.toFloat()
        val layoutResId = if (aspectRatio >= 1.1f) {
            R.layout.widget_megafolder_10_layout
        } else {
            R.layout.widget_megafolder_10v_layout
        }

        val views = RemoteViews(context.packageName, layoutResId)
        val bitmap = generateGoogleMegaFolder10Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
        views.setImageViewBitmap(R.id.widget_image_view, bitmap)

        val directSearchIntent = Intent().apply {
            setClassName("com.google.android.googlequicksearchbox", "com.google.android.googlequicksearchbox.SearchActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val fallbackSearchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, "")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val safeSearchIntent = if (directSearchIntent.resolveActivity(context.packageManager) != null) directSearchIntent else fallbackSearchIntent

        val intents = listOf(
            safeSearchIntent,
            context.packageManager.getLaunchIntentForPackage("com.google.android.youtube") ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK },
            context.packageManager.getLaunchIntentForPackage("com.google.android.gm") ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://mail.google.com")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK },
            context.packageManager.getLaunchIntentForPackage("com.google.android.apps.docs") ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://drive.google.com")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK },
            context.packageManager.getLaunchIntentForPackage("com.google.android.apps.photos") ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://photos.google.com")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK },
            context.packageManager.getLaunchIntentForPackage("com.google.android.apps.maps") ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK },
            context.packageManager.getLaunchIntentForPackage("com.google.android.calendar") ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://calendar.google.com")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK },
            context.packageManager.getLaunchIntentForPackage("com.android.chrome") ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/chrome")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK },
            context.packageManager.getLaunchIntentForPackage("com.android.vending") ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK },
            context.packageManager.getLaunchIntentForPackage("com.google.android.apps.bard") ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://gemini.google.com")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        )

        val slotIds = intArrayOf(
            R.id.touch_slot_0, R.id.touch_slot_1, R.id.touch_slot_2, R.id.touch_slot_3, R.id.touch_slot_4,
            R.id.touch_slot_5, R.id.touch_slot_6, R.id.touch_slot_7, R.id.touch_slot_8, R.id.touch_slot_9
        )

        for (i in 0 until 10) {
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

// 5. YOUTUBE & MEDIA DISCOVERY CAPSULE (3x1 / 4x1)
class GoogleMediaCapsuleReceiver : BaseGoogleReceiver() {

    private fun parseAndLockIsResponsive(context: Context, widgetId: Int): Boolean {
        val widgetPrefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
        val modeKey = "widget_${widgetId}_mode"
        if (widgetPrefs.contains(modeKey)) {
            return widgetPrefs.getString(modeKey, "RESPONSIVE") == "RESPONSIVE"
        }
        val respKey = "widget_${widgetId}_is_responsive"
        if (widgetPrefs.contains(respKey)) {
            return widgetPrefs.getBoolean(respKey, true)
        }
        val launcherPrefs = context.getSharedPreferences("slate_app_launcher_prefs", Context.MODE_PRIVATE)
        val defaultResp = launcherPrefs.getBoolean("default_is_responsive", true)
        widgetPrefs.edit().putBoolean(respKey, defaultResp).apply()
        return defaultResp
    }

    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        return generateGoogleMediaCapsuleBitmap(context, config, false, wDp, hDp, appWidgetId)
    }

    override fun renderAndApplyWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int, options: Bundle?) {
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 220) ?: 220 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 220) ?: 220
        val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 70) ?: 70 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 70) ?: 70
        val wDp = if (wDpRaw <= 0) 220 else wDpRaw
        val hDp = if (hDpRaw <= 0) 70 else hDpRaw

        val isResponsive = parseAndLockIsResponsive(context, widgetId)
        val config = loadSlateWidgetConfig(context, widgetId)

        val aspectRatio = wDp.toFloat() / hDp.toFloat()
        val layoutResId = if (isResponsive && aspectRatio < 0.85f) {
            R.layout.widget_appfolder_grid3v_layout
        } else {
            R.layout.widget_appfolder_grid3_layout
        }

        val views = RemoteViews(context.packageName, layoutResId)
        val bitmap = generateGoogleMediaCapsuleBitmap(context, config, isResponsive, wDp, hDp, widgetId)
        views.setImageViewBitmap(R.id.widget_image_view, bitmap)

        // 1. YouTube Intent
        val youtubeIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
            ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }

        // 2. Direct Google Sound Search Intent with robust fallbacks
        val directMusicSearch = Intent("com.google.android.googlequicksearchbox.MUSIC_SEARCH").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val voiceMusicSearch = Intent("android.intent.action.MAIN").apply {
            setClassName("com.google.android.googlequicksearchbox", "com.google.android.googlequicksearchbox.SearchActivity")
            putExtra("android.speech.extra.SEARCH_TYPE", "music")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val fallbackSoundSearch = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, "what song is this")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val safeSoundSearchIntent = when {
            directMusicSearch.resolveActivity(context.packageManager) != null -> directMusicSearch
            voiceMusicSearch.resolveActivity(context.packageManager) != null -> voiceMusicSearch
            else -> fallbackSoundSearch
        }

        // 3. YouTube Music Intent
        val ytMusicIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.youtube.music")
            ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://music.youtube.com")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }

        val intents = listOf(youtubeIntent, safeSoundSearchIntent, ytMusicIntent)
        val slotIds = intArrayOf(R.id.touch_slot_0, R.id.touch_slot_1, R.id.touch_slot_2)

        for (i in 0..2) {
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
