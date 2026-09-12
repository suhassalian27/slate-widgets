package com.altusix.slate.widgets.productivity

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
import android.os.Build

// =========================================================================
// CATALOG & UPDATE HELPERS
// =========================================================================

fun getProductivityWidgetsCatalog(): List<SlateWidgetInfo> {
    return listOf(
        SlateWidgetInfo("Pomodoro Focus Dial", "2x2", "Productivity", ProductivityPomodoroReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Habit Streak Matrix", "4x2", "Productivity", ProductivityHabitMatrixReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Daily Top 3 Wins", "4x2", "Productivity", ProductivityTop3Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Eisenhower Priority Matrix", "4x2", "Productivity", ProductivityEisenhowerReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Goal Milestone Countdown", "2x2", "Productivity", ProductivityGoalReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Weekly Habit Rings", "2x2", "Productivity", ProductivityHabitRingsReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Minimal Task Pipeline", "4x1", "Productivity", ProductivityPipelineReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Bookmark List", "4x2", "Productivity", ProductivityBookmarksReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Screen Time Balance", "2x2", "Productivity", ProductivityScreenTimeReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("Clipboard Vault", "2x2", "Productivity", ProductivityClipboardReceiver::class.java, hasModeOption = true)
    )
}

fun updateAllProductivityWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context) ?: return
    val receivers = listOf(
        ProductivityPomodoroReceiver::class.java,
        ProductivityHabitMatrixReceiver::class.java,
        ProductivityTop3Receiver::class.java,
        ProductivityEisenhowerReceiver::class.java,
        ProductivityGoalReceiver::class.java,
        ProductivityHabitRingsReceiver::class.java,
        ProductivityPipelineReceiver::class.java,
        ProductivityBookmarksReceiver::class.java,
        ProductivityScreenTimeReceiver::class.java,
        ProductivityClipboardReceiver::class.java
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
// BASE PRODUCTIVITY RECEIVER
// =========================================================================

abstract class BaseProductivityReceiver(
    private val layoutResId: Int,
    protected open val targetAspect: Float = 1.0f
) : AppWidgetProvider() {

    companion object {
        const val ACTION_EDIT_PRODUCTIVITY = "com.altusix.slate.productivity.ACTION_EDIT_PRODUCTIVITY"
        const val ACTION_TOGGLE_TOP3 = "com.altusix.slate.productivity.ACTION_TOGGLE_TOP3"
        const val ACTION_TOGGLE_HABIT_TODAY = "com.altusix.slate.productivity.ACTION_TOGGLE_HABIT_TODAY"
        const val ACTION_TOGGLE_TIMER = "com.altusix.slate.productivity.ACTION_TOGGLE_TIMER"
        const val ACTION_RESET_TIMER = "com.altusix.slate.productivity.ACTION_RESET_TIMER"
        const val ACTION_COPY_CLIPBOARD = "com.altusix.slate.productivity.ACTION_COPY_CLIPBOARD"
        const val EXTRA_INDEX = "extra_index"
        const val EXTRA_TAB = "extra_tab"
    }

    protected open val defaultTab: String = "TOP3"

    abstract fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap

    override fun onReceive(context: Context, intent: Intent) {
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

        when (intent.action) {
            ACTION_EDIT_PRODUCTIVITY -> {
                val tab = intent.getStringExtra(EXTRA_TAB) ?: defaultTab
                val editIntent = Intent(context, ProductivityConfigActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    putExtra(EXTRA_TAB, tab)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(editIntent)
                return
            }
            ACTION_TOGGLE_TOP3 -> {
                val index = intent.getIntExtra(EXTRA_INDEX, 0)
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    ProductivityStorageManager.toggleTop3Task(context, appWidgetId, index)
                    updateSingleWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
                }
                return
            }
            ACTION_TOGGLE_HABIT_TODAY -> {
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    ProductivityStorageManager.toggleHabitToday(context, appWidgetId)
                    updateSingleWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
                }
                return
            }
            ACTION_TOGGLE_TIMER -> {
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    ProductivityStorageManager.toggleFocusTimer(context, appWidgetId)
                    val config = ProductivityStorageManager.getConfig(context, appWidgetId)

                    val serviceIntent = Intent(context, PomodoroTimerService::class.java).apply {
                        putExtra(PomodoroTimerService.EXTRA_WIDGET_ID, appWidgetId)
                        action = if (config.focusTimer.isRunning) {
                            PomodoroTimerService.ACTION_START
                        } else {
                            PomodoroTimerService.ACTION_STOP
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                    updateSingleWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
                }
                return
            }
            ACTION_RESET_TIMER -> {
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    ProductivityStorageManager.resetFocusTimer(context, appWidgetId)
                    updateSingleWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
                }
                return
            }
            ACTION_COPY_CLIPBOARD -> {
                val index = intent.getIntExtra(EXTRA_INDEX, 0)
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    ProductivityStorageManager.copySnippet(context, appWidgetId, index)
                }
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
            val density = context.resources.displayMetrics.density

            val padH: Int
            val padV: Int
            val effWDp: Int
            val effHDp: Int

            if (!isResponsive && targetAspect > 0f) {
                val currentAspect = wDp.toFloat() / hDp.toFloat().coerceAtLeast(1f)
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

            // Render to effective size so the canvas fills 100% of the visible area
            val bitmap = renderWidgetBitmap(context, id, config, isResponsive, effWDp, effHDp)
            val views = RemoteViews(context.packageName, layoutResId)
            views.setViewPadding(R.id.layout_productivity_root, padH, padV, padH, padV)
            views.setImageViewBitmap(R.id.widget_canvas_surface, bitmap)

            setupTouchTargets(context, views, id)
            manager.updateAppWidget(id, views)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    protected open fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        val editIntent = Intent(context, ProductivityConfigActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(EXTRA_TAB, defaultTab)
            data = Uri.parse("slate_prod://$appWidgetId/$defaultTab")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val editPi = PendingIntent.getActivity(
            context,
            appWidgetId * 100 + 1,
            editIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            views.setOnClickPendingIntent(R.id.btn_productivity_open, editPi)
        } catch (_: Exception) {}
    }
}

// =========================================================================
// 1. POMODORO FOCUS DIAL (2x2)
// =========================================================================
class ProductivityPomodoroReceiver : BaseProductivityReceiver(R.layout.widget_productivity_timer_layout, targetAspect = 1.0f) {
    override val defaultTab: String = "TIMER"

    companion object {
        const val ACTION_ADJUST_TIMER = "com.altusix.slate.productivity.ACTION_ADJUST_TIMER"
        const val EXTRA_DELTA_MINUTES = "extra_delta_minutes"
    }

    override fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        val prodConfig = ProductivityStorageManager.getConfig(context, appWidgetId)
        return generatePomodoroTimerBitmap(
            context,
            prodConfig.focusTimer,
            config,
            isResponsive,
            wDp,
            hDp
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (intent.action == ACTION_ADJUST_TIMER && appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val delta = intent.getIntExtra(EXTRA_DELTA_MINUTES, 0)
            ProductivityStorageManager.adjustFocusTimer(context, appWidgetId, delta)
            updateSingleWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
            return
        }
        super.onReceive(context, intent)
    }

    override fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        val prodConfig = ProductivityStorageManager.getConfig(context, appWidgetId)

        val openIntent = Intent(context, ProductivityConfigActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(EXTRA_TAB, "TOP3")
            data = Uri.parse("slate_prod://$appWidgetId/top3_edit")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPi = PendingIntent.getActivity(
            context,
            appWidgetId * 100 + 1,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_top3_open, openPi)

        val targetIds = listOf(R.id.btn_top3_check_0, R.id.btn_top3_check_1, R.id.btn_top3_check_2)
        for (i in targetIds.indices) {
            val item = prodConfig.top3Tasks.getOrNull(i)
            // If task is configured, clicking checkbox toggles it.
            // If task is empty, clicking opens config directly to add it.
            val pi = if (item != null && item.title.isNotBlank()) {
                val checkIntent = Intent(context, this.javaClass).apply {
                    action = ACTION_TOGGLE_TOP3
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    putExtra(EXTRA_INDEX, i)
                    data = Uri.parse("slate_prod://$appWidgetId/top3_check_$i")
                }
                PendingIntent.getBroadcast(
                    context,
                    appWidgetId * 100 + 10 + i,
                    checkIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                openPi
            }
            views.setOnClickPendingIntent(targetIds[i], pi)
        }
    }
}

// =========================================================================
// 2. HABIT STREAK MATRIX
// =========================================================================

class ProductivityHabitMatrixReceiver : BaseProductivityReceiver(
    layoutResId = R.layout.widget_productivity_habit_layout,
    targetAspect = 0f
) {
    override val defaultTab: String = "HABIT"

    override fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        val prodConfig = ProductivityStorageManager.getConfig(context, appWidgetId)
        return generateHabitMatrixBitmap(context, prodConfig.habit, config, isResponsive, wDp, hDp)
    }

    override fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        val openIntent = Intent(context, ProductivityConfigActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(EXTRA_TAB, "HABIT")
            data = Uri.parse("slate_prod://$appWidgetId/habit_edit")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPi = PendingIntent.getActivity(
            context,
            appWidgetId * 100 + 1,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_habit_open, openPi)

        val toggleIntent = Intent(context, this.javaClass).apply {
            action = ACTION_TOGGLE_HABIT_TODAY
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse("slate_prod://$appWidgetId/habit_toggle_today")
        }
        val togglePi = PendingIntent.getBroadcast(
            context,
            appWidgetId * 100 + 4,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_habit_toggle_today, togglePi)
    }
}

// =========================================================================
// 3. DAILY TOP 3 WINS (4x2)
// =========================================================================

class ProductivityTop3Receiver : BaseProductivityReceiver(R.layout.widget_productivity_top3_layout, targetAspect = 2.0f) {
    override val defaultTab: String = "TOP3"

    override fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        val prodConfig = ProductivityStorageManager.getConfig(context, appWidgetId)
        return generateTop3TasksBitmap(context, prodConfig.top3Tasks, config, isResponsive, wDp, hDp)
    }

    override fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        val prodConfig = ProductivityStorageManager.getConfig(context, appWidgetId)
        val validTasks = prodConfig.top3Tasks.take(3).filter { it.title.isNotBlank() }

        val openIntent = Intent(context, ProductivityConfigActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(EXTRA_TAB, "TOP3")
            data = Uri.parse("slate_prod://$appWidgetId/top3_edit")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPi = PendingIntent.getActivity(
            context,
            appWidgetId * 100 + 1,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // Background / header tap
        views.setOnClickPendingIntent(R.id.btn_top3_open, openPi)

        val targetIds = listOf(R.id.btn_top3_check_0, R.id.btn_top3_check_1, R.id.btn_top3_check_2)

        // If no tasks are set, every touch area delegates to opening the config screen
        if (validTasks.isEmpty()) {
            for (targetId in targetIds) {
                views.setOnClickPendingIntent(targetId, openPi)
            }
            return
        }

        // Once configured, checkboxes toggle their task while blank slots open config
        for (i in targetIds.indices) {
            val item = prodConfig.top3Tasks.getOrNull(i)
            val pi = if (item != null && item.title.isNotBlank()) {
                val checkIntent = Intent(context, this.javaClass).apply {
                    action = ACTION_TOGGLE_TOP3
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    putExtra(EXTRA_INDEX, i)
                    data = Uri.parse("slate_prod://$appWidgetId/top3_check_$i")
                }
                PendingIntent.getBroadcast(
                    context,
                    appWidgetId * 100 + 10 + i,
                    checkIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                openPi
            }
            views.setOnClickPendingIntent(targetIds[i], pi)
        }
    }
}

// =========================================================================
// 4. EISENHOWER PRIORITY MATRIX (4x2)
// =========================================================================

class ProductivityEisenhowerReceiver : BaseProductivityReceiver(
    layoutResId = R.layout.widget_productivity_card_layout,
    targetAspect = 2.0f
) {
    override val defaultTab: String = "EISENHOWER"

    override fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        val prodConfig = ProductivityStorageManager.getConfig(context, appWidgetId)
        return generateEisenhowerMatrixBitmap(context, prodConfig.eisenhowerTasks, config, isResponsive, wDp, hDp)
    }

    override fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        val openIntent = Intent(context, ProductivityConfigActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(EXTRA_TAB, "EISENHOWER")
            data = Uri.parse("slate_prod://$appWidgetId/eisenhower_edit")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPi = PendingIntent.getActivity(
            context,
            appWidgetId * 100 + 1,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_productivity_open, openPi)
    }
}


// =========================================================================
// 6. GOAL MILESTONE COUNTDOWN (2x2)
// =========================================================================

class ProductivityGoalReceiver : BaseProductivityReceiver(R.layout.widget_productivity_card_layout, targetAspect = 1.0f) {
    override val defaultTab: String = "GOAL"

    override fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        val prodConfig = ProductivityStorageManager.getConfig(context, appWidgetId)
        return generateGoalMilestoneBitmap(context, prodConfig.goal, config, isResponsive, wDp, hDp)
    }
}

// =========================================================================
// 7. WEEKLY HABIT RINGS (2x2)
// =========================================================================

class ProductivityHabitRingsReceiver : BaseProductivityReceiver(R.layout.widget_productivity_card_layout, targetAspect = 1.0f) {
    override val defaultTab: String = "RINGS"

    override fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        val prodConfig = ProductivityStorageManager.getConfig(context, appWidgetId)
        return generateHabitRingsBitmap(context, prodConfig.habitRings, config, isResponsive, wDp, hDp)
    }
}

// =========================================================================
// 8. MINIMAL TASK PIPELINE (Touch Targets: Cards vs Gaps)
// =========================================================================

class ProductivityPipelineReceiver : BaseProductivityReceiver(
    layoutResId = R.layout.widget_productivity_pipeline_layout,
    targetAspect = 3.8f
) {
    override val defaultTab: String = "PIPELINE"

    companion object {
        const val ACTION_PIPELINE_TODO = "com.altusix.slate.productivity.ACTION_PIPELINE_TODO"
        const val ACTION_PIPELINE_ACTIVE = "com.altusix.slate.productivity.ACTION_PIPELINE_ACTIVE"
        const val ACTION_PIPELINE_DONE = "com.altusix.slate.productivity.ACTION_PIPELINE_DONE"
    }

    override fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        val prodConfig = ProductivityStorageManager.getConfig(context, appWidgetId)
        return generateTaskPipelineBitmap(context, prodConfig.pipeline, config, isResponsive, wDp, hDp)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            when (intent.action) {
                ACTION_PIPELINE_TODO -> {
                    ProductivityStorageManager.advancePipelineStage(context, appWidgetId, "TODO")
                    updateSingleWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
                    return
                }
                ACTION_PIPELINE_ACTIVE -> {
                    ProductivityStorageManager.advancePipelineStage(context, appWidgetId, "ACTIVE")
                    updateSingleWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
                    return
                }
                ACTION_PIPELINE_DONE -> {
                    ProductivityStorageManager.advancePipelineStage(context, appWidgetId, "DONE")
                    updateSingleWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
                    return
                }
            }
        }
        super.onReceive(context, intent)
    }

    override fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        val openIntent = Intent(context, ProductivityConfigActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(EXTRA_TAB, "PIPELINE")
            data = Uri.parse("slate_prod://$appWidgetId/pipeline_edit")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPi = PendingIntent.getActivity(
            context,
            appWidgetId * 100 + 1,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 1. Background, progress bar, and both chevron gaps open Config Studio
        views.setOnClickPendingIntent(R.id.btn_pipeline_open, openPi)
        views.setOnClickPendingIntent(R.id.btn_pipeline_open_gap1, openPi)
        views.setOnClickPendingIntent(R.id.btn_pipeline_open_gap2, openPi)

        // 2. Individual card taps execute fast Kanban increments
        val todoIntent = Intent(context, this.javaClass).apply {
            action = ACTION_PIPELINE_TODO
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse("slate_prod://$appWidgetId/pipeline_todo")
        }
        views.setOnClickPendingIntent(
            R.id.btn_pipeline_todo,
            PendingIntent.getBroadcast(context, appWidgetId * 100 + 10, todoIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        )

        val activeIntent = Intent(context, this.javaClass).apply {
            action = ACTION_PIPELINE_ACTIVE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse("slate_prod://$appWidgetId/pipeline_active")
        }
        views.setOnClickPendingIntent(
            R.id.btn_pipeline_active,
            PendingIntent.getBroadcast(context, appWidgetId * 100 + 11, activeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        )

        val doneIntent = Intent(context, this.javaClass).apply {
            action = ACTION_PIPELINE_DONE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse("slate_prod://$appWidgetId/pipeline_done")
        }
        views.setOnClickPendingIntent(
            R.id.btn_pipeline_done,
            PendingIntent.getBroadcast(context, appWidgetId * 100 + 12, doneIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        )
    }
}

// =========================================================================
// 9. BOOKMARKS QUICK LAUNCHER (Interactive Bento Grid with Pagination)
// =========================================================================

class ProductivityBookmarksReceiver : BaseProductivityReceiver(
    layoutResId = R.layout.widget_productivity_bookmarks_layout,
    targetAspect = 2.0f
) {
    override val defaultTab: String = "BOOKMARKS"

    companion object {
        const val ACTION_BOOKMARK_PREV = "com.altusix.slate.productivity.ACTION_BOOKMARK_PREV"
        const val ACTION_BOOKMARK_NEXT = "com.altusix.slate.productivity.ACTION_BOOKMARK_NEXT"
    }

    override fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        val prodConfig = ProductivityStorageManager.getConfig(context, appWidgetId)
        return generateBookmarkListBitmap(context, prodConfig, config, isResponsive, wDp, hDp)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            when (intent.action) {
                ACTION_BOOKMARK_PREV -> {
                    ProductivityStorageManager.cycleBookmarkPage(context, appWidgetId, -1)
                    updateSingleWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
                    return
                }
                ACTION_BOOKMARK_NEXT -> {
                    ProductivityStorageManager.cycleBookmarkPage(context, appWidgetId, 1)
                    updateSingleWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
                    return
                }
            }
        }
        super.onReceive(context, intent)
    }

    override fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        val prodConfig = ProductivityStorageManager.getConfig(context, appWidgetId)
        val bookmarks = prodConfig.bookmarks
        val pageSize = 4
        val totalPages = kotlin.math.ceil(bookmarks.size / pageSize.toFloat()).toInt().coerceAtLeast(1)
        val validPage = prodConfig.bookmarkPageIndex.coerceIn(0, totalPages - 1)

        // 1. Header Tap -> Open Configuration Studio
        val openConfigIntent = Intent(context, ProductivityConfigActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(EXTRA_TAB, "BOOKMARKS")
            data = Uri.parse("slate_prod://$appWidgetId/bookmarks_edit")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openConfigPi = PendingIntent.getActivity(
            context,
            appWidgetId * 100 + 1,
            openConfigIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_bookmark_header, openConfigPi)

        // 2. Pagination Buttons
        val prevIntent = Intent(context, this.javaClass).apply {
            action = ACTION_BOOKMARK_PREV
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse("slate_prod://$appWidgetId/bookmarks_prev")
        }
        views.setOnClickPendingIntent(
            R.id.btn_bookmark_prev,
            PendingIntent.getBroadcast(context, appWidgetId * 100 + 10, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        )

        val nextIntent = Intent(context, this.javaClass).apply {
            action = ACTION_BOOKMARK_NEXT
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse("slate_prod://$appWidgetId/bookmarks_next")
        }
        views.setOnClickPendingIntent(
            R.id.btn_bookmark_next,
            PendingIntent.getBroadcast(context, appWidgetId * 100 + 11, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        )

        // 3. Four Grid Slots (Open URL directly in browser)
        val slotIds = listOf(
            R.id.btn_bookmark_slot_0,
            R.id.btn_bookmark_slot_1,
            R.id.btn_bookmark_slot_2,
            R.id.btn_bookmark_slot_3
        )

        val pageStart = validPage * pageSize
        for (i in 0 until 4) {
            val item = bookmarks.getOrNull(pageStart + i)
            if (item != null && item.url.isNotBlank()) {
                val cleanUrl = if (!item.url.startsWith("http://") && !item.url.startsWith("https://")) {
                    "https://${item.url}"
                } else {
                    item.url
                }
                val launchIntent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val launchPi = PendingIntent.getActivity(
                    context,
                    appWidgetId * 100 + 20 + i,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(slotIds[i], launchPi)
            } else {
                // Empty slot taps open the configuration studio to add more links
                views.setOnClickPendingIntent(slotIds[i], openConfigPi)
            }
        }
    }
}

// =========================================================================
// 10. SCREEN TIME BALANCE (2x2)
// =========================================================================

class ProductivityScreenTimeReceiver : BaseProductivityReceiver(R.layout.widget_productivity_card_layout, targetAspect = 1.0f) {
    override val defaultTab: String = "SCREENTIME"

    override fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        val liveData = ProductivityStorageManager.resolveLiveScreenTime(context)
        return generateScreenTimeBitmap(context, liveData, config, isResponsive, wDp, hDp)
    }

    override fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        val openIntent = Intent(context, ProductivityConfigActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(EXTRA_TAB, "SCREENTIME")
            data = Uri.parse("slate_prod://$appWidgetId/screentime_edit")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPi = PendingIntent.getActivity(
            context,
            appWidgetId * 100 + 1,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_productivity_open, openPi)
    }
}

// =========================================================================
// 11. CLIPBOARD (4-Card Bento Deck: Touch & Pagination Routing)
// =========================================================================

class ProductivityClipboardReceiver : BaseProductivityReceiver(
    layoutResId = R.layout.widget_productivity_clipboard_layout,
    targetAspect = 2.0f
) {
    override val defaultTab: String = "CLIPBOARD"

    companion object {
        const val ACTION_CLIPBOARD_PREV = "com.altusix.slate.productivity.ACTION_CLIPBOARD_PREV"
        const val ACTION_CLIPBOARD_NEXT = "com.altusix.slate.productivity.ACTION_CLIPBOARD_NEXT"
        const val ACTION_CLIPBOARD_COPY_0 = "com.altusix.slate.productivity.ACTION_CLIPBOARD_COPY_0"
        const val ACTION_CLIPBOARD_COPY_1 = "com.altusix.slate.productivity.ACTION_CLIPBOARD_COPY_1"
        const val ACTION_CLIPBOARD_COPY_2 = "com.altusix.slate.productivity.ACTION_CLIPBOARD_COPY_2"
        const val ACTION_CLIPBOARD_COPY_3 = "com.altusix.slate.productivity.ACTION_CLIPBOARD_COPY_3"
    }

    override fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        val prodConfig = ProductivityStorageManager.getConfig(context, appWidgetId)
        return generateClipboardVaultBitmap(context, prodConfig, config, isResponsive, wDp, hDp)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            when (intent.action) {
                ACTION_CLIPBOARD_PREV -> {
                    ProductivityStorageManager.cycleClipboardPage(context, appWidgetId, -1)
                    updateSingleWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
                    return
                }
                ACTION_CLIPBOARD_NEXT -> {
                    ProductivityStorageManager.cycleClipboardPage(context, appWidgetId, 1)
                    updateSingleWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
                    return
                }
                ACTION_CLIPBOARD_COPY_0 -> {
                    ProductivityStorageManager.copySnippetBySlot(context, appWidgetId, 0)
                    return
                }
                ACTION_CLIPBOARD_COPY_1 -> {
                    ProductivityStorageManager.copySnippetBySlot(context, appWidgetId, 1)
                    return
                }
                ACTION_CLIPBOARD_COPY_2 -> {
                    ProductivityStorageManager.copySnippetBySlot(context, appWidgetId, 2)
                    return
                }
                ACTION_CLIPBOARD_COPY_3 -> {
                    ProductivityStorageManager.copySnippetBySlot(context, appWidgetId, 3)
                    return
                }
            }
        }
        super.onReceive(context, intent)
    }

    override fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        val prodConfig = ProductivityStorageManager.getConfig(context, appWidgetId)
        val snippets = prodConfig.clipboardSnippets
        val pageSize = 4
        val totalPages = kotlin.math.ceil(snippets.size / pageSize.toFloat()).toInt().coerceAtLeast(1)
        val validPage = prodConfig.clipboardPageIndex.coerceIn(0, totalPages - 1)

        // 1. Intent to Open Configuration Studio
        val openConfigIntent = Intent(context, ProductivityConfigActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(EXTRA_TAB, "CLIPBOARD")
            data = Uri.parse("slate_prod://$appWidgetId/clipboard_edit")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openConfigPi = PendingIntent.getActivity(
            context,
            appWidgetId * 100 + 1,
            openConfigIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_clipboard_header, openConfigPi)

        // 2. Pagination Buttons
        val prevIntent = Intent(context, this.javaClass).apply {
            action = ACTION_CLIPBOARD_PREV
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse("slate_prod://$appWidgetId/clipboard_prev")
        }
        views.setOnClickPendingIntent(
            R.id.btn_clipboard_prev,
            PendingIntent.getBroadcast(context, appWidgetId * 100 + 10, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        )

        val nextIntent = Intent(context, this.javaClass).apply {
            action = ACTION_CLIPBOARD_NEXT
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse("slate_prod://$appWidgetId/clipboard_next")
        }
        views.setOnClickPendingIntent(
            R.id.btn_clipboard_next,
            PendingIntent.getBroadcast(context, appWidgetId * 100 + 11, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        )

        // 3. Four Slots (Copy if filled; Open Studio if empty '+ Add')
        val slotIds = listOf(
            R.id.btn_clipboard_slot_0,
            R.id.btn_clipboard_slot_1,
            R.id.btn_clipboard_slot_2,
            R.id.btn_clipboard_slot_3
        )
        val actions = listOf(
            ACTION_CLIPBOARD_COPY_0,
            ACTION_CLIPBOARD_COPY_1,
            ACTION_CLIPBOARD_COPY_2,
            ACTION_CLIPBOARD_COPY_3
        )

        val pageOffset = validPage * pageSize
        for (i in 0 until 4) {
            val item = snippets.getOrNull(pageOffset + i)
            if (item != null && item.content.isNotBlank()) {
                val copyIntent = Intent(context, this.javaClass).apply {
                    action = actions[i]
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    data = Uri.parse("slate_prod://$appWidgetId/clipboard_copy_$i")
                }
                views.setOnClickPendingIntent(
                    slotIds[i],
                    PendingIntent.getBroadcast(context, appWidgetId * 100 + 20 + i, copyIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                )
            } else {
                // Clicking an empty '+ Add' card opens the configuration screen
                views.setOnClickPendingIntent(slotIds[i], openConfigPi)
            }
        }
    }
}
