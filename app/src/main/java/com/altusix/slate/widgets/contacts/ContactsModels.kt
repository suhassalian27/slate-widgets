package com.altusix.slate.widgets.contacts

import android.content.Context

enum class ContactActionType(val label: String, val badgeText: String, val description: String) {
    CALL("Phone Call", "CALL", "Direct phone dialer"),
    SMS("Send Message", "TEXT", "Open SMS messaging app"),
    WHATSAPP("WhatsApp", "WA", "Open WhatsApp chat"),
    TELEGRAM("Telegram", "TG", "Open Telegram chat")
}

data class ContactWidgetConfig(
    val contactName: String = "",
    val phoneNumber: String = "",
    val photoUri: String? = null,
    val initials: String = "",
    val actionType: ContactActionType = ContactActionType.CALL,
    val isConfigured: Boolean = false
)

object ContactsWidgetPreferences {
    private const val PREFS_NAME = "slate_contacts_prefs"

    fun saveConfig(context: Context, widgetId: Int, config: ContactWidgetConfig) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString("widget_${widgetId}_name", config.contactName)
            .putString("widget_${widgetId}_phone", config.phoneNumber)
            .putString("widget_${widgetId}_photo", config.photoUri)
            .putString("widget_${widgetId}_initials", config.initials)
            .putString("widget_${widgetId}_action_type", config.actionType.name)
            .putBoolean("widget_${widgetId}_is_configured", config.isConfigured)
            .commit()
    }

    fun loadConfig(context: Context, widgetId: Int): ContactWidgetConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isConfigured = prefs.getBoolean("widget_${widgetId}_is_configured", false)
        val name = prefs.getString("widget_${widgetId}_name", "") ?: ""
        val phone = prefs.getString("widget_${widgetId}_phone", "") ?: ""
        val photo = prefs.getString("widget_${widgetId}_photo", null)
        val initials = prefs.getString("widget_${widgetId}_initials", getInitials(name)) ?: getInitials(name)
        val actionTypeName = prefs.getString("widget_${widgetId}_action_type", ContactActionType.CALL.name) ?: ContactActionType.CALL.name
        val actionType = try { ContactActionType.valueOf(actionTypeName) } catch (_: Exception) { ContactActionType.CALL }

        return ContactWidgetConfig(name, phone, photo, initials, actionType, isConfigured)
    }

    fun getInitials(name: String): String {
        if (name.isBlank()) return ""
        val parts = name.trim().split("\\s+".toRegex())
        return when {
            parts.size >= 2 -> "${parts[0].firstOrNull()?.uppercaseChar() ?: ""}${parts[1].firstOrNull()?.uppercaseChar() ?: ""}"
            parts.isNotEmpty() && parts[0].isNotEmpty() -> parts[0].take(2).uppercase()
            else -> ""
        }
    }
}