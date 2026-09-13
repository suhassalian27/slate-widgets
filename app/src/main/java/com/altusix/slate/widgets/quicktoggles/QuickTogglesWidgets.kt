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
        SlateWidgetInfo("Flashlight Torch Switch", "2x2", "Quick Toggles", QuickTogglesTorchSwitchReceiver::class.java, hasModeOption = false),
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
        QuickTogglesControlCenterReceiver(),
        QuickTogglesToolbarReceiver(),
        QuickTogglesConnectivityReceiver(),
        QuickTogglesQuadReceiver(),
        QuickTogglesAlertSliderHReceiver(),
        QuickTogglesAlertSliderVReceiver(),
        QuickTogglesTorchSwitchReceiver(),
        QuickTogglesWifiPillReceiver(),
        QuickTogglesBluetoothPillReceiver(),
        QuickTogglesSoundPillReceiver(),
        QuickTogglesRotatePillReceiver(),
        QuickTogglesUtilityDeckReceiver(),
        QuickTogglesMicroTorchReceiver(),
        QuickTogglesMicroSoundReceiver()
    )
    for (receiver in receivers) {
        val ids = manager.getAppWidgetIds(ComponentName(context, receiver::class.java)) ?: intArrayOf()
        for (id in ids) {
            receiver.updateSingleWidget(context, manager, id)
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

    open fun getLayoutResId(wDp: Int, hDp: Int, isResponsive: Boolean): Int = layoutResId

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
                        context.startActivity(QuickTogglesStateManager.createWriteSettingsIntent(context))
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
            // Real-time system hardware broadcasts & screen wake
            "android.net.wifi.WIFI_AP_STATE_CHANGED" -> {
                val apState = intent.getIntExtra("wifi_state", 0)
                context.getSharedPreferences("slate_toggles_prefs", Context.MODE_PRIVATE)
                    .edit().putBoolean("hotspot_broadcast_state", apState == 13).apply()
                updateAllQuickTogglesWidgets(context)
                return
            }
            "android.net.wifi.WIFI_STATE_CHANGED",
            "android.net.wifi.STATE_CHANGE",
            "android.net.conn.CONNECTIVITY_CHANGE",
            "android.bluetooth.adapter.action.STATE_CHANGED",
            "android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED",
            "android.media.RINGER_MODE_CHANGED",
            "android.location.PROVIDERS_CHANGED",
            "android.location.MODE_CHANGED",
            Intent.ACTION_AIRPLANE_MODE_CHANGED,
            Intent.ACTION_USER_PRESENT,
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

            // Dynamic layout resolution based on orientation/aspect ratio
            val activeLayoutId = getLayoutResId(wDp, hDp, isResponsive)
            val views = RemoteViews(context.packageName, activeLayoutId)

            val sharedGridLayouts = setOf(
                R.layout.widget_base_grid_4x2,
                R.layout.widget_base_grid_2x4,
                R.layout.widget_base_row_5,
                R.layout.widget_base_column_2,
                R.layout.widget_base_grid_2x2,
                R.layout.widget_base_row_3,
                R.layout.widget_base_column_3,
                R.layout.widget_base_single
            )

            val isSharedGrid = activeLayoutId in sharedGridLayouts
            val imageViewId = if (isSharedGrid) R.id.widget_image_view else R.id.widget_canvas_surface
            views.setImageViewBitmap(imageViewId, bitmap)

            if (activeLayoutId != R.layout.widget_base_grid_2x2) {
                val rootLayoutId = if (isSharedGrid) R.id.layout_grid_root else R.id.layout_toggles_root
                views.setViewPadding(rootLayoutId, 0, 0, 0, 0)
            }

            val rootLayoutId = if (isSharedGrid) R.id.layout_grid_root else R.id.layout_toggles_root
            views.setViewPadding(rootLayoutId, 0, 0, 0, 0)

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
            appWidgetId * 1000 + requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    protected fun createTrampolineActivityPendingIntent(context: Context, targetIntent: Intent, appWidgetId: Int, requestCode: Int): PendingIntent {
        val trampolineIntent = Intent(context, QuickTogglesTrampolineActivity::class.java).apply {
            putExtra(QuickTogglesTrampolineActivity.EXTRA_TARGET_INTENT, targetIntent)
            data = Uri.parse("slate_toggles_trampoline://$appWidgetId/$requestCode")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            appWidgetId * 1000 + requestCode,
            trampolineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

// =========================================================================
// 1. CONTROL CENTER DECK (4x2 or 2x4 Dynamic Grid)
// =========================================================================
class QuickTogglesControlCenterReceiver : BaseQuickTogglesReceiver(R.layout.widget_base_grid_4x2, targetAspect = 2.0f) {

    override fun getLayoutResId(wDp: Int, hDp: Int, isResponsive: Boolean): Int {
        val isTallMode = isResponsive && (wDp.toFloat() / hDp.toFloat() < 1.15f)
        return if (isTallMode) R.layout.widget_base_grid_2x4 else R.layout.widget_base_grid_4x2
    }

    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val state = QuickTogglesStateManager.readCurrentState(context)
        return generateControlCenterDeckBitmap(context, state, config, isResponsive, wDp, hDp)
    }

    override fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        // Slot 0: Wi-Fi
        views.setOnClickPendingIntent(R.id.touch_slot_0, createTrampolineActivityPendingIntent(context, QuickTogglesStateManager.createWifiIntent(), appWidgetId, 0))
        // Slot 1: Bluetooth
        views.setOnClickPendingIntent(R.id.touch_slot_1, createTrampolineActivityPendingIntent(context, QuickTogglesStateManager.createBluetoothIntent(), appWidgetId, 1))
        // Slot 2: Torch
        views.setOnClickPendingIntent(R.id.touch_slot_2, createBroadcastPendingIntent(context, ACTION_TOGGLE_TORCH, appWidgetId, 2))
        // Slot 3: Sound
        views.setOnClickPendingIntent(R.id.touch_slot_3, createBroadcastPendingIntent(context, ACTION_CYCLE_SOUND, appWidgetId, 3))
        // Slot 4: Auto-Rotate
        views.setOnClickPendingIntent(R.id.touch_slot_4, createBroadcastPendingIntent(context, ACTION_TOGGLE_ROTATE, appWidgetId, 4))
        // Slot 5: Hotspot
        views.setOnClickPendingIntent(R.id.touch_slot_5, createTrampolineActivityPendingIntent(context, QuickTogglesStateManager.createHotspotIntent(context), appWidgetId, 5))
        // Slot 6: Location
        views.setOnClickPendingIntent(R.id.touch_slot_6, createTrampolineActivityPendingIntent(context, QuickTogglesStateManager.createLocationIntent(), appWidgetId, 6))
        // Slot 7: Dark Mode
        views.setOnClickPendingIntent(R.id.touch_slot_7, createTrampolineActivityPendingIntent(context, QuickTogglesStateManager.createDisplaySettingsIntent(), appWidgetId, 7))
    }
}

// =========================================================================
// 2. MINIMALIST ACTION TOOLBAR (4x1)
// =========================================================================
class QuickTogglesToolbarReceiver : BaseQuickTogglesReceiver(R.layout.widget_base_row_5, targetAspect = 4.0f) {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val state = QuickTogglesStateManager.readCurrentState(context)
        return generateMinimalistToolbarBitmap(context, state, config, isResponsive, wDp, hDp)
    }

    override fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        views.setOnClickPendingIntent(R.id.touch_slot_0, createTrampolineActivityPendingIntent(context, QuickTogglesStateManager.createWifiIntent(), appWidgetId, 0))
        views.setOnClickPendingIntent(R.id.touch_slot_1, createTrampolineActivityPendingIntent(context, QuickTogglesStateManager.createBluetoothIntent(), appWidgetId, 1))
        views.setOnClickPendingIntent(R.id.touch_slot_2, createBroadcastPendingIntent(context, ACTION_TOGGLE_TORCH, appWidgetId, 2))
        views.setOnClickPendingIntent(R.id.touch_slot_3, createBroadcastPendingIntent(context, ACTION_CYCLE_SOUND, appWidgetId, 3))
        views.setOnClickPendingIntent(R.id.touch_slot_4, createBroadcastPendingIntent(context, ACTION_TOGGLE_ROTATE, appWidgetId, 4))
    }
}

