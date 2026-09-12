package com.altusix.slate.widgets.quicktoggles

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

// =========================================================================
// CATALOG & BROADCAST UPDATES
// =========================================================================

fun getQuickTogglesWidgetsCatalog(): List<SlateWidgetInfo> {
    return listOf(
        SlateWidgetInfo("Control Center Deck", "4x2", "Quick Toggles", QuickTogglesControlCenterReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Minimalist Action Toolbar", "4x1", "Quick Toggles", QuickTogglesToolbarReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Connectivity Duo Bento", "2x2", "Quick Toggles", QuickTogglesConnectivityReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Quad Action Matrix", "2x2", "Quick Toggles", QuickTogglesQuadReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Tactile Alert Slider", "2x1", "Quick Toggles", QuickTogglesAlertSliderHReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Vertical Alert Slider", "1x2", "Quick Toggles", QuickTogglesAlertSliderVReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Flashlight Torch Switch", "2x2", "Quick Toggles", QuickTogglesTorchSwitchReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Wi-Fi Network Pill", "2x1", "Quick Toggles", QuickTogglesWifiPillReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Bluetooth Device Pill", "2x1", "Quick Toggles", QuickTogglesBluetoothPillReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Sound Mode Pill", "2x1", "Quick Toggles", QuickTogglesSoundPillReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Auto-Rotate Pill", "2x1", "Quick Toggles", QuickTogglesRotatePillReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("System Utility Deck", "4x2", "Quick Toggles", QuickTogglesUtilityDeckReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Micro Toggle: Flashlight", "1x1", "Quick Toggles", QuickTogglesMicroTorchReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Micro Toggle: Sound Mode", "1x1", "Quick Toggles", QuickTogglesMicroSoundReceiver::class.java, hasModeOption = false)
    )
}

fun updateAllQuickTogglesWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context) ?: return
    val receivers = listOf(
        QuickTogglesControlCenterReceiver::class.java,
        QuickTogglesToolbarReceiver::class.java,
        QuickTogglesConnectivityReceiver::class.java,
        QuickTogglesQuadReceiver::class.java,
        QuickTogglesAlertSliderHReceiver::class.java,
        QuickTogglesAlertSliderVReceiver::class.java,
        QuickTogglesTorchSwitchReceiver::class.java,
        QuickTogglesWifiPillReceiver::class.java,
        QuickTogglesBluetoothPillReceiver::class.java,
        QuickTogglesSoundPillReceiver::class.java,
        QuickTogglesRotatePillReceiver::class.java,
        QuickTogglesUtilityDeckReceiver::class.java,
        QuickTogglesMicroTorchReceiver::class.java,
        QuickTogglesMicroSoundReceiver::class.java
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

// -------------------------------------------------------------------------
// PREFERENCE & RESPONSIVE HELPERS
// -------------------------------------------------------------------------

private fun loadSlateWidgetConfig(context: Context, widgetId: Int): SlateWidgetConfig {
    val widgetPrefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
    val bgKey = "widget_${widgetId}_bg_color"

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

// =========================================================================
// BASE QUICK TOGGLES RECEIVER
// =========================================================================

abstract class BaseQuickTogglesReceiver(
    private val layoutResId: Int,
    protected open val targetAspect: Float = 1.0f
) : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE_TORCH = "com.altusix.slate.toggles.ACTION_TOGGLE_TORCH"
        const val ACTION_CYCLE_SOUND = "com.altusix.slate.toggles.ACTION_CYCLE_SOUND"
        const val ACTION_SET_SLIDER_MODE = "com.altusix.slate.toggles.ACTION_SET_SLIDER_MODE"
        const val ACTION_TOGGLE_ROTATE = "com.altusix.slate.toggles.ACTION_TOGGLE_ROTATE"
        const val ACTION_CYCLE_TIMEOUT = "com.altusix.slate.toggles.ACTION_CYCLE_TIMEOUT"
        const val EXTRA_SLIDER_MODE = "extra_slider_mode"
    }

    abstract fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TOGGLE_TORCH -> {
                QuickTogglesStateManager.toggleTorch(context)
                updateAllQuickTogglesWidgets(context)
                return
            }
            ACTION_CYCLE_SOUND -> {
                QuickTogglesStateManager.cycleSoundMode(context)
                updateAllQuickTogglesWidgets(context)
                return
            }
            ACTION_SET_SLIDER_MODE -> {
                val modeStr = intent.getStringExtra(EXTRA_SLIDER_MODE)
                val targetMode = AlertSliderMode.values().firstOrNull { it.name == modeStr } ?: AlertSliderMode.RING
                QuickTogglesStateManager.setAlertSliderMode(context, targetMode)
                updateAllQuickTogglesWidgets(context)
                return
            }
            ACTION_TOGGLE_ROTATE -> {
                val success = QuickTogglesStateManager.toggleAutoRotate(context)
                if (!success) {
                    try {
                        context.startActivity(QuickTogglesStateManager.createWriteSettingsIntent())
                    } catch (_: Exception) {
                        context.startActivity(QuickTogglesStateManager.createDisplaySettingsIntent())
                    }
                }
                updateAllQuickTogglesWidgets(context)
                return
            }
            ACTION_CYCLE_TIMEOUT -> {
                QuickTogglesStateManager.cycleScreenTimeout(context)
                updateAllQuickTogglesWidgets(context)
                return
            }
            // Real-time system state updates
            "android.net.wifi.WIFI_STATE_CHANGED",
            "android.net.conn.CONNECTIVITY_CHANGE",
            "android.bluetooth.adapter.action.STATE_CHANGED",
            "android.media.RINGER_MODE_CHANGED",
            Intent.ACTION_AIRPLANE_MODE_CHANGED,
            "android.os.action.POWER_SAVE_MODE_CHANGED" -> {
                updateAllQuickTogglesWidgets(context)
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

    fun updateSingleWidget(context: Context, manager: AppWidgetManager, id: Int) {
        try {
            val config = loadSlateWidgetConfig(context, id)
            val options = manager.getAppWidgetOptions(id)
            val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 160) ?: 160 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 160) ?: 160
            val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 160) ?: 160 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 160) ?: 160
            val wDp = if (wDpRaw <= 0) 160 else wDpRaw
            val hDp = if (hDpRaw <= 0) 160 else hDpRaw

            val isResponsive = if (id == -1) false else parseAndLockIsResponsive(context, id)
            val bitmap = renderWidgetBitmap(context, id, config, isResponsive, wDp, hDp)
            val views = RemoteViews(context.packageName, layoutResId)

            // Dynamically select target ImageView ID based on layout
            val imageViewId = if (layoutResId == R.layout.widget_base_grid_4x2) R.id.widget_image_view else R.id.widget_canvas_surface
            views.setImageViewBitmap(imageViewId, bitmap)

            val rootLayoutId = if (layoutResId == R.layout.widget_base_grid_4x2) R.id.layout_grid_root else R.id.layout_toggles_root

            // Letterbox margins for Fixed Aspect mode
            if (!isResponsive) {
                val density = context.resources.displayMetrics.density
                val currentAspect = wDp.toFloat() / hDp.toFloat().coerceAtLeast(1f)
                val padH: Int
                val padV: Int

                if (currentAspect > targetAspect) {
                    val contentW = hDp * targetAspect
                    padH = (((wDp - contentW) / 2f) * density).toInt()
                    padV = 0
                } else {
                    val contentH = wDp / targetAspect
                    padH = 0
                    padV = (((hDp - contentH) / 2f) * density).toInt()
                }
                views.setViewPadding(rootLayoutId, padH, padV, padH, padV)
            } else {
                views.setViewPadding(rootLayoutId, 0, 0, 0, 0)
            }

            setupTouchTargets(context, views, id)
            manager.updateAppWidget(id, views)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    protected open fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {}

    protected fun createBroadcastPendingIntent(context: Context, action: String, appWidgetId: Int, requestCode: Int, extraKey: String? = null, extraVal: String? = null): PendingIntent {
        val intent = Intent(context, this.javaClass).apply {
            this.action = action
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            if (extraKey != null && extraVal != null) putExtra(extraKey, extraVal)
            data = Uri.parse("slate_toggles://$appWidgetId/$action/${extraVal ?: ""}")
        }
        return PendingIntent.getBroadcast(
            context,
            appWidgetId * 100 + requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    protected fun createActivityPendingIntent(context: Context, intent: Intent, appWidgetId: Int, requestCode: Int): PendingIntent {
        return PendingIntent.getActivity(
            context,
            appWidgetId * 100 + requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

// =========================================================================
// 1. CONTROL CENTER DECK (4x2)
// =========================================================================

class QuickTogglesControlCenterReceiver : BaseQuickTogglesReceiver(R.layout.widget_base_grid_4x2, targetAspect = 2.0f) {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val state = QuickTogglesStateManager.readCurrentState(context)
        return generateControlCenterDeckBitmap(context, state, config, isResponsive, wDp, hDp)
    }

    override fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        // Row 1: Slots 0 to 3
        views.setOnClickPendingIntent(R.id.touch_slot_0, createActivityPendingIntent(context, QuickTogglesStateManager.createWifiIntent(), appWidgetId, 0))
        views.setOnClickPendingIntent(R.id.touch_slot_1, createActivityPendingIntent(context, QuickTogglesStateManager.createBluetoothIntent(), appWidgetId, 1))
        views.setOnClickPendingIntent(R.id.touch_slot_2, createBroadcastPendingIntent(context, ACTION_TOGGLE_TORCH, appWidgetId, 2))
        views.setOnClickPendingIntent(R.id.touch_slot_3, createBroadcastPendingIntent(context, ACTION_CYCLE_SOUND, appWidgetId, 3))

        // Row 2: Slots 4 to 7
        views.setOnClickPendingIntent(R.id.touch_slot_4, createBroadcastPendingIntent(context, ACTION_TOGGLE_ROTATE, appWidgetId, 4))
        views.setOnClickPendingIntent(R.id.touch_slot_5, createActivityPendingIntent(context, QuickTogglesStateManager.createHotspotIntent(), appWidgetId, 5))
        views.setOnClickPendingIntent(R.id.touch_slot_6, createActivityPendingIntent(context, QuickTogglesStateManager.createLocationIntent(), appWidgetId, 6))
        views.setOnClickPendingIntent(R.id.touch_slot_7, createActivityPendingIntent(context, QuickTogglesStateManager.createDisplaySettingsIntent(), appWidgetId, 7))
    }
}

// =========================================================================
// 2. MINIMALIST ACTION TOOLBAR (4x1)
// =========================================================================

class QuickTogglesToolbarReceiver : BaseQuickTogglesReceiver(R.layout.widget_toggles_toolbar_4x1_layout, targetAspect = 4.0f) {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val state = QuickTogglesStateManager.readCurrentState(context)
        return generateMinimalistToolbarBitmap(context, state, config, isResponsive, wDp, hDp)
    }

    override fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        views.setOnClickPendingIntent(R.id.btn_toggle_0, createActivityPendingIntent(context, QuickTogglesStateManager.createWifiIntent(), appWidgetId, 0))
        views.setOnClickPendingIntent(R.id.btn_toggle_1, createActivityPendingIntent(context, QuickTogglesStateManager.createBluetoothIntent(), appWidgetId, 1))
        views.setOnClickPendingIntent(R.id.btn_toggle_2, createBroadcastPendingIntent(context, ACTION_TOGGLE_TORCH, appWidgetId, 2))
        views.setOnClickPendingIntent(R.id.btn_toggle_3, createBroadcastPendingIntent(context, ACTION_CYCLE_SOUND, appWidgetId, 3))
        views.setOnClickPendingIntent(R.id.btn_toggle_4, createBroadcastPendingIntent(context, ACTION_TOGGLE_ROTATE, appWidgetId, 4))
    }
}

