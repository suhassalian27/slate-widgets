package com.altusix.slate.widgets.camera

import android.content.Context
import android.content.SharedPreferences

enum class PhotoClickAction(val label: String) {
    OPEN_GALLERY("Open Gallery"),
    OPEN_CAMERA("Open Camera App"),
    OPEN_SETTINGS("Open Settings"),
    NOTHING("Do Nothing")
}

enum class PhotoFilterStyle(val label: String) {
    NONE("Original"),
    GRAYSCALE("Monochrome"),
    SEPIA("Warm Sepia"),
    DARK_DIM("Moody Dark"),
    VINTAGE("Retro Film"),
    COOL_BLUE("Cool Cyber"),
    WARM_GOLD("Golden Hour"),
    HIGH_CONTRAST("Pop Contrast")
}

enum class PhotoFrameBorder(val label: String) {
    NONE("Clean Edge"),
    THIN_BORDER("Minimal Border"),
    POLAROID("Polaroid Frame"),
    VIGNETTE("Soft Vignette"),
    INNER_OUTLINE("Inner Line"),
    FILM_STRIP("Film Strip")
}

data class CameraWidgetConfig(
    val photoUri: String? = null,
    val clickAction: PhotoClickAction = PhotoClickAction.OPEN_GALLERY,
    val filterStyle: PhotoFilterStyle = PhotoFilterStyle.NONE,
    val borderStyle: PhotoFrameBorder = PhotoFrameBorder.NONE,
    val customCaption: String = "",
    val showDateTaken: Boolean = false,
    val isResponsive: Boolean = false
)

object CameraWidgetPreferences {
    private const val PREF_NAME = "slate_camera_widget_prefs"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveConfig(context: Context, widgetId: Int, config: CameraWidgetConfig) {
        getPrefs(context).edit().apply {
            putString("photo_uri_$widgetId", config.photoUri)
            putString("click_action_$widgetId", config.clickAction.name)
            putString("filter_style_$widgetId", config.filterStyle.name)
            putString("border_style_$widgetId", config.borderStyle.name)
            putString("caption_$widgetId", config.customCaption)
            putBoolean("show_date_$widgetId", config.showDateTaken)
            putBoolean("responsive_$widgetId", config.isResponsive)
            apply()
        }
    }

    fun loadConfig(context: Context, widgetId: Int): CameraWidgetConfig {
        val prefs = getPrefs(context)
        val uri = prefs.getString("photo_uri_$widgetId", null)
        val clickAction = try {
            PhotoClickAction.valueOf(prefs.getString("click_action_$widgetId", PhotoClickAction.OPEN_GALLERY.name)!!)
        } catch (_: Exception) { PhotoClickAction.OPEN_GALLERY }

        val filter = try {
            PhotoFilterStyle.valueOf(prefs.getString("filter_style_$widgetId", PhotoFilterStyle.NONE.name)!!)
        } catch (_: Exception) { PhotoFilterStyle.NONE }

        val border = try {
            PhotoFrameBorder.valueOf(prefs.getString("border_style_$widgetId", PhotoFrameBorder.NONE.name)!!)
        } catch (_: Exception) { PhotoFrameBorder.NONE }

        val caption = prefs.getString("caption_$widgetId", "") ?: ""
        val showDateTaken = prefs.getBoolean("show_date_$widgetId", false)
        val isResponsive = prefs.getBoolean("responsive_$widgetId", false)

        return CameraWidgetConfig(uri, clickAction, filter, border, caption, showDateTaken, isResponsive)
    }
}

fun parseAndLockIsResponsive(context: Context, widgetId: Int): Boolean {
    val widgetPrefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
    val launcherPrefs = context.getSharedPreferences("slate_app_launcher_prefs", Context.MODE_PRIVATE)

    if (widgetPrefs.contains("widget_${widgetId}_mode")) {
        val modeStr = widgetPrefs.getString("widget_${widgetId}_mode", "FIXED")
        val isResp = modeStr.equals("RESPONSIVE", ignoreCase = true)
        widgetPrefs.edit().putBoolean("widget_${widgetId}_is_responsive", isResp).apply()
        return isResp
    }

    if (widgetPrefs.contains("widget_${widgetId}_is_responsive")) {
        return widgetPrefs.getBoolean("widget_${widgetId}_is_responsive", false)
    }

    val defaultResp = launcherPrefs.getBoolean("default_is_responsive", false)
    widgetPrefs.edit().putBoolean("widget_${widgetId}_is_responsive", defaultResp).apply()
    return defaultResp
}