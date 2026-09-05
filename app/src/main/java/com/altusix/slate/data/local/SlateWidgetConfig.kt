package com.altusix.slate.data.local

import android.content.Context
import com.altusix.slate.core.theme.ThemePreferences

data class SlateWidgetConfig(
    val themeMode: String = "DARK",
    val backgroundColorHex: Long = 0xFF161618L,
    val opacity: Float = 1.0f,
    val accentColorHex: Long = 0xFFFFFFFFL,
)

fun loadSlateWidgetConfig(context: Context, widgetId: Int): SlateWidgetConfig {
    val widgetPrefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
    val bgKey = "widget_${widgetId}_bg_color"

    // Snapshot and lock current global Theme Studio settings when widget is added
    if (!widgetPrefs.contains(bgKey) && widgetId != -1) {
        val globalSettings = ThemePreferences(context).getThemeSettings()
        widgetPrefs.edit()
            .putLong("widget_${widgetId}_bg_color", globalSettings.bgHex)
            .putLong("widget_${widgetId}_accent_color", globalSettings.accentHex)
            .putFloat("widget_${widgetId}_opacity", globalSettings.opacity)
            .apply()
    }

    val globalSettings = ThemePreferences(context).getThemeSettings()
    val bgColor = widgetPrefs.getLong("widget_${widgetId}_bg_color", globalSettings.bgHex)
    val opacity = widgetPrefs.getFloat("widget_${widgetId}_opacity", globalSettings.opacity)
    val accentColor = widgetPrefs.getLong("widget_${widgetId}_accent_color", globalSettings.accentHex)

    val isLight = (((bgColor shr 16 and 0xFFL) * 0.2126f) + ((bgColor shr 8 and 0xFFL) * 0.7152f) + ((bgColor and 0xFFL) * 0.0722f)) / 255f > 0.5f

    return SlateWidgetConfig(
        themeMode = if (isLight) "LIGHT" else "DARK",
        backgroundColorHex = bgColor,
        opacity = opacity,
        accentColorHex = accentColor
    )
}