// =========================================================================
// 3. CONNECTIVITY DUO BENTO (2x2)
// =========================================================================

class QuickTogglesConnectivityReceiver : BaseQuickTogglesReceiver(R.layout.widget_toggles_connectivity_2x2_layout, targetAspect = 1.0f) {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val state = QuickTogglesStateManager.readCurrentState(context)
        return generateConnectivityBentoBitmap(context, state, config, isResponsive, wDp, hDp)
    }

    override fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        views.setOnClickPendingIntent(R.id.btn_toggle_wifi, createActivityPendingIntent(context, QuickTogglesStateManager.createWifiIntent(), appWidgetId, 10))
        views.setOnClickPendingIntent(R.id.btn_toggle_bt, createActivityPendingIntent(context, QuickTogglesStateManager.createBluetoothIntent(), appWidgetId, 11))
    }
}

// =========================================================================
// 4. QUAD ACTION MATRIX (2x2)
// =========================================================================

class QuickTogglesQuadReceiver : BaseQuickTogglesReceiver(R.layout.widget_toggles_quad_2x2_layout, targetAspect = 1.0f) {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val state = QuickTogglesStateManager.readCurrentState(context)
        return generateQuadActionMatrixBitmap(context, state, config, isResponsive, wDp, hDp)
    }

    override fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        views.setOnClickPendingIntent(R.id.btn_toggle_tl, createActivityPendingIntent(context, QuickTogglesStateManager.createWifiIntent(), appWidgetId, 20))
        views.setOnClickPendingIntent(R.id.btn_toggle_tr, createActivityPendingIntent(context, QuickTogglesStateManager.createBluetoothIntent(), appWidgetId, 21))
        views.setOnClickPendingIntent(R.id.btn_toggle_bl, createBroadcastPendingIntent(context, ACTION_TOGGLE_TORCH, appWidgetId, 22))
        views.setOnClickPendingIntent(R.id.btn_toggle_br, createBroadcastPendingIntent(context, ACTION_CYCLE_SOUND, appWidgetId, 23))
    }
}

