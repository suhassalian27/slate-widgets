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
        SlateWidgetInfo("App Launcher - Heart", "1x1", "App Launcher", HeartLauncherReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("App Launcher - Triangle", "1x1", "App Launcher", TriangleLauncherReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("App Launcher - Soft Star", "1x1", "App Launcher", Star5LauncherReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("App Launcher - M3 Pentagon", "1x1", "App Launcher", PentagonLauncherReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("App Launcher - M3 Flower", "1x1", "App Launcher", FlowerLauncherReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("App Launcher - M3 Clover", "1x1", "App Launcher", CloverLauncherReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("App Launcher - M3 Diamond", "1x1", "App Launcher", DiamondLauncherReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("App Launcher - M3 Octagon", "1x1", "App Launcher", OctagonLauncherReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("App Launcher - Circle", "1x1", "App Launcher", CircleLauncherReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("App Launcher - Blob Bottom Right", "1x1", "App Launcher", BlobBottomRightLauncherReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("App Launcher - Blob Bottom Left", "1x1", "App Launcher", BlobBottomLeftLauncherReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("App Launcher - Blob Top Right", "1x1", "App Launcher", BlobTopRightLauncherReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("App Launcher - Blob Top Left", "1x1", "App Launcher", BlobTopLeftLauncherReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("AppLauncher - Pixel Star", "1x1", "App Launcher", PixelStarLauncherReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Cyber Glitch Launcher", "2x1", "App Launcher", GlitchTextLauncherReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo("Neon Halo Launcher", "1x1", "App Launcher", NeonRingLauncherReceiver::class.java, hasModeOption = false)
    )
}

class AdaptiveAppLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId)
        return generateAdaptiveLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class CustomTextAppLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId)
        return generateRectangleLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class SquircleLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId)
        return generateSquircleLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class HeartLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId)
        return generateHeartLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class TriangleLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId)
        return generateTriangleLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class Star5LauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId)
        return generateStar5LauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class PentagonLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId)
        return generatePentagonLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class FlowerLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId)
        return generateFlowerLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class CloverLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId)
        return generateCloverLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class DiamondLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId)
        return generateDiamondLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class OctagonLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId)
        return generateOctagonLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class CircleLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId)
        return generateCircleLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class BlobBottomRightLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId)
        return generateBlobBottomRightLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class BlobBottomLeftLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId)
        return generateBlobBottomLeftLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class BlobTopRightLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId)
        return generateBlobTopRightLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class BlobTopLeftLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId)
        return generateBlobTopLeftLauncherBitmap(context, config, launcherConfig, wDp, hDp)
    }
}

class PixelStarLauncherReceiver : BaseAppLauncherReceiver() {
    override fun renderWidgetBitmap(context: Context, appWidgetId: Int, config: SlateWidgetConfig, wDp: Int, hDp: Int): Bitmap {
        val launcherConfig = AppLauncherWidgetConfig.load(context, appWidgetId)
        return generatePixelStarLauncherBitmap(context, config, launcherConfig, wDp, hDp)
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
        HeartLauncherReceiver::class.java,
        TriangleLauncherReceiver::class.java,
        Star5LauncherReceiver::class.java,
        PentagonLauncherReceiver::class.java,
        FlowerLauncherReceiver::class.java,
        CloverLauncherReceiver::class.java,
        DiamondLauncherReceiver::class.java,
        OctagonLauncherReceiver::class.java,
        CircleLauncherReceiver::class.java,
        BlobBottomRightLauncherReceiver::class.java,
        BlobBottomLeftLauncherReceiver::class.java,
        BlobTopRightLauncherReceiver::class.java,
        BlobTopLeftLauncherReceiver::class.java,
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