package com.altusix.slate.widgets.bluetooth

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.Bundle
import android.widget.RemoteViews
import com.altusix.slate.R
import com.altusix.slate.core.model.SlateWidgetInfo
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.data.local.loadSlateWidgetConfig

// =========================================================================
// CATALOG PROVIDER
// =========================================================================

fun getBluetoothWidgetsCatalog(): List<SlateWidgetInfo> {
    return listOf(
        SlateWidgetInfo("Bluetooth Earbuds", "2x2", "Bluetooth", EarbudsSquareReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Bluetooth Circular Dial", "2x2", "Bluetooth", EarbudsCircularReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Bluetooth Ring Widget", "2x2", "Bluetooth", EarbudsRingReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Bluetooth Volume Control", "2x2", "Bluetooth", EarbudsVolumeReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Bluetooth Tri-Battery Dock", "2x2", "Bluetooth", EarbudsTriDockReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Bluetooth Tri-Battery Circle", "2x2", "Bluetooth", EarbudsTriCircleReceiver::class.java, hasModeOption = true)
    )
}

// =========================================================================
// HELPER FOR RESPONSIVE MODE
// =========================================================================

fun parseAndLockIsResponsive(context: Context, widgetId: Int): Boolean {
    val widgetPrefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
    val modeKey = "widget_${widgetId}_mode"
    val isResponsiveKey = "widget_${widgetId}_is_responsive"

    if (widgetPrefs.contains(modeKey)) {
        return widgetPrefs.getString(modeKey, "RESPONSIVE") == "RESPONSIVE"
    }
    if (widgetPrefs.contains(isResponsiveKey)) {
        return widgetPrefs.getBoolean(isResponsiveKey, true)
    }

    val btPrefs = context.getSharedPreferences("slate_bluetooth_prefs", Context.MODE_PRIVATE)
    val btKey = "bluetooth_${widgetId}_is_responsive"
    if (btPrefs.contains(btKey)) {
        return btPrefs.getBoolean(btKey, true)
    }

    val launcherPrefs = context.getSharedPreferences("slate_app_launcher_prefs", Context.MODE_PRIVATE)
    val defaultResponsive = launcherPrefs.getBoolean("default_is_responsive", true)
    widgetPrefs.edit().putBoolean(isResponsiveKey, defaultResponsive).apply()
    return defaultResponsive
}

// =========================================================================
// BASE RECEIVER
// =========================================================================

abstract class BaseBluetoothReceiver(
    open val targetAspect: Float = 1.0f
) : AppWidgetProvider() {

    abstract fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap

    fun renderBitmapForWidget(
        context: Context,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int,
        widgetId: Int
    ): Bitmap = renderWidgetBitmap(context, widgetId, config, isResponsive, wDp, hDp)

    open fun getClickPendingIntent(context: Context, appWidgetId: Int): PendingIntent? {
        val hasPerm = BluetoothDataReader.hasBluetoothPermission(context)
        val intent = if (!hasPerm) {
            Intent(context, BluetoothPermissionActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }

        return PendingIntent.getActivity(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateSingleWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateSingleWidget(context, appWidgetManager, appWidgetId)
    }

    fun updateSingleWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        try {
            val config = loadSlateWidgetConfig(context, appWidgetId)
            val isResponsive = if (appWidgetId == -1) true else parseAndLockIsResponsive(context, appWidgetId)

            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val fallbackSize = 150 to 150

            val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, fallbackSize.first) ?: fallbackSize.first
            else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, fallbackSize.first) ?: fallbackSize.first
            val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, fallbackSize.second) ?: fallbackSize.second
            else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, fallbackSize.second) ?: fallbackSize.second

            val wDp = if (wDpRaw <= 0) fallbackSize.first else wDpRaw
            val hDp = if (hDpRaw <= 0) fallbackSize.second else hDpRaw
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

            val bitmap = renderWidgetBitmap(context, appWidgetId, config, isResponsive, effWDp, effHDp)
            val views = RemoteViews(context.packageName, R.layout.widget_base_single)
            views.setViewPadding(R.id.layout_grid_root, padH, padV, padH, padV)
            views.setImageViewBitmap(R.id.widget_image_view, bitmap)

            val pi = getClickPendingIntent(context, appWidgetId)
            if (pi != null) {
                views.setOnClickPendingIntent(R.id.touch_slot_0, pi)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// =========================================================================
// WIDGET RECEIVER IMPLEMENTATIONS
// =========================================================================

/**
 * 1. Bluetooth Earbuds Card Receiver (2x2 Square / Adaptive)
 */
class EarbudsSquareReceiver : BaseBluetoothReceiver(targetAspect = 1.0f) {
    override fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        val deviceData = if (appWidgetId == -1) {
            BluetoothDataReader.getPreviewDeviceStatus()
        } else {
            BluetoothDataReader.readCurrentDeviceStatus(context)
        }
        return generateEarbudsSquareBitmap(context, deviceData, config, isResponsive, wDp, hDp)
    }
}

/**
 * 2. Bluetooth Circular Dial Receiver (2x2 Circle / Dial)
 */
class EarbudsCircularReceiver : BaseBluetoothReceiver(targetAspect = 1.0f) {
    override fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        val deviceData = if (appWidgetId == -1) {
            BluetoothDataReader.getPreviewDeviceStatus()
        } else {
            BluetoothDataReader.readCurrentDeviceStatus(context)
        }
        return generateBluetoothCircularDialBitmap(context, deviceData, config, isResponsive, wDp, hDp)
    }
}

