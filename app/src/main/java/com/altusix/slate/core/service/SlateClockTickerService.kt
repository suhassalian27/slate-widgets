package com.altusix.slate.core.service

import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import com.altusix.slate.widgets.calendar.CalendarAnalogCalendarHybridReceiver
import com.altusix.slate.widgets.calendar.CalendarAnalogTimelineReceiver
import com.altusix.slate.widgets.calendar.CalendarArchitecturalAnalogReceiver
import com.altusix.slate.widgets.calendar.CalendarRadialArcReceiver
import com.altusix.slate.widgets.clock.analog.ClockAnalogPrecisionReceiver
import com.altusix.slate.widgets.clock.analog.ClockBauhausReceiver
import com.altusix.slate.widgets.clock.analog.ClockCyberSkeletonReceiver
import com.altusix.slate.widgets.clock.analog.ClockSculptedPillReceiver
import com.altusix.slate.widgets.clock.analog.ClockBoldTypographyReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
            registerReceiver(screenStateReceiver, filter)
        } catch (_: Exception) {}
        startTicking()
    }

    private fun startTicking() {
        if (tickerJob?.isActive == true) return
        tickerJob = serviceScope.launch {
            while (isActive) {
                updateAllTickingWidgets(this@SlateClockTickerService)
                delay(1000)
            }
        }
    }

    private fun stopTicking() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun updateAllTickingWidgets(context: Context) {
        val manager = AppWidgetManager.getInstance(context)

        // Master registry of ALL widget receivers that require 1-second updates
        val tickingReceivers = listOf(
            // Calendar Analog Hybrids
            CalendarAnalogTimelineReceiver::class.java,
            CalendarAnalogCalendarHybridReceiver::class.java,
            CalendarArchitecturalAnalogReceiver::class.java,
            CalendarRadialArcReceiver::class.java,

            // Clock - Analog Widgets
            ClockAnalogPrecisionReceiver::class.java,
            ClockBauhausReceiver::class.java,
            ClockCyberSkeletonReceiver::class.java,
            ClockSculptedPillReceiver::class.java,
            ClockBoldTypographyReceiver::class.java,
        )

        var totalActiveWidgets = 0

        for (receiverClass in tickingReceivers) {
            val ids = manager.getAppWidgetIds(ComponentName(context, receiverClass)) ?: intArrayOf()
            if (ids.isNotEmpty()) {
                totalActiveWidgets += ids.size
                val intent = Intent(context, receiverClass).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            }
        }

        // Automatically stop background service if NO clock widgets are active on the home screen
        if (totalActiveWidgets == 0) {
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