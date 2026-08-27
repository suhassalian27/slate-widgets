package com.altusix.slate.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.altusix.slate.widgets.battery.updateAllBatteryWidgets

class BatteryWidgetService : Service() {

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            // Trigger instant updates on battery ticks and screen wakes
            if (action == Intent.ACTION_BATTERY_CHANGED ||
                action == Intent.ACTION_SCREEN_ON ||
                action == Intent.ACTION_USER_PRESENT) {
                updateAllBatteryWidgets(context)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceWithNotification()

        // Dynamically register the receiver (This bypasses Manifest limitations)
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(updateReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Keep the service running
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(updateReceiver)
        } catch (e: Exception) { /* Ignored */ }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundServiceWithNotification() {
        val channelId = "slate_widget_service_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Widget Sync Service",
                NotificationManager.IMPORTANCE_MIN // IMPORTANCE_MIN hides the status bar icon
            ).apply {
                description = "Keeps Slate widgets updated in real-time"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Slate Widgets Active")
            .setContentText("Keeping widgets updated...")
            // Make sure this drawable exists, or change it to one you have (like R.mipmap.ic_launcher)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        startForeground(1998, notification)
    }
}