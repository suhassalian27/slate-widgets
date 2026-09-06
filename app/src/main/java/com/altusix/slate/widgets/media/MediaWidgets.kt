package com.altusix.slate.widgets.media

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

fun getMediaWidgetsCatalog(): List<SlateWidgetInfo> {
    return listOf(
        SlateWidgetInfo("Vinyl Turntable", "2x2", "Music & Media", MediaVinylReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Bento Media Player", "4x2", "Music & Media", MediaBentoReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Media Capsule Pill", "4x1", "Music & Media", MediaCapsulePillReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Media Mini Capsule", "2x1", "Music & Media", MediaMiniCapsuleReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Retro Cassette Tape", "4x2", "Music & Media", MediaCassetteReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Spectrum Soundwave", "2x2", "Music & Media", MediaSpectrumReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Editorial Media Card", "2x2", "Music & Media", MediaEditorialReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Media Streaming Dock", "4x1", "Music & Media", MediaDockReceiver::class.java, hasModeOption = true)
    )
}

fun updateAllMediaWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val receivers = listOf(
        MediaVinylReceiver::class.java,
        MediaBentoReceiver::class.java,
        MediaCapsulePillReceiver::class.java,
        MediaMiniCapsuleReceiver::class.java,
        MediaCassetteReceiver::class.java,
        MediaSpectrumReceiver::class.java,
        MediaEditorialReceiver::class.java,
        MediaDockReceiver::class.java
    )
    for (receiverClass in receivers) {
        val ids = manager.getAppWidgetIds(ComponentName(context, receiverClass)) ?: intArrayOf()
        if (ids.isNotEmpty()) {
            val intent = Intent(context, receiverClass).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}

private fun loadSlateWidgetConfig(context: Context, widgetId: Int): SlateWidgetConfig {
    val widgetPrefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
    val bgKey = "widget_${widgetId}_bg_color"

    // Snapshot and permanently lock current global theme on placement
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

abstract class BaseMediaReceiver(private val layoutResId: Int) : AppWidgetProvider() {

    companion object {
        const val ACTION_PLAY_PAUSE = "com.altusix.slate.media.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.altusix.slate.media.ACTION_NEXT"
        const val ACTION_PREV = "com.altusix.slate.media.ACTION_PREV"
        const val ACTION_OPEN_PLAYER = "com.altusix.slate.media.ACTION_OPEN_PLAYER"
        const val ACTION_LAUNCH_APP = "com.altusix.slate.media.ACTION_LAUNCH_APP"
        const val EXTRA_PKG = "extra_package_name"
    }

    abstract fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        wDp: Int,
        hDp: Int
    ): Bitmap

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_PLAY_PAUSE -> {
                SlateMediaNotificationService.handlePlayPause(context)
                return
            }
            ACTION_NEXT -> {
                SlateMediaNotificationService.handleNext(context)
                return
            }
            ACTION_PREV -> {
                SlateMediaNotificationService.handlePrev(context)
                return
            }
            ACTION_OPEN_PLAYER -> {
                if (!MediaPermissionActivity.isNotificationListenerEnabled(context)) {
                    val permIntent = Intent(context, MediaPermissionActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(permIntent)
                } else {
                    SlateMediaNotificationService.openActivePlayer(context)
                }
                return
            }
            ACTION_LAUNCH_APP -> {
                val pkg = intent.getStringExtra(EXTRA_PKG)
                if (!pkg.isNullOrBlank()) {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                    } else {
                        // Open Play Store listing
                        try {
                            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(marketIntent)
                        } catch (_: Exception) {}
                    }
                }
                return
            }
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) updateSingleWidget(context, appWidgetManager, id)
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle?) {
        updateSingleWidget(context, appWidgetManager, appWidgetId)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    private fun updateSingleWidget(context: Context, manager: AppWidgetManager, id: Int) {
        try {
            val config = loadSlateWidgetConfig(context, id)
            val options = manager.getAppWidgetOptions(id)
            val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 160) ?: 160 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 160) ?: 160
            val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 160) ?: 160 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 160) ?: 160
            val wDp = if (wDpRaw <= 0) 160 else wDpRaw
            val hDp = if (hDpRaw <= 0) 160 else hDpRaw

            val bitmap = renderWidgetBitmap(context, id, config, wDp, hDp)
            val views = RemoteViews(context.packageName, layoutResId)
            views.setImageViewBitmap(R.id.widget_canvas_surface, bitmap)

            // Setup Touch Target PendingIntents
            setupTouchTargets(context, views, id)

            manager.updateAppWidget(id, views)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    protected open fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        // Open Player
        val openIntent = Intent(context, this.javaClass).apply {
            action = ACTION_OPEN_PLAYER
            data = Uri.parse("slate_media://$appWidgetId/open")
        }
        val openPi = PendingIntent.getBroadcast(context, (appWidgetId * 31 + 1), openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.btn_media_open_app, openPi)

        // Track info region if present in layout
        try {
            views.setOnClickPendingIntent(R.id.btn_media_track_info, openPi)
        } catch (_: Exception) {}

        // Play / Pause
        val playIntent = Intent(context, this.javaClass).apply {
            action = ACTION_PLAY_PAUSE
            data = Uri.parse("slate_media://$appWidgetId/play_pause")
        }
        val playPi = PendingIntent.getBroadcast(context, (appWidgetId * 31 + 2), playIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.btn_media_play_pause, playPi)

        // Next
        val nextIntent = Intent(context, this.javaClass).apply {
            action = ACTION_NEXT
            data = Uri.parse("slate_media://$appWidgetId/next")
        }
        val nextPi = PendingIntent.getBroadcast(context, (appWidgetId * 31 + 3), nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.btn_media_next, nextPi)

        // Prev
        val prevIntent = Intent(context, this.javaClass).apply {
            action = ACTION_PREV
            data = Uri.parse("slate_media://$appWidgetId/prev")
        }
        val prevPi = PendingIntent.getBroadcast(context, (appWidgetId * 31 + 4), prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.btn_media_prev, prevPi)
    }
}

