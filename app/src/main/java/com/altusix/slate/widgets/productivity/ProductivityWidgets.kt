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
// 8. MINIMAL TASK PIPELINE (4x1)
// =========================================================================

class ProductivityPipelineReceiver : BaseProductivityReceiver(R.layout.widget_productivity_card_layout, targetAspect = 4.0f) {
    override val defaultTab: String = "PIPELINE"

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
}

// =========================================================================
// 9. BOOKMARK LIST (4x2)
// =========================================================================

class ProductivityBookmarksReceiver : BaseProductivityReceiver(R.layout.widget_productivity_bookmarks_layout, targetAspect = 2.0f) {
    override val defaultTab: String = "BOOKMARKS"

    override fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        val prodConfig = ProductivityStorageManager.getConfig(context, appWidgetId)
        return generateBookmarkListBitmap(context, prodConfig.bookmarks, config, isResponsive, wDp, hDp)
    }

    override fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        val openIntent = Intent(context, ProductivityConfigActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(EXTRA_TAB, "BOOKMARKS")
            data = Uri.parse("slate_prod://$appWidgetId/bookmarks_edit")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPi = PendingIntent.getActivity(
            context,
            appWidgetId * 100 + 1,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_bookmark_header, openPi)

        val prodConfig = ProductivityStorageManager.getConfig(context, appWidgetId)
        val rowIds = listOf(R.id.btn_bookmark_0, R.id.btn_bookmark_1, R.id.btn_bookmark_2, R.id.btn_bookmark_3)
        for (i in rowIds.indices) {
            val item = prodConfig.bookmarks.getOrNull(i)
            val targetPi = if (item != null && item.url.isNotBlank()) {
                val urlToLaunch = if (!item.url.startsWith("http://") && !item.url.startsWith("https://")) {
                    "https://${item.url}"
                } else item.url
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(urlToLaunch)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                PendingIntent.getActivity(
                    context,
                    appWidgetId * 100 + 20 + i,
                    browserIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                openPi
            }
            views.setOnClickPendingIntent(rowIds[i], targetPi)
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
// 11. CLIPBOARD VAULT (2x2)
// =========================================================================

class ProductivityClipboardReceiver : BaseProductivityReceiver(R.layout.widget_productivity_clipboard_layout, targetAspect = 1.0f) {
    override val defaultTab: String = "CLIPBOARD"

    override fun renderWidgetBitmap(
        context: Context,
        appWidgetId: Int,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int
    ): Bitmap {
        val prodConfig = ProductivityStorageManager.getConfig(context, appWidgetId)
        return generateClipboardVaultBitmap(context, prodConfig.clipboardSnippets, config, isResponsive, wDp, hDp)
    }

    override fun setupTouchTargets(context: Context, views: RemoteViews, appWidgetId: Int) {
        val openIntent = Intent(context, ProductivityConfigActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(EXTRA_TAB, "CLIPBOARD")
            data = Uri.parse("slate_prod://$appWidgetId/clipboard_edit")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPi = PendingIntent.getActivity(
            context,
            appWidgetId * 100 + 1,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_clip_header, openPi)

        val clipTargets = listOf(R.id.btn_clip_0, R.id.btn_clip_1)
        for (i in clipTargets.indices) {
            val copyIntent = Intent(context, this.javaClass).apply {
                action = ACTION_COPY_CLIPBOARD
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(EXTRA_INDEX, i)
                data = Uri.parse("slate_prod://$appWidgetId/clip_copy_$i")
            }
            val copyPi = PendingIntent.getBroadcast(
                context,
                appWidgetId * 100 + 30 + i,
                copyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(clipTargets[i], copyPi)
        }
    }
}
