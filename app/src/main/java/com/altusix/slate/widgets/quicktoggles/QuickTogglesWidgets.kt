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
import android.app.NotificationManager
import com.altusix.slate.widgets.common.bindFolderTouchPendingIntents

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
        SlateWidgetInfo("System Utility Deck", "4x2", "Quick Toggles", QuickTogglesUtilityDeckReceiver::class.java, hasModeOption = true),

        // 2x1 Pill Toggles
        SlateWidgetInfo("Wi-Fi Network Pill", "2x1", "Quick Toggles", QuickTogglesWifiPillReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Bluetooth Device Pill", "2x1", "Quick Toggles", QuickTogglesBluetoothPillReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Sound Mode Pill", "2x1", "Quick Toggles", QuickTogglesSoundPillReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Auto-Rotate Pill", "2x1", "Quick Toggles", QuickTogglesRotatePillReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Hotspot Pill", "2x1", "Quick Toggles", QuickTogglesHotspotPillReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Airplane Mode Pill", "2x1", "Quick Toggles", QuickTogglesAirplanePillReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Dark Mode Pill", "2x1", "Quick Toggles", QuickTogglesDarkModePillReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Location Pill", "2x1", "Quick Toggles", QuickTogglesLocationPillReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Battery Saver Pill", "2x1", "Quick Toggles", QuickTogglesBatterySaverPillReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Do Not Disturb Pill", "2x1", "Quick Toggles", QuickTogglesDndPillReceiver::class.java, hasModeOption = false),

        // 1x1 Micro Toggles
        SlateWidgetInfo("Micro Toggle: Flashlight", "1x1", "Quick Toggles", QuickTogglesMicroTorchReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Micro Toggle: Sound Mode", "1x1", "Quick Toggles", QuickTogglesMicroSoundReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Micro Toggle: Wi-Fi", "1x1", "Quick Toggles", QuickTogglesMicroWifiReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Micro Toggle: Bluetooth", "1x1", "Quick Toggles", QuickTogglesMicroBluetoothReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Micro Toggle: Auto-Rotate", "1x1", "Quick Toggles", QuickTogglesMicroRotateReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Micro Toggle: Hotspot", "1x1", "Quick Toggles", QuickTogglesMicroHotspotReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Micro Toggle: Airplane", "1x1", "Quick Toggles", QuickTogglesMicroAirplaneReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Micro Toggle: Dark Mode", "1x1", "Quick Toggles", QuickTogglesMicroDarkModeReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Micro Toggle: Location", "1x1", "Quick Toggles", QuickTogglesMicroLocationReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Micro Toggle: Battery Saver", "1x1", "Quick Toggles", QuickTogglesMicroBatterySaverReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Micro Toggle: DND", "1x1", "Quick Toggles", QuickTogglesMicroDndReceiver::class.java, hasModeOption = false)
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
        QuickTogglesUtilityDeckReceiver(),

        // 2x1 Pills
        QuickTogglesWifiPillReceiver(),
        QuickTogglesBluetoothPillReceiver(),
        QuickTogglesSoundPillReceiver(),
        QuickTogglesRotatePillReceiver(),
        QuickTogglesHotspotPillReceiver(),
        QuickTogglesAirplanePillReceiver(),
        QuickTogglesDarkModePillReceiver(),
        QuickTogglesLocationPillReceiver(),
        QuickTogglesBatterySaverPillReceiver(),
        QuickTogglesDndPillReceiver(),

        // 1x1 Micro Toggles
        QuickTogglesMicroTorchReceiver(),
        QuickTogglesMicroSoundReceiver(),
        QuickTogglesMicroWifiReceiver(),
        QuickTogglesMicroBluetoothReceiver(),
        QuickTogglesMicroRotateReceiver(),
        QuickTogglesMicroHotspotReceiver(),
        QuickTogglesMicroAirplaneReceiver(),
        QuickTogglesMicroDarkModeReceiver(),
        QuickTogglesMicroLocationReceiver(),
        QuickTogglesMicroBatterySaverReceiver(),
        QuickTogglesMicroDndReceiver()
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
        const val ACTION_TOGGLE_DND = "com.altusix.slate.toggles.ACTION_TOGGLE_DND"
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
        // Essential: Allow AppWidgetProvider to process onUpdate/onOptionsChanged
        super.onReceive(context, intent)

        when (intent.action) {
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                val manager = AppWidgetManager.getInstance(context) ?: return
                val ids = manager.getAppWidgetIds(ComponentName(context, this::class.java)) ?: intArrayOf()
                for (id in ids) updateSingleWidget(context, manager, id)
                return
            }
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
            ACTION_TOGGLE_DND -> {
                QuickTogglesStateManager.toggleDnd(context)
                updateAllQuickTogglesWidgets(context)
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    updateAllQuickTogglesWidgets(context)
                }, 200)
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    updateAllQuickTogglesWidgets(context)
                }, 600)
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
            "android.app.action.INTERRUPTION_FILTER_CHANGED",
            Intent.ACTION_AIRPLANE_MODE_CHANGED,
            Intent.ACTION_USER_PRESENT,
            "android.os.action.POWER_SAVE_MODE_CHANGED" -> {
                updateAllQuickTogglesWidgets(context)
                return
            }
        }
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
                R.layout.widget_base_single,
                R.layout.widget_base_grid_3x2
            )

            val isSharedGrid = activeLayoutId in sharedGridLayouts
            val imageViewId = if (isSharedGrid) R.id.widget_image_view else R.id.widget_canvas_surface
            views.setImageViewBitmap(imageViewId, bitmap)

            // Safe padding reset only for layouts that declare root IDs
            if (isSharedGrid && activeLayoutId != R.layout.widget_base_grid_2x2) {
                try {
                    views.setViewPadding(R.id.layout_grid_root, 0, 0, 0, 0)
                } catch (_: Exception) {}
            }

            setupTouchTargets(context, views, id)
            manager.updateAppWidget(id, views)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    protected open fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {}

    protected fun createBroadcastPendingIntent(
        context: Context,
        action: String,
        appWidgetId: Int,
        requestCode: Int,
        extraKey: String? = null,
        extraVal: String? = null
    ): PendingIntent {
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

    protected fun createTrampolineActivityPendingIntent(
        context: Context,
        targetIntent: Intent,
        appWidgetId: Int,
        requestCode: Int
    ): PendingIntent {
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

    protected fun createBroadcastIntent(
        context: Context,
        action: String,
        appWidgetId: Int,
        requestCode: Int,
        extraKey: String? = null,
        extraVal: String? = null
    ): PendingIntent = createBroadcastPendingIntent(context, action, appWidgetId, requestCode, extraKey, extraVal)

    protected fun createTrampolineIntent(
        context: Context,
        targetIntent: Intent,
        appWidgetId: Int,
        requestCode: Int
    ): PendingIntent = createTrampolineActivityPendingIntent(context, targetIntent, appWidgetId, requestCode)
}

// =========================================================================
// GENERIC SINGLE-TARGET BASE RECEIVERS (Uses widget_base_single.xml)
// =========================================================================

abstract class BaseSingleTogglePillReceiver(
    private val selector: (QuickTogglesState) -> ToggleItemState
) : BaseQuickTogglesReceiver(R.layout.widget_base_single, targetAspect = 2.0f) {

    abstract fun createActionPendingIntent(context: Context, appWidgetId: Int): PendingIntent

    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val state = QuickTogglesStateManager.readCurrentState(context)
        val item = selector(state)
        val alertMode = if (item.type == ToggleType.RINGER) state.alertSlider else null
        return generateTogglePillBitmap(context, item, alertMode, config, isResponsive, wDp, hDp)
    }

    override fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        views.setOnClickPendingIntent(R.id.touch_slot_0, createActionPendingIntent(context, appWidgetId))
    }
}