// =========================================================================
// 3. CONNECTIVITY DUO BENTO (2x2)
// =========================================================================

class QuickTogglesConnectivityReceiver : BaseQuickTogglesReceiver(R.layout.widget_base_column_2, targetAspect = 1.0f) {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val state = QuickTogglesStateManager.readCurrentState(context)
        return generateConnectivityBentoBitmap(context, state, config, isResponsive, wDp, hDp)
    }

    override fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        views.setOnClickPendingIntent(R.id.touch_slot_0, createTrampolineActivityPendingIntent(context, QuickTogglesStateManager.createWifiIntent(), appWidgetId, 10))
        views.setOnClickPendingIntent(R.id.touch_slot_1, createTrampolineActivityPendingIntent(context, QuickTogglesStateManager.createBluetoothIntent(), appWidgetId, 11))
    }
}

// =========================================================================
// 4. QUAD ACTION MATRIX (2x2)
// =========================================================================

class QuickTogglesQuadReceiver : BaseQuickTogglesReceiver(R.layout.widget_base_grid_2x2, targetAspect = 1.0f) {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val state = QuickTogglesStateManager.readCurrentState(context)
        return generateQuadActionMatrixBitmap(context, state, config, isResponsive, wDp, hDp)
    }

    override fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        // slot_0: Wi-Fi (Top-Left)
        views.setOnClickPendingIntent(R.id.slot_0, createTrampolineActivityPendingIntent(context, QuickTogglesStateManager.createWifiIntent(), appWidgetId, 20))
        // slot_1: Bluetooth (Top-Right)
        views.setOnClickPendingIntent(R.id.slot_1, createTrampolineActivityPendingIntent(context, QuickTogglesStateManager.createBluetoothIntent(), appWidgetId, 21))
        // slot_2: Torch (Bottom-Left)
        views.setOnClickPendingIntent(R.id.slot_2, createBroadcastPendingIntent(context, ACTION_TOGGLE_TORCH, appWidgetId, 22))
        // slot_3: Sound (Bottom-Right)
        views.setOnClickPendingIntent(R.id.slot_3, createBroadcastPendingIntent(context, ACTION_CYCLE_SOUND, appWidgetId, 23))
    }
}

