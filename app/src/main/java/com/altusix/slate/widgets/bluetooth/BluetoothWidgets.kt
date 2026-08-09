package com.altusix.slate.widgets.bluetooth

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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
        SlateWidgetInfo("Bluetooth Ring Widget", "2x2", "Bluetooth", EarbudsRingReceiver::class.java)
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


// =========================================================================
// GLOBAL UPDATE BROADCAST
// =========================================================================
fun updateAllBluetoothWidgets(context: Context) {
    val receivers = listOf(
        EarbudsSquareReceiver::class.java,
        EarbudsCircularReceiver::class.java,
        EarbudsRingReceiver::class.java
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