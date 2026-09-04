package com.altusix.slate.widgets.appfolder

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
import com.altusix.slate.ui.config.AppFolderWidgetConfigActivity

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
    val launcherPrefs = context.getSharedPreferences("slate_app_launcher_prefs", Context.MODE_PRIVATE)
    val defaultResponsive = launcherPrefs.getBoolean("default_is_responsive", true)
    widgetPrefs.edit().putBoolean(isResponsiveKey, defaultResponsive).apply()
    return defaultResponsive
}

abstract class BaseAppFolderGridReceiver(private val slotCount: Int, private val layoutResId: Int) : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
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

        val views = RemoteViews(context.packageName, layoutResId)
        views.setImageViewBitmap(R.id.widget_image_view, bitmap)

        // Maps both naming conventions (widget_base_grid_3x3 uses slot_x, older layouts use touch_slot_x)
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

        val folderConfig = AppFolderWidgetConfig.load(context, widgetId, slotCount)

        for (i in 0 until slotCount) {
            val slotConfig = folderConfig.slots.getOrElse(i) { AppSlotConfig() }

            val intent = if (!slotConfig.isConfigured) {
                Intent(context, AppFolderWidgetConfigActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    putExtra("extra_slot_index", i)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            } else {
                context.packageManager.getLaunchIntentForPackage(slotConfig.packageName)?.apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                } ?: Intent(context, AppFolderWidgetConfigActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    putExtra("extra_slot_index", i)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            }

            val pi = PendingIntent.getActivity(
                context,
                widgetId * 100 + i,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Bind to whichever ID format is present in the layout
            touchSlotIds.getOrNull(i)?.let { views.setOnClickPendingIntent(it, pi) }
            legacyTouchSlotIds.getOrNull(i)?.let { views.setOnClickPendingIntent(it, pi) }
        }

        // Global fallback: If no apps configured, tapping the background card opens config
        if (folderConfig.slots.none { it.isConfigured }) {
            val rootIntent = Intent(context, AppFolderWidgetConfigActivity::class.java).apply {
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


fun getAppFolderWidgetsCatalog(): List<SlateWidgetInfo> {
    return listOf(
        SlateWidgetInfo(name = "4-App Grid Folder", sizeText = "2x2", category = "App Folders", receiverClass = AppFolder4Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "8-App Grid Folder", sizeText = "4x2", category = "App Folders", receiverClass = AppFolder8Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "3-App Horizontal", sizeText = "3x1", category = "App Folders", receiverClass = AppFolderHorizontal3Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "3-App Vertical", sizeText = "1x3", category = "App Folders", receiverClass = AppFolderVertical3Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "4-App Row", sizeText = "4x1", category = "App Folders", receiverClass = AppFolderRow4Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "5-App Row", sizeText = "5x1", category = "App Folders", receiverClass = AppFolderRow5Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "6-App Circle Dial", sizeText = "2x2", category = "App Folders", receiverClass = AppFolderCircle6Receiver::class.java, hasModeOption = false),
        SlateWidgetInfo(name = "4-App Triangle Folder", sizeText = "2x2", category = "App Folders", receiverClass = AppFolderTriangle4Receiver::class.java, hasModeOption = false),
        SlateWidgetInfo(name = "7-App Bento Folder", sizeText = "2x2", category = "App Folders", receiverClass = AppFolderBento7Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "9-App Grid Folder", sizeText = "3x3", category = "App Folders", receiverClass = AppFolderGrid9Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "10-App Bento Left", sizeText = "4x2", category = "App Folders", receiverClass = AppFolderBento10LeftReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "10-App Bento Top", sizeText = "4x2", category = "App Folders", receiverClass = AppFolderBento10TopReceiver::class.java, hasModeOption = true),
    )
}

fun updateAllAppFolderWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val receivers = listOf(
        AppFolder4Receiver::class.java,
        AppFolder8Receiver::class.java,
        AppFolderHorizontal3Receiver::class.java,
        AppFolderVertical3Receiver::class.java,
        AppFolderRow4Receiver::class.java,
        AppFolderRow5Receiver::class.java,
        AppFolderCircle6Receiver::class.java,
        AppFolderBento7Receiver::class.java,
        AppFolderGrid9Receiver::class.java,
        AppFolderBento10LeftReceiver::class.java,
        AppFolderBento10TopReceiver::class.java,
        AppFolderTriangle4Receiver::class.java
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


// 1. 4-APP FOLDER (2x2)
class AppFolder4Receiver : BaseAppFolderGridReceiver(4, R.layout.widget_appfolder_grid4_layout) {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap = generateAppFolder4Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 2. 8-APP FOLDER (4x2)
class AppFolder8Receiver : BaseAppFolderGridReceiver(8, R.layout.widget_appfolder_grid8_layout) {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap = generateAppFolder8Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 3. 3-APP HORIZONTAL (3x1)
class AppFolderHorizontal3Receiver : BaseAppFolderGridReceiver(3, R.layout.widget_appfolder_grid3_layout) {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap = generateAppFolderHorizontal3Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 4. 3-APP VERTICAL (1x3)
class AppFolderVertical3Receiver : BaseAppFolderGridReceiver(3, R.layout.widget_appfolder_grid3v_layout) {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap = generateAppFolderVertical3Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 5. 4-APP ROW (4x1)
class AppFolderRow4Receiver : BaseAppFolderGridReceiver(4, R.layout.widget_appfolder_grid4row_layout) {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap = generateAppFolderRow4Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 6. 5-APP ROW (5x1)
class AppFolderRow5Receiver : BaseAppFolderGridReceiver(5, R.layout.widget_appfolder_grid5row_layout) {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap = generateAppFolderRow5Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 7. 6-APP CIRCLE DIAL (2x2)
class AppFolderCircle6Receiver : BaseAppFolderGridReceiver(6, R.layout.widget_appfolder_circle6_layout) {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap = generateAppFolderCircle6Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 8. 7-APP BENTO (2x2)
class AppFolderBento7Receiver : BaseAppFolderGridReceiver(7, R.layout.widget_appfolder_bento7_layout) {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap = generateAppFolderBento7Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 9. 9-APP GRID (3x3)
class AppFolderGrid9Receiver : BaseAppFolderGridReceiver(9, R.layout.widget_base_grid_3x3_layout) {
    override fun renderBitmapForWidget(
        context: Context,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int,
        widgetId: Int
    ): Bitmap = generateAppFolderGrid9Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 10. 10-APP BENTO LEFT BIG (4x2)
class AppFolderBento10LeftReceiver : BaseAppFolderGridReceiver(10, R.layout.widget_appfolder_bento10left_layout) {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap = generateAppFolderBento10LeftBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 11. 10-APP BENTO TOP BIG (4x2)
class AppFolderBento10TopReceiver : BaseAppFolderGridReceiver(10, R.layout.widget_appfolder_bento10top_layout) {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap = generateAppFolderBento10TopBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 12. 4-APP TRIANGLE FOLDER (2x2)
class AppFolderTriangle4Receiver : BaseAppFolderGridReceiver(4, R.layout.widget_base_triangle4) {
    override fun renderBitmapForWidget(
        context: Context,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int,
        widgetId: Int
    ): Bitmap = generateAppFolderTriangle4Bitmap(context, config, false, wDp, hDp, widgetId)
}
