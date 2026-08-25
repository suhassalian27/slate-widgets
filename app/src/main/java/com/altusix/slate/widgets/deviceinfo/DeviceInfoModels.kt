package com.altusix.slate.widgets.deviceinfo

import android.content.Context

data class DeviceInfoData(
    val batteryPct: Int = 0,
    val isCharging: Boolean = false,
    val usedStorageGb: Double = 0.0,
    val totalStorageGb: Double = 0.0,
    val storagePct: Int = 0,
    val usedRamGb: Double = 0.0,
    val totalRamGb: Double = 0.0,
    val ramPct: Int = 0,
    val deviceModel: String = "",
    val androidVersion: String = ""
)

data class DeviceInfoWidgetConfig(
    val showStorage: Boolean = true,
    val showRam: Boolean = true,
    val showBattery: Boolean = true,
    val showSystem: Boolean = true
) {
    companion object {
        fun load(context: Context, widgetId: Int): DeviceInfoWidgetConfig {
            val prefs = context.getSharedPreferences("slate_deviceinfo_prefs", Context.MODE_PRIVATE)
            return DeviceInfoWidgetConfig(
                showStorage = prefs.getBoolean("widget_${widgetId}_show_storage", true),
                showRam = prefs.getBoolean("widget_${widgetId}_show_ram", true),
                showBattery = prefs.getBoolean("widget_${widgetId}_show_battery", true),
                showSystem = prefs.getBoolean("widget_${widgetId}_show_system", true)
            )
        }

        fun save(context: Context, widgetId: Int, config: DeviceInfoWidgetConfig) {
            val prefs = context.getSharedPreferences("slate_deviceinfo_prefs", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putBoolean("widget_${widgetId}_show_storage", config.showStorage)
                putBoolean("widget_${widgetId}_show_ram", config.showRam)
                putBoolean("widget_${widgetId}_show_battery", config.showBattery)
                putBoolean("widget_${widgetId}_show_system", config.showSystem)
                apply()
            }
        }
    }
}