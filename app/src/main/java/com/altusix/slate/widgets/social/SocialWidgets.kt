package com.altusix.slate.widgets.social

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import com.altusix.slate.R
import com.altusix.slate.core.model.SlateWidgetInfo
import com.altusix.slate.core.theme.ThemePreferences
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.widgets.common.bindFolderTouchSlots

private fun loadSlateWidgetConfig(context: Context, widgetId: Int): SlateWidgetConfig {
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

abstract class BaseSocialGridReceiver(
    open val slotCount: Int = 4,
    open val defaultLayoutResId: Int = R.layout.widget_base_grid_2x2,
    open val layoutTag: String = "QUAD_4",
    open val targetAspect: Float = 0f
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
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle?) {
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    open fun resolveLayoutResId(isResponsive: Boolean, wDp: Int, hDp: Int): Int = defaultLayoutResId

    fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 200) ?: 200
        val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 200) ?: 200
        val wDp = if (wDpRaw <= 0) 200 else wDpRaw
        val hDp = if (hDpRaw <= 0) 200 else hDpRaw
        val density = context.resources.displayMetrics.density

        val isResponsive = if (widgetId == -1) true else parseAndLockIsResponsive(context, widgetId)
        val config = loadSlateWidgetConfig(context, widgetId)

        val padH: Int
        val padV: Int
        val effWDp: Int
        val effHDp: Int

        if (!isResponsive && targetAspect > 0f) {
            val currentAspect = wDp.toFloat() / hDp.toFloat()
            if (targetAspect >= 3.0f) {
                // Wide bars prioritize 100% width flush against launcher bounds
                val contentH = (wDp / targetAspect).coerceAtMost(hDp.toFloat())
                padH = 0
                padV = (((hDp - contentH) / 2f) * density).toInt()
                effWDp = wDp
                effHDp = maxOf(1, contentH.toInt())
            } else if (currentAspect > targetAspect) {
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

        val bitmap = renderBitmapForWidget(context, config, isResponsive, effWDp, effHDp, widgetId)
        val layoutId = resolveLayoutResId(isResponsive, effWDp, effHDp)
        val views = RemoteViews(context.packageName, layoutId)

        try {
            views.setViewPadding(R.id.layout_grid_root, padH, padV, padH, padV)
        } catch (_: Exception) {}

        views.setImageViewBitmap(R.id.widget_image_view, bitmap)

        val socialConfig = SocialStorageManager.load(context, widgetId, slotCount, layoutTag)

        // Single unified binding for slot_0..9 and legacy touch_slot_0..9
        views.bindFolderTouchSlots(context, widgetId, slotCount) { i ->
            val slot = socialConfig.slots.getOrElse(i) { SocialSlotConfig() }
            if (!slot.isConfigured) {
                Intent(context, SocialConfigActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    putExtra("extra_slot_index", i)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            } else {
                SocialStorageManager.createLaunchIntent(context, slot)
            }
        }

        if (socialConfig.slots.none { it.isConfigured }) {
            val rootIntent = Intent(context, SocialConfigActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putExtra("extra_slot_index", 0)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val rootPi = PendingIntent.getActivity(
                context,
                widgetId,
                rootIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_image_view, rootPi)
        }

        appWidgetManager.updateAppWidget(widgetId, views)
    }

    abstract fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap
}

fun getSocialWidgetsCatalog(): List<SlateWidgetInfo> {
    return listOf(
        SlateWidgetInfo(name = "Social Bar", sizeText = "4x1", category = "Social", receiverClass = SocialBar5Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Social Folder 1", sizeText = "2x2", category = "Social", receiverClass = SocialQuad4Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Social Folder 3", sizeText = "2x2", category = "Social", receiverClass = SocialMatrix9Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Social Folder 2", sizeText = "4x2", category = "Social", receiverClass = SocialDeck10Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Social Folder 4", sizeText = "4x2", category = "Social", receiverClass = SocialBento10TopReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Social Folder 5", sizeText = "4x2", category = "Social", receiverClass = SocialBento10LeftReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Social Orbit", sizeText = "2x2", category = "Social", receiverClass = SocialOrbit6Receiver::class.java, hasModeOption = false),
        SlateWidgetInfo(name = "Social DMs Hub", sizeText = "4x1", category = "Social", receiverClass = SocialMessaging4Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Social Stream", sizeText = "3x1", category = "Social", receiverClass = SocialStream3Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Social Octa Deck", sizeText = "4x2", category = "Social", receiverClass = SocialOcta8Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Social Twin", sizeText = "1x2", category = "Social", receiverClass = SocialTwin2Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Micro Social", sizeText = "1x1", category = "Social", receiverClass = SocialMicro1Receiver::class.java, hasModeOption = true)
    )
}

fun updateAllSocialWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context) ?: return
    val receivers = listOf(
        SocialBar5Receiver(),
        SocialQuad4Receiver(),
        SocialMatrix9Receiver(),
        SocialDeck10Receiver(),
        SocialBento10TopReceiver(),
        SocialBento10LeftReceiver(),
        SocialOrbit6Receiver(),
        SocialMessaging4Receiver(),
        SocialStream3Receiver(),
        SocialOcta8Receiver(),
        SocialTwin2Receiver(),
        SocialMicro1Receiver()
    )
    for (receiver in receivers) {
        val ids = manager.getAppWidgetIds(ComponentName(context, receiver::class.java)) ?: intArrayOf()
        for (id in ids) {
            receiver.updateWidget(context, manager, id)
        }
    }
}

// 1. Social Bar (5 Apps Row - 4x1 / 5x1)
class SocialBar5Receiver : BaseSocialGridReceiver(slotCount = 5, defaultLayoutResId = R.layout.widget_base_row_5,    layoutTag = "BAR_5",    targetAspect = 0f) {
    override fun resolveLayoutResId(isResponsive: Boolean, wDp: Int, hDp: Int): Int {
        val isVertical = isResponsive && (hDp > wDp)
        return if (isVertical) R.layout.widget_base_col_5 else R.layout.widget_base_row_5
    }

    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateSocialBar5Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 2. Social Quad (4 Apps 2x2 Grid)
class SocialQuad4Receiver : BaseSocialGridReceiver(4, R.layout.widget_base_grid_2x2, "QUAD_4", targetAspect = 1.0f) {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateSocialQuad4Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 3. Social Matrix (9 Apps 3x3 Grid)
class SocialMatrix9Receiver : BaseSocialGridReceiver(9, R.layout.widget_base_grid_3x3_layout, "MATRIX_9", targetAspect = 1.0f) {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateSocialMatrix9Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 4. Social Deck (10 Apps 5x2 / 2x5 Smart Grid)
class SocialDeck10Receiver : BaseSocialGridReceiver(slotCount = 10, defaultLayoutResId = R.layout.widget_base_grid_5x2, layoutTag = "DECK_10", targetAspect = 2.5f) {
    override fun resolveLayoutResId(isResponsive: Boolean, wDp: Int, hDp: Int): Int {
        val isVertical = isResponsive && (hDp > wDp)
        return if (isVertical) R.layout.widget_base_grid_2x5 else R.layout.widget_base_grid_5x2
    }

    override fun renderBitmapForWidget(
        context: Context,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int,
        widgetId: Int
    ): Bitmap = generateSocialDeck10Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 5. Social Bento Top (10 Apps: 2 Big Top + 8 Small Bottom)
class SocialBento10TopReceiver : BaseSocialGridReceiver(slotCount = 10, defaultLayoutResId = R.layout.widget_base_bento_top_10, layoutTag = "BENTO_10_TOP", targetAspect = 2.0f) {
    override fun renderBitmapForWidget(
        context: Context,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int,
        widgetId: Int
    ): Bitmap = generateSocialBento10TopBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 6. Social Bento Left (10 Apps: 2 Big Left + 8 Small Right)
class SocialBento10LeftReceiver : BaseSocialGridReceiver(slotCount = 10, defaultLayoutResId = R.layout.widget_base_bento_left_10, layoutTag = "BENTO_10_LEFT", targetAspect = 2.0f) {
    override fun renderBitmapForWidget(
        context: Context,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int,
        widgetId: Int
    ): Bitmap = generateSocialBento10LeftBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 7. Social Orbit (6 Apps Circular Dial)
class SocialOrbit6Receiver : BaseSocialGridReceiver(slotCount = 6, defaultLayoutResId = R.layout.widget_base_orbit_6, layoutTag = "ORBIT_6", targetAspect = 1.0f) {
    override fun renderBitmapForWidget(
        context: Context,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int,
        widgetId: Int
    ): Bitmap = generateSocialOrbit6Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 8. Social Direct Messaging (4 Apps Messaging Row - 4x1 / 1x4 Pivot)
class SocialMessaging4Receiver : BaseSocialGridReceiver(
    slotCount = 4,
    defaultLayoutResId = R.layout.widget_base_row_4,
    layoutTag = "MESSAGING_4",
    targetAspect = 0f // Set to 0f for width-dominant fit
) {
    override fun resolveLayoutResId(isResponsive: Boolean, wDp: Int, hDp: Int): Int {
        val isVertical = isResponsive && (hDp > wDp)
        return if (isVertical) R.layout.widget_base_col_4 else R.layout.widget_base_row_4
    }

    override fun renderBitmapForWidget(
        context: Context,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int,
        widgetId: Int
    ): Bitmap = generateSocialMessaging4Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}
// 9. Social Stream (3 Apps Row - 3x1 / 1x3 Pivot)
class SocialStream3Receiver : BaseSocialGridReceiver(
    slotCount = 3,
    defaultLayoutResId = R.layout.widget_base_row_3,
    layoutTag = "STREAM_3",
    targetAspect = 0f // Set to 0f for width-dominant fit
) {
    override fun resolveLayoutResId(isResponsive: Boolean, wDp: Int, hDp: Int): Int {
        val isVertical = isResponsive && (hDp > wDp)
        return if (isVertical) R.layout.widget_base_column_3 else R.layout.widget_base_row_3
    }

    override fun renderBitmapForWidget(
        context: Context,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int,
        widgetId: Int
    ): Bitmap = generateSocialStream3Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 10. Social Octa Deck (8 Apps 4x2 / 2x4 Pivot)
class SocialOcta8Receiver : BaseSocialGridReceiver(
    slotCount = 8,
    defaultLayoutResId = R.layout.widget_base_grid_4x2,
    layoutTag = "OCTA_8",
    targetAspect = 2.0f
) {
    override fun resolveLayoutResId(isResponsive: Boolean, wDp: Int, hDp: Int): Int {
        val isVertical = isResponsive && (hDp > wDp)
        return if (isVertical) R.layout.widget_base_grid_2x4 else R.layout.widget_base_grid_4x2
    }

    override fun renderBitmapForWidget(
        context: Context,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int,
        widgetId: Int
    ): Bitmap = generateSocialOcta8Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 11. Social Twin (2 Apps Vertical Column)
class SocialTwin2Receiver : BaseSocialGridReceiver(2, R.layout.widget_base_column_2, "TWIN_2", targetAspect = 0.5f) {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateSocialTwin2Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 12. Micro Social (1 App Single Tile)
class SocialMicro1Receiver : BaseSocialGridReceiver(1, R.layout.widget_base_single, "MICRO_1", targetAspect = 1.0f) {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateSocialMicro1Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}