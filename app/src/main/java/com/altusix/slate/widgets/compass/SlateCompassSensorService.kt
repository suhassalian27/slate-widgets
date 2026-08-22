package com.altusix.slate.widgets.compass

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class SlateCompassSensorService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var rotationVectorSensor: Sensor? = null
    private var lastAzimuth = 0f

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> checkAndStartListening()
                Intent.ACTION_SCREEN_OFF -> stopListening()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
            registerReceiver(screenStateReceiver, filter)
        } catch (_: Exception) {}

        checkAndStartListening()
    }

    private fun hasActiveCompassWidgets(): Boolean {
        val prefs = getSharedPreferences("slate_compass_prefs", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        return prefs.all.keys.any { it.startsWith("widget_") && it.endsWith("_active_until") && (prefs.all[it] as? Long ?: 0L) > now }
    }

    private fun checkAndStartListening() {
        if (hasActiveCompassWidgets()) {
            rotationVectorSensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
        } else {
            stopSelf()
        }
    }

    private fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!hasActiveCompassWidgets()) {
            stopSelf()
            return
        }

        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)

            var azimuthDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
            azimuthDeg = (azimuthDeg + 360) % 360

            if (Math.abs(azimuthDeg - lastAzimuth) >= 1.5f) {
                lastAzimuth = azimuthDeg
                val prefs = getSharedPreferences("slate_compass_prefs", Context.MODE_PRIVATE)
                prefs.edit().putFloat("last_azimuth", azimuthDeg).apply()
                updateAllCompassWidgets(this)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        checkAndStartListening()
        return START_STICKY
    }

    override fun onDestroy() {
        stopListening()
        try { unregisterReceiver(screenStateReceiver) } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Slate Compass Active", NotificationManager.IMPORTANCE_MIN).apply {
                description = "Keeps compass direction updated"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Slate Compass Active")
            .setContentText("Listening to orientation sensors")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "slate_compass_service_channel"
        private const val NOTIFICATION_ID = 9002

        fun ensureStarted(context: Context) {
            try {
                val intent = Intent(context, SlateCompassSensorService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}