package com.altusix.slate.widgets.health

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
import com.altusix.slate.data.local.SlateWidgetConfig
import com.altusix.slate.data.local.loadSlateWidgetConfig

fun getHealthWidgetsCatalog(): List<SlateWidgetInfo> {
    return listOf(
        SlateWidgetInfo(name = "Kinetic Tri-Ring", sizeText = "2x2", category = "Health & Fitness", receiverClass = HealthKineticTriRingReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo(name = "Pedometer Chronograph", sizeText = "2x2", category = "Health & Fitness", receiverClass = HealthPedometerChronoReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Interactive Hydration Cell", sizeText = "2x2", category = "Health & Fitness", receiverClass = HealthHydrationReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Weekly Activity Matrix", sizeText = "4x2", category = "Health & Fitness", receiverClass = HealthWeeklyMatrixReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "8-Glass Hydration Matrix", sizeText = "2x2", category = "Health & Fitness", receiverClass = HealthEightGlassMatrixReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Pure Circle Hydro-Chrono", sizeText = "2x2", category = "Health & Fitness", receiverClass = HealthHydroChronoReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo(name = "Hydro Arc Droplet", sizeText = "2x2", category = "Health & Fitness", receiverClass = HealthHydroArcDropletReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo(name = "Landscape Ridge Pedometer", sizeText = "2x2", category = "Health & Fitness", receiverClass = HealthLandscapeRidgeReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Dash Chrono Pedometer", sizeText = "2x2", category = "Health & Fitness", receiverClass = HealthDashPedometerReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo(name = "Mechanical Odometer", sizeText = "3x1", category = "Health & Fitness", receiverClass = HealthOdometerReceiver::class.java, hasModeOption = true)
    )
}

fun updateAllHealthWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val receivers = listOf(
        HealthKineticTriRingReceiver::class.java,
        HealthPedometerChronoReceiver::class.java,
        HealthHydrationReceiver::class.java,
        HealthWeeklyMatrixReceiver::class.java,
        HealthEightGlassMatrixReceiver::class.java,
        HealthHydroChronoReceiver::class.java,
        HealthHydroArcDropletReceiver::class.java,
        HealthLandscapeRidgeReceiver::class.java,
        HealthDashPedometerReceiver::class.java,
        HealthOdometerReceiver::class.java
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

private fun parseAndLockIsResponsive(context: Context, widgetId: Int): Boolean {
    val widgetPrefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
    val key = "widget_${widgetId}_is_responsive"
    if (widgetPrefs.contains(key)) {
        return widgetPrefs.getBoolean(key, true)
    }
    val launcherPrefs = context.getSharedPreferences("slate_app_launcher_prefs", Context.MODE_PRIVATE)
    val defaultResp = launcherPrefs.getBoolean("default_is_responsive", true)
    widgetPrefs.edit().putBoolean(key, defaultResp).apply()
    return defaultResp
}

abstract class BaseHealthReceiver : AppWidgetProvider() {

    abstract fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        StepSensorManager.pollCurrentHardwareSteps(context) {
            for (widgetId in appWidgetIds) {
                updateWidget(context, appWidgetManager, widgetId)
            }
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int, newOptions: Bundle?) {
        updateWidget(context, appWidgetManager, widgetId)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, widgetId, newOptions)
    }

    open fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 160) ?: 160 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 160) ?: 160
        val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 160) ?: 160 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 160) ?: 160
        val wDp = if (wDpRaw <= 0) 160 else wDpRaw
        val hDp = if (hDpRaw <= 0) 160 else hDpRaw

        val isResponsive = parseAndLockIsResponsive(context, widgetId)
        val config = loadSlateWidgetConfig(context, widgetId)
        val views = RemoteViews(context.packageName, R.layout.widget_base_grid_2x2)
        val bitmap = renderBitmapForWidget(context, config, isResponsive, wDp, hDp, widgetId)
        views.setImageViewBitmap(R.id.widget_image_view, bitmap)

        val clickIntent = if (!StepSensorManager.hasPermission(context)) {
            Intent(context, HealthPermissionActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
        } else {
            val pm = context.packageManager
            pm.getLaunchIntentForPackage("com.google.android.apps.fitness")
                ?: pm.getLaunchIntentForPackage("com.sec.android.app.shealth")
                ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://fit.google.com")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        }

        val pendingIntent = PendingIntent.getActivity(context, widgetId, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.slot_0, pendingIntent)
        views.setOnClickPendingIntent(R.id.slot_1, pendingIntent)
        views.setOnClickPendingIntent(R.id.slot_2, pendingIntent)
        views.setOnClickPendingIntent(R.id.slot_3, pendingIntent)

        appWidgetManager.updateAppWidget(widgetId, views)
    }
}