// =========================================================================
// 5. TACTILE ALERT SLIDER (HORIZONTAL 2x1)
// =========================================================================

class QuickTogglesAlertSliderHReceiver : BaseQuickTogglesReceiver(R.layout.widget_base_row_3, targetAspect = 2.0f) {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val state = QuickTogglesStateManager.readCurrentState(context)
        return generateAlertSliderHorizontalBitmap(context, state.alertSlider, config, isResponsive, wDp, hDp)
    }

    override fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        // Slot 0: Silent, Slot 1: Vibrate, Slot 2: Ring
        views.setOnClickPendingIntent(R.id.touch_slot_0, createBroadcastPendingIntent(context, ACTION_SET_SLIDER_MODE, appWidgetId, 30, EXTRA_SLIDER_MODE, AlertSliderMode.SILENT.name))
        views.setOnClickPendingIntent(R.id.touch_slot_1, createBroadcastPendingIntent(context, ACTION_SET_SLIDER_MODE, appWidgetId, 31, EXTRA_SLIDER_MODE, AlertSliderMode.VIBRATE.name))
        views.setOnClickPendingIntent(R.id.touch_slot_2, createBroadcastPendingIntent(context, ACTION_SET_SLIDER_MODE, appWidgetId, 32, EXTRA_SLIDER_MODE, AlertSliderMode.RING.name))
    }
}

