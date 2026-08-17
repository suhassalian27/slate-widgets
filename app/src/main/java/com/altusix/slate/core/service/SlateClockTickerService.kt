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
import com.altusix.slate.widgets.clock.digital.ClockDigitalAsymmetricOverlayReceiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalAsymmetricSlantedReceiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalBoldTypographicReceiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalMinimalDividerReceiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalLcdSevenSegmentReceiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalCompactBlockReceiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalDualPillStackReceiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalGiantHourCapsuleReceiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalGradientTallReceiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalModern3dLedReceiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalScriptOverlayReceiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalSplitFlapReceiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalTextFont10Receiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalTextFont11Receiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalTextFont12Receiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalTextFont13Receiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalTextFont14Receiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalTextFont15Receiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalTextFont16Receiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalTextFont17Receiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalTextFont18Receiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalTextFont19Receiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalTextFont1Receiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalTextFont20Receiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalTextFont21Receiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalTextFont22Receiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalTextFont23Receiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalTextFont24Receiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalTextFont25Receiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalTextFont26Receiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalTextFont27Receiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalTextFont28Receiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalTextFont29Receiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalTextFont2Receiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalTextFont30Receiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalTextFont31Receiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalTextFont32Receiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalTextFont33Receiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalTextFont34Receiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalTextFont3Receiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalTextFont4Receiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalTextFont5Receiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalTextFont6Receiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalTextFont7Receiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalTextFont8Receiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalTextFont9Receiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalTextWordReceiver
import com.altusix.slate.widgets.clock.digital.ClockDigitalVerticalCapsuleReceiver

class SlateClockTickerService : Service() {

    private var tickerJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // 1-Second Ticker Receivers (Analog / Animated)
    private val secondReceivers: Map<Class<*>, Any> by lazy {
        listOf(
            CalendarAnalogTimelineReceiver::class.java,
            CalendarAnalogCalendarHybridReceiver::class.java,
            CalendarArchitecturalAnalogReceiver::class.java,
            CalendarRadialArcReceiver::class.java,
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
            ClockRadarScopeReceiver::class.java
        ).associateWith { clazz ->
            try { clazz.getDeclaredConstructor().newInstance() } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // 1-Minute Ticker Receivers (Digital / Static Clocks)
    private val minuteReceivers: Map<Class<*>, Any> by lazy {
        listOf(
            ClockDigitalBoldTypographicReceiver::class.java,
            ClockDigitalMinimalDividerReceiver::class.java,
            ClockDigitalLcdSevenSegmentReceiver::class.java,
            ClockDigitalAsymmetricSlantedReceiver::class.java,
            ClockDigitalCompactBlockReceiver::class.java,
            ClockDigitalAsymmetricOverlayReceiver::class.java,
            ClockDigitalTextWordReceiver::class.java,
            ClockDigitalGiantHourCapsuleReceiver::class.java,
            ClockDigitalModern3dLedReceiver::class.java,
            ClockDigitalGradientTallReceiver::class.java,
            ClockDigitalScriptOverlayReceiver::class.java,
            ClockDigitalSplitFlapReceiver::class.java,
            ClockDigitalVerticalCapsuleReceiver::class.java,
            ClockDigitalDualPillStackReceiver::class.java,
            ClockDigitalTextFont1Receiver::class.java,
            ClockDigitalTextFont2Receiver::class.java,
            ClockDigitalTextFont3Receiver::class.java,
            ClockDigitalTextFont4Receiver::class.java,
            ClockDigitalTextFont5Receiver::class.java,
            ClockDigitalTextFont6Receiver::class.java,
            ClockDigitalTextFont7Receiver::class.java,
            ClockDigitalTextFont8Receiver::class.java,
            ClockDigitalTextFont9Receiver::class.java,
            ClockDigitalTextFont10Receiver::class.java,
            ClockDigitalTextFont11Receiver::class.java,
            ClockDigitalTextFont12Receiver::class.java,
            ClockDigitalTextFont13Receiver::class.java,
            ClockDigitalTextFont14Receiver::class.java,
            ClockDigitalTextFont15Receiver::class.java,
            ClockDigitalTextFont16Receiver::class.java,
            ClockDigitalTextFont17Receiver::class.java,
            ClockDigitalTextFont18Receiver::class.java,
            ClockDigitalTextFont19Receiver::class.java,
            ClockDigitalTextFont20Receiver::class.java,
            ClockDigitalTextFont21Receiver::class.java,
            ClockDigitalTextFont22Receiver::class.java,
            ClockDigitalTextFont23Receiver::class.java,
            ClockDigitalTextFont24Receiver::class.java,
            ClockDigitalTextFont25Receiver::class.java,
            ClockDigitalTextFont26Receiver::class.java,
            ClockDigitalTextFont27Receiver::class.java,
            ClockDigitalTextFont28Receiver::class.java,
            ClockDigitalTextFont29Receiver::class.java,
            ClockDigitalTextFont30Receiver::class.java,
            ClockDigitalTextFont31Receiver::class.java,
            ClockDigitalTextFont32Receiver::class.java,
            ClockDigitalTextFont33Receiver::class.java,
            ClockDigitalTextFont34Receiver::class.java
        ).associateWith { clazz ->
            try { clazz.getDeclaredConstructor().newInstance() } catch (e: Exception) { e.printStackTrace() }
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
                val hasActiveSecondWidgets = updateSecondWidgets()
                val hasActiveMinuteWidgets = updateMinuteWidgets()

                if (!hasActiveSecondWidgets && !hasActiveMinuteWidgets) {
                    stopSelf()
                    break
                }

                if (hasActiveSecondWidgets) {
                    // Precision 1-second alignment
                    val millisInSecond = now % 1000L
                    val nextTickDelay = (1000L - millisInSecond).coerceIn(100L, 1000L)
                    delay(nextTickDelay)
                } else {
                    // Exact top-of-minute alignment (:00.050s)
                    val millisInMinute = now % 60000L
                    val nextMinuteDelay = (60000L - millisInMinute + 50L).coerceIn(500L, 60000L)
                    delay(nextMinuteDelay)
                }
            }
        }
    }

    private fun stopTicking() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun updateSecondWidgets(): Boolean {
        val manager = AppWidgetManager.getInstance(this)
        var count = 0
        for ((receiverClass, instance) in secondReceivers) {
            val ids = manager.getAppWidgetIds(ComponentName(this, receiverClass)) ?: intArrayOf()
            if (ids.isNotEmpty()) {
                count += ids.size
                try {
                    val method = receiverClass.getMethod("onUpdate", Context::class.java, AppWidgetManager::class.java, IntArray::class.java)
                    method.invoke(instance, this, manager, ids)
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
        return count > 0
    }

    private fun updateMinuteWidgets(): Boolean {
        val manager = AppWidgetManager.getInstance(this)
        var count = 0
        for ((receiverClass, instance) in minuteReceivers) {
            val ids = manager.getAppWidgetIds(ComponentName(this, receiverClass)) ?: intArrayOf()
            if (ids.isNotEmpty()) {
                count += ids.size
                try {
                    val method = receiverClass.getMethod("onUpdate", Context::class.java, AppWidgetManager::class.java, IntArray::class.java)
                    method.invoke(instance, this, manager, ids)
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
        return count > 0
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startTicking()
        return START_STICKY
    }

    override fun onDestroy() {
        stopTicking()
        try { unregisterReceiver(screenStateReceiver) } catch (_: Exception) {}
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Slate Clock Ticker", NotificationManager.IMPORTANCE_MIN).apply {
                description = "Keeps live clock widgets updated"
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