// 1. KINETIC TRI-RING (2x2)
class HealthKineticTriRingReceiver : BaseHealthReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateHealthKineticTriRingBitmap(context, config, wDp, hDp, widgetId)
}

// 2. PEDOMETER CHRONOGRAPH (2x2)
class HealthPedometerChronoReceiver : BaseHealthReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateHealthPedometerChronoBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 3. INTERACTIVE HYDRATION CELL (2x2)
class HealthHydrationReceiver : BaseHealthReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            HealthStorageKeys.ACTION_ADD_WATER_250 -> {
                logSip(context, 250)
                updateAllHealthWidgets(context)
                return
            }
            HealthStorageKeys.ACTION_ADD_WATER_500 -> {
                logSip(context, 500)
                updateAllHealthWidgets(context)
                return
            }
            HealthStorageKeys.ACTION_RESET_WATER -> {
                resetHydration(context)
                updateAllHealthWidgets(context)
                return
            }
        }
        super.onReceive(context, intent)
    }

    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateHealthHydrationBitmap(context, config, isResponsive, wDp, hDp, widgetId)

    override fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 160) ?: 160 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 160) ?: 160
        val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 160) ?: 160 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 160) ?: 160
        val wDp = if (wDpRaw <= 0) 160 else wDpRaw
        val hDp = if (hDpRaw <= 0) 160 else hDpRaw

        val isResponsive = parseAndLockIsResponsive(context, widgetId)
        val config = loadSlateWidgetConfig(context, widgetId)
        val views = RemoteViews(context.packageName, R.layout.widget_base_grid_2x2)
        val bitmap = renderBitmapForWidget(context, config, isResponsive, wDp, hDp, widgetId)
        views.setImageViewBitmap(R.id.widget_image_view, bitmap)

        val pm = context.packageManager
        val fitnessIntent = pm.getLaunchIntentForPackage("com.google.android.apps.fitness")
            ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://fit.google.com")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        val openPending = PendingIntent.getActivity(context, widgetId * 10, fitnessIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.slot_0, openPending)
        views.setOnClickPendingIntent(R.id.slot_2, openPending)

        val add250Intent = Intent(context, HealthHydrationReceiver::class.java).apply { action = HealthStorageKeys.ACTION_ADD_WATER_250 }
        views.setOnClickPendingIntent(R.id.slot_1, PendingIntent.getBroadcast(context, widgetId * 10 + 1, add250Intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

        val add500Intent = Intent(context, HealthHydrationReceiver::class.java).apply { action = HealthStorageKeys.ACTION_ADD_WATER_500 }
        views.setOnClickPendingIntent(R.id.slot_3, PendingIntent.getBroadcast(context, widgetId * 10 + 3, add500Intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

        appWidgetManager.updateAppWidget(widgetId, views)
    }
}

// 4. WEEKLY ACTIVITY MATRIX (4x2)
class HealthWeeklyMatrixReceiver : BaseHealthReceiver() {

    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateHealthWeeklyMatrixBitmap(context, config, isResponsive, wDp, hDp, widgetId)

    override fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 320) ?: 320 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 320) ?: 320
        val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 160) ?: 160 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 160) ?: 160
        val wDp = if (wDpRaw <= 0) 320 else wDpRaw
        val hDp = if (hDpRaw <= 0) 160 else hDpRaw

        val isResponsive = parseAndLockIsResponsive(context, widgetId)
        val config = loadSlateWidgetConfig(context, widgetId)

        val views = RemoteViews(context.packageName, R.layout.widget_image_container)
        val bitmap = renderBitmapForWidget(context, config, isResponsive, wDp, hDp, widgetId)
        views.setImageViewBitmap(R.id.widget_image_view, bitmap)

        val clickIntent = if (!StepSensorManager.hasPermission(context)) {
            Intent(context, HealthPermissionActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
        } else {
            val pm = context.packageManager
            pm.getLaunchIntentForPackage("com.google.android.apps.fitness")
                ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://fit.google.com")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        }

        val pendingIntent = PendingIntent.getActivity(context, widgetId, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.widget_image_view, pendingIntent)

        appWidgetManager.updateAppWidget(widgetId, views)
    }
}

