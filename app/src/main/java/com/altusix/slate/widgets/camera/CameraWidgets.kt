package com.altusix.slate.widgets.camera

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import android.widget.RemoteViews
import com.altusix.slate.R
import com.altusix.slate.core.model.SlateWidgetInfo
import com.altusix.slate.data.local.SlateWidgetConfig

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

class CameraPhotoFrameReceiver : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            updatePhotoWidget(context, appWidgetManager, widgetId)
        }
    }

    companion object {
        fun updatePhotoWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            val cameraConfig = CameraWidgetPreferences.loadConfig(context, widgetId)

            val baseConfig = SlateWidgetConfig(themeMode = "DARK", backgroundColorHex = 0xFF121214L, opacity = 1.0f)

            val bitmap = generatePhotoFrameWidgetBitmap(
                context = context,
                config = baseConfig,
                cameraConfig = cameraConfig,
                wDp = 200,
                hDp = 200
            )

            val views = RemoteViews(context.packageName, R.layout.widget_image_container)
            views.setImageViewBitmap(R.id.widget_image_view, bitmap)

            val clickIntent = when (cameraConfig.clickAction) {
                PhotoClickAction.OPEN_CAMERA -> Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                PhotoClickAction.OPEN_GALLERY -> Intent(Intent.ACTION_VIEW).apply { type = "image/*" }
                PhotoClickAction.OPEN_SETTINGS -> createConfigIntent(context, widgetId)
                PhotoClickAction.NOTHING -> Intent()
            }

            val finalIntent = if (cameraConfig.photoUri.isNullOrEmpty()) {
                createConfigIntent(context, widgetId)
            } else {
                clickIntent
            }

            if (cameraConfig.clickAction != PhotoClickAction.NOTHING || cameraConfig.photoUri.isNullOrEmpty()) {
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    widgetId,
                    finalIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_image_view, pendingIntent)
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