/**
 * 3. Bluetooth Ring Widget Receiver (2x2 Thick Ring)
 */
class EarbudsRingReceiver : BaseBluetoothReceiver(targetAspect = 1.0f) {
    override fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        val deviceData = if (appWidgetId == -1) {
            BluetoothDataReader.getPreviewDeviceStatus()
        } else {
            BluetoothDataReader.readCurrentDeviceStatus(context)
        }
        return generateBluetoothRingBitmap(context, deviceData, config, isResponsive, wDp, hDp)
    }
}

/**
 * 4. Bluetooth Earbuds Volume Control Receiver (2x2 with Interactive Volume Buttons)
 */
class EarbudsVolumeReceiver : AppWidgetProvider() {

    val targetAspect: Float = 1.0f

    companion object {
        const val ACTION_VOLUME_UP = "com.altusix.slate.ACTION_BT_VOLUME_UP"
        const val ACTION_VOLUME_DOWN = "com.altusix.slate.ACTION_BT_VOLUME_DOWN"
    }

    fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        val deviceData = if (appWidgetId == -1) {
            BluetoothDataReader.getPreviewDeviceStatus()
        } else {
            BluetoothDataReader.readCurrentDeviceStatus(context)
        }
        return generateEarbudsVolumeControlBitmap(context, deviceData, config, isResponsive, wDp, hDp)
    }

    fun renderBitmapForWidget(
        context: Context,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int,
        widgetId: Int
    ): Bitmap = renderWidgetBitmap(context, widgetId, config, isResponsive, wDp, hDp)

    override fun onReceive(context: Context, intent: Intent) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        when (intent.action) {
            ACTION_VOLUME_UP -> {
                audioManager?.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_SHOW_UI
                )
                updateAllBluetoothWidgets(context)
                return
            }
            ACTION_VOLUME_DOWN -> {
                audioManager?.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_SHOW_UI
                )
                updateAllBluetoothWidgets(context)
                return
            }
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateSingleVolumeWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateSingleVolumeWidget(context, appWidgetManager, appWidgetId)
    }

    fun updateSingleVolumeWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        try {
            val config = loadSlateWidgetConfig(context, appWidgetId)
            val isResponsive = if (appWidgetId == -1) true else parseAndLockIsResponsive(context, appWidgetId)

            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val fallbackSize = 150 to 150

            val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, fallbackSize.first) ?: fallbackSize.first
            else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, fallbackSize.first) ?: fallbackSize.first
            val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, fallbackSize.second) ?: fallbackSize.second
            else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, fallbackSize.second) ?: fallbackSize.second

            val wDp = if (wDpRaw <= 0) fallbackSize.first else wDpRaw
            val hDp = if (hDpRaw <= 0) fallbackSize.second else hDpRaw
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

            val bitmap = renderWidgetBitmap(context, appWidgetId, config, isResponsive, effWDp, effHDp)

            val views = RemoteViews(context.packageName, R.layout.widget_split_vertical_control_layout)
            views.setViewPadding(R.id.layout_grid_root, padH, padV, padH, padV)
            views.setImageViewBitmap(R.id.widget_canvas_surface, bitmap)

            // 1. Volume Up Pending Intent (+)
            val upIntent = Intent(context, EarbudsVolumeReceiver::class.java).apply {
                action = ACTION_VOLUME_UP
            }
            val upPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId * 100 + 1,
                upIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_side_top, upPendingIntent)

            // 2. Volume Down Pending Intent (-)
            val downIntent = Intent(context, EarbudsVolumeReceiver::class.java).apply {
                action = ACTION_VOLUME_DOWN
            }
            val downPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId * 100 + 2,
                downIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_side_bottom, downPendingIntent)

            // 3. Bluetooth Settings / Permission Pending Intent
            val hasPerm = BluetoothDataReader.hasBluetoothPermission(context)
            val settingsIntent = if (!hasPerm) {
                Intent(context, BluetoothPermissionActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            } else {
                Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
            val settingsPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId * 100 + 3,
                settingsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_main_panel, settingsPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * 5. Bluetooth Tri-Battery Dock Receiver (Structured Pod Layout)
 */
class EarbudsTriDockReceiver : BaseBluetoothReceiver(targetAspect = 1.0f) {
    override fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        val deviceData = if (appWidgetId == -1) {
            BluetoothDataReader.getPreviewDeviceStatus()
        } else {
            BluetoothDataReader.readCurrentDeviceStatus(context)
        }
        return generateBluetoothTriBatteryDockBitmap(context, deviceData, config, isResponsive, wDp, hDp)
    }
}

/**
 * 6. Bluetooth Tri-Battery Circle Receiver (Curved Arc Stage Layout)
 */
class EarbudsTriCircleReceiver : BaseBluetoothReceiver(targetAspect = 1.0f) {
    override fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        val deviceData = if (appWidgetId == -1) {
            BluetoothDataReader.getPreviewDeviceStatus()
        } else {
            BluetoothDataReader.readCurrentDeviceStatus(context)
        }
        return generateBluetoothTriBatteryCircleBitmap(context, deviceData, config, isResponsive, wDp, hDp)
    }
}

// =========================================================================
// GLOBAL UPDATE BROADCAST
// =========================================================================

fun updateAllBluetoothWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context) ?: return
    val standardReceivers = listOf(
        EarbudsSquareReceiver::class.java,
        EarbudsCircularReceiver::class.java,
        EarbudsRingReceiver::class.java,
        EarbudsTriDockReceiver::class.java,
        EarbudsTriCircleReceiver::class.java
    )
    for (receiverClass in standardReceivers) {
        val ids = manager.getAppWidgetIds(ComponentName(context, receiverClass))
        if (ids.isNotEmpty()) {
            val receiver = receiverClass.getDeclaredConstructor().newInstance()
            for (id in ids) {
                receiver.updateSingleWidget(context, manager, id)
            }
        }
    }
    val volIds = manager.getAppWidgetIds(ComponentName(context, EarbudsVolumeReceiver::class.java))
    if (volIds.isNotEmpty()) {
        val volReceiver = EarbudsVolumeReceiver()
        for (id in volIds) {
            volReceiver.updateSingleVolumeWidget(context, manager, id)
        }
    }
}