// 1. Vinyl Turntable (2x2)
class MediaVinylReceiver : BaseMediaReceiver(R.layout.widget_media_2x2_layout) {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val state = if (appWidgetId == -1) MediaStateManager.getMockPreviewState().first else MediaStateManager.loadState(context)
        val art = if (appWidgetId == -1) MediaStateManager.getMockPreviewState().second else MediaStateManager.getArtwork(context)
        val isResponsive = if (appWidgetId == -1) false else parseAndLockIsResponsive(context, appWidgetId)
        return generateVinylPlayerBitmap(context, state, art, config, isResponsive, wDp, hDp)
    }
}

// 2. Bento Media Player (4x2)
class MediaBentoReceiver : BaseMediaReceiver(R.layout.widget_media_4x2_layout) {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val state = if (appWidgetId == -1) MediaStateManager.getMockPreviewState().first else MediaStateManager.loadState(context)
        val art = if (appWidgetId == -1) MediaStateManager.getMockPreviewState().second else MediaStateManager.getArtwork(context)
        val isResponsive = if (appWidgetId == -1) false else parseAndLockIsResponsive(context, appWidgetId)
        return generateBentoMediaBitmap(context, state, art, config, isResponsive, wDp, hDp)
    }
}

// 3. Media Capsule Pill (4x1)
class MediaCapsulePillReceiver : BaseMediaReceiver(R.layout.widget_media_4x1_layout) {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val state = if (appWidgetId == -1) MediaStateManager.getMockPreviewState().first else MediaStateManager.loadState(context)
        val art = if (appWidgetId == -1) MediaStateManager.getMockPreviewState().second else MediaStateManager.getArtwork(context)
        val isResponsive = if (appWidgetId == -1) true else parseAndLockIsResponsive(context, appWidgetId)
        return generateCapsulePillBitmap(context, state, art, config, isResponsive, wDp, hDp)
    }
}

// 4. Media Mini Capsule (2x1)
class MediaMiniCapsuleReceiver : BaseMediaReceiver(R.layout.widget_media_2x1_layout) {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val state = if (appWidgetId == -1) MediaStateManager.getMockPreviewState().first else MediaStateManager.loadState(context)
        val art = if (appWidgetId == -1) MediaStateManager.getMockPreviewState().second else MediaStateManager.getArtwork(context)
        val isResponsive = if (appWidgetId == -1) true else parseAndLockIsResponsive(context, appWidgetId)
        return generateMiniCapsuleBitmap(context, state, art, config, isResponsive, wDp, hDp)
    }
}

// 5. Retro Cassette Tape (4x2)
class MediaCassetteReceiver : BaseMediaReceiver(R.layout.widget_media_4x2_layout) {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val state = if (appWidgetId == -1) MediaStateManager.getMockPreviewState().first else MediaStateManager.loadState(context)
        val art = if (appWidgetId == -1) MediaStateManager.getMockPreviewState().second else MediaStateManager.getArtwork(context)
        val isResponsive = if (appWidgetId == -1) true else parseAndLockIsResponsive(context, appWidgetId)
        return generateCassetteTapeBitmap(context, state, art, config, isResponsive, wDp, hDp)
    }
}

// 6. Spectrum Soundwave (2x2)
class MediaSpectrumReceiver : BaseMediaReceiver(R.layout.widget_media_2x2_layout) {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val state = if (appWidgetId == -1) MediaStateManager.getMockPreviewState().first else MediaStateManager.loadState(context)
        val art = if (appWidgetId == -1) MediaStateManager.getMockPreviewState().second else MediaStateManager.getArtwork(context)
        val isResponsive = if (appWidgetId == -1) true else parseAndLockIsResponsive(context, appWidgetId)
        return generateSpectrumBitmap(context, state, art, config, isResponsive, wDp, hDp)
    }
}

// 7. Editorial Media Card (2x2)
class MediaEditorialReceiver : BaseMediaReceiver(R.layout.widget_media_2x2_layout) {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val state = if (appWidgetId == -1) MediaStateManager.getMockPreviewState().first else MediaStateManager.loadState(context)
        val art = if (appWidgetId == -1) MediaStateManager.getMockPreviewState().second else MediaStateManager.getArtwork(context)
        val isResponsive = if (appWidgetId == -1) true else parseAndLockIsResponsive(context, appWidgetId)
        return generateEditorialBitmap(context, state, art, config, isResponsive, wDp, hDp)
    }
}

// 8. Media Streaming Dock (4x1)
class MediaDockReceiver : BaseMediaReceiver(R.layout.widget_media_dock_layout) {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val isResponsive = if (appWidgetId == -1) true else parseAndLockIsResponsive(context, appWidgetId)
        return generateMediaDockBitmap(context, config, isResponsive, wDp, hDp)
    }

    override fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        val apps = listOf(
            Pair(R.id.btn_dock_spotify, "com.spotify.music"),
            Pair(R.id.btn_dock_ytmusic, "com.google.android.apps.youtube.music"),
            Pair(R.id.btn_dock_applemusic, "com.apple.android.music"),
            Pair(R.id.btn_dock_soundcloud, "com.soundcloud.android"),
            Pair(R.id.btn_dock_shazam, "com.shazam.android")
        )

        for ((viewId, pkg) in apps) {
            val intent = Intent(context, this.javaClass).apply {
                action = ACTION_LAUNCH_APP
                putExtra(EXTRA_PKG, pkg)
                data = Uri.parse("slate_dock://$appWidgetId/$viewId")
            }
            val pi = PendingIntent.getBroadcast(
                context,
                (appWidgetId * 37 + viewId),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(viewId, pi)
        }
    }
}
