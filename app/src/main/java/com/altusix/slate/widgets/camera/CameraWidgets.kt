package com.altusix.slate.widgets.camera

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import android.widget.RemoteViews
import com.altusix.slate.R
import com.altusix.slate.core.model.SlateWidgetInfo
import com.altusix.slate.data.local.SlateWidgetConfig

// 1. PHOTO FRAME & GALLERY (2x2 / Responsive & Fixed Aspect Photo Display)
fun getCameraWidgetsCatalog(): List<SlateWidgetInfo> {
    return listOf(
        SlateWidgetInfo(
            name = "Photo Frame & Gallery",
            sizeText = "2x2",
            category = "Camera",
            receiverClass = CameraPhotoFrameReceiver::class.java,
            hasModeOption = true
        )
    )
}

fun updateAllCameraWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val receivers = listOf(
        CameraPhotoFrameReceiver::class.java
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

// 1. PHOTO FRAME & GALLERY (2x2 / Responsive & Fixed Aspect Photo Display)
class CameraPhotoFrameReceiver : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            updatePhotoWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle?
    ) {
        updatePhotoWidget(context, appWidgetManager, appWidgetId)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    companion object {
        fun updatePhotoWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            val cameraConfig = CameraWidgetPreferences.loadConfig(context, widgetId)

            val options = appWidgetManager.getAppWidgetOptions(widgetId)
            val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val wDp = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) ?: 200
            val hDp = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT) ?: 200

            val isResponsive = parseAndLockIsResponsive(context, widgetId)

            val widgetPrefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
            val themeMode = widgetPrefs.getString("widget_${widgetId}_theme_mode", "DARK") ?: "DARK"
            val bgColor = widgetPrefs.getLong("widget_${widgetId}_bg_color", 0xFF121214L)
            val opacity = widgetPrefs.getFloat("widget_${widgetId}_opacity", 1.0f)
            val accentColor = widgetPrefs.getLong("widget_${widgetId}_accent_color", 0xFFFFFFFFL)

            val baseConfig = SlateWidgetConfig(themeMode, bgColor, opacity, accentColor)

            val bitmap = generatePhotoFrameCameraBitmap(context, baseConfig, cameraConfig, isResponsive, wDp, hDp)

            val views = RemoteViews(context.packageName, R.layout.widget_image_container)
            views.setImageViewBitmap(R.id.widget_image_view, bitmap)

            // STRICT INTENT ROUTING
            if (cameraConfig.photoUri.isNullOrEmpty()) {
                // Always force settings if empty
                val configIntent = PendingIntent.getActivity(context, widgetId, createConfigIntent(context, widgetId), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.widget_image_view, configIntent)
            } else {
                val pendingIntent: PendingIntent? = when (cameraConfig.clickAction) {
                    PhotoClickAction.OPEN_GALLERY -> {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            type = "image/*"
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        PendingIntent.getActivity(context, widgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    }
                    PhotoClickAction.OPEN_CAMERA -> {
                        val intent = Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        PendingIntent.getActivity(context, widgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    }
                    PhotoClickAction.OPEN_SETTINGS -> {
                        PendingIntent.getActivity(context, widgetId, createConfigIntent(context, widgetId), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    }
                    PhotoClickAction.NOTHING -> null
                }

                if (pendingIntent != null) {
                    views.setOnClickPendingIntent(R.id.widget_image_view, pendingIntent)
                } else {
                    // Feed a dead broadcast intent to disable any previously cached click actions
                    val dummyIntent = PendingIntent.getBroadcast(context, widgetId, Intent(), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    views.setOnClickPendingIntent(R.id.widget_image_view, dummyIntent)
                }
            }

            appWidgetManager.updateAppWidget(widgetId, views)
        }

        private fun createConfigIntent(context: Context, widgetId: Int): Intent {
            return Intent(context, CameraWidgetConfigActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }
    }
}