package com.altusix.slate.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import com.altusix.slate.widgets.calendar.CalendarAnalogCalendarHybridReceiver
import com.altusix.slate.widgets.calendar.CalendarAnalogTimelineReceiver
import com.altusix.slate.widgets.calendar.CalendarArchitecturalAnalogReceiver
import com.altusix.slate.widgets.calendar.CalendarRadialArcReceiver
import com.altusix.slate.widgets.clock.analog.ClockAnalogPrecisionReceiver
import com.altusix.slate.widgets.clock.analog.ClockBauhausReceiver
import com.altusix.slate.widgets.clock.analog.ClockCyberSkeletonReceiver
import com.altusix.slate.widgets.clock.analog.ClockSculptedPillReceiver
import com.altusix.slate.widgets.clock.analog.ClockBoldTypographyReceiver
import com.altusix.slate.widgets.clock.analog.ClockCyberCondensedReceiver
import com.altusix.slate.widgets.clock.analog.ClockCapsuleSkeletonReceiver
import com.altusix.slate.widgets.clock.analog.ClockApexArrowheadReceiver
import com.altusix.slate.widgets.clock.analog.ClockConcentricOrbitalReceiver
import com.altusix.slate.widgets.clock.analog.ClockTripleOrbitalDotsReceiver
import com.altusix.slate.widgets.clock.analog.ClockSectorSweepReceiver
import com.altusix.slate.widgets.clock.analog.ClockRotatingRingReceiver
import com.altusix.slate.widgets.clock.analog.ClockHourglassReceiver
import com.altusix.slate.widgets.clock.analog.ClockMinimalDotsReceiver
import com.altusix.slate.widgets.clock.analog.ClockRadarScopeReceiver
import com.altusix.slate.widgets.clock.analog.BaseClockReceiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalBoldTypographicReceiver

class SlateClockTickerService : Service() {

    private var tickerJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // ⚡ CACHED RECEIVERS: Prevents Garbage Collection churn every second
    private val receiverInstances: Map<Class<*>, Any> by lazy {
        listOf(
            // Calendar Analog Hybrids
            CalendarAnalogTimelineReceiver::class.java,
            CalendarAnalogCalendarHybridReceiver::class.java,
            CalendarArchitecturalAnalogReceiver::class.java,
            CalendarRadialArcReceiver::class.java,

            // Clock Analog Widgets
            ClockAnalogPrecisionReceiver::class.java,
            ClockBauhausReceiver::class.java,
            ClockCyberSkeletonReceiver::class.java,
            ClockSculptedPillReceiver::class.java,
            ClockBoldTypographyReceiver::class.java,
            ClockCyberCondensedReceiver::class.java,
            ClockCapsuleSkeletonReceiver::class.java,
            ClockApexArrowheadReceiver::class.java,
            ClockConcentricOrbitalReceiver::class.java,
            ClockTripleOrbitalDotsReceiver::class.java,
            ClockSectorSweepReceiver::class.java,
            ClockRotatingRingReceiver::class.java,
            ClockHourglassReceiver::class.java,
            ClockMinimalDotsReceiver::class.java,
            ClockRadarScopeReceiver::class.java,

            // Clock Digital Widgets
            ClockDigitalBoldTypographicReceiver::class.java
        ).associateWith { clazz ->
            try {
                clazz.getDeclaredConstructor().newInstance()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

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
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

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
                val now = System.currentTimeMillis()

                // 1. Perform Widget Updates
                updateAllTickingWidgets(this@SlateClockTickerService)

                // 2. Exact Millisecond Alignment: Prevents timing drift
                val millisInSecond = now % 1000L
                val nextTickDelay = (1000L - millisInSecond).coerceIn(100L, 1000L)

                delay(nextTickDelay)
            }
        }
    }

    private fun stopTicking() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun updateAllTickingWidgets(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        var totalActiveWidgets = 0

        for ((receiverClass, instance) in receiverInstances) {
            val ids = manager.getAppWidgetIds(ComponentName(context, receiverClass)) ?: intArrayOf()
            if (ids.isNotEmpty()) {
                totalActiveWidgets += ids.size

                try {
                    when (instance) {
                        is BaseClockReceiver -> instance.onUpdate(context, manager, ids)
                        else -> {
                            // Fallback invocation for hybrid receivers
                            val method = receiverClass.getMethod(
                                "onUpdate",
                                Context::class.java,
                                AppWidgetManager::class.java,
                                IntArray::class.java
                            )
                            method.invoke(instance, context, manager, ids)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Auto-stop service if no clock/hybrid widgets are placed on home screen
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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Slate Clock Ticker",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Keeps analog clock widgets active"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Slate Clock Active")
            .setContentText("Updating live home screen widgets")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "slate_clock_ticker_channel"
        private const val NOTIFICATION_ID = 9001
    }
}