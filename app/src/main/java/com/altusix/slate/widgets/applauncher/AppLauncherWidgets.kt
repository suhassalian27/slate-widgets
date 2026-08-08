package com.altusix.slate.widgets.applauncher

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import com.altusix.slate.core.model.SlateWidgetInfo
import com.altusix.slate.core.receiver.BaseCanvasWidgetProvider
import com.altusix.slate.data.local.SlateWidgetConfig

abstract class BaseAppLauncherReceiver : BaseCanvasWidgetProvider() {

    override fun getClickPendingIntent(context: Context, appWidgetId: Int): PendingIntent? {
        val config = AppLauncherWidgetConfig.load(context, appWidgetId)

        if (config.packageName.isEmpty()) {
            val configIntent = Intent(context, AppLauncherConfigActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(
                context,
                appWidgetId,
                configIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(config.packageName) ?: return null
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(
            context,
            appWidgetId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

fun getAppLauncherWidgetsCatalog(): List<SlateWidgetInfo> {
    return listOf(
        SlateWidgetInfo("App Launcher Adaptive", "1x1", "App Launcher", AdaptiveAppLauncherReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("App Launcher Rectangle", "2x1", "App Launcher", CustomTextAppLauncherReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo("App Launcher - Squircle", "1x1", "App Launcher", SquircleLauncherReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("App Launcher - M3 Pentagon", "1x1", "App Launcher", PentagonLauncherReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("App Launcher - M3 Flower", "1x1", "App Launcher", FlowerLauncherReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("App Launcher - M3 Clover", "1x1", "App Launcher", CloverLauncherReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("App Launcher - M3 Diamond", "1x1", "App Launcher", DiamondLauncherReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("App Launcher - M3 Octagon", "1x1", "App Launcher", OctagonLauncherReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("App Launcher - Circle", "1x1", "App Launcher", CircleLauncherReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("App Launcher - Blob", "1x1", "App Launcher", BlobLauncherReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("AppLauncher - Pixel Star", "1x1", "App Launcher", PixelStarLauncherReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Cyber Glitch Launcher", "2x1", "App Launcher", GlitchTextLauncherReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Neon Halo Launcher", "1x1", "App Launcher", NeonRingLauncherReceiver::class.java, hasModeOption = false)
    )
}

// 1. Adaptive Launcher (Supports Responsive vs Fixed)
class AdaptiveAppLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId).copy(shape = LauncherShape.SQUIRCLE)
        return generateAdaptiveLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

// 2. Rectangle Launcher Receiver
class CustomTextAppLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId)
        return generateRectangleLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

// GEOMETRIC SHAPE LAUNCHERS (Always locked to isResponsive = false)
class SquircleLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId).copy(shape = LauncherShape.SQUIRCLE, isResponsive = false)
        return generateAdaptiveLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class PentagonLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId).copy(shape = LauncherShape.M3_PENTAGON, isResponsive = false)
        return generateAdaptiveLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class FlowerLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId).copy(shape = LauncherShape.M3_FLOWER, isResponsive = false)
        return generateAdaptiveLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class CloverLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId).copy(shape = LauncherShape.M3_CLOVER, isResponsive = false)
        return generateAdaptiveLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class DiamondLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId).copy(shape = LauncherShape.M3_DIAMOND, isResponsive = false)
        return generateAdaptiveLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class OctagonLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId).copy(shape = LauncherShape.M3_OCTAGON, isResponsive = false)
        return generateAdaptiveLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class CircleLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId).copy(shape = LauncherShape.CIRCLE, isResponsive = false)
        return generateAdaptiveLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class BlobLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId).copy(shape = LauncherShape.BLOB, isResponsive = false)
        return generateAdaptiveLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class PixelStarLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId).copy(shape = LauncherShape.PIXEL_STAR, isResponsive = false)
        return generateAdaptiveLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class GlitchTextLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId)
        return generateGlitchTextLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class NeonRingLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId)
        return generateNeonRingLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

fun updateAllAppLauncherWidgets(context: Context) {
    val receivers = listOf(
        AdaptiveAppLauncherReceiver::class.java,
        CustomTextAppLauncherReceiver::class.java,
        SquircleLauncherReceiver::class.java,
        PentagonLauncherReceiver::class.java,
        FlowerLauncherReceiver::class.java,
        CloverLauncherReceiver::class.java,
        DiamondLauncherReceiver::class.java,
        OctagonLauncherReceiver::class.java,
        CircleLauncherReceiver::class.java,
        BlobLauncherReceiver::class.java,
        PixelStarLauncherReceiver::class.java,
        GlitchTextLauncherReceiver::class.java,
        NeonRingLauncherReceiver::class.java
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