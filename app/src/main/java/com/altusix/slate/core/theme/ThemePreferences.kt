package com.altusix.slate.core.theme

import android.content.Context
import androidx.compose.ui.graphics.Color

data class SlateThemeSettings(
    val bgHex: Long = 0xFF000000L,
    val accentHex: Long = 0xFFFFFFFFL,
    val opacity: Float = 1.0f
) {
    val accentColor: Color get() = Color(accentHex)
    val backgroundColor: Color get() = Color(bgHex)
    val resolvedBackgroundColor: Color get() = Color(bgHex).copy(alpha = opacity)
}

class ThemePreferences(private val context: Context) {
    private val prefs = context.getSharedPreferences("slate_global_theme_prefs", Context.MODE_PRIVATE)

    fun getThemeSettings(): SlateThemeSettings {
        val bgHex = prefs.getLong("global_bg_hex", 0xFF000000L)
        val accentHex = prefs.getLong("global_accent_hex", 0xFFFFFFFFL)
        val opacity = prefs.getFloat("global_opacity", 1.0f)

        return SlateThemeSettings(
            bgHex = bgHex,
            accentHex = accentHex,
            opacity = opacity
        )
    }

    fun saveThemeSettings(settings: SlateThemeSettings) {
        prefs.edit()
            .putLong("global_bg_hex", settings.bgHex)
            .putLong("global_accent_hex", settings.accentHex)
            .putFloat("global_opacity", settings.opacity)
            .apply()
    }
}