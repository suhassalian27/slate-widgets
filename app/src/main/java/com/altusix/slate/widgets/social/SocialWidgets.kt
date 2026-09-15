package com.altusix.slate.widgets.social

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
    if (widgetPrefs.contains(modeKey)) {
        return widgetPrefs.getString(modeKey, "RESPONSIVE") == "RESPONSIVE"
    }
    if (widgetPrefs.contains(isResponsiveKey)) {
        return widgetPrefs.getBoolean(isResponsiveKey, true)
    }
    val defaultResponsive = true
    widgetPrefs.edit().putBoolean(isResponsiveKey, defaultResponsive).apply()
    return defaultResponsive
}

abstract class BaseSocialGridReceiver(
    private val slotCount: Int,
    private val defaultLayoutResId: Int,
    private val layoutTag: String
) : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle?) {
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    // Allows dynamic layout switching (e.g. row -> col)
    open fun resolveLayoutResId(isResponsive: Boolean, wDp: Int, hDp: Int): Int = defaultLayoutResId

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

        val layoutId = resolveLayoutResId(isResponsive, wDp, hDp)
        val views = RemoteViews(context.packageName, layoutId)
        views.setImageViewBitmap(R.id.widget_image_view, bitmap)

        val touchSlotIds = intArrayOf(
            R.id.slot_0, R.id.slot_1, R.id.slot_2,
            R.id.slot_3, R.id.slot_4, R.id.slot_5,
            R.id.slot_6, R.id.slot_7, R.id.slot_8
        )
        val legacyTouchSlotIds = intArrayOf(
            R.id.touch_slot_0, R.id.touch_slot_1, R.id.touch_slot_2, R.id.touch_slot_3,
            R.id.touch_slot_4, R.id.touch_slot_5, R.id.touch_slot_6, R.id.touch_slot_7,
            R.id.touch_slot_8, R.id.touch_slot_9
        )

        val socialConfig = SocialStorageManager.load(context, widgetId, slotCount, layoutTag)

        for (i in 0 until slotCount) {
            val slot = socialConfig.slots.getOrElse(i) { SocialSlotConfig() }

            val intent = if (!slot.isConfigured) {
                Intent(context, SocialConfigActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    putExtra("extra_slot_index", i)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            } else {
                SocialStorageManager.createLaunchIntent(context, slot)
            }

            val pi = PendingIntent.getActivity(
                context,
                widgetId * 100 + i,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            touchSlotIds.getOrNull(i)?.let { views.setOnClickPendingIntent(it, pi) }
            legacyTouchSlotIds.getOrNull(i)?.let { views.setOnClickPendingIntent(it, pi) }
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

// Synchronous updates to prevent dropped broadcasts
fun updateAllSocialWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context) ?: return
    val receivers: List<BaseSocialGridReceiver> = listOf(
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

// 1. Social Bar (5 Apps Row - 4x1 / 5x1)
class SocialBar5Receiver : BaseSocialGridReceiver(5, R.layout.widget_base_row_5, "BAR_5") {
    override fun resolveLayoutResId(isResponsive: Boolean, wDp: Int, hDp: Int): Int {
        return if (isResponsive && hDp > wDp) R.layout.widget_base_col_5 else R.layout.widget_base_row_5
    }

    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateSocialBar5Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}


// 2. Social Quad (4 Apps 2x2 Grid)
class SocialQuad4Receiver : BaseSocialGridReceiver(4, R.layout.widget_appfolder_grid4_layout, "QUAD_4") {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateSocialQuad4Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 3. Social Matrix (9 Apps 3x3 Grid)
class SocialMatrix9Receiver : BaseSocialGridReceiver(9, R.layout.widget_appfolder_grid9_layout, "MATRIX_9") {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateSocialMatrix9Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 4. Social Deck (10 Apps 2x5 Grid)
class SocialDeck10Receiver : BaseSocialGridReceiver(10, R.layout.widget_megafolder_10_layout, "DECK_10") {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateSocialDeck10Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 5. Social Bento Top (10 Apps: 2 Big Top + 8 Small Bottom)
class SocialBento10TopReceiver : BaseSocialGridReceiver(10, R.layout.widget_appfolder_bento10top_layout, "BENTO_10_TOP") {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateSocialBento10TopBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 6. Social Bento Left (10 Apps: 2 Big Left + 8 Small Right)
class SocialBento10LeftReceiver : BaseSocialGridReceiver(10, R.layout.widget_appfolder_bento10left_layout, "BENTO_10_LEFT") {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateSocialBento10LeftBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 7. Social Orbit (6 Apps Circular Dial)
class SocialOrbit6Receiver : BaseSocialGridReceiver(6, R.layout.widget_appfolder_circle6_layout, "ORBIT_6") {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateSocialOrbit6Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 8. Social Direct Messaging (4 Apps Messaging Row)
class SocialMessaging4Receiver : BaseSocialGridReceiver(4, R.layout.widget_appfolder_grid4row_layout, "MESSAGING_4") {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateSocialMessaging4Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 9. Social Stream (3 Apps Row)
class SocialStream3Receiver : BaseSocialGridReceiver(3, R.layout.widget_base_row_3, "STREAM_3") {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateSocialStream3Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 10. Social Octa Deck (8 Apps 4x2 Grid)
class SocialOcta8Receiver : BaseSocialGridReceiver(8, R.layout.widget_base_grid_4x2, "OCTA_8") {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateSocialOcta8Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 11. Social Twin (2 Apps Vertical Column)
class SocialTwin2Receiver : BaseSocialGridReceiver(2, R.layout.widget_base_column_2, "TWIN_2") {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateSocialTwin2Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 12. Micro Social (1 App Single Tile)
class SocialMicro1Receiver : BaseSocialGridReceiver(1, R.layout.widget_base_single, "MICRO_1") {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateSocialMicro1Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}
