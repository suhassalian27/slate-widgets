package com.altusix.slate.data.local

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.GlanceId
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "slate_widget_prefs")

class SlateDataStore(private val context: Context) {

    fun getWidgetConfig(widgetId: Int): Flow<SlateWidgetConfig> {
        return context.dataStore.data.map { prefs ->
            val mode = prefs[PreferencesKeys.widgetThemeModeKey(widgetId)] ?: "DARK"
            val defaultBg = if (mode == "LIGHT") 0xFFFFFFFFL else 0xFF161618L
            val bg = prefs[PreferencesKeys.widgetBgColorKey(widgetId)] ?: defaultBg
            val opacity = prefs[PreferencesKeys.widgetOpacityKey(widgetId)] ?: 1.0f
            val accent = prefs[PreferencesKeys.widgetAccentColorKey(widgetId)] ?: 0xFF00D166L

            SlateWidgetConfig(
                themeMode = mode,
                backgroundColorHex = bg,
                opacity = opacity,
                accentColorHex = accent
            )
        }
    }

    suspend fun saveWidgetConfig(widgetId: Int, config: SlateWidgetConfig) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.widgetThemeModeKey(widgetId)] = config.themeMode
            prefs[PreferencesKeys.widgetBgColorKey(widgetId)] = config.backgroundColorHex
            prefs[PreferencesKeys.widgetOpacityKey(widgetId)] = config.opacity
            prefs[PreferencesKeys.widgetAccentColorKey(widgetId)] = config.accentColorHex
        }
    }
}

suspend fun getWidgetConfigSafely(context: Context, id: GlanceId): SlateWidgetConfig {
    // Direct cast bypasses GlanceAppWidgetManager internal DB lookup failure
    val appWidgetId = if (id is AppWidgetId) {
        id.appWidgetId
    } else {
        try {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        } catch (e: Exception) {
            val str = id.toString()
            val match = Regex("""\d+""").find(str)
            match?.value?.toIntOrNull() ?: AppWidgetManager.INVALID_APPWIDGET_ID
        }
    }

    if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
        return SlateWidgetConfig()
    }

    return try {
        SlateDataStore(context).getWidgetConfig(appWidgetId)
            .catch { emit(SlateWidgetConfig()) }
            .first()
    } catch (e: Exception) {
        SlateWidgetConfig()
    }
}