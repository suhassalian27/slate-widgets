package com.altusix.slate.widgets.health

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StepSensorManager {

    private const val PREFS_NAME = "slate_health_sensor_prefs"
    private const val KEY_MIDNIGHT_BASELINE = "midnight_baseline"
    private const val KEY_LAST_RECORDED_STEPS = "last_recorded_steps"
    private const val KEY_TODAY_STEPS = "today_steps"
    private const val KEY_DATE_STAMP = "date_stamp"
    private const val KEY_DAILY_STEP_GOAL = "daily_step_goal"

    fun hasPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun getTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun getTodaySteps(context: Context): Int {
        if (!hasPermission(context)) return 0
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedDate = prefs.getString(KEY_DATE_STAMP, "")
        val today = getTodayDate()

        // If today is a new calendar day, reset current steps to 0
        if (savedDate != today) {
            return 0
        }
        return prefs.getInt(KEY_TODAY_STEPS, 0)
    }

    fun getDailyGoal(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_DAILY_STEP_GOAL, 10000)
    }

    fun setDailyGoal(context: Context, goal: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_DAILY_STEP_GOAL, goal).apply()
    }

    /**
     * One-shot hardware sensor poll. Registers listener, reads current cumulative hardware count,
     * computes today's steps, updates preferences, and unregisters to save power.
     */
    fun pollCurrentHardwareSteps(context: Context, onUpdated: ((Int) -> Unit)? = null) {
        if (!hasPermission(context)) {
            onUpdated?.invoke(0)
            return
        }

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
        val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null || event.values.isEmpty()) return

                val totalHardwareSteps = event.values[0].toInt()
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val today = getTodayDate()
                val savedDate = prefs.getString(KEY_DATE_STAMP, "")
                var baseline = prefs.getInt(KEY_MIDNIGHT_BASELINE, -1)

                // Date rollover or initial setup: establish new baseline
                if (savedDate != today || baseline == -1 || totalHardwareSteps < baseline) {
                    baseline = totalHardwareSteps
                    prefs.edit()
                        .putString(KEY_DATE_STAMP, today)
                        .putInt(KEY_MIDNIGHT_BASELINE, baseline)
                        .apply()
                }

                val computedSteps = (totalHardwareSteps - baseline).coerceAtLeast(0)

                prefs.edit()
                    .putInt(KEY_LAST_RECORDED_STEPS, totalHardwareSteps)
                    .putInt(KEY_TODAY_STEPS, computedSteps)
                    .apply()

                sensorManager.unregisterListener(this)
                onUpdated?.invoke(computedSteps)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun computeActiveKcal(steps: Int): Int = (steps * 0.04f).toInt()

    fun computeDistanceKm(steps: Int): Float = ((steps * 0.000762f) * 10f).toInt() / 10f

    fun computeActiveMinutes(steps: Int): Int = (steps / 100).coerceAtLeast(0)
}