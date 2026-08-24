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
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.ui.config.AppFolderWidgetConfigActivity

private fun loadSlateWidgetConfig(context: Context, widgetId: Int): SlateWidgetConfig {
    val prefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
    val themeMode = prefs.getString("widget_${widgetId}_theme_mode", "DARK") ?: "DARK"
    val bgColor = prefs.getLong("widget_${widgetId}_bg_color", 0xFF161618L)
    val opacity = prefs.getFloat("widget_${widgetId}_opacity", 1.0f)
    val accentColor = prefs.getLong("widget_${widgetId}_accent_color", 0xFFFFFFFFL)
    return SlateWidgetConfig(
        themeMode = themeMode,
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

        val touchSlotIds = intArrayOf(R.id.touch_slot_0, R.id.touch_slot_1, R.id.touch_slot_2, R.id.touch_slot_3, R.id.touch_slot_4, R.id.touch_slot_5, R.id.touch_slot_6, R.id.touch_slot_7)
        val folderConfig = AppFolderWidgetConfig.load(context, widgetId, slotCount)

        for (i in 0 until slotCount) {
            val slotConfig = folderConfig.slots.getOrElse(i) { AppSlotConfig() }
            val targetViewId = touchSlotIds.getOrNull(i) ?: continue

            val intent = if (!slotConfig.isConfigured) {
                Intent(context, AppFolderWidgetConfigActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    putExtra("extra_slot_index", i)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            } else {
                context.packageManager.getLaunchIntentForPackage(slotConfig.packageName)?.apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                } ?: Intent()
            }

            val pi = PendingIntent.getActivity(context, widgetId * 100 + i, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(targetViewId, pi)
        }
        appWidgetManager.updateAppWidget(widgetId, views)
    }

    abstract fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap
}

// 1. 4-APP FOLDER (2x2)
class AppFolder4Receiver : BaseAppFolderGridReceiver(4, R.layout.widget_appfolder_grid4_layout) {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap = generateAppFolder4Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 2. 8-APP FOLDER (4x2)
class AppFolder8Receiver : BaseAppFolderGridReceiver(8, R.layout.widget_appfolder_grid8_layout) {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap = generateAppFolder8Bitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

fun getAppFolderWidgetsCatalog(): List<SlateWidgetInfo> {
    return listOf(
        SlateWidgetInfo(name = "4-App Grid Folder", sizeText = "2x2", category = "App Folders", receiverClass = AppFolder4Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "8-App Grid Folder", sizeText = "4x2", category = "App Folders", receiverClass = AppFolder8Receiver::class.java, hasModeOption = true)
    )
}

fun updateAllAppFolderWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val receivers = listOf(AppFolder4Receiver::class.java, AppFolder8Receiver::class.java)
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