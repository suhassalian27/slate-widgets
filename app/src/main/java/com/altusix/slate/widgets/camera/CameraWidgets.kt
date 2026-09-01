package com.altusix.slate.widgets.camera

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.RemoteViews
import com.altusix.slate.R
import com.altusix.slate.core.model.SlateWidgetInfo
import com.altusix.slate.core.theme.ThemePreferences
import com.altusix.slate.data.local.SlateWidgetConfig

fun getCameraWidgetsCatalog(): List<SlateWidgetInfo> {
    return listOf(
        SlateWidgetInfo(name = "Stacked Photo Frame", sizeText = "2x2", category = "Camera", receiverClass = CameraPhotoFrameStackedReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo(name = "Taped Polaroid Frame", sizeText = "2x2", category = "Camera", receiverClass = CameraPhotoFrameTapedReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo(name = "Push Pin Polaroid Frame", sizeText = "2x2", category = "Camera", receiverClass = CameraPhotoFramePushPinReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo(name = "Camera Shutter Launcher", sizeText = "2x2", category = "Camera", receiverClass = CameraShutterLauncherReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Wide Photo Frame", sizeText = "4x2", category = "Camera", receiverClass = CameraPhotoFrame4x2Receiver::class.java, hasModeOption = false),
        SlateWidgetInfo(name = "Photo Frame & Gallery", sizeText = "2x2", category = "Camera", receiverClass = CameraPhotoFrameReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Circle Photo Frame", sizeText = "2x2", category = "Camera", receiverClass = CameraPhotoFrameCircleReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo(name = "Organic Blob Photo Frame", sizeText = "2x2", category = "Camera", receiverClass = CameraPhotoFrameBlobReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo(name = "Fluid Blob Photo Frame", sizeText = "2x2", category = "Camera", receiverClass = CameraPhotoFrameFluidBlobReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo(name = "Aperture Lens Capsule", sizeText = "2x1", category = "Camera", receiverClass = CameraAperturePillReceiver::class.java, hasModeOption = true)
    )
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

private fun createConfigIntent(context: Context, widgetId: Int): Intent {
    return Intent(context, CameraWidgetConfigActivity::class.java).apply {
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
}

private fun getPhotoPendingIntent(context: Context, widgetId: Int, cameraConfig: CameraWidgetConfig): PendingIntent? {
    if (cameraConfig.photoUri.isNullOrEmpty()) {
        return PendingIntent.getActivity(
            context,
            widgetId,
            createConfigIntent(context, widgetId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    return when (cameraConfig.clickAction) {
        PhotoClickAction.OPEN_GALLERY -> {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                type = "image/*"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            PendingIntent.getActivity(context, widgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        PhotoClickAction.OPEN_CAMERA -> {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            PendingIntent.getActivity(context, widgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        PhotoClickAction.OPEN_SETTINGS -> {
            PendingIntent.getActivity(context, widgetId, createConfigIntent(context, widgetId), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        PhotoClickAction.NOTHING -> null
    }
}

fun updateAllCameraWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val receivers = listOf(
        CameraPhotoFrameReceiver::class.java,
        CameraPhotoFrame4x2Receiver::class.java,
        CameraPhotoFrameCircleReceiver::class.java,
        CameraPhotoFrameBlobReceiver::class.java,
        CameraPhotoFrameFluidBlobReceiver::class.java,
        CameraPhotoFrameStackedReceiver::class.java,
        CameraPhotoFrameTapedReceiver::class.java,
        CameraPhotoFramePushPinReceiver::class.java,
        CameraShutterLauncherReceiver::class.java,
        CameraAperturePillReceiver::class.java
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

fun updateCameraWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
    val info = appWidgetManager.getAppWidgetInfo(widgetId)
    val providerClass = info?.provider?.className ?: ""

    when {
        providerClass.contains("4x2") -> CameraPhotoFrame4x2Receiver.updatePhotoWidget(context, appWidgetManager, widgetId)
        providerClass.contains("Circle") -> CameraPhotoFrameCircleReceiver.updatePhotoWidget(context, appWidgetManager, widgetId)
        providerClass.contains("FluidBlob") -> CameraPhotoFrameFluidBlobReceiver.updatePhotoWidget(context, appWidgetManager, widgetId)
        providerClass.contains("Blob") -> CameraPhotoFrameBlobReceiver.updatePhotoWidget(context, appWidgetManager, widgetId)
        providerClass.contains("Stacked") -> CameraPhotoFrameStackedReceiver.updatePhotoWidget(context, appWidgetManager, widgetId)
        providerClass.contains("Taped") -> CameraPhotoFrameTapedReceiver.updatePhotoWidget(context, appWidgetManager, widgetId)
        providerClass.contains("PushPin") -> CameraPhotoFramePushPinReceiver.updatePhotoWidget(context, appWidgetManager, widgetId)
        providerClass.contains("ShutterLauncher") -> CameraShutterLauncherReceiver.updatePhotoWidget(context, appWidgetManager, widgetId)
        providerClass.contains("AperturePill") -> CameraAperturePillReceiver.updatePhotoWidget(context, appWidgetManager, widgetId)
        else -> CameraPhotoFrameReceiver.updatePhotoWidget(context, appWidgetManager, widgetId)
    }
}

// 1. WIDE PHOTO FRAME (4x2 / Fixed Aspect Photo Display)
class CameraPhotoFrame4x2Receiver : AppWidgetProvider() {

    fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val cameraConfig = CameraWidgetPreferences.loadConfig(context, -1)
        return generatePhotoFrame4x2Bitmap(context, config, cameraConfig, wDp, hDp)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) { updatePhotoWidget(context, appWidgetManager, widgetId) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle?) {
        updatePhotoWidget(context, appWidgetManager, appWidgetId)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    companion object {
        fun updatePhotoWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            val cameraConfig = CameraWidgetPreferences.loadConfig(context, widgetId)
            val options = appWidgetManager.getAppWidgetOptions(widgetId)
            val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 320) ?: 320 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 320) ?: 320
            val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 160) ?: 160 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 160) ?: 160
            val wDp = if (wDpRaw <= 0) 320 else wDpRaw
            val hDp = if (hDpRaw <= 0) 160 else hDpRaw

            val config = loadSlateWidgetConfig(context, widgetId)
            val bitmap = generatePhotoFrame4x2Bitmap(context, config, cameraConfig, wDp, hDp)

            val views = RemoteViews(context.packageName, R.layout.widget_image_container)
            views.setImageViewBitmap(R.id.widget_image_view, bitmap)

            val pendingIntent = getPhotoPendingIntent(context, widgetId, cameraConfig)
            if (pendingIntent != null) {
                views.setOnClickPendingIntent(R.id.widget_image_view, pendingIntent)
            } else {
                val dummyIntent = PendingIntent.getBroadcast(context, widgetId, Intent(), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.widget_image_view, dummyIntent)
            }

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}

// 2. PHOTO FRAME & GALLERY (2x2 / Responsive & Fixed Aspect Photo Display)
class CameraPhotoFrameReceiver : AppWidgetProvider() {

    fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val cameraConfig = CameraWidgetPreferences.loadConfig(context, -1)
        return generatePhotoFrameCameraBitmap(context, config, cameraConfig, false, wDp, hDp)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) { updatePhotoWidget(context, appWidgetManager, widgetId) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle?) {
        updatePhotoWidget(context, appWidgetManager, appWidgetId)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    companion object {
        fun updatePhotoWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            val cameraConfig = CameraWidgetPreferences.loadConfig(context, widgetId)
            val options = appWidgetManager.getAppWidgetOptions(widgetId)
            val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 200) ?: 200
            val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 200) ?: 200
            val wDp = if (wDpRaw <= 0) 200 else wDpRaw
            val hDp = if (hDpRaw <= 0) 200 else hDpRaw

            val isResponsive = parseAndLockIsResponsive(context, widgetId)
            val config = loadSlateWidgetConfig(context, widgetId)
            val bitmap = generatePhotoFrameCameraBitmap(context, config, cameraConfig, isResponsive, wDp, hDp)

            val views = RemoteViews(context.packageName, R.layout.widget_image_container)
            views.setImageViewBitmap(R.id.widget_image_view, bitmap)

            val pendingIntent = getPhotoPendingIntent(context, widgetId, cameraConfig)
            if (pendingIntent != null) {
                views.setOnClickPendingIntent(R.id.widget_image_view, pendingIntent)
            } else {
                val dummyIntent = PendingIntent.getBroadcast(context, widgetId, Intent(), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.widget_image_view, dummyIntent)
            }

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}

// 3. CIRCLE PHOTO FRAME (2x2 / Circular Aspect Display)
class CameraPhotoFrameCircleReceiver : AppWidgetProvider() {

    fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val cameraConfig = CameraWidgetPreferences.loadConfig(context, -1)
        return generatePhotoFrameCircleBitmap(context, config, cameraConfig, wDp, hDp)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) { updatePhotoWidget(context, appWidgetManager, widgetId) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle?) {
        updatePhotoWidget(context, appWidgetManager, appWidgetId)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    companion object {
        fun updatePhotoWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            val cameraConfig = CameraWidgetPreferences.loadConfig(context, widgetId)
            val options = appWidgetManager.getAppWidgetOptions(widgetId)
            val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 200) ?: 200
            val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 200) ?: 200
            val wDp = if (wDpRaw <= 0) 200 else wDpRaw
            val hDp = if (hDpRaw <= 0) 200 else hDpRaw

            val config = loadSlateWidgetConfig(context, widgetId)
            val bitmap = generatePhotoFrameCircleBitmap(context, config, cameraConfig, wDp, hDp)

            val views = RemoteViews(context.packageName, R.layout.widget_image_container)
            views.setImageViewBitmap(R.id.widget_image_view, bitmap)

            val pendingIntent = getPhotoPendingIntent(context, widgetId, cameraConfig)
            if (pendingIntent != null) {
                views.setOnClickPendingIntent(R.id.widget_image_view, pendingIntent)
            } else {
                val dummyIntent = PendingIntent.getBroadcast(context, widgetId, Intent(), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.widget_image_view, dummyIntent)
            }

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}

// 4. ORGANIC BLOB PHOTO FRAME (2x2 / Asymmetric Pebble Display)
class CameraPhotoFrameBlobReceiver : AppWidgetProvider() {

    fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val cameraConfig = CameraWidgetPreferences.loadConfig(context, -1)
        return generatePhotoFrameBlobCameraBitmap(context, config, cameraConfig, wDp, hDp)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) { updatePhotoWidget(context, appWidgetManager, widgetId) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle?) {
        updatePhotoWidget(context, appWidgetManager, appWidgetId)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    companion object {
        fun updatePhotoWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            val cameraConfig = CameraWidgetPreferences.loadConfig(context, widgetId)
            val options = appWidgetManager.getAppWidgetOptions(widgetId)
            val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 200) ?: 200
            val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 200) ?: 200
            val wDp = if (wDpRaw <= 0) 200 else wDpRaw
            val hDp = if (hDpRaw <= 0) 200 else hDpRaw

            val config = loadSlateWidgetConfig(context, widgetId)
            val bitmap = generatePhotoFrameBlobCameraBitmap(context, config, cameraConfig, wDp, hDp)

            val views = RemoteViews(context.packageName, R.layout.widget_image_container)
            views.setImageViewBitmap(R.id.widget_image_view, bitmap)

            val pendingIntent = getPhotoPendingIntent(context, widgetId, cameraConfig)
            if (pendingIntent != null) {
                views.setOnClickPendingIntent(R.id.widget_image_view, pendingIntent)
            } else {
                val dummyIntent = PendingIntent.getBroadcast(context, widgetId, Intent(), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.widget_image_view, dummyIntent)
            }

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}

// 5. FLUID BLOB PHOTO FRAME (2x2 / Organic Wave Display)
class CameraPhotoFrameFluidBlobReceiver : AppWidgetProvider() {

    fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val cameraConfig = CameraWidgetPreferences.loadConfig(context, -1)
        return generatePhotoFrameFluidBlobCameraBitmap(context, config, cameraConfig, wDp, hDp)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) { updatePhotoWidget(context, appWidgetManager, widgetId) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle?) {
        updatePhotoWidget(context, appWidgetManager, appWidgetId)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    companion object {
        fun updatePhotoWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            val cameraConfig = CameraWidgetPreferences.loadConfig(context, widgetId)
            val options = appWidgetManager.getAppWidgetOptions(widgetId)
            val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 200) ?: 200
            val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 200) ?: 200
            val wDp = if (wDpRaw <= 0) 200 else wDpRaw
            val hDp = if (hDpRaw <= 0) 200 else hDpRaw

            val config = loadSlateWidgetConfig(context, widgetId)
            val bitmap = generatePhotoFrameFluidBlobCameraBitmap(context, config, cameraConfig, wDp, hDp)

            val views = RemoteViews(context.packageName, R.layout.widget_image_container)
            views.setImageViewBitmap(R.id.widget_image_view, bitmap)

            val pendingIntent = getPhotoPendingIntent(context, widgetId, cameraConfig)
            if (pendingIntent != null) {
                views.setOnClickPendingIntent(R.id.widget_image_view, pendingIntent)
            } else {
                val dummyIntent = PendingIntent.getBroadcast(context, widgetId, Intent(), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.widget_image_view, dummyIntent)
            }

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}

// 6. STACKED PHOTO FRAME (2x2 / Layered Polaroid Stack Display)
class CameraPhotoFrameStackedReceiver : AppWidgetProvider() {

    fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val cameraConfig = CameraWidgetPreferences.loadConfig(context, -1)
        return generatePhotoFrameStackedCameraBitmap(context, config, cameraConfig, wDp, hDp)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) { updatePhotoWidget(context, appWidgetManager, widgetId) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle?) {
        updatePhotoWidget(context, appWidgetManager, appWidgetId)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    companion object {
        fun updatePhotoWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            val cameraConfig = CameraWidgetPreferences.loadConfig(context, widgetId)
            val options = appWidgetManager.getAppWidgetOptions(widgetId)
            val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 200) ?: 200
            val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 200) ?: 200
            val wDp = if (wDpRaw <= 0) 200 else wDpRaw
            val hDp = if (hDpRaw <= 0) 200 else hDpRaw

            val config = loadSlateWidgetConfig(context, widgetId)
            val bitmap = generatePhotoFrameStackedCameraBitmap(context, config, cameraConfig, wDp, hDp)

            val views = RemoteViews(context.packageName, R.layout.widget_image_container)
            views.setImageViewBitmap(R.id.widget_image_view, bitmap)

            val pendingIntent = getPhotoPendingIntent(context, widgetId, cameraConfig)
            if (pendingIntent != null) {
                views.setOnClickPendingIntent(R.id.widget_image_view, pendingIntent)
            } else {
                val dummyIntent = PendingIntent.getBroadcast(context, widgetId, Intent(), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.widget_image_view, dummyIntent)
            }

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}

// 7. TAPED POLAROID PHOTO FRAME (2x2 / Masking Tape Mounted Display)
class CameraPhotoFrameTapedReceiver : AppWidgetProvider() {

    fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val cameraConfig = CameraWidgetPreferences.loadConfig(context, -1)
        return generatePhotoFrameTapedCameraBitmap(context, config, cameraConfig, wDp, hDp)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) { updatePhotoWidget(context, appWidgetManager, widgetId) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle?) {
        updatePhotoWidget(context, appWidgetManager, appWidgetId)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    companion object {
        fun updatePhotoWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            val cameraConfig = CameraWidgetPreferences.loadConfig(context, widgetId)
            val options = appWidgetManager.getAppWidgetOptions(widgetId)
            val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 200) ?: 200
            val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 200) ?: 200
            val wDp = if (wDpRaw <= 0) 200 else wDpRaw
            val hDp = if (hDpRaw <= 0) 200 else hDpRaw

            val config = loadSlateWidgetConfig(context, widgetId)
            val bitmap = generatePhotoFrameTapedCameraBitmap(context, config, cameraConfig, wDp, hDp)

            val views = RemoteViews(context.packageName, R.layout.widget_image_container)
            views.setImageViewBitmap(R.id.widget_image_view, bitmap)

            val pendingIntent = getPhotoPendingIntent(context, widgetId, cameraConfig)
            if (pendingIntent != null) {
                views.setOnClickPendingIntent(R.id.widget_image_view, pendingIntent)
            } else {
                val dummyIntent = PendingIntent.getBroadcast(context, widgetId, Intent(), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.widget_image_view, dummyIntent)
            }

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}

// 8. PUSH PIN POLAROID PHOTO FRAME (2x2 / Red Thumbtack Mounted Display)
class CameraPhotoFramePushPinReceiver : AppWidgetProvider() {

    fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        val cameraConfig = CameraWidgetPreferences.loadConfig(context, -1)
        return generatePhotoFramePushPinCameraBitmap(context, config, cameraConfig, wDp, hDp)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) { updatePhotoWidget(context, appWidgetManager, widgetId) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle?) {
        updatePhotoWidget(context, appWidgetManager, appWidgetId)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    companion object {
        fun updatePhotoWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            val cameraConfig = CameraWidgetPreferences.loadConfig(context, widgetId)
            val options = appWidgetManager.getAppWidgetOptions(widgetId)
            val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 200) ?: 200
            val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 200) ?: 200
            val wDp = if (wDpRaw <= 0) 200 else wDpRaw
            val hDp = if (hDpRaw <= 0) 200 else hDpRaw

            val config = loadSlateWidgetConfig(context, widgetId)
            val bitmap = generatePhotoFramePushPinCameraBitmap(context, config, cameraConfig, wDp, hDp)

            val views = RemoteViews(context.packageName, R.layout.widget_image_container)
            views.setImageViewBitmap(R.id.widget_image_view, bitmap)

            val pendingIntent = getPhotoPendingIntent(context, widgetId, cameraConfig)
            if (pendingIntent != null) {
                views.setOnClickPendingIntent(R.id.widget_image_view, pendingIntent)
            } else {
                val dummyIntent = PendingIntent.getBroadcast(context, widgetId, Intent(), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.widget_image_view, dummyIntent)
            }

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}

// 9. SHUTTER LAUNCHER (2x2 / Minimal Camera Trigger Display)
class CameraShutterLauncherReceiver : AppWidgetProvider() {

    fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        return generateCameraShutterLauncherBitmap(context, config, false, wDp, hDp)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) { updatePhotoWidget(context, appWidgetManager, widgetId) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle?) {
        updatePhotoWidget(context, appWidgetManager, appWidgetId)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    companion object {
        fun updatePhotoWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            val options = appWidgetManager.getAppWidgetOptions(widgetId)
            val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 200) ?: 200
            val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 200) ?: 200
            val wDp = if (wDpRaw <= 0) 200 else wDpRaw
            val hDp = if (hDpRaw <= 0) 200 else hDpRaw

            val isResponsive = parseAndLockIsResponsive(context, widgetId)
            val config = loadSlateWidgetConfig(context, widgetId)
            val bitmap = generateCameraShutterLauncherBitmap(context, config, isResponsive, wDp, hDp)

            val views = RemoteViews(context.packageName, R.layout.widget_image_container)
            views.setImageViewBitmap(R.id.widget_image_view, bitmap)

            val cameraIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            val pendingIntent = PendingIntent.getActivity(context, widgetId, cameraIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_image_view, pendingIntent)

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}

// 10. APERTURE LENS CAPSULE (2x1 / Sleek Dual-Action Camera Pill)
class CameraAperturePillReceiver : AppWidgetProvider() {

    fun renderBitmap(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int): Bitmap {
        return generateCameraAperturePillBitmap(context, config, false, wDp, hDp)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) { updatePhotoWidget(context, appWidgetManager, widgetId) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle?) {
        updatePhotoWidget(context, appWidgetManager, appWidgetId)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    companion object {
        fun updatePhotoWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            val options = appWidgetManager.getAppWidgetOptions(widgetId)
            val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 240) ?: 240 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 240) ?: 240
            val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 120) ?: 120 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 120) ?: 120
            val wDp = if (wDpRaw <= 0) 240 else wDpRaw
            val hDp = if (hDpRaw <= 0) 120 else hDpRaw

            val isResponsive = parseAndLockIsResponsive(context, widgetId)
            val config = loadSlateWidgetConfig(context, widgetId)
            val bitmap = generateCameraAperturePillBitmap(context, config, isResponsive, wDp, hDp)

            val views = RemoteViews(context.packageName, R.layout.widget_image_container)
            views.setImageViewBitmap(R.id.widget_image_view, bitmap)

            val cameraIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            val pendingIntent = PendingIntent.getActivity(context, widgetId, cameraIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_image_view, pendingIntent)

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}