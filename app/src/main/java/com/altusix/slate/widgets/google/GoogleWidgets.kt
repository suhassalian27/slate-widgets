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
import android.speech.RecognizerIntent

fun getGoogleWidgetsCatalog(): List<SlateWidgetInfo> {
    return listOf(
        SlateWidgetInfo("Search Capsule", "4x1", "Google", GoogleSearchCapsuleReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Workspace Hub", "2x2", "Google", GoogleWorkspaceQuadReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Google Trio", "2x2", "Google", GoogleTrioReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Google 3x3 Hub", "2x2", "Google", GoogleGrid9Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Google Mega Folder", "4x2", "Google", GoogleMegaFolderReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Media Discovery Capsule", "3x1", "Google", GoogleMediaCapsuleReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Lightbar Horizon", "2x2", "Google", GoogleLightbarReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Lens Viewfinder", "2x2", "Google", GoogleLensReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Search Action Dock", "3x2", "Google", GoogleSearchDockReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("YouTube Vinyl & Viewfinder", "2x2", "Google", GoogleYouTubeDualReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Maps Compass & Waypoint", "2x2", "Google", GoogleMapsCompassReceiver::class.java, hasModeOption = true)
    )
}

fun updateAllGoogleWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val receivers = listOf(
        GoogleSearchCapsuleReceiver::class.java,
        GoogleWorkspaceQuadReceiver::class.java,
        GoogleTrioReceiver::class.java,
        GoogleMegaFolderReceiver::class.java,
        GoogleMediaCapsuleReceiver::class.java,
        GoogleLightbarReceiver::class.java,
        GoogleLensReceiver::class.java,
        GoogleSearchDockReceiver::class.java,
        GoogleGrid9Receiver::class.java,
        GoogleYouTubeDualReceiver::class.java,
        GoogleMapsCompassReceiver::class.java
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

// 4. GOOGLE 3x3 GRID FOLDER (2x2)
class GoogleGrid9Receiver : BaseGoogleReceiver() {

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
        return generateGoogleGrid9Bitmap(context, config, false, wDp, hDp, appWidgetId)
    }

    override fun renderAndApplyWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int, options: Bundle?) {
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 160) ?: 160 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 160) ?: 160
        val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 160) ?: 160 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 160) ?: 160
        val wDp = if (wDpRaw <= 0) 160 else wDpRaw
        val hDp = if (hDpRaw <= 0) 160 else hDpRaw

        val isResponsive = parseAndLockIsResponsive(context, widgetId)
        val config = loadSlateWidgetConfig(context, widgetId)

        val views = RemoteViews(context.packageName, R.layout.widget_base_grid_3x3_layout)
        val bitmap = generateGoogleGrid9Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
        views.setImageViewBitmap(R.id.widget_image_view, bitmap)

        val pm = context.packageManager
        fun safeAppIntent(pkg: String, webUrl: String): Intent {
            return pm.getLaunchIntentForPackage(pkg)?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            } ?: Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }

        val intents = listOf(
            // 0: Google Search
            Intent().apply {
                setClassName("com.google.android.googlequicksearchbox", "com.google.android.googlequicksearchbox.SearchActivity")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }.let { if (it.resolveActivity(pm) != null) it else safeAppIntent("com.google.android.googlequicksearchbox", "https://www.google.com") },

            // 1: Chrome
            safeAppIntent("com.android.chrome", "https://www.google.com/chrome"),

            // 2: Gmail
            safeAppIntent("com.google.android.gm", "https://mail.google.com"),

            // 3: Maps
            safeAppIntent("com.google.android.apps.maps", "https://maps.google.com"),

            // 4: YouTube
            safeAppIntent("com.google.android.youtube", "https://youtube.com"),

            // 5: Photos
            safeAppIntent("com.google.android.apps.photos", "https://photos.google.com"),

            // 6: Drive
            safeAppIntent("com.google.android.apps.docs", "https://drive.google.com"),

            // 7: Calendar
            safeAppIntent("com.google.android.calendar", "https://calendar.google.com"),

            // 8: Gemini Live Voice Session
            Intent(RecognizerIntent.ACTION_WEB_SEARCH).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
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

// 5. GOOGLE MEGA FOLDER (4x2 / 10 Apps)
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

// 6. YOUTUBE & MEDIA DISCOVERY CAPSULE (3x1 / 4x1)
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

// 7. GOOGLE LIGHTBAR HORIZON (2x2)
class GoogleLightbarReceiver : BaseGoogleReceiver() {

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
        return generateGoogleLightbarBitmap(context, config, false, wDp, hDp, appWidgetId)
    }

    override fun renderAndApplyWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int, options: Bundle?) {
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 160) ?: 160 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 160) ?: 160
        val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 160) ?: 160 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 160) ?: 160
        val wDp = if (wDpRaw <= 0) 160 else wDpRaw
        val hDp = if (hDpRaw <= 0) 160 else hDpRaw

        val isResponsive = parseAndLockIsResponsive(context, widgetId)
        val config = loadSlateWidgetConfig(context, widgetId)

        val views = RemoteViews(context.packageName, R.layout.widget_image_container)
        val bitmap = generateGoogleLightbarBitmap(context, config, isResponsive, wDp, hDp, widgetId)
        views.setImageViewBitmap(R.id.widget_image_view, bitmap)

        // Launches the Gemini Live Voice session directly
        val geminiLiveVoiceIntent = Intent(RecognizerIntent.ACTION_WEB_SEARCH).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            widgetId,
            geminiLiveVoiceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_image_view, pendingIntent)

        appWidgetManager.updateAppWidget(widgetId, views)
    }
}