// 5. 8-GLASS HYDRATION MATRIX (2x2)
class HealthEightGlassMatrixReceiver : BaseHealthReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            HealthStorageKeys.ACTION_ADD_WATER_250 -> {
                logSip(context, 250)
                updateAllHealthWidgets(context)
                return
            }
            HealthStorageKeys.ACTION_SUB_WATER_250 -> {
                subHydrationMl(context, 250)
                updateAllHealthWidgets(context)
                return
            }
            HealthStorageKeys.ACTION_RESET_WATER -> {
                resetHydration(context)
                updateAllHealthWidgets(context)
                return
            }
        }
        super.onReceive(context, intent)
    }

    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateHealthEightGlassMatrixBitmap(context, config, isResponsive, wDp, hDp, widgetId)

    override fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 160) ?: 160 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 160) ?: 160
        val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 160) ?: 160 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 160) ?: 160
        val wDp = if (wDpRaw <= 0) 160 else wDpRaw
        val hDp = if (hDpRaw <= 0) 160 else hDpRaw

        val isResponsive = parseAndLockIsResponsive(context, widgetId)
        val config = loadSlateWidgetConfig(context, widgetId)
        val views = RemoteViews(context.packageName, R.layout.widget_base_grid_2x2)
        val bitmap = renderBitmapForWidget(context, config, isResponsive, wDp, hDp, widgetId)
        views.setImageViewBitmap(R.id.widget_image_view, bitmap)

        val pm = context.packageManager
        val fitnessIntent = pm.getLaunchIntentForPackage("com.google.android.apps.fitness")
            ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://fit.google.com")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        val openPending = PendingIntent.getActivity(context, widgetId * 10, fitnessIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.slot_0, openPending)
        views.setOnClickPendingIntent(R.id.slot_1, openPending)

        val sub250Intent = Intent(context, HealthEightGlassMatrixReceiver::class.java).apply { action = HealthStorageKeys.ACTION_SUB_WATER_250 }
        views.setOnClickPendingIntent(R.id.slot_2, PendingIntent.getBroadcast(context, widgetId * 10 + 2, sub250Intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

        val add250Intent = Intent(context, HealthEightGlassMatrixReceiver::class.java).apply { action = HealthStorageKeys.ACTION_ADD_WATER_250 }
        views.setOnClickPendingIntent(R.id.slot_3, PendingIntent.getBroadcast(context, widgetId * 10 + 3, add250Intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

        appWidgetManager.updateAppWidget(widgetId, views)
    }
}

// 6. PURE CIRCLE HYDRO-CHRONO (2x2)
class HealthHydroChronoReceiver : BaseHealthReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == HealthStorageKeys.ACTION_SIP_CHRONO) {
            logSip(context, 250)
            updateAllHealthWidgets(context)
            return
        }
        super.onReceive(context, intent)
    }

    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateHealthHydroChronoBitmap(context, config, wDp, hDp, widgetId)

    override fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 160) ?: 160 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 160) ?: 160
        val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 160) ?: 160 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 160) ?: 160
        val wDp = if (wDpRaw <= 0) 160 else wDpRaw
        val hDp = if (hDpRaw <= 0) 160 else hDpRaw

        val config = loadSlateWidgetConfig(context, widgetId)
        val views = RemoteViews(context.packageName, R.layout.widget_base_grid_2x2)
        val bitmap = renderBitmapForWidget(context, config, false, wDp, hDp, widgetId)
        views.setImageViewBitmap(R.id.widget_image_view, bitmap)

        val sipIntent = Intent(context, HealthHydroChronoReceiver::class.java).apply {
            action = HealthStorageKeys.ACTION_SIP_CHRONO
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            widgetId * 300,
            sipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        views.setOnClickPendingIntent(R.id.slot_0, pendingIntent)
        views.setOnClickPendingIntent(R.id.slot_1, pendingIntent)
        views.setOnClickPendingIntent(R.id.slot_2, pendingIntent)
        views.setOnClickPendingIntent(R.id.slot_3, pendingIntent)

        appWidgetManager.updateAppWidget(widgetId, views)
    }
}