// =========================================================================
// 5. TACTILE ALERT SLIDER (HORIZONTAL 2x1)
// =========================================================================

class QuickTogglesAlertSliderHReceiver : BaseQuickTogglesReceiver(R.layout.widget_toggles_slider_2x1_layout, targetAspect = 2.0f) {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val state = QuickTogglesStateManager.readCurrentState(context)
        return generateAlertSliderHorizontalBitmap(context, state.alertSlider, config, isResponsive, wDp, hDp)
    }

    override fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        views.setOnClickPendingIntent(R.id.btn_slider_silent, createBroadcastPendingIntent(context, ACTION_SET_SLIDER_MODE, appWidgetId, 30, EXTRA_SLIDER_MODE, AlertSliderMode.SILENT.name))
        views.setOnClickPendingIntent(R.id.btn_slider_vibrate, createBroadcastPendingIntent(context, ACTION_SET_SLIDER_MODE, appWidgetId, 31, EXTRA_SLIDER_MODE, AlertSliderMode.VIBRATE.name))
        views.setOnClickPendingIntent(R.id.btn_slider_ring, createBroadcastPendingIntent(context, ACTION_SET_SLIDER_MODE, appWidgetId, 32, EXTRA_SLIDER_MODE, AlertSliderMode.RING.name))
    }
}

