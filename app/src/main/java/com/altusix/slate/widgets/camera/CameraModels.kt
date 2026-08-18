package com.altusix.slate.widgets.camera

import android.content.Context
import android.content.SharedPreferences

enum class PhotoClickAction(val label: String) {
    OPEN_GALLERY("Open Gallery / Photos"),
    OPEN_CAMERA("Open Camera App"),
    OPEN_SETTINGS("Open Widget Settings"),
    NOTHING("Do Nothing")
}

enum class PhotoFilterStyle(val label: String) {
    NONE("Original"),
    GRAYSCALE("Black & White"),
    SEPIA("Warm Sepia"),
    VINTAGE_GRAIN("Vintage Film"),
    DARK_DIM("Moody Dark")
}

enum class PhotoFrameBorder(val label: String) {
    NONE("Clean Edge"),
    THIN_BORDER("Minimal Border"),
    POLAROID("Polaroid Frame"),
    VIGNETTE("Soft Vignette")
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