// 8. GOOGLE LENS VIEWFINDER (2x2)
class GoogleLensReceiver : BaseGoogleReceiver() {

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
        return generateGoogleLensViewfinderBitmap(context, config, false, wDp, hDp, appWidgetId)
    }

    override fun renderAndApplyWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int, options: Bundle?) {
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 160) ?: 160 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 160) ?: 160
        val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 160) ?: 160 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 160) ?: 160
        val wDp = if (wDpRaw <= 0) 160 else wDpRaw
        val hDp = if (hDpRaw <= 0) 160 else hDpRaw

        val isResponsive = parseAndLockIsResponsive(context, widgetId)
        val config = loadSlateWidgetConfig(context, widgetId)

        val views = RemoteViews(context.packageName, R.layout.widget_image_container)
        val bitmap = generateGoogleLensViewfinderBitmap(context, config, isResponsive, wDp, hDp, widgetId)
        views.setImageViewBitmap(R.id.widget_image_view, bitmap)

        val lensIntent = getDirectGoogleLensIntent(context)
        val pendingIntent = PendingIntent.getActivity(
            context,
            widgetId,
            lensIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_image_view, pendingIntent)

        appWidgetManager.updateAppWidget(widgetId, views)
    }

    private fun getDirectGoogleLensIntent(context: Context): Intent {
        val pm = context.packageManager

        // 1. Direct Google Lens Standalone Activity
        val standaloneLens = Intent().apply {
            setClassName("com.google.ar.lens", "com.google.vr.apps.ornament.app.lens.LensLauncherActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        // 2. Google Search App Embedded Lens Camera Activity
        val gsaLens = Intent().apply {
            setClassName("com.google.android.googlequicksearchbox", "com.google.android.apps.search.lens.LensActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        // 3. Fallback to package launcher or general camera query
        val lensPackageLaunch = pm.getLaunchIntentForPackage("com.google.ar.lens")?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return when {
            standaloneLens.resolveActivity(pm) != null -> standaloneLens
            gsaLens.resolveActivity(pm) != null -> gsaLens
            lensPackageLaunch != null -> lensPackageLaunch
            else -> Intent(Intent.ACTION_VIEW, Uri.parse("https://lens.google.com")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
    }
}

// 9. GOOGLE SEARCH & ACTION DOCK (3x2)
class GoogleSearchDockReceiver : BaseGoogleReceiver() {

    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        return generateGoogleSearchDockBitmap(context, config, false, wDp, hDp, appWidgetId)
    }

    override fun renderAndApplyWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int, options: Bundle?) {
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 260) ?: 260 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 260) ?: 260
        val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 130) ?: 130 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 130) ?: 130
        val wDp = if (wDpRaw <= 0) 260 else wDpRaw
        val hDp = if (hDpRaw <= 0) 130 else hDpRaw

        val config = loadSlateWidgetConfig(context, widgetId)

        val views = RemoteViews(context.packageName, R.layout.widget_google_search_action_dock_layout)
        val bitmap = generateGoogleSearchDockBitmap(context, config, false, wDp, hDp, widgetId)
        views.setImageViewBitmap(R.id.widget_image_view, bitmap)

        val pm = context.packageManager

        // 1. Text Search Intent (Search Pill Body)
        val directSearchIntent = Intent().apply {
            setClassName("com.google.android.googlequicksearchbox", "com.google.android.googlequicksearchbox.SearchActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val fallbackSearchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, "")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val searchIntent = if (directSearchIntent.resolveActivity(pm) != null) directSearchIntent else fallbackSearchIntent

        // 2. Bar Microphone: Widget 1 Voice Assistant Trigger
        val voiceAssistIntent = Intent("android.intent.action.VOICE_ASSIST").apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        val assistIntent = Intent(Intent.ACTION_ASSIST).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        val googleOpaVoiceIntent = Intent("com.google.android.googlequicksearchbox.action.OPA_VOICE_SEARCH").apply {
            setPackage("com.google.android.googlequicksearchbox")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val safeBarMicIntent = when {
            voiceAssistIntent.resolveActivity(pm) != null -> voiceAssistIntent
            assistIntent.resolveActivity(pm) != null -> assistIntent
            googleOpaVoiceIntent.resolveActivity(pm) != null -> googleOpaVoiceIntent
            else -> voiceAssistIntent
        }

        // 3. Bottom Disc 1 (Gemini Icon): Restored to the working Voice Intent
        val geminiLiveVoiceIntent = Intent(RecognizerIntent.ACTION_WEB_SEARCH).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        // 4. Bottom Disc 2 (Lens Icon)
        val standaloneLens = Intent().apply {
            setClassName("com.google.ar.lens", "com.google.vr.apps.ornament.app.lens.LensLauncherActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val gsaLens = Intent().apply {
            setClassName("com.google.android.googlequicksearchbox", "com.google.android.apps.search.lens.LensActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val lensIntent = when {
            standaloneLens.resolveActivity(pm) != null -> standaloneLens
            gsaLens.resolveActivity(pm) != null -> gsaLens
            else -> pm.getLaunchIntentForPackage("com.google.ar.lens") ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://lens.google.com")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        }

        // 5. Bottom Disc 3: Chrome Browser Launch Intent
        val chromeIntent = pm.getLaunchIntentForPackage("com.android.chrome")?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        } ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }


        views.setOnClickPendingIntent(
            R.id.touch_slot_search_pill,
            PendingIntent.getActivity(context, widgetId * 100 + 1, searchIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        )
        views.setOnClickPendingIntent(
            R.id.touch_slot_pill_mic,
            PendingIntent.getActivity(context, widgetId * 100 + 2, safeBarMicIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        )
        views.setOnClickPendingIntent(
            R.id.touch_slot_action_mic,
            PendingIntent.getActivity(context, widgetId * 100 + 3, geminiLiveVoiceIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        )
        views.setOnClickPendingIntent(
            R.id.touch_slot_action_lens,
            PendingIntent.getActivity(context, widgetId * 100 + 4, lensIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        )
        views.setOnClickPendingIntent(
            R.id.touch_slot_action_incognito,
            PendingIntent.getActivity(context, widgetId * 100 + 5, chromeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        )

        appWidgetManager.updateAppWidget(widgetId, views)
    }
}

// 10. YOUTUBE DUAL-DIAL RECEIVER (2x2)
class GoogleYouTubeDualReceiver : BaseGoogleReceiver() {

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
        return generateYouTubeVinylViewfinderBitmap(context, config, false, wDp, hDp, appWidgetId)
    }

    override fun renderAndApplyWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int, options: Bundle?) {
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 160) ?: 160 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 160) ?: 160
        val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 160) ?: 160 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 160) ?: 160
        val wDp = if (wDpRaw <= 0) 160 else wDpRaw
        val hDp = if (hDpRaw <= 0) 160 else hDpRaw

        val isResponsive = parseAndLockIsResponsive(context, widgetId)
        val config = loadSlateWidgetConfig(context, widgetId)

        val views = RemoteViews(context.packageName, R.layout.widget_base_grid_2x2)
        val bitmap = generateYouTubeVinylViewfinderBitmap(context, config, isResponsive, wDp, hDp, widgetId)
        views.setImageViewBitmap(R.id.widget_image_view, bitmap)

        val pm = context.packageManager
        fun safeAppIntent(pkg: String, webUrl: String): Intent {
            return pm.getLaunchIntentForPackage(pkg)?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            } ?: Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }

        // Ordered by reading direction: slot_0, slot_1, slot_2, slot_3
        val intents = listOf(
            // Slot 0 (Top-Left): YouTube Feed
            safeAppIntent("com.google.android.youtube", "https://youtube.com"),

            // Slot 1 (Top-Right): YouTube Shorts
            Intent(Intent.ACTION_VIEW, Uri.parse("https://youtube.com/shorts")).apply {
                setPackage("com.google.android.youtube")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }.let { if (it.resolveActivity(pm) != null) it else safeAppIntent("com.google.android.youtube", "https://youtube.com/shorts") },

            // Slot 2 (Bottom-Left): Liked Music / Mix
            Intent(Intent.ACTION_VIEW, Uri.parse("https://music.youtube.com/playlist?list=LM")).apply {
                setPackage("com.google.android.apps.youtube.music")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }.let { if (it.resolveActivity(pm) != null) it else safeAppIntent("com.google.android.apps.youtube.music", "https://music.youtube.com") },

            // Slot 3 (Bottom-Right): YouTube Music App
            safeAppIntent("com.google.android.apps.youtube.music", "https://music.youtube.com")
        )

        val slotIds = intArrayOf(
            R.id.slot_0,
            R.id.slot_1,
            R.id.slot_2,
            R.id.slot_3
        )

        for (i in 0..3) {
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

// 11. GOOGLE MAPS COMPASS & WAYPOINT RECEIVER (2x2)
class GoogleMapsCompassReceiver : BaseGoogleReceiver() {

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
        return generateGoogleMapsCompassBitmap(context, config, false, wDp, hDp, appWidgetId)
    }

    override fun renderAndApplyWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int, options: Bundle?) {
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 160) ?: 160 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 160) ?: 160
        val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 160) ?: 160 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 160) ?: 160
        val wDp = if (wDpRaw <= 0) 160 else wDpRaw
        val hDp = if (hDpRaw <= 0) 160 else hDpRaw

        val isResponsive = parseAndLockIsResponsive(context, widgetId)
        val config = loadSlateWidgetConfig(context, widgetId)

        val views = RemoteViews(context.packageName, R.layout.widget_base_grid_2x2)
        val bitmap = generateGoogleMapsCompassBitmap(context, config, isResponsive, wDp, hDp, widgetId)
        views.setImageViewBitmap(R.id.widget_image_view, bitmap)

        val pm = context.packageManager
        val isMapsInstalled = pm.getLaunchIntentForPackage("com.google.android.apps.maps") != null

        fun safeMapsIntent(uri: Uri): Intent {
            return Intent(Intent.ACTION_VIEW, uri).apply {
                if (isMapsInstalled) {
                    setPackage("com.google.android.apps.maps")
                }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }

        // Slot 2: Dedicated native navigation intent to the account's saved Home
        val homeIntent = if (isMapsInstalled) {
            Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=Home")).apply {
                setPackage("com.google.android.apps.maps")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=Home")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }

        val intents = listOf(
            // Slot 0 (Top-Left): Google Maps App
            pm.getLaunchIntentForPackage("com.google.android.apps.maps")?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            } ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            },

            // Slot 1 (Top-Right): Direct Commute & Route Planner
            safeMapsIntent(Uri.parse("https://www.google.com/maps/dir/?api=1")),

            // Slot 2 (Bottom-Left): Native Directions to Saved Home
            homeIntent,

            // Slot 3 (Bottom-Right): Explore Nearby
            safeMapsIntent(Uri.parse("https://www.google.com/maps/search/?api=1&query=Explore+nearby"))
        )

        val slotIds = intArrayOf(
            R.id.slot_0,
            R.id.slot_1,
            R.id.slot_2,
            R.id.slot_3
        )

        for (i in 0..3) {
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