// =========================================================================
// 6. VERTICAL ALERT SLIDER (1x2)
// =========================================================================

class QuickTogglesAlertSliderVReceiver : BaseQuickTogglesReceiver(R.layout.widget_toggles_slider_1x2_layout, targetAspect = 0.5f) {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val state = QuickTogglesStateManager.readCurrentState(context)
        return generateAlertSliderVerticalBitmap(context, state.alertSlider, config, isResponsive, wDp, hDp)
    }

    override fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        views.setOnClickPendingIntent(R.id.btn_slider_ring, createBroadcastPendingIntent(context, ACTION_SET_SLIDER_MODE, appWidgetId, 40, EXTRA_SLIDER_MODE, AlertSliderMode.RING.name))
        views.setOnClickPendingIntent(R.id.btn_slider_vibrate, createBroadcastPendingIntent(context, ACTION_SET_SLIDER_MODE, appWidgetId, 41, EXTRA_SLIDER_MODE, AlertSliderMode.VIBRATE.name))
        views.setOnClickPendingIntent(R.id.btn_slider_silent, createBroadcastPendingIntent(context, ACTION_SET_SLIDER_MODE, appWidgetId, 42, EXTRA_SLIDER_MODE, AlertSliderMode.SILENT.name))
    }
}

// =========================================================================
// 7. FLASHLIGHT TORCH SWITCH (2x2)
// =========================================================================

class QuickTogglesTorchSwitchReceiver : BaseQuickTogglesReceiver(R.layout.widget_toggles_torch_2x2_layout, targetAspect = 1.0f) {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val state = QuickTogglesStateManager.readCurrentState(context)
        return generateFlashlightTorchBitmap(context, state.torch.isEnabled, config, isResponsive, wDp, hDp)
    }

    override fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        views.setOnClickPendingIntent(R.id.btn_toggle_torch, createBroadcastPendingIntent(context, ACTION_TOGGLE_TORCH, appWidgetId, 50))
    }
}

// =========================================================================
// 8. WI-FI NETWORK PILL (2x1)
// =========================================================================

class QuickTogglesWifiPillReceiver : BaseQuickTogglesReceiver(R.layout.widget_toggles_pill_2x1_layout, targetAspect = 2.0f) {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val state = QuickTogglesStateManager.readCurrentState(context)
        return generateTogglePillBitmap(context, state.wifi, config, isResponsive, wDp, hDp)
    }

    override fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        views.setOnClickPendingIntent(R.id.btn_toggle_pill, createActivityPendingIntent(context, QuickTogglesStateManager.createWifiIntent(), appWidgetId, 60))
    }
}

// =========================================================================
// 9. BLUETOOTH DEVICE PILL (2x1)
// =========================================================================

class QuickTogglesBluetoothPillReceiver : BaseQuickTogglesReceiver(R.layout.widget_toggles_pill_2x1_layout, targetAspect = 2.0f) {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val state = QuickTogglesStateManager.readCurrentState(context)
        return generateTogglePillBitmap(context, state.bluetooth, config, isResponsive, wDp, hDp)
    }

    override fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        views.setOnClickPendingIntent(R.id.btn_toggle_pill, createActivityPendingIntent(context, QuickTogglesStateManager.createBluetoothIntent(), appWidgetId, 70))
    }
}

