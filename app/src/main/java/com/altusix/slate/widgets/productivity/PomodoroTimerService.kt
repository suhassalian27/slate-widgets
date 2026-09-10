package com.altusix.slate.widgets.productivity

import android.app.*
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class PomodoroTimerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var tickerJob: Job? = null

    companion object {
        const val ACTION_START = "com.altusix.slate.productivity.service.START"
        const val ACTION_STOP = "com.altusix.slate.productivity.service.STOP"
        const val EXTRA_WIDGET_ID = "extra_widget_id"
        private const val CHANNEL_ID = "slate_pomodoro_channel"
        private const val NOTIFICATION_ID = 9011
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val widgetId = intent?.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID

        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, buildNotification("Focus session active"))
                startTicker(widgetId)
            }
            ACTION_STOP -> {
                stopTicker()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTicker(widgetId: Int) {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (isActive) {
                delay(1000L)
                val config = ProductivityStorageManager.getConfig(this@PomodoroTimerService, widgetId)
                val timer = config.focusTimer
                if (!timer.isRunning) break

                val elapsed = ((System.currentTimeMillis() - timer.lastTimestamp) / 1000).toInt()
                val remaining = maxOf(0, timer.remainingSeconds - elapsed)

                if (remaining <= 0) {
                    // Session complete
                    ProductivityStorageManager.saveConfig(
                        this@PomodoroTimerService,
                        widgetId,
                        config.copy(focusTimer = timer.copy(isRunning = false, remainingSeconds = 0))
                    )
                    updateWidget(widgetId)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    break
                } else {
                    updateWidget(widgetId)
                }
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun updateWidget(widgetId: Int) {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val receiver = ProductivityPomodoroReceiver()
        receiver.updateSingleWidget(this, appWidgetManager, widgetId)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pomodoro Focus Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live status of active focus session"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Pomodoro Focus")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}