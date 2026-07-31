package com.altusix.slate.data.local

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object PreferencesKeys {
    fun widgetThemeModeKey(widgetId: Int) = stringPreferencesKey("widget_${widgetId}_theme_mode")
    fun widgetBgColorKey(widgetId: Int) = longPreferencesKey("widget_${widgetId}_bg_color")
    fun widgetOpacityKey(widgetId: Int) = floatPreferencesKey("widget_${widgetId}_opacity")
    fun widgetAccentColorKey(widgetId: Int) = longPreferencesKey("widget_${widgetId}_accent_color")
}

data class SlateWidgetConfig(
    val themeMode: String = "DARK",
    val backgroundColorHex: Long = 0xFF161618L,
    val opacity: Float = 1.0f,
    val accentColorHex: Long = 0xFF00D166L
)