// =========================================================================
// 10. SOUND MODE PILL (2x1)
// =========================================================================

class QuickTogglesSoundPillReceiver : BaseQuickTogglesReceiver(R.layout.widget_toggles_pill_2x1_layout, targetAspect = 2.0f) {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val state = QuickTogglesStateManager.readCurrentState(context)
        return generateTogglePillBitmap(context, state.ringer, config, isResponsive, wDp, hDp)
    }

    override fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        views.setOnClickPendingIntent(R.id.btn_toggle_pill, createBroadcastPendingIntent(context, ACTION_CYCLE_SOUND, appWidgetId, 80))
    }
}

// =========================================================================
// 11. AUTO-ROTATE PILL (2x1)
// =========================================================================

class QuickTogglesRotatePillReceiver : BaseQuickTogglesReceiver(R.layout.widget_toggles_pill_2x1_layout, targetAspect = 2.0f) {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val state = QuickTogglesStateManager.readCurrentState(context)
        return generateTogglePillBitmap(context, state.autoRotate, config, isResponsive, wDp, hDp)
    }

    override fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        views.setOnClickPendingIntent(R.id.btn_toggle_pill, createBroadcastPendingIntent(context, ACTION_TOGGLE_ROTATE, appWidgetId, 90))
    }
}

// =========================================================================
// 12. SYSTEM UTILITY DECK (4x2)
// =========================================================================

class QuickTogglesUtilityDeckReceiver : BaseQuickTogglesReceiver(R.layout.widget_toggles_utility_4x2_layout, targetAspect = 2.0f) {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val state = QuickTogglesStateManager.readCurrentState(context)
        return generateSystemUtilityDeckBitmap(context, state, config, isResponsive, wDp, hDp)
    }

    override fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        // 0: Timeout (Direct cycle)
        views.setOnClickPendingIntent(R.id.btn_util_0, createBroadcastPendingIntent(context, ACTION_CYCLE_TIMEOUT, appWidgetId, 100))
        // 1: Auto-Rotate (Direct toggle)
        views.setOnClickPendingIntent(R.id.btn_util_1, createBroadcastPendingIntent(context, ACTION_TOGGLE_ROTATE, appWidgetId, 101))
        // 2: Battery Saver
        views.setOnClickPendingIntent(R.id.btn_util_2, createActivityPendingIntent(context, QuickTogglesStateManager.createBatterySaverIntent(), appWidgetId, 102))
        // 3: Dark Mode
        views.setOnClickPendingIntent(R.id.btn_util_3, createActivityPendingIntent(context, QuickTogglesStateManager.createDisplaySettingsIntent(), appWidgetId, 103))
        // 4: Airplane Mode
        views.setOnClickPendingIntent(R.id.btn_util_4, createActivityPendingIntent(context, QuickTogglesStateManager.createAirplaneIntent(), appWidgetId, 104))
        // 5: Hotspot
        views.setOnClickPendingIntent(R.id.btn_util_5, createActivityPendingIntent(context, QuickTogglesStateManager.createHotspotIntent(), appWidgetId, 105))
    }
}

// =========================================================================
// 13. MICRO TOGGLE: FLASHLIGHT (1x1)
// =========================================================================

class QuickTogglesMicroTorchReceiver : BaseQuickTogglesReceiver(R.layout.widget_toggles_micro_1x1_layout, targetAspect = 1.0f) {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val state = QuickTogglesStateManager.readCurrentState(context)
        return generateMicroToggleBitmap(context, state.torch, null, config, isResponsive, wDp, hDp)
    }

    override fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        views.setOnClickPendingIntent(R.id.btn_toggle_micro, createBroadcastPendingIntent(context, ACTION_TOGGLE_TORCH, appWidgetId, 110))
    }
}

// =========================================================================
// 14. MICRO TOGGLE: SOUND MODE (1x1)
// =========================================================================

class QuickTogglesMicroSoundReceiver : BaseQuickTogglesReceiver(R.layout.widget_toggles_micro_1x1_layout, targetAspect = 1.0f) {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val state = QuickTogglesStateManager.readCurrentState(context)
        return generateMicroToggleBitmap(context, state.ringer, state.alertSlider, config, isResponsive, wDp, hDp)
    }

    override fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        views.setOnClickPendingIntent(R.id.btn_toggle_micro, createBroadcastPendingIntent(context, ACTION_CYCLE_SOUND, appWidgetId, 120))
    }
}
