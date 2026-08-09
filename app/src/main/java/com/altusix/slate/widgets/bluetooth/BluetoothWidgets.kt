package com.altusix.slate.widgets.bluetooth

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioManager
import android.widget.RemoteViews
import com.altusix.slate.R
import com.altusix.slate.core.model.SlateWidgetInfo
import com.altusix.slate.core.receiver.BaseCanvasWidgetProvider
import com.altusix.slate.data.local.SlateWidgetConfig

// =========================================================================
// CATALOG PROVIDER
// =========================================================================

fun getBluetoothWidgetsCatalog(): List<SlateWidgetInfo> {
    return listOf(
        SlateWidgetInfo("Bluetooth Earbuds", "2x2", "Bluetooth", EarbudsSquareReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Bluetooth Circular Dial", "2x2", "Bluetooth", EarbudsCircularReceiver::class.java),
        SlateWidgetInfo("Bluetooth Ring Widget", "2x2", "Bluetooth", EarbudsRingReceiver::class.java),
        SlateWidgetInfo("Bluetooth Volume Control", "2x2", "Bluetooth", EarbudsVolumeReceiver::class.java, hasModeOption = true)
    )
}

// =========================================================================
// BASE RECEIVER
// =========================================================================

abstract class BaseBluetoothReceiver : BaseCanvasWidgetProvider() {
    override fun getClickPendingIntent(context: Context, appWidgetId: Int): PendingIntent? {
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
}

// =========================================================================
// WIDGET RECEIVER IMPLEMENTATIONS
// =========================================================================

/**
 * 1. Bluetooth Earbuds Card Receiver (2x2 Square)
 */
class EarbudsSquareReceiver : BaseBluetoothReceiver() {
    override fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        val prefs = context.getSharedPreferences("slate_bluetooth_prefs", Context.MODE_PRIVATE)
        val prefix = "bluetooth_${appWidgetId}_"
        val defaultResponsive = context.getSharedPreferences("slate_app_launcher_prefs", Context.MODE_PRIVATE)
            .getBoolean("default_is_responsive", true)

        val isResponsive = if (prefs.contains("${prefix}is_responsive")) {
            prefs.getBoolean("${prefix}is_responsive", defaultResponsive)
        } else {
            prefs.edit().putBoolean("${prefix}is_responsive", defaultResponsive).apply()
            defaultResponsive
        }

        val deviceData = BluetoothDataReader.readCurrentDeviceStatus(context)
        return generateEarbudsSquareBitmap(context, deviceData, config, isResponsive, wDp, hDp)
    }
}

/**
 * 2. Bluetooth Circular Dial Receiver (2x2 Circle / Dial)
 */
class EarbudsCircularReceiver : BaseBluetoothReceiver() {
    override fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        val prefs = context.getSharedPreferences("slate_bluetooth_prefs", Context.MODE_PRIVATE)
        val prefix = "bluetooth_${appWidgetId}_"
        val defaultResponsive = context.getSharedPreferences("slate_app_launcher_prefs", Context.MODE_PRIVATE)
            .getBoolean("default_is_responsive", true)

        val isResponsive = if (prefs.contains("${prefix}is_responsive")) {
            prefs.getBoolean("${prefix}is_responsive", defaultResponsive)
        } else {
            prefs.edit().putBoolean("${prefix}is_responsive", defaultResponsive).apply()
            defaultResponsive
        }

        val deviceData = BluetoothDataReader.readCurrentDeviceStatus(context)
        return generateBluetoothCircularDialBitmap(context, deviceData, config, isResponsive, wDp, hDp)
    }
}

/**
 * 3. Bluetooth Ring Widget Receiver (2x2 Thick Ring)
 */
class EarbudsRingReceiver : BaseBluetoothReceiver() {
    override fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        val prefs = context.getSharedPreferences("slate_bluetooth_prefs", Context.MODE_PRIVATE)
        val prefix = "bluetooth_${appWidgetId}_"
        val defaultResponsive = context.getSharedPreferences("slate_app_launcher_prefs", Context.MODE_PRIVATE)
            .getBoolean("default_is_responsive", true)

        val isResponsive = if (prefs.contains("${prefix}is_responsive")) {
            prefs.getBoolean("${prefix}is_responsive", defaultResponsive)
        } else {
            prefs.edit().putBoolean("${prefix}is_responsive", defaultResponsive).apply()
            defaultResponsive
        }

        val deviceData = BluetoothDataReader.readCurrentDeviceStatus(context)
        return generateBluetoothRingBitmap(context, deviceData, config, isResponsive, wDp, hDp)
    }
}

/**
 * 4. Bluetooth Earbuds Volume Control Receiver (2x2 with Interactive Volume Buttons)
 */
class EarbudsVolumeReceiver : AppWidgetProvider() {

    companion object {
        const val ACTION_VOLUME_UP = "com.altusix.slate.ACTION_BT_VOLUME_UP"
        const val ACTION_VOLUME_DOWN = "com.altusix.slate.ACTION_BT_VOLUME_DOWN"
    }

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
        newOptions: android.os.Bundle?
    ) {
        updateSingleVolumeWidget(context, appWidgetManager, appWidgetId)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    private fun updateSingleVolumeWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        try {
            val prefs = context.getSharedPreferences("slate_bluetooth_prefs", Context.MODE_PRIVATE)
            val widgetPrefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)

            val themeMode = widgetPrefs.getString("widget_${appWidgetId}_theme_mode", "DARK") ?: "DARK"
            val bgColor = widgetPrefs.getLong("widget_${appWidgetId}_bg_color", if (themeMode == "LIGHT") 0xFFFFFFFFL else 0xFF161618L)
            val opacity = widgetPrefs.getFloat("widget_${appWidgetId}_opacity", 1.0f)
            val accentColor = widgetPrefs.getLong("widget_${appWidgetId}_accent_color", if (themeMode == "LIGHT") 0xFF000000L else 0xFFFFFFFFL)

            val config = SlateWidgetConfig(
                themeMode = themeMode,
                backgroundColorHex = bgColor,
                opacity = opacity,
                accentColorHex = accentColor
            )

            val prefix = "bluetooth_${appWidgetId}_"
            val defaultResponsive = context.getSharedPreferences("slate_app_launcher_prefs", Context.MODE_PRIVATE)
                .getBoolean("default_is_responsive", true)

            val isResponsive = if (prefs.contains("${prefix}is_responsive")) {
                prefs.getBoolean("${prefix}is_responsive", defaultResponsive)
            } else {
                prefs.edit().putBoolean("${prefix}is_responsive", defaultResponsive).apply()
                defaultResponsive
            }

            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val wDp = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) ?: 160
            val hDp = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) ?: 160

            val deviceData = BluetoothDataReader.readCurrentDeviceStatus(context)
            val bitmap = generateEarbudsVolumeControlBitmap(context, deviceData, config, isResponsive, wDp, hDp)

            val views = RemoteViews(context.packageName, R.layout.widget_split_vertical_control_layout)
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

// =========================================================================
// GLOBAL UPDATE BROADCAST
// =========================================================================

fun updateAllBluetoothWidgets(context: Context) {
    val receivers = listOf(
        EarbudsSquareReceiver::class.java,
        EarbudsCircularReceiver::class.java,
        EarbudsRingReceiver::class.java,
        EarbudsVolumeReceiver::class.java
    )

    val manager = AppWidgetManager.getInstance(context)
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