abstract class BaseSingleMicroToggleReceiver(
    private val selector: (QuickTogglesState) -> ToggleItemState
) : BaseQuickTogglesReceiver(R.layout.widget_base_single, targetAspect = 1.0f) {

    abstract fun createActionPendingIntent(context: Context, appWidgetId: Int): PendingIntent

    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val state = QuickTogglesStateManager.readCurrentState(context)
        val item = selector(state)
        val alertMode = if (item.type == ToggleType.RINGER) state.alertSlider else null
        return generateMicroToggleBitmap(context, item, alertMode, config, isResponsive, wDp, hDp)
    }

    override fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        views.setOnClickPendingIntent(R.id.touch_slot_0, createActionPendingIntent(context, appWidgetId))
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
        val intents = listOf(
            createTrampolineActivityPendingIntent(context, QuickTogglesStateManager.createWifiIntent(), appWidgetId, 0),
            createTrampolineActivityPendingIntent(context, QuickTogglesStateManager.createBluetoothIntent(), appWidgetId, 1),
            createBroadcastPendingIntent(context, ACTION_TOGGLE_TORCH, appWidgetId, 2),
            createBroadcastPendingIntent(context, ACTION_CYCLE_SOUND, appWidgetId, 3),
            createBroadcastPendingIntent(context, ACTION_TOGGLE_ROTATE, appWidgetId, 4),
            createTrampolineActivityPendingIntent(context, QuickTogglesStateManager.createHotspotIntent(context), appWidgetId, 5),
            createTrampolineActivityPendingIntent(context, QuickTogglesStateManager.createLocationIntent(), appWidgetId, 6),
            createTrampolineActivityPendingIntent(context, QuickTogglesStateManager.createDisplaySettingsIntent(), appWidgetId, 7)
        )
        views.bindFolderTouchPendingIntents(8) { i -> intents.getOrNull(i) }
    }
}

// =========================================================================
// 2. MINIMALIST ACTION TOOLBAR (5x1 / 1x5 Pivot)
// =========================================================================
class QuickTogglesToolbarReceiver : BaseQuickTogglesReceiver(R.layout.widget_base_row_5, targetAspect = 4.0f) {

    override fun getLayoutResId(wDp: Int, hDp: Int, isResponsive: Boolean): Int {
        val isVertical = isResponsive && (hDp > wDp)
        return if (isVertical) R.layout.widget_base_col_5 else R.layout.widget_base_row_5
    }

    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val state = QuickTogglesStateManager.readCurrentState(context)
        return generateMinimalistToolbarBitmap(context, state, config, isResponsive, wDp, hDp)
    }

    override fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        val intents = listOf(
            createTrampolineActivityPendingIntent(context, QuickTogglesStateManager.createWifiIntent(), appWidgetId, 0),
            createTrampolineActivityPendingIntent(context, QuickTogglesStateManager.createBluetoothIntent(), appWidgetId, 1),
            createBroadcastPendingIntent(context, ACTION_TOGGLE_TORCH, appWidgetId, 2),
            createBroadcastPendingIntent(context, ACTION_CYCLE_SOUND, appWidgetId, 3),
            createBroadcastPendingIntent(context, ACTION_TOGGLE_ROTATE, appWidgetId, 4)
        )
        views.bindFolderTouchPendingIntents(5) { i -> intents.getOrNull(i) }
    }
}

// =========================================================================
// 3. CONNECTIVITY DUO BENTO (2x2 / 1x2 / 2x1 Pivot)
// =========================================================================
class QuickTogglesConnectivityReceiver : BaseQuickTogglesReceiver(R.layout.widget_base_column_2, targetAspect = 1.0f) {

    override fun getLayoutResId(wDp: Int, hDp: Int, isResponsive: Boolean): Int {
        val isWide = isResponsive && (wDp > hDp * 1.35f)
        return if (isWide) R.layout.widget_base_row_2 else R.layout.widget_base_column_2
    }

    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val state = QuickTogglesStateManager.readCurrentState(context)
        return generateConnectivityBentoBitmap(context, state, config, isResponsive, wDp, hDp)
    }

    override fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        val intents = listOf(
            createTrampolineActivityPendingIntent(context, QuickTogglesStateManager.createWifiIntent(), appWidgetId, 10),
            createTrampolineActivityPendingIntent(context, QuickTogglesStateManager.createBluetoothIntent(), appWidgetId, 11)
        )
        views.bindFolderTouchPendingIntents(2) { i -> intents.getOrNull(i) }
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
        val intents = listOf(
            createTrampolineActivityPendingIntent(context, QuickTogglesStateManager.createWifiIntent(), appWidgetId, 20),
            createTrampolineActivityPendingIntent(context, QuickTogglesStateManager.createBluetoothIntent(), appWidgetId, 21),
            createBroadcastPendingIntent(context, ACTION_TOGGLE_TORCH, appWidgetId, 22),
            createBroadcastPendingIntent(context, ACTION_CYCLE_SOUND, appWidgetId, 23)
        )
        views.bindFolderTouchPendingIntents(4) { i -> intents.getOrNull(i) }
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
// 12. SYSTEM UTILITY DECK (4x2 / 3x2)
// =========================================================================
class QuickTogglesUtilityDeckReceiver : BaseQuickTogglesReceiver(R.layout.widget_base_grid_3x2, targetAspect = 2.0f) {

    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val state = QuickTogglesStateManager.readCurrentState(context)
        return generateSystemUtilityDeckBitmap(context, state, config, isResponsive, wDp, hDp)
    }

    override fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        val intents = listOf(
            createBroadcastPendingIntent(context, ACTION_CYCLE_TIMEOUT, appWidgetId, 100),
            createBroadcastPendingIntent(context, ACTION_TOGGLE_ROTATE, appWidgetId, 101),
            createTrampolineActivityPendingIntent(context, QuickTogglesStateManager.createBatterySaverIntent(), appWidgetId, 102),
            createTrampolineActivityPendingIntent(context, QuickTogglesStateManager.createDisplaySettingsIntent(), appWidgetId, 103),
            createTrampolineActivityPendingIntent(context, QuickTogglesStateManager.createAirplaneIntent(), appWidgetId, 104),
            createTrampolineActivityPendingIntent(context, QuickTogglesStateManager.createHotspotIntent(context), appWidgetId, 105)
        )
        views.bindFolderTouchPendingIntents(6) { i -> intents.getOrNull(i) }
    }
}

// =========================================================================
// 2x1 PILL VARIANTS (BaseSingleTogglePillReceiver)
// =========================================================================

class QuickTogglesWifiPillReceiver : BaseSingleTogglePillReceiver({ it.wifi }) {
    override fun createActionPendingIntent(context: Context, appWidgetId: Int): PendingIntent =
        createTrampolineIntent(context, QuickTogglesStateManager.createWifiIntent(), appWidgetId, 201)
}

class QuickTogglesBluetoothPillReceiver : BaseSingleTogglePillReceiver({ it.bluetooth }) {
    override fun createActionPendingIntent(context: Context, appWidgetId: Int): PendingIntent =
        createTrampolineIntent(context, QuickTogglesStateManager.createBluetoothIntent(), appWidgetId, 202)
}

class QuickTogglesSoundPillReceiver : BaseSingleTogglePillReceiver({ it.ringer }) {
    override fun createActionPendingIntent(context: Context, appWidgetId: Int): PendingIntent =
        createBroadcastIntent(context, ACTION_CYCLE_SOUND, appWidgetId, 203)
}

class QuickTogglesRotatePillReceiver : BaseSingleTogglePillReceiver({ it.autoRotate }) {
    override fun createActionPendingIntent(context: Context, appWidgetId: Int): PendingIntent =
        createBroadcastIntent(context, ACTION_TOGGLE_ROTATE, appWidgetId, 204)
}

class QuickTogglesHotspotPillReceiver : BaseSingleTogglePillReceiver({ it.hotspot }) {
    override fun createActionPendingIntent(context: Context, appWidgetId: Int): PendingIntent =
        createTrampolineIntent(context, QuickTogglesStateManager.createHotspotIntent(context), appWidgetId, 205)
}

class QuickTogglesAirplanePillReceiver : BaseSingleTogglePillReceiver({ it.airplane }) {
    override fun createActionPendingIntent(context: Context, appWidgetId: Int): PendingIntent =
        createTrampolineIntent(context, QuickTogglesStateManager.createAirplaneIntent(), appWidgetId, 206)
}

class QuickTogglesDarkModePillReceiver : BaseSingleTogglePillReceiver({ it.darkMode }) {
    override fun createActionPendingIntent(context: Context, appWidgetId: Int): PendingIntent =
        createTrampolineIntent(context, QuickTogglesStateManager.createDisplaySettingsIntent(), appWidgetId, 207)
}

class QuickTogglesLocationPillReceiver : BaseSingleTogglePillReceiver({ it.location }) {
    override fun createActionPendingIntent(context: Context, appWidgetId: Int): PendingIntent =
        createTrampolineIntent(context, QuickTogglesStateManager.createLocationIntent(), appWidgetId, 208)
}

class QuickTogglesBatterySaverPillReceiver : BaseSingleTogglePillReceiver({ it.batterySaver }) {
    override fun createActionPendingIntent(context: Context, appWidgetId: Int): PendingIntent =
        createTrampolineIntent(context, QuickTogglesStateManager.createBatterySaverIntent(), appWidgetId, 209)
}