// =========================================================================
// 6. VERTICAL ALERT SLIDER (1x2)
// =========================================================================

class QuickTogglesAlertSliderVReceiver : BaseQuickTogglesReceiver(R.layout.widget_base_column_3, targetAspect = 0.5f) {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val state = QuickTogglesStateManager.readCurrentState(context)
        return generateAlertSliderVerticalBitmap(context, state.alertSlider, config, isResponsive, wDp, hDp)
    }

    override fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        // Slot 0: Ring (Top), Slot 1: Vibrate (Middle), Slot 2: Silent (Bottom)
        views.setOnClickPendingIntent(R.id.touch_slot_0, createBroadcastPendingIntent(context, ACTION_SET_SLIDER_MODE, appWidgetId, 40, EXTRA_SLIDER_MODE, AlertSliderMode.RING.name))
        views.setOnClickPendingIntent(R.id.touch_slot_1, createBroadcastPendingIntent(context, ACTION_SET_SLIDER_MODE, appWidgetId, 41, EXTRA_SLIDER_MODE, AlertSliderMode.VIBRATE.name))
        views.setOnClickPendingIntent(R.id.touch_slot_2, createBroadcastPendingIntent(context, ACTION_SET_SLIDER_MODE, appWidgetId, 42, EXTRA_SLIDER_MODE, AlertSliderMode.SILENT.name))
    }
}


// =========================================================================
// 7. FLASHLIGHT TORCH SWITCH (2x2)
// =========================================================================

class QuickTogglesTorchSwitchReceiver : BaseQuickTogglesReceiver(R.layout.widget_base_single, targetAspect = 1.0f) {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val state = QuickTogglesStateManager.readCurrentState(context)
        return generateFlashlightTorchBitmap(context, state.torch.isEnabled, config, isResponsive, wDp, hDp)
    }

    override fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        views.setOnClickPendingIntent(
            R.id.touch_slot_0,
            createBroadcastPendingIntent(context, ACTION_TOGGLE_TORCH, appWidgetId, 50)
        )
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
        views.setOnClickPendingIntent(R.id.btn_toggle_pill, createTrampolineActivityPendingIntent(context, QuickTogglesStateManager.createWifiIntent(), appWidgetId, 60))
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
        views.setOnClickPendingIntent(R.id.btn_toggle_pill, createTrampolineActivityPendingIntent(context, QuickTogglesStateManager.createBluetoothIntent(), appWidgetId, 70))
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
        views.setOnClickPendingIntent(R.id.btn_util_0, createBroadcastPendingIntent(context, ACTION_CYCLE_TIMEOUT, appWidgetId, 100))
        views.setOnClickPendingIntent(R.id.btn_util_1, createBroadcastPendingIntent(context, ACTION_TOGGLE_ROTATE, appWidgetId, 101))
        views.setOnClickPendingIntent(R.id.btn_util_2, createTrampolineActivityPendingIntent(context, QuickTogglesStateManager.createBatterySaverIntent(), appWidgetId, 102))
        views.setOnClickPendingIntent(R.id.btn_util_3, createTrampolineActivityPendingIntent(context, QuickTogglesStateManager.createDisplaySettingsIntent(), appWidgetId, 103))
        views.setOnClickPendingIntent(R.id.btn_util_4, createTrampolineActivityPendingIntent(context, QuickTogglesStateManager.createAirplaneIntent(), appWidgetId, 104))
        views.setOnClickPendingIntent(R.id.btn_util_5, createTrampolineActivityPendingIntent(context, QuickTogglesStateManager.createHotspotIntent(context), appWidgetId, 105))
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
