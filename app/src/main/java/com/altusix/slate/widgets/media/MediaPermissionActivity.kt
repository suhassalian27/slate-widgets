package com.altusix.slate.widgets.media

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast

class MediaPermissionActivity : Activity() {

    companion object {
        fun isNotificationListenerEnabled(context: Context): Boolean {
            val enabledListeners = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: ""
            val myComponent = ComponentName(context, SlateMediaNotificationService::class.java).flattenToString()
            return enabledListeners.contains(myComponent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isNotificationListenerEnabled(this)) {
            Toast.makeText(this, "Enable 'Slate' in Notification Access to display live media", Toast.LENGTH_LONG).show()
            try {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                startActivity(intent)
            } catch (_: Exception) {
                val fallbackIntent = Intent(Settings.ACTION_SETTINGS)
                startActivity(fallbackIntent)
            }
        } else {
            Toast.makeText(this, "Notification access already enabled!", Toast.LENGTH_SHORT).show()
            updateAllMediaWidgets(this)
        }
        finish()
    }
}
