package com.altusix.slate.widgets.contacts

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews
import com.altusix.slate.R
import com.altusix.slate.core.model.SlateWidgetInfo
import com.altusix.slate.data.local.SlateWidgetConfig

fun getContactsWidgetsCatalog(): List<SlateWidgetInfo> {
    return listOf(
        SlateWidgetInfo(name = "Single Avatar Capsule", sizeText = "1x2", category = "Contacts", receiverClass = ContactsSingleAvatarCapsuleReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Horizontal Speed Dial", sizeText = "2x1", category = "Contacts", receiverClass = ContactsHorizontalSpeedDialReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "3-Section Bento Contact", sizeText = "4x2", category = "Contacts", receiverClass = ContactsEditorialBentoReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Stacked Bento Contact", sizeText = "2x2", category = "Contacts", receiverClass = ContactsStackedBentoReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Editorial 3-Action Bento", sizeText = "4x2", category = "Contacts", receiverClass = ContactsEditorial3ActionBentoReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Stacked 3-Action Bento", sizeText = "2x2", category = "Contacts", receiverClass = ContactsStacked3ActionBentoReceiver::class.java, hasModeOption = true)
    )
}

private fun parseAndLockIsResponsive(context: Context, widgetId: Int): Boolean {
    val widgetPrefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
    val key = "widget_${widgetId}_is_responsive"
    if (widgetPrefs.contains(key)) {
        return widgetPrefs.getBoolean(key, true)
    }
    val launcherPrefs = context.getSharedPreferences("slate_app_launcher_prefs", Context.MODE_PRIVATE)
    val defaultResp = launcherPrefs.getBoolean("default_is_responsive", true)
    widgetPrefs.edit().putBoolean(key, defaultResp).apply()
    return defaultResp
}

private fun loadSlateWidgetConfig(context: Context, widgetId: Int): SlateWidgetConfig {
    val prefs = context.getSharedPreferences("slate_widget_prefs", Context.MODE_PRIVATE)
    val themeMode = prefs.getString("widget_${widgetId}_theme_mode", "DARK") ?: "DARK"
    val bgColor = prefs.getLong("widget_${widgetId}_bg_color", 0xFF161618L)
    val opacity = prefs.getFloat("widget_${widgetId}_opacity", 1.0f)
    val accentColor = prefs.getLong("widget_${widgetId}_accent_color", 0xFFFFFFFFL)
    return SlateWidgetConfig(
        themeMode = themeMode,
        backgroundColorHex = bgColor,
        opacity = opacity,
        accentColorHex = accentColor
    )
}

abstract class BaseContactsReceiver : android.appwidget.AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int, newOptions: Bundle?) {
        updateWidget(context, appWidgetManager, widgetId)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, widgetId, newOptions)
    }

    open fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 200) ?: 200
        val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 200) ?: 200
        val wDp = if (wDpRaw <= 0) 200 else wDpRaw
        val hDp = if (hDpRaw <= 0) 200 else hDpRaw

        val isResponsive = parseAndLockIsResponsive(context, widgetId)
        val config = loadSlateWidgetConfig(context, widgetId)
        val contactConfig = ContactsWidgetPreferences.loadConfig(context, widgetId)

        val bitmap = renderBitmapForWidget(context, config, isResponsive, wDp, hDp, widgetId)

        val views = RemoteViews(context.packageName, R.layout.widget_image_container)
        views.setImageViewBitmap(R.id.widget_image_view, bitmap)

        val pendingIntent = if (!contactConfig.isConfigured) {
            val configIntent = Intent(context, ContactsWidgetConfigActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            PendingIntent.getActivity(
                context, widgetId, configIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            val phoneDigits = contactConfig.phoneNumber.replace("[^0-9]".toRegex(), "")
            val actionIntent = when (contactConfig.actionType) {
                ContactActionType.CALL -> Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:${contactConfig.phoneNumber}")
                }
                ContactActionType.SMS -> Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("smsto:${contactConfig.phoneNumber}")
                }
                ContactActionType.WHATSAPP -> Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://wa.me/$phoneDigits")
                }
                ContactActionType.TELEGRAM -> Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("tg://resolve?phone=$phoneDigits")
                }
            }.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            PendingIntent.getActivity(
                context, widgetId, actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        views.setOnClickPendingIntent(R.id.widget_image_view, pendingIntent)
        appWidgetManager.updateAppWidget(widgetId, views)
    }

    abstract fun renderBitmapForWidget(
        context: Context,
        config: SlateWidgetConfig,
        isResponsive: Boolean,
        wDp: Int,
        hDp: Int,
        widgetId: Int
    ): Bitmap
}

fun updateAllContactsWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val receivers = listOf(
        ContactsSingleAvatarCapsuleReceiver::class.java,
        ContactsHorizontalSpeedDialReceiver::class.java,
        ContactsEditorialBentoReceiver::class.java,
        ContactsStackedBentoReceiver::class.java,
        ContactsEditorial3ActionBentoReceiver::class.java,
        ContactsStacked3ActionBentoReceiver::class.java
    )
    for (receiverClass in receivers) {
        val ids = manager.getAppWidgetIds(ComponentName(context, receiverClass)) ?: intArrayOf()
        if (ids.isNotEmpty()) {
            val intent = Intent(context, receiverClass).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}

// 1. SINGLE AVATAR CAPSULE (1x2)
class ContactsSingleAvatarCapsuleReceiver : BaseContactsReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateSingleAvatarCapsuleBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 2. HORIZONTAL SPEED DIAL (2x1)
class ContactsHorizontalSpeedDialReceiver : BaseContactsReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateHorizontalSpeedDialBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 3. EDITORIAL BENTO CONTACTS (4x2 / 3-Section Bento Widget)
class ContactsEditorialBentoReceiver : BaseContactsReceiver() {

    override fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 200) ?: 200
        val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 200) ?: 200
        val wDp = if (wDpRaw <= 0) 200 else wDpRaw
        val hDp = if (hDpRaw <= 0) 200 else hDpRaw

        val isResponsive = parseAndLockIsResponsive(context, widgetId)
        val config = loadSlateWidgetConfig(context, widgetId)
        val contactConfig = ContactsWidgetPreferences.loadConfig(context, widgetId)

        val bitmap = renderBitmapForWidget(context, config, isResponsive, wDp, hDp, widgetId)

        val views = RemoteViews(context.packageName, R.layout.widget_contacts_bento_layout)
        views.setImageViewBitmap(R.id.widget_image_view, bitmap)

        if (!contactConfig.isConfigured) {
            val configIntent = Intent(context, ContactsWidgetConfigActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pi = PendingIntent.getActivity(
                context, widgetId, configIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            // Bind configuration intent to the entire root view & all touch targets when unconfigured
            views.setOnClickPendingIntent(R.id.widget_bento_root, pi)
            views.setOnClickPendingIntent(R.id.touch_hero, pi)
            views.setOnClickPendingIntent(R.id.touch_call, pi)
            views.setOnClickPendingIntent(R.id.touch_message, pi)
        } else {
            // 1. Left Hero Touch Target -> Re-open Setup Config Sheet
            val configIntent = Intent(context, ContactsWidgetConfigActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val heroPendingIntent = PendingIntent.getActivity(
                context, widgetId * 10 + 1, configIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.touch_hero, heroPendingIntent)

            // 2. Top Right Call Touch Target -> Direct Phone Dialer
            val callIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${contactConfig.phoneNumber}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val callPendingIntent = PendingIntent.getActivity(
                context, widgetId * 10 + 2, callIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.touch_call, callPendingIntent)

            // 3. Bottom Right Message Touch Target -> SMS Messages App
            val msgIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${contactConfig.phoneNumber}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val msgPendingIntent = PendingIntent.getActivity(
                context, widgetId * 10 + 3, msgIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.touch_message, msgPendingIntent)
        }

        appWidgetManager.updateAppWidget(widgetId, views)
    }

    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateEditorialBentoContactsBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 4. STACKED BENTO CONTACTS (2x2 / Top Hero, Bottom Actions)
class ContactsStackedBentoReceiver : BaseContactsReceiver() {

    override fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 200) ?: 200
        val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 200) ?: 200
        val wDp = if (wDpRaw <= 0) 200 else wDpRaw
        val hDp = if (hDpRaw <= 0) 200 else hDpRaw

        val isResponsive = parseAndLockIsResponsive(context, widgetId)
        val config = loadSlateWidgetConfig(context, widgetId)
        val contactConfig = ContactsWidgetPreferences.loadConfig(context, widgetId)

        val bitmap = renderBitmapForWidget(context, config, isResponsive, wDp, hDp, widgetId)

        val views = RemoteViews(context.packageName, R.layout.widget_contacts_stacked_bento_layout)
        views.setImageViewBitmap(R.id.widget_image_view, bitmap)

        if (!contactConfig.isConfigured) {
            val configIntent = Intent(context, ContactsWidgetConfigActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pi = PendingIntent.getActivity(
                context, widgetId, configIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_bento_root, pi)
            views.setOnClickPendingIntent(R.id.touch_hero, pi)
            views.setOnClickPendingIntent(R.id.touch_call, pi)
            views.setOnClickPendingIntent(R.id.touch_message, pi)
        } else {
            // 1. Top Hero Touch Target -> Setup Config Sheet
            val configIntent = Intent(context, ContactsWidgetConfigActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val heroPendingIntent = PendingIntent.getActivity(
                context, widgetId * 10 + 1, configIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.touch_hero, heroPendingIntent)

            // 2. Bottom Left Call Touch Target -> Phone Dialer
            val callIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${contactConfig.phoneNumber}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val callPendingIntent = PendingIntent.getActivity(
                context, widgetId * 10 + 2, callIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.touch_call, callPendingIntent)

            // 3. Bottom Right Message Touch Target -> SMS Messages App
            val msgIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${contactConfig.phoneNumber}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val msgPendingIntent = PendingIntent.getActivity(
                context, widgetId * 10 + 3, msgIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.touch_message, msgPendingIntent)
        }

        appWidgetManager.updateAppWidget(widgetId, views)
    }

    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateStackedBentoContactsBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 5. EDITORIAL 3-ACTION BENTO RECEIVER
class ContactsEditorial3ActionBentoReceiver : BaseContactsReceiver() {
    override fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 200) ?: 200
        val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 200) ?: 200
        val wDp = if (wDpRaw <= 0) 200 else wDpRaw
        val hDp = if (hDpRaw <= 0) 200 else hDpRaw

        val isResponsive = parseAndLockIsResponsive(context, widgetId)
        val config = loadSlateWidgetConfig(context, widgetId)
        val contactConfig = ContactsWidgetPreferences.loadConfig(context, widgetId)

        val bitmap = renderBitmapForWidget(context, config, isResponsive, wDp, hDp, widgetId)

        val views = RemoteViews(context.packageName, R.layout.widget_contacts_editorial_3action_layout)
        views.setImageViewBitmap(R.id.widget_image_view, bitmap)

        if (!contactConfig.isConfigured) {
            val configIntent = Intent(context, ContactsWidgetConfigActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pi = PendingIntent.getActivity(context, widgetId, configIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_bento_root, pi)
            views.setOnClickPendingIntent(R.id.touch_hero, pi)
            views.setOnClickPendingIntent(R.id.touch_call, pi)
            views.setOnClickPendingIntent(R.id.touch_message, pi)
            views.setOnClickPendingIntent(R.id.touch_whatsapp, pi)
        } else {
            val configIntent = Intent(context, ContactsWidgetConfigActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            views.setOnClickPendingIntent(R.id.touch_hero, PendingIntent.getActivity(context, widgetId * 10 + 1, configIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

            val callIntent = Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:${contactConfig.phoneNumber}"); flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            views.setOnClickPendingIntent(R.id.touch_call, PendingIntent.getActivity(context, widgetId * 10 + 2, callIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

            val msgIntent = Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse("smsto:${contactConfig.phoneNumber}"); flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            views.setOnClickPendingIntent(R.id.touch_message, PendingIntent.getActivity(context, widgetId * 10 + 3, msgIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

            val phoneDigits = contactConfig.phoneNumber.replace("[^0-9]".toRegex(), "")
            val waIntent = Intent(Intent.ACTION_VIEW).apply { data = Uri.parse("https://wa.me/$phoneDigits"); flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            views.setOnClickPendingIntent(R.id.touch_whatsapp, PendingIntent.getActivity(context, widgetId * 10 + 4, waIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        }

        appWidgetManager.updateAppWidget(widgetId, views)
    }

    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateEditorial3ActionBentoContactsBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 6. STACKED 3-ACTION BENTO RECEIVER
class ContactsStacked3ActionBentoReceiver : BaseContactsReceiver() {
    override fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 200) ?: 200
        val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 200) ?: 200
        val wDp = if (wDpRaw <= 0) 200 else wDpRaw
        val hDp = if (hDpRaw <= 0) 200 else hDpRaw

        val isResponsive = parseAndLockIsResponsive(context, widgetId)
        val config = loadSlateWidgetConfig(context, widgetId)
        val contactConfig = ContactsWidgetPreferences.loadConfig(context, widgetId)

        val bitmap = renderBitmapForWidget(context, config, isResponsive, wDp, hDp, widgetId)

        val views = RemoteViews(context.packageName, R.layout.widget_contacts_stacked_3action_layout)
        views.setImageViewBitmap(R.id.widget_image_view, bitmap)

        if (!contactConfig.isConfigured) {
            val configIntent = Intent(context, ContactsWidgetConfigActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pi = PendingIntent.getActivity(context, widgetId, configIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_bento_root, pi)
            views.setOnClickPendingIntent(R.id.touch_hero, pi)
            views.setOnClickPendingIntent(R.id.touch_call, pi)
            views.setOnClickPendingIntent(R.id.touch_message, pi)
            views.setOnClickPendingIntent(R.id.touch_whatsapp, pi)
        } else {
            val configIntent = Intent(context, ContactsWidgetConfigActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            views.setOnClickPendingIntent(R.id.touch_hero, PendingIntent.getActivity(context, widgetId * 10 + 1, configIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

            val callIntent = Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:${contactConfig.phoneNumber}"); flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            views.setOnClickPendingIntent(R.id.touch_call, PendingIntent.getActivity(context, widgetId * 10 + 2, callIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

            val msgIntent = Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse("smsto:${contactConfig.phoneNumber}"); flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            views.setOnClickPendingIntent(R.id.touch_message, PendingIntent.getActivity(context, widgetId * 10 + 3, msgIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

            val phoneDigits = contactConfig.phoneNumber.replace("[^0-9]".toRegex(), "")
            val waIntent = Intent(Intent.ACTION_VIEW).apply { data = Uri.parse("https://wa.me/$phoneDigits"); flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            views.setOnClickPendingIntent(R.id.touch_whatsapp, PendingIntent.getActivity(context, widgetId * 10 + 4, waIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        }

        appWidgetManager.updateAppWidget(widgetId, views)
    }

    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateStacked3ActionBentoContactsBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

