package com.altusix.slate.widgets.health

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DailyActivitySummary(
    val steps: Int,
    val stepTarget: Int,
    val activeKcal: Int,
    val distanceKm: Float,
    val activeMinutes: Int,
    val activeTargetMinutes: Int = 45,
    val isPermissionGranted: Boolean
)

data class WeeklyDayStat(
    val dayLabel: String,
    val steps: Int,
    val isToday: Boolean
)

object HealthStorageKeys {
    const val PREFS_NAME = "slate_health_widget_prefs"
    const val KEY_WATER_CURRENT = "current_water_ml"
    const val KEY_WATER_DATE = "water_date_key"

    const val ACTION_ADD_WATER_250 = "com.altusix.slate.health.ADD_WATER_250"
    const val ACTION_SUB_WATER_250 = "com.altusix.slate.health.SUB_WATER_250"
    const val ACTION_ADD_WATER_500 = "com.altusix.slate.health.ADD_WATER_500"
    const val ACTION_RESET_WATER = "com.altusix.slate.health.RESET_WATER"
}

fun subHydrationMl(context: Context, delta: Int) {
    val prefs = context.getSharedPreferences(HealthStorageKeys.PREFS_NAME, Context.MODE_PRIVATE)
    val today = getTodayDateKey()
    val current = getHydrationMl(context)
    val next = (current - delta).coerceAtLeast(0)
    prefs.edit()
        .putString(HealthStorageKeys.KEY_WATER_DATE, today)
        .putInt(HealthStorageKeys.KEY_WATER_CURRENT, next)
        .apply()
}

private fun getTodayDateKey(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

fun getHydrationMl(context: Context): Int {
    val prefs = context.getSharedPreferences(HealthStorageKeys.PREFS_NAME, Context.MODE_PRIVATE)
    val savedDate = prefs.getString(HealthStorageKeys.KEY_WATER_DATE, "")
    val today = getTodayDateKey()
    return if (savedDate == today) {
        prefs.getInt(HealthStorageKeys.KEY_WATER_CURRENT, 0)
    } else {
        prefs.edit()
            .putString(HealthStorageKeys.KEY_WATER_DATE, today)
            .putInt(HealthStorageKeys.KEY_WATER_CURRENT, 0)
            .apply()
        0
    }
}

fun addHydrationMl(context: Context, delta: Int) {
    val prefs = context.getSharedPreferences(HealthStorageKeys.PREFS_NAME, Context.MODE_PRIVATE)
    val today = getTodayDateKey()
    val current = getHydrationMl(context)
    val next = (current + delta).coerceIn(0, 5000)
    prefs.edit()
        .putString(HealthStorageKeys.KEY_WATER_DATE, today)
        .putInt(HealthStorageKeys.KEY_WATER_CURRENT, next)
        .apply()
}

fun resetHydration(context: Context) {
    val prefs = context.getSharedPreferences(HealthStorageKeys.PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit()
        .putString(HealthStorageKeys.KEY_WATER_DATE, getTodayDateKey())
        .putInt(HealthStorageKeys.KEY_WATER_CURRENT, 0)
        .apply()
}

fun getDailyActivitySummary(context: Context): DailyActivitySummary {
    val hasPerm = StepSensorManager.hasPermission(context)
    val steps = StepSensorManager.getTodaySteps(context)
    val goal = StepSensorManager.getDailyGoal(context)

    return DailyActivitySummary(
        steps = steps,
        stepTarget = goal,
        activeKcal = StepSensorManager.computeActiveKcal(steps),
        distanceKm = StepSensorManager.computeDistanceKm(steps),
        activeMinutes = StepSensorManager.computeActiveMinutes(steps),
        activeTargetMinutes = 45,
        isPermissionGranted = hasPerm
    )
}

fun getWeeklyStepStats(context: Context): List<WeeklyDayStat> {
    val todaySteps = StepSensorManager.getTodaySteps(context)
    return listOf(
        WeeklyDayStat("M", 0, false),
        WeeklyDayStat("T", 0, false),
        WeeklyDayStat("W", 0, false),
        WeeklyDayStat("T", 0, false),
        WeeklyDayStat("F", 0, false),
        WeeklyDayStat("S", 0, false),
        WeeklyDayStat("S", todaySteps, true)
    )
}