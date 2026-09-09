package com.altusix.slate.widgets.photos

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

fun getPhotosWidgetsCatalog(): List<SlateWidgetInfo> {
    return listOf(
        SlateWidgetInfo("Taped Polaroid Frame", "2x2", "Photos & Memories", PhotosTapedReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Push Pin Polaroid Frame", "2x2", "Photos & Memories", PhotosPushPinReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Stacked Photo Frame", "2x2", "Photos & Memories", PhotosStackedReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Polaroid Memory", "2x2", "Photos & Memories", PhotosPolaroidReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("On This Day", "4x2", "Photos & Memories", PhotosOnThisDayReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("35mm Film Strip", "4x2", "Photos & Memories", PhotosFilmStripReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Bento Collage", "4x2", "Photos & Memories", PhotosCollageBentoReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Photo Carousel", "2x2", "Photos & Memories", PhotosCarouselReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Photo Stamp", "2x2", "Photos & Memories", PhotosStampReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Locket Memory", "2x2", "Photos & Memories", PhotosLocketReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Full-Bleed Clock", "2x2", "Photos & Memories", PhotosClockOverlayReceiver::class.java, hasModeOption = true),

    )
}

fun updateAllPhotosWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val receivers = listOf(
        PhotosPolaroidReceiver::class.java,
        PhotosOnThisDayReceiver::class.java,
        PhotosFilmStripReceiver::class.java,
        PhotosCollageBentoReceiver::class.java,
        PhotosCarouselReceiver::class.java,
        PhotosStampReceiver::class.java,
        PhotosLocketReceiver::class.java,
        PhotosClockOverlayReceiver::class.java,
        PhotosStackedReceiver::class.java,
        PhotosTapedReceiver::class.java,
        PhotosPushPinReceiver::class.java
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
    val hasCustomTheme = widgetPrefs.getBoolean("widget_${widgetId}_has_custom_theme", false)
    val globalSettings = ThemePreferences(context).getThemeSettings()

    val bgColor = if (hasCustomTheme) {
        widgetPrefs.getLong("widget_${widgetId}_bg_color", globalSettings.bgHex)
    } else {
        globalSettings.bgHex
    }
    val opacity = if (hasCustomTheme) {
        widgetPrefs.getFloat("widget_${widgetId}_opacity", globalSettings.opacity)
    } else {
        globalSettings.opacity
    }
    val accentColor = if (hasCustomTheme) {
        widgetPrefs.getLong("widget_${widgetId}_accent_color", globalSettings.accentHex)
    } else {
        globalSettings.accentHex
    }

    val isLight = (((bgColor shr 16 and 0xFFL) * 0.2126f) +
            ((bgColor shr 8 and 0xFFL) * 0.7152f) +
            ((bgColor and 0xFFL) * 0.0722f)) / 255f > 0.5f
    val mode = if (hasCustomTheme) {
        widgetPrefs.getString("widget_${widgetId}_theme_mode", if (isLight) "LIGHT" else "DARK")
            ?: if (isLight) "LIGHT" else "DARK"
    } else {
        if (isLight) "LIGHT" else "DARK"
    }

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

abstract class BasePhotosReceiver(private val layoutResId: Int) : AppWidgetProvider() {

    open val targetAspect: Float = 1.0f

    companion object {
        const val ACTION_OPEN_CONFIG = "com.altusix.slate.photos.ACTION_OPEN_CONFIG"
        const val ACTION_CYCLE_PHOTO = "com.altusix.slate.photos.ACTION_CYCLE_PHOTO"
        const val EXTRA_DELTA = "extra_delta"
    }

    abstract fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        wDp: Int,
        hDp: Int
    ): Bitmap

    override fun onReceive(context: Context, intent: Intent) {
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

        when (intent.action) {
            ACTION_OPEN_CONFIG -> {
                val editIntent = Intent(context, PhotosConfigActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(editIntent)
                return
            }
            ACTION_CYCLE_PHOTO -> {
                val delta = intent.getIntExtra(EXTRA_DELTA, 1)
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    PhotosStorageManager.cyclePhoto(context, appWidgetId, delta)
                    updateSingleWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
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
            val isResponsive = if (id == -1) (targetAspect == 2.0f) else parseAndLockIsResponsive(context, id)
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

            if (!isResponsive) {
                val currentAspect = wDp.toFloat() / hDp.toFloat()
                if (currentAspect > targetAspect) {
                    val contentW = hDp * targetAspect
                    padH = (((wDp - contentW) / 2f) * density).toInt()
                    padV = 0
                    effWDp = maxOf(1, (wDp - (wDp - contentW)).toInt())
                    effHDp = hDp
                } else {
                    val contentH = wDp / targetAspect
                    padH = 0
                    padV = (((hDp - contentH) / 2f) * density).toInt()
                    effWDp = wDp
                    effHDp = maxOf(1, (hDp - (hDp - contentH)).toInt())
                }
            } else {
                padH = 0
                padV = 0
                effWDp = wDp
                effHDp = hDp
            }

            val bitmap = renderWidgetBitmap(context, id, config, effWDp, effHDp)
            val views = RemoteViews(context.packageName, layoutResId)
            try {
                views.setViewPadding(R.id.layout_photos_root, padH, padV, padH, padV)
            } catch (_: Exception) {}
            views.setImageViewBitmap(R.id.widget_canvas_surface, bitmap)

            setupTouchTargets(context, views, id)
            manager.updateAppWidget(id, views)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    protected open fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        // Base open config intent (Direct Activity Launch - Android 12+ BAL Compliant)
        val openIntent = Intent(context, PhotosConfigActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data = Uri.parse("slate_photos://$appWidgetId/config")
        }
        val openPi = PendingIntent.getActivity(
            context,
            (appWidgetId * 53 + 1),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Bind standard card targets
        try { views.setOnClickPendingIntent(R.id.btn_photo_open, openPi) } catch (_: Exception) {}
        try { views.setOnClickPendingIntent(R.id.btn_photo_open_alt, openPi) } catch (_: Exception) {}
        try { views.setOnClickPendingIntent(R.id.btn_photo_bento_hero, openPi) } catch (_: Exception) {}
        try { views.setOnClickPendingIntent(R.id.btn_photo_bento_sub1, openPi) } catch (_: Exception) {}
        try { views.setOnClickPendingIntent(R.id.btn_photo_bento_sub2, openPi) } catch (_: Exception) {}
        try { views.setOnClickPendingIntent(R.id.btn_film_frame_0, openPi) } catch (_: Exception) {}
        try { views.setOnClickPendingIntent(R.id.btn_film_frame_1, openPi) } catch (_: Exception) {}
        try { views.setOnClickPendingIntent(R.id.btn_film_frame_2, openPi) } catch (_: Exception) {}

        // Bind carousel navigation buttons if present
        val prevIntent = Intent(context, this.javaClass).apply {
            action = ACTION_CYCLE_PHOTO
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(EXTRA_DELTA, -1)
            data = Uri.parse("slate_photos://$appWidgetId/prev")
        }
        val prevPi = PendingIntent.getBroadcast(
            context,
            (appWidgetId * 53 + 2),
            prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try { views.setOnClickPendingIntent(R.id.btn_carousel_prev, prevPi) } catch (_: Exception) {}

        val nextIntent = Intent(context, this.javaClass).apply {
            action = ACTION_CYCLE_PHOTO
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(EXTRA_DELTA, 1)
            data = Uri.parse("slate_photos://$appWidgetId/next")
        }
        val nextPi = PendingIntent.getBroadcast(
            context,
            (appWidgetId * 53 + 3),
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try { views.setOnClickPendingIntent(R.id.btn_carousel_next, nextPi) } catch (_: Exception) {}
    }
}

// 1. Polaroid Memory (2x2)
class PhotosPolaroidReceiver : BasePhotosReceiver(R.layout.widget_photos_card_layout) {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val photoConfig = if (appWidgetId == -1) PhotosWidgetConfig.getDefaultConfig() else PhotosStorageManager.getConfig(context, appWidgetId)
        val isResponsive = if (appWidgetId == -1) false else parseAndLockIsResponsive(context, appWidgetId)
        return generatePolaroidMemoryBitmap(
            context,
            photoConfig.currentItem,
            config,
            isResponsive,
            wDp,
            hDp,
            photoConfig.showCaption
        )
    }
}

// 2. On This Day (Time Machine) (4x2)
class PhotosOnThisDayReceiver : BasePhotosReceiver(R.layout.widget_photos_card_layout) {
    override val targetAspect = 2.0f
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val photoConfig = if (appWidgetId == -1) PhotosWidgetConfig.getDefaultConfig() else PhotosStorageManager.getConfig(context, appWidgetId)
        val isResponsive = if (appWidgetId == -1) true else parseAndLockIsResponsive(context, appWidgetId)
        return generateOnThisDayBitmap(context, photoConfig.currentItem, config, isResponsive, wDp, hDp)
    }
}

// 3. 35mm Film Strip (4x2)
class PhotosFilmStripReceiver : BasePhotosReceiver(R.layout.widget_photos_filmstrip_layout) {
    override val targetAspect = 2.0f
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val photoConfig = if (appWidgetId == -1) PhotosWidgetConfig.getDefaultConfig() else PhotosStorageManager.getConfig(context, appWidgetId)
        val isResponsive = if (appWidgetId == -1) true else parseAndLockIsResponsive(context, appWidgetId)
        return generateFilmStripBitmap(context, photoConfig.items, config, isResponsive, wDp, hDp)
    }
}

// 4. Bento Collage (4x2)
class PhotosCollageBentoReceiver : BasePhotosReceiver(R.layout.widget_photos_bento_layout) {
    override val targetAspect = 2.0f
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val photoConfig = if (appWidgetId == -1) PhotosWidgetConfig.getDefaultConfig() else PhotosStorageManager.getConfig(context, appWidgetId)
        val isResponsive = if (appWidgetId == -1) true else parseAndLockIsResponsive(context, appWidgetId)
        return generateCollageBentoBitmap(context, photoConfig.items, config, isResponsive, wDp, hDp)
    }
}

// 5. Photo Carousel Slideshow (2x2)
class PhotosCarouselReceiver : BasePhotosReceiver(R.layout.widget_photos_carousel_layout) {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val photoConfig = if (appWidgetId == -1) PhotosWidgetConfig.getDefaultConfig() else PhotosStorageManager.getConfig(context, appWidgetId)
        val isResponsive = if (appWidgetId == -1) false else parseAndLockIsResponsive(context, appWidgetId)
        return generatePhotoCarouselBitmap(context, photoConfig, config, isResponsive, wDp, hDp)
    }
}

// 6. Photo Stamp (Vintage Postage) (2x2)
class PhotosStampReceiver : BasePhotosReceiver(R.layout.widget_photos_card_layout) {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val photoConfig = if (appWidgetId == -1) PhotosWidgetConfig.getDefaultConfig() else PhotosStorageManager.getConfig(context, appWidgetId)
        val isResponsive = if (appWidgetId == -1) false else parseAndLockIsResponsive(context, appWidgetId)
        return generatePhotoStampBitmap(
            context = context,
            item = photoConfig.currentItem,
            slateConfig = config,
            isResponsive = isResponsive,
            wDp = wDp,
            hDp = hDp,
            showCaption = photoConfig.showCaption
        )
    }
}

// 7. Locket Memory (2x2)
class PhotosLocketReceiver : BasePhotosReceiver(R.layout.widget_photos_card_layout) {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val photoConfig = if (appWidgetId == -1) PhotosWidgetConfig.getDefaultConfig() else PhotosStorageManager.getConfig(context, appWidgetId)
        val isResponsive = if (appWidgetId == -1) false else parseAndLockIsResponsive(context, appWidgetId)
        return generateLocketMemoryBitmap(context, photoConfig.currentItem, config, isResponsive, wDp, hDp)
    }
}

// 8. Full-Bleed Clock Overlay (2x2)
class PhotosClockOverlayReceiver : BasePhotosReceiver(R.layout.widget_photos_card_layout) {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val photoConfig = if (appWidgetId == -1) PhotosWidgetConfig.getDefaultConfig() else PhotosStorageManager.getConfig(context, appWidgetId)
        val isResponsive = if (appWidgetId == -1) false else parseAndLockIsResponsive(context, appWidgetId)
        return generatePhotoClockOverlayBitmap(
            context = context,
            item = photoConfig.currentItem,
            slateConfig = config,
            isResponsive = isResponsive,
            wDp = wDp,
            hDp = hDp,
            showCaption = photoConfig.showCaption
        )
    }
}

// 9. Stacked Photo Frame (2x2)
class PhotosStackedReceiver : BasePhotosReceiver(R.layout.widget_photos_card_layout) {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val photoConfig = if (appWidgetId == -1) PhotosWidgetConfig.getDefaultConfig() else PhotosStorageManager.getConfig(context, appWidgetId)
        val isResponsive = if (appWidgetId == -1) false else parseAndLockIsResponsive(context, appWidgetId)
        return generateStackedMemoryBitmap(context, photoConfig.currentItem, config, isResponsive, wDp, hDp)
    }
}

// 10. Taped Polaroid Frame (2x2)
class PhotosTapedReceiver : BasePhotosReceiver(R.layout.widget_photos_card_layout) {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val photoConfig = if (appWidgetId == -1) PhotosWidgetConfig.getDefaultConfig() else PhotosStorageManager.getConfig(context, appWidgetId)
        val isResponsive = if (appWidgetId == -1) false else parseAndLockIsResponsive(context, appWidgetId)
        return generateTapedPolaroidBitmap(context, photoConfig.currentItem, config, isResponsive, wDp, hDp)
    }
}

// 11. Push Pin Polaroid Frame (2x2)
class PhotosPushPinReceiver : BasePhotosReceiver(R.layout.widget_photos_card_layout) {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val photoConfig = if (appWidgetId == -1) PhotosWidgetConfig.getDefaultConfig() else PhotosStorageManager.getConfig(context, appWidgetId)
        val isResponsive = if (appWidgetId == -1) false else parseAndLockIsResponsive(context, appWidgetId)
        return generatePushPinBitmap(context, photoConfig.currentItem, config, isResponsive, wDp, hDp)
    }
}