class QuickTogglesDndPillReceiver : BaseSingleTogglePillReceiver({ it.dnd }) {
    override fun createActionPendingIntent(context: Context, appWidgetId: Int): PendingIntent {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        return if (nm?.isNotificationPolicyAccessGranted == true) {
            createBroadcastIntent(context, ACTION_TOGGLE_DND, appWidgetId, 210)
        } else {
            createTrampolineIntent(context, QuickTogglesStateManager.createNotificationPolicyIntent(), appWidgetId, 210)
        }
    }
}

// =========================================================================
// 1x1 MICRO TOGGLE VARIANTS (BaseSingleMicroToggleReceiver)
// =========================================================================

class QuickTogglesMicroTorchReceiver : BaseSingleMicroToggleReceiver({ it.torch }) {
    override fun createActionPendingIntent(context: Context, appWidgetId: Int): PendingIntent =
        createBroadcastIntent(context, ACTION_TOGGLE_TORCH, appWidgetId, 301)
}

class QuickTogglesMicroSoundReceiver : BaseSingleMicroToggleReceiver({ it.ringer }) {
    override fun createActionPendingIntent(context: Context, appWidgetId: Int): PendingIntent =
        createBroadcastIntent(context, ACTION_CYCLE_SOUND, appWidgetId, 302)
}

class QuickTogglesMicroWifiReceiver : BaseSingleMicroToggleReceiver({ it.wifi }) {
    override fun createActionPendingIntent(context: Context, appWidgetId: Int): PendingIntent =
        createTrampolineIntent(context, QuickTogglesStateManager.createWifiIntent(), appWidgetId, 303)
}

class QuickTogglesMicroBluetoothReceiver : BaseSingleMicroToggleReceiver({ it.bluetooth }) {
    override fun createActionPendingIntent(context: Context, appWidgetId: Int): PendingIntent =
        createTrampolineIntent(context, QuickTogglesStateManager.createBluetoothIntent(), appWidgetId, 304)
}

class QuickTogglesMicroRotateReceiver : BaseSingleMicroToggleReceiver({ it.autoRotate }) {
    override fun createActionPendingIntent(context: Context, appWidgetId: Int): PendingIntent =
        createBroadcastIntent(context, ACTION_TOGGLE_ROTATE, appWidgetId, 305)
}

class QuickTogglesMicroHotspotReceiver : BaseSingleMicroToggleReceiver({ it.hotspot }) {
    override fun createActionPendingIntent(context: Context, appWidgetId: Int): PendingIntent =
        createTrampolineIntent(context, QuickTogglesStateManager.createHotspotIntent(context), appWidgetId, 306)
}

class QuickTogglesMicroAirplaneReceiver : BaseSingleMicroToggleReceiver({ it.airplane }) {
    override fun createActionPendingIntent(context: Context, appWidgetId: Int): PendingIntent =
        createTrampolineIntent(context, QuickTogglesStateManager.createAirplaneIntent(), appWidgetId, 307)
}

class QuickTogglesMicroDarkModeReceiver : BaseSingleMicroToggleReceiver({ it.darkMode }) {
    override fun createActionPendingIntent(context: Context, appWidgetId: Int): PendingIntent =
        createTrampolineIntent(context, QuickTogglesStateManager.createDisplaySettingsIntent(), appWidgetId, 308)
}

class QuickTogglesMicroLocationReceiver : BaseSingleMicroToggleReceiver({ it.location }) {
    override fun createActionPendingIntent(context: Context, appWidgetId: Int): PendingIntent =
        createTrampolineIntent(context, QuickTogglesStateManager.createLocationIntent(), appWidgetId, 309)
}

class QuickTogglesMicroBatterySaverReceiver : BaseSingleMicroToggleReceiver({ it.batterySaver }) {
    override fun createActionPendingIntent(context: Context, appWidgetId: Int): PendingIntent =
        createTrampolineIntent(context, QuickTogglesStateManager.createBatterySaverIntent(), appWidgetId, 310)
}

class QuickTogglesMicroDndReceiver : BaseSingleMicroToggleReceiver({ it.dnd }) {
    override fun createActionPendingIntent(context: Context, appWidgetId: Int): PendingIntent {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        return if (nm?.isNotificationPolicyAccessGranted == true) {
            createBroadcastIntent(context, ACTION_TOGGLE_DND, appWidgetId, 311)
        } else {
            createTrampolineIntent(context, QuickTogglesStateManager.createNotificationPolicyIntent(), appWidgetId, 311)
        }
    }
}