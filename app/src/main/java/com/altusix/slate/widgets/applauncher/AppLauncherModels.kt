package com.altusix.slate.widgets.applauncher

import android.content.Context

enum class LauncherShape {
    SQUIRCLE,
    M3_PENTAGON,
    M3_FLOWER,
    M3_CLOVER,
    M3_DIAMOND,
    M3_OCTAGON,
    CIRCLE,
    BLOB,
    PIXEL_STAR
}

enum class LauncherIconType {
    APP_ICON,
    EMOJI,
    VECTOR_ICON,
    CUSTOM_TEXT
}

data class AppLauncherWidgetConfig(
    val packageName: String = "",
    val shape: LauncherShape = LauncherShape.SQUIRCLE,
    val iconType: LauncherIconType = LauncherIconType.APP_ICON,
    val customText: String = "APP",
    val selectedEmoji: String = "🚀",
    val selectedVectorResName: String = "ic_sparkle",
    val isResponsive: Boolean = true,
    val useSystemAccent: Boolean = false,
    val accentColorHex: Long = 0xFF00D166L,
    val themeMode: String = "DARK",
    val opacity: Float = 1.0f
) {
    companion object {
        fun load(context: Context, widgetId: Int): AppLauncherWidgetConfig {
            val prefs = context.getSharedPreferences("slate_app_launcher_prefs", Context.MODE_PRIVATE)
            val prefix = "launcher_${widgetId}_"
            val defaultResponsive = prefs.getBoolean("default_is_responsive", true)

            return AppLauncherWidgetConfig(
                packageName = prefs.getString("${prefix}package", "") ?: "",
                shape = LauncherShape.valueOf(prefs.getString("${prefix}shape", LauncherShape.SQUIRCLE.name) ?: LauncherShape.SQUIRCLE.name),
                iconType = LauncherIconType.valueOf(prefs.getString("${prefix}icon_type", LauncherIconType.APP_ICON.name) ?: LauncherIconType.APP_ICON.name),
                customText = prefs.getString("${prefix}custom_text", "APP") ?: "APP",
                selectedEmoji = prefs.getString("${prefix}selected_emoji", "🚀") ?: "🚀",
                selectedVectorResName = prefs.getString("${prefix}vector_res", "ic_sparkle") ?: "ic_sparkle",
                isResponsive = prefs.getBoolean("${prefix}is_responsive", defaultResponsive),
                useSystemAccent = prefs.getBoolean("${prefix}use_system_accent", false),
                accentColorHex = prefs.getLong("${prefix}accent_color", 0xFF00D166L),
                themeMode = prefs.getString("${prefix}theme_mode", "DARK") ?: "DARK",
                opacity = prefs.getFloat("${prefix}opacity", 1.0f)
            )
        }

        fun save(context: Context, widgetId: Int, config: AppLauncherWidgetConfig) {
            val prefs = context.getSharedPreferences("slate_app_launcher_prefs", Context.MODE_PRIVATE)
            val prefix = "launcher_${widgetId}_"
            prefs.edit()
                .putString("${prefix}package", config.packageName)
                .putString("${prefix}shape", config.shape.name)
                .putString("${prefix}icon_type", config.iconType.name)
                .putString("${prefix}custom_text", config.customText)
                .putString("${prefix}selected_emoji", config.selectedEmoji)
                .putString("${prefix}vector_res", config.selectedVectorResName)
                .putBoolean("${prefix}is_responsive", config.isResponsive)
                .putBoolean("${prefix}use_system_accent", config.useSystemAccent)
                .putLong("${prefix}accent_color", config.accentColorHex)
                .putString("${prefix}theme_mode", config.themeMode)
                .putFloat("${prefix}opacity", config.opacity)
                .apply()
        }
    }
}