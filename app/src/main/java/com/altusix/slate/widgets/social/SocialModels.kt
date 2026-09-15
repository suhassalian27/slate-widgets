package com.altusix.slate.widgets.social

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

data class SocialAppPreset(
    val id: String,
    val name: String,
    val packageName: String,
    val fallbackUrl: String
)

data class SocialSlotConfig(
    val packageName: String = "",
    val appName: String = "",
    val presetId: String = "", // Matches SocialAppPreset.id, or "custom"
    val isConfigured: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("package", packageName)
        put("name", appName)
        put("presetId", presetId)
        put("configured", isConfigured)
    }

    companion object {
        fun fromJson(json: JSONObject): SocialSlotConfig {
            return SocialSlotConfig(
                packageName = json.optString("package", ""),
                appName = json.optString("name", ""),
                presetId = json.optString("presetId", ""),
                isConfigured = json.optBoolean("configured", false)
            )
        }
    }
}

data class SocialWidgetConfig(
    val slotCount: Int = 4,
    val iconStyle: String = "MONOCHROME", // "MONOCHROME", "ACCENT", "ORIGINAL"
    val showTileBackground: Boolean = true,
    val showAppNames: Boolean = false,
    val slots: List<SocialSlotConfig> = emptyList()
)

object SocialStorageManager {
    private const val PREFS_NAME = "slate_social_prefs"

    // 16 Popular Global Social Networks
    val SOCIAL_PRESETS = listOf(
        SocialAppPreset("instagram", "Instagram", "com.instagram.android", "https://instagram.com"),
        SocialAppPreset("facebook", "Facebook", "com.facebook.katana", "https://facebook.com"),
        SocialAppPreset("x", "X / Twitter", "com.twitter.android", "https://x.com"),
        SocialAppPreset("discord", "Discord", "com.discord", "https://discord.com"),
        SocialAppPreset("reddit", "Reddit", "com.reddit.frontpage", "https://reddit.com"),
        SocialAppPreset("snapchat", "Snapchat", "com.snapchat.android", "https://snapchat.com"),
        SocialAppPreset("pinterest", "Pinterest", "com.pinterest", "https://pinterest.com"),
        SocialAppPreset("messenger", "Messenger", "com.facebook.orca", "https://messenger.com"),
        SocialAppPreset("tiktok", "TikTok", "com.zhiliaoapp.musically", "https://tiktok.com"),
        SocialAppPreset("twitch", "Twitch", "tv.twitch.android", "https://twitch.tv"),
        SocialAppPreset("threads", "Threads", "com.instagram.barcelona", "https://threads.net"),
        SocialAppPreset("whatsapp", "WhatsApp", "com.whatsapp", "https://whatsapp.com"),
        SocialAppPreset("telegram", "Telegram", "org.telegram.messenger", "https://telegram.org"),
        SocialAppPreset("youtube", "YouTube", "com.google.android.youtube", "https://youtube.com"),
        SocialAppPreset("linkedin", "LinkedIn", "com.linkedin.android", "https://linkedin.com"),
        SocialAppPreset("spotify", "Spotify", "com.spotify.music", "https://spotify.com")
    )

    fun getPresetById(id: String): SocialAppPreset? = SOCIAL_PRESETS.find { it.id.equals(id, ignoreCase = true) }

    fun findPresetByPackage(packageName: String): SocialAppPreset? = SOCIAL_PRESETS.find { it.packageName == packageName }

    // Tailored defaults for each layout matching the user's screenshot
    fun getDefaultSlotsForLayout(slotCount: Int, layoutTag: String): List<SocialSlotConfig> {
        val presetIds = when (layoutTag) {
            "BAR_5" -> listOf("snapchat", "facebook", "instagram", "discord", "messenger")
            "QUAD_4" -> listOf("instagram", "facebook", "pinterest", "snapchat")
            "MATRIX_9" -> listOf("instagram", "facebook", "pinterest", "discord", "messenger", "x", "threads", "reddit", "snapchat")
            "DECK_10" -> listOf("instagram", "facebook", "x", "threads", "twitch", "pinterest", "snapchat", "discord", "tiktok", "messenger")
            "BENTO_10_TOP" -> listOf("discord", "instagram", "pinterest", "messenger", "linkedin", "x", "tiktok", "twitch", "snapchat", "reddit")
            "BENTO_10_LEFT" -> listOf("instagram", "discord", "x", "reddit", "linkedin", "snapchat", "messenger", "twitch", "pinterest", "tiktok")
            "ORBIT_6" -> listOf("instagram", "x", "discord", "reddit", "whatsapp", "tiktok")
            "MESSAGING_4" -> listOf("whatsapp", "telegram", "messenger", "discord")
            "STREAM_3" -> listOf("instagram", "x", "whatsapp")
            "OCTA_8" -> listOf("instagram", "facebook", "x", "discord", "reddit", "snapchat", "tiktok", "youtube")
            "TWIN_2" -> listOf("instagram", "threads")
            "MICRO_1" -> listOf("instagram")
            else -> SOCIAL_PRESETS.take(slotCount).map { it.id }
        }

        return (0 until slotCount).map { i ->
            val pid = presetIds.getOrNull(i) ?: SOCIAL_PRESETS.getOrNull(i % SOCIAL_PRESETS.size)?.id ?: "instagram"
            val preset = getPresetById(pid)
            if (preset != null) {
                SocialSlotConfig(
                    packageName = preset.packageName,
                    appName = preset.name,
                    presetId = preset.id,
                    isConfigured = true
                )
            } else {
                SocialSlotConfig()
            }
        }
    }

    fun load(context: Context, widgetId: Int, slotCount: Int, layoutTag: String): SocialWidgetConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val style = prefs.getString("widget_${widgetId}_icon_style", "MONOCHROME") ?: "MONOCHROME"
        val showTile = prefs.getBoolean("widget_${widgetId}_show_tile", true)
        val showNames = prefs.getBoolean("widget_${widgetId}_show_names", false)

        val slotsJsonStr = prefs.getString("widget_${widgetId}_slots", null)
        val slots = if (!slotsJsonStr.isNullOrBlank()) {
            try {
                val arr = JSONArray(slotsJsonStr)
                val list = mutableListOf<SocialSlotConfig>()
                for (i in 0 until slotCount) {
                    val obj = arr.optJSONObject(i)
                    if (obj != null) {
                        list.add(SocialSlotConfig.fromJson(obj))
                    } else {
                        list.add(SocialSlotConfig())
                    }
                }
                list
            } catch (_: Exception) {
                getDefaultSlotsForLayout(slotCount, layoutTag)
            }
        } else {
            getDefaultSlotsForLayout(slotCount, layoutTag)
        }

        return SocialWidgetConfig(
            slotCount = slotCount,
            iconStyle = style,
            showTileBackground = showTile,
            showAppNames = showNames,
            slots = slots
        )
    }

    fun save(context: Context, widgetId: Int, config: SocialWidgetConfig) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        for (slot in config.slots) {
            arr.put(slot.toJson())
        }
        prefs.edit()
            .putString("widget_${widgetId}_icon_style", config.iconStyle)
            .putBoolean("widget_${widgetId}_show_tile", config.showTileBackground)
            .putBoolean("widget_${widgetId}_show_names", config.showAppNames)
            .putString("widget_${widgetId}_slots", arr.toString())
            .apply()
    }

    /**
     * Resolves click intent:
     * 1. Launches installed app if present
     * 2. Otherwise opens fallback URL (e.g. web version) or Play Store
     */
    fun createLaunchIntent(context: Context, slotConfig: SocialSlotConfig): Intent {
        val pm = context.packageManager

        if (slotConfig.packageName.isNotBlank()) {
            val launchIntent = pm.getLaunchIntentForPackage(slotConfig.packageName)
            if (launchIntent != null) {
                return launchIntent.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
            }
        }

        val preset = getPresetById(slotConfig.presetId) ?: findPresetByPackage(slotConfig.packageName)
        if (preset != null && preset.fallbackUrl.isNotBlank()) {
            return Intent(Intent.ACTION_VIEW, Uri.parse(preset.fallbackUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        if (slotConfig.packageName.isNotBlank()) {
            return Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${slotConfig.packageName}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        return Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
