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
import com.altusix.slate.ui.config.ContactsWidgetConfigActivity

fun getContactsWidgetsCatalog(): List<SlateWidgetInfo> {
    return listOf(
        SlateWidgetInfo(name = "Single Avatar Capsule", sizeText = "1x2", category = "Contacts", receiverClass = ContactsSingleAvatarCapsuleReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Horizontal Speed Dial", sizeText = "2x1", category = "Contacts", receiverClass = ContactsHorizontalSpeedDialReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "3-Section Bento Contact", sizeText = "4x2", category = "Contacts", receiverClass = ContactsEditorialBentoReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Stacked Bento Contact", sizeText = "2x2", category = "Contacts", receiverClass = ContactsStackedBentoReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Editorial 3-Action Bento", sizeText = "4x2", category = "Contacts", receiverClass = ContactsEditorial3ActionBentoReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Stacked 3-Action Bento", sizeText = "2x2", category = "Contacts", receiverClass = ContactsStacked3ActionBentoReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Full Photo Frame", sizeText = "2x2", category = "Contacts", receiverClass = ContactsFullPhotoReceiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "Squircle Contact", sizeText = "2x2", category = "Contacts", receiverClass = ContactsSquircleReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo(name = "Circle Contact", sizeText = "2x2", category = "Contacts", receiverClass = ContactsCircleReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo(name = "Pentagon Contact", sizeText = "2x2", category = "Contacts", receiverClass = ContactsPentagonReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo(name = "Octagon Contact", sizeText = "2x2", category = "Contacts", receiverClass = ContactsOctagonReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo(name = "Diamond Contact", sizeText = "2x2", category = "Contacts", receiverClass = ContactsDiamondReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo(name = "Flower Contact", sizeText = "2x2", category = "Contacts", receiverClass = ContactsFlowerReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo(name = "Clover Contact", sizeText = "2x2", category = "Contacts", receiverClass = ContactsCloverReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo(name = "Blob Bottom Right Contact", sizeText = "2x2", category = "Contacts", receiverClass = ContactsBlobBottomRightReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo(name = "Blob Bottom Left Contact", sizeText = "2x2", category = "Contacts", receiverClass = ContactsBlobBottomLeftReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo(name = "Blob Top Right Contact", sizeText = "2x2", category = "Contacts", receiverClass = ContactsBlobTopRightReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo(name = "Blob Top Left Contact", sizeText = "2x2", category = "Contacts", receiverClass = ContactsBlobTopLeftReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo(name = "Pixel Star Contact", sizeText = "2x2", category = "Contacts", receiverClass = ContactsPixelStarReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo(name = "Star 5 Contact", sizeText = "2x2", category = "Contacts", receiverClass = ContactsStar5Receiver::class.java, hasModeOption = false),
        SlateWidgetInfo(name = "Heart Contact", sizeText = "2x2", category = "Contacts", receiverClass = ContactsHeartReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo(name = "Triangle Contact", sizeText = "2x2", category = "Contacts", receiverClass = ContactsTriangleReceiver::class.java, hasModeOption = false),
        SlateWidgetInfo(name = "2 Contact Speed Dial", sizeText = "2x1", category = "Contacts", receiverClass = ContactsGrid2Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "3 Contact Speed Dial", sizeText = "3x1", category = "Contacts", receiverClass = ContactsGrid3Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "4 Contact Speed Dial", sizeText = "2x2", category = "Contacts", receiverClass = ContactsGrid4Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "6 Contact Speed Dial", sizeText = "3x2", category = "Contacts", receiverClass = ContactsGrid6Receiver::class.java, hasModeOption = true),
        SlateWidgetInfo(name = "8 Contact Speed Dial", sizeText = "4x2", category = "Contacts", receiverClass = ContactsGrid8Receiver::class.java, hasModeOption = true)
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

fun loadSlotConfig(context: Context, widgetId: Int, slotIndex: Int): ContactWidgetConfig {
    val prefs = context.getSharedPreferences("slate_contacts_prefs", Context.MODE_PRIVATE)
    val prefix = "widget_${widgetId}_slot_${slotIndex}_"
    val name = prefs.getString("${prefix}name", "") ?: ""
    val phone = prefs.getString("${prefix}phone", "") ?: ""
    val photo = prefs.getString("${prefix}photo", null)
    val initials = prefs.getString("${prefix}initials", "") ?: ""
    val actionStr = prefs.getString("${prefix}action", ContactActionType.CALL.name) ?: ContactActionType.CALL.name
    val isConfigured = prefs.getBoolean("${prefix}configured", false)

    val actionType = try { ContactActionType.valueOf(actionStr) } catch (_: Exception) { ContactActionType.CALL }
    return ContactWidgetConfig(
        contactName = name,
        phoneNumber = phone,
        photoUri = photo,
        initials = initials,
        actionType = actionType,
        isConfigured = isConfigured
    )
}

fun saveSlotConfig(context: Context, widgetId: Int, slotIndex: Int, config: ContactWidgetConfig) {
    val prefs = context.getSharedPreferences("slate_contacts_prefs", Context.MODE_PRIVATE)
    val prefix = "widget_${widgetId}_slot_${slotIndex}_"
    prefs.edit().apply {
        putString("${prefix}name", config.contactName)
        putString("${prefix}phone", config.phoneNumber)
        putString("${prefix}photo", config.photoUri)
        putString("${prefix}initials", config.initials)
        putString("${prefix}action", config.actionType.name)
        putBoolean("${prefix}configured", config.isConfigured)
        apply()
    }
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
        ContactsStacked3ActionBentoReceiver::class.java,
        ContactsFullPhotoReceiver::class.java,
        ContactsSquircleReceiver::class.java,
        ContactsCircleReceiver::class.java,
        ContactsPentagonReceiver::class.java,
        ContactsOctagonReceiver::class.java,
        ContactsDiamondReceiver::class.java,
        ContactsFlowerReceiver::class.java,
        ContactsCloverReceiver::class.java,
        ContactsBlobBottomRightReceiver::class.java,
        ContactsBlobBottomLeftReceiver::class.java,
        ContactsBlobTopRightReceiver::class.java,
        ContactsBlobTopLeftReceiver::class.java,
        ContactsPixelStarReceiver::class.java,
        ContactsStar5Receiver::class.java,
        ContactsHeartReceiver::class.java,
        ContactsTriangleReceiver::class.java,
        ContactsGrid2Receiver::class.java,
        ContactsGrid3Receiver::class.java,
        ContactsGrid4Receiver::class.java,
        ContactsGrid6Receiver::class.java,
        ContactsGrid8Receiver::class.java
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

// 2. HORIZONTAL SPEED DIAL (2x1)BaseMultiContactGridReceiver
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

// 7. FULL PHOTO CONTACT (2x2 / Full-Bleed Contact Frame)
class ContactsFullPhotoReceiver : BaseContactsReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateFullPhotoContactBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 8. SQUIRCLE CONTACT (2x2)
class ContactsSquircleReceiver : BaseContactsReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateSquircleContactBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 9. CIRCLE CONTACT (2x2)
class ContactsCircleReceiver : BaseContactsReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateCircleContactBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 10. PENTAGON CONTACT (2x2)
class ContactsPentagonReceiver : BaseContactsReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generatePentagonContactBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 11. OCTAGON CONTACT (2x2)
class ContactsOctagonReceiver : BaseContactsReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateOctagonContactBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 12. DIAMOND CONTACT (2x2)
class ContactsDiamondReceiver : BaseContactsReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateDiamondContactBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 13. FLOWER CONTACT (2x2)
class ContactsFlowerReceiver : BaseContactsReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateFlowerContactBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 14. CLOVER CONTACT (2x2)
class ContactsCloverReceiver : BaseContactsReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateCloverContactBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 15. BLOB BOTTOM RIGHT CONTACT (2x2)
class ContactsBlobBottomRightReceiver : BaseContactsReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateBlobBottomRightContactBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 16. BLOB BOTTOM LEFT CONTACT (2x2)
class ContactsBlobBottomLeftReceiver : BaseContactsReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateBlobBottomLeftContactBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 17. BLOB TOP RIGHT CONTACT (2x2)
class ContactsBlobTopRightReceiver : BaseContactsReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateBlobTopRightContactBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 18. BLOB TOP LEFT CONTACT (2x2)
class ContactsBlobTopLeftReceiver : BaseContactsReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateBlobTopLeftContactBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 19. PIXEL STAR CONTACT (2x2)
class ContactsPixelStarReceiver : BaseContactsReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generatePixelStarContactBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 20. STAR 5 CONTACT (2x2)
class ContactsStar5Receiver : BaseContactsReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateStar5ContactBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 21. HEART CONTACT (2x2)
class ContactsHeartReceiver : BaseContactsReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateHeartContactBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 22. TRIANGLE CONTACT (2x2)
class ContactsTriangleReceiver : BaseContactsReceiver() {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateTriangleContactBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

abstract class BaseMultiContactGridReceiver(
    private val slotCount: Int,
    private val layoutResId: Int
) : BaseContactsReceiver() {

    override fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val wDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 200) ?: 200
        val hDpRaw = if (isLandscape) options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 200) ?: 200 else options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 200) ?: 200
        val wDp = if (wDpRaw <= 0) 200 else wDpRaw
        val hDp = if (hDpRaw <= 0) 200 else hDpRaw

        val isResponsive = parseAndLockIsResponsive(context, widgetId)
        val config = loadSlateWidgetConfig(context, widgetId)

        val bitmap = renderBitmapForWidget(context, config, isResponsive, wDp, hDp, widgetId)

        val views = RemoteViews(context.packageName, layoutResId)
        views.setImageViewBitmap(R.id.widget_image_view, bitmap)

        val touchSlotIds = intArrayOf(
            R.id.touch_slot_0, R.id.touch_slot_1, R.id.touch_slot_2, R.id.touch_slot_3,
            R.id.touch_slot_4, R.id.touch_slot_5, R.id.touch_slot_6, R.id.touch_slot_7
        )

        for (i in 0 until slotCount) {
            val slotConfig = loadSlotConfig(context, widgetId, i)
            val targetViewId = touchSlotIds.getOrNull(i) ?: continue

            if (!slotConfig.isConfigured) {
                val configIntent = Intent(context, ContactsWidgetConfigActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    putExtra("extra_slot_index", i)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pi = PendingIntent.getActivity(
                    context, widgetId * 100 + i, configIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(targetViewId, pi)
            } else {
                val phoneDigits = slotConfig.phoneNumber.replace("[^0-9]".toRegex(), "")
                val actionIntent = when (slotConfig.actionType) {
                    ContactActionType.CALL -> Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:${slotConfig.phoneNumber}")
                    }
                    ContactActionType.SMS -> Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("smsto:${slotConfig.phoneNumber}")
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

                val pi = PendingIntent.getActivity(
                    context, widgetId * 100 + i, actionIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(targetViewId, pi)
            }
        }

        appWidgetManager.updateAppWidget(widgetId, views)
    }
}

// 23. 2-Contact Grid
class ContactsGrid2Receiver : BaseMultiContactGridReceiver(2, R.layout.widget_contacts_grid2_layout) {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateGrid2ContactBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 24. 3-Contact Grid
class ContactsGrid3Receiver : BaseMultiContactGridReceiver(3, R.layout.widget_contacts_grid3_layout) {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateGrid3ContactBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 25. 4-Contact Grid
class ContactsGrid4Receiver : BaseMultiContactGridReceiver(4, R.layout.widget_contacts_grid4_layout) {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateGrid4ContactBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 26. 6-Contact Grid
class ContactsGrid6Receiver : BaseMultiContactGridReceiver(6, R.layout.widget_contacts_grid6_layout) {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateGrid6ContactBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}

// 27. 8-Contact Grid
class ContactsGrid8Receiver : BaseMultiContactGridReceiver(8, R.layout.widget_contacts_grid8_layout) {
    override fun renderBitmapForWidget(context: Context, config: SlateWidgetConfig, isResponsive: Boolean, wDp: Int, hDp: Int, widgetId: Int): Bitmap =
        generateGrid8ContactBitmap(context, config, isResponsive, wDp, hDp, widgetId)
}