// 7. HYDRO ARC DROPLET (2x2)
class HealthHydroArcDropletReceiver : BaseHealthReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == HealthStorageKeys.ACTION_SIP_CHRONO) {
            logSip(context, 250)
            updateAllHealthWidgets(context)
            return
        }
        super.onReceive(context, intent)
    }

    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateHealthHydroArcDropletBitmap(context, config, wDp, hDp, widgetId)

    override fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 160) ?: 160 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 160) ?: 160
        val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 160) ?: 160 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 160) ?: 160
        val wDp = if (wDpRaw <= 0) 160 else wDpRaw
        val hDp = if (hDpRaw <= 0) 160 else hDpRaw

        val config = loadSlateWidgetConfig(context, widgetId)
        val views = RemoteViews(context.packageName, R.layout.widget_base_grid_2x2)
        val bitmap = renderBitmapForWidget(context, config, false, wDp, hDp, widgetId)
        views.setImageViewBitmap(R.id.widget_image_view, bitmap)

        val sipIntent = Intent(context, HealthHydroArcDropletReceiver::class.java).apply {
            action = HealthStorageKeys.ACTION_SIP_CHRONO
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            widgetId * 350,
            sipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        views.setOnClickPendingIntent(R.id.slot_0, pendingIntent)
        views.setOnClickPendingIntent(R.id.slot_1, pendingIntent)
        views.setOnClickPendingIntent(R.id.slot_2, pendingIntent)
        views.setOnClickPendingIntent(R.id.slot_3, pendingIntent)

        appWidgetManager.updateAppWidget(widgetId, views)
    }
}

// 8. LANDSCAPE RIDGE PEDOMETER (2x2)
class HealthLandscapeRidgeReceiver : BaseHealthReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateHealthLandscapeRidgeBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 9. DASH CHRONO PEDOMETER (2x2)
class HealthDashPedometerReceiver : BaseHealthReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateHealthDashPedometerBitmap(context, config, wDp, hDp, widgetId)
}

// 10. MECHANICAL ODOMETER PEDOMETER (3x1)
class HealthOdometerReceiver : BaseHealthReceiver() {

    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateHealthOdometerBitmap(context, config, isResponsive, wDp, hDp, widgetId)

    override fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 250) ?: 250 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250) ?: 250
        val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80) ?: 80 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 80) ?: 80
        val wDp = if (wDpRaw <= 0) 250 else wDpRaw
        val hDp = if (hDpRaw <= 0) 80 else hDpRaw

        val isResponsive = parseAndLockIsResponsive(context, widgetId)
        val config = loadSlateWidgetConfig(context, widgetId)

        val views = RemoteViews(context.packageName, R.layout.widget_image_container)
        val bitmap = renderBitmapForWidget(context, config, isResponsive, wDp, hDp, widgetId)
        views.setImageViewBitmap(R.id.widget_image_view, bitmap)

        val clickIntent = if (!StepSensorManager.hasPermission(context)) {
            Intent(context, HealthPermissionActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
        } else {
            val pm = context.packageManager
            pm.getLaunchIntentForPackage("com.google.android.apps.fitness")
                ?: pm.getLaunchIntentForPackage("com.sec.android.app.shealth")
                ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://fit.google.com")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        }

        val pendingIntent = PendingIntent.getActivity(context, widgetId, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.widget_image_view, pendingIntent)

        appWidgetManager.updateAppWidget(widgetId, views)
    }
}
