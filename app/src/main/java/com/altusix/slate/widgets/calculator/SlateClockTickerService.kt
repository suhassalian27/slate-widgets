package com.altusix.slate.widgets.calendar

import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import kotlinx.coroutines.*

class SlateClockTickerService : Service() {

    private var tickerJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> startTicking()
                Intent.ACTION_SCREEN_OFF -> stopTicking()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenStateReceiver, filter)
        startTicking()
    }

    private fun startTicking() {
        if (tickerJob?.isActive == true) return
        tickerJob = serviceScope.launch {
            while (isActive) {
                updateAnalogClockWidgets(this@SlateClockTickerService)
                delay(1000) // Ticks every 1 second
            }
        }
    }

    private fun stopTicking() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun updateAnalogClockWidgets(context: Context) {
        val manager = AppWidgetManager.getInstance(context)

        // Collect IDs for both analog clock receivers
        val ids14 = manager.getAppWidgetIds(ComponentName(context, CalendarAnalogTimelineReceiver::class.java)) ?: intArrayOf()
        val ids28 = manager.getAppWidgetIds(ComponentName(context, CalendarAnalogCalendarHybridReceiver::class.java)) ?: intArrayOf()

        // Trigger update for Widget #14
        if (ids14.isNotEmpty()) {
            val intent = Intent(context, CalendarAnalogTimelineReceiver::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids14)
            }
            context.sendBroadcast(intent)
        }

        // Trigger update for Widget #28
        if (ids28.isNotEmpty()) {
            val intent = Intent(context, CalendarAnalogCalendarHybridReceiver::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids28)
            }
            context.sendBroadcast(intent)
        }

        // Stop service only if NO analog widgets exist on screen
        if (ids14.isEmpty() && ids28.isEmpty()) {
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startTicking()
        return START_STICKY
    }

    override fun onDestroy() {
        stopTicking()
        try {
            unregisterReceiver(screenStateReceiver)
        } catch (_: Exception) {}
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}