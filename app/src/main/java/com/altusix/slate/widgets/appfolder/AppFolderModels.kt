package com.altusix.slate.widgets.appfolder

import android.content.Context

data class AppSlotConfig(
    val packageName: String = "",
    val appName: String = "",
    val isConfigured: Boolean = false
)

data class AppFolderWidgetConfig(
    val slotCount: Int = 4,
    val showAppNames: Boolean = true,
    val showTileBackground: Boolean = true,
    val slots: List<AppSlotConfig> = List(8) { AppSlotConfig() }
) {
    companion object {
        fun load(context: Context, widgetId: Int, slotCount: Int): AppFolderWidgetConfig {
            val prefs = context.getSharedPreferences("slate_appfolder_prefs", Context.MODE_PRIVATE)
            val showNames = prefs.getBoolean("widget_${widgetId}_show_names", true)
            val showTiles = prefs.getBoolean("widget_${widgetId}_show_tiles", true)

            val loadedSlots = mutableListOf<AppSlotConfig>()
            for (i in 0 until slotCount) {
                val prefix = "widget_${widgetId}_slot_${i}_"
                val pkg = prefs.getString("${prefix}package", "") ?: ""
                val name = prefs.getString("${prefix}name", "") ?: ""
                val isConfigured = prefs.getBoolean("${prefix}configured", false)
                loadedSlots.add(AppSlotConfig(packageName = pkg, appName = name, isConfigured = isConfigured))
            }

            return AppFolderWidgetConfig(
                slotCount = slotCount,
                showAppNames = showNames,
                showTileBackground = showTiles,
                slots = loadedSlots
            )
        }

        fun save(context: Context, widgetId: Int, config: AppFolderWidgetConfig) {
            val prefs = context.getSharedPreferences("slate_appfolder_prefs", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putBoolean("widget_${widgetId}_show_names", config.showAppNames)
                putBoolean("widget_${widgetId}_show_tiles", config.showTileBackground)
                config.slots.forEachIndexed { i, slot ->
                    val prefix = "widget_${widgetId}_slot_${i}_"
                    putString("${prefix}package", slot.packageName)
                    putString("${prefix}name", slot.appName)
                    putBoolean("${prefix}configured", slot.isConfigured)
                }
                apply()
            }
        }
    }
}