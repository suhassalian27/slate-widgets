package com.altusix.slate.widgets.clock.analog

import java.util.Calendar

/**
 * Domain UI State holder for Analog Clock rendering
 */
data class AnalogClockTimeState(
    val hours: Int,
    val minutes: Int,
    val seconds: Int,
    val millis: Int
) {
    val secondsWithMillis: Float get() = seconds + (millis / 1000f)
    val minutesWithSeconds: Float get() = minutes + (secondsWithMillis / 60f)
    val hoursWithMinutes: Float get() = (hours % 12) + (minutesWithSeconds / 60f)

    val hourAngleRad: Double get() = Math.toRadians((hoursWithMinutes * 30f - 90f).toDouble())
    val minuteAngleRad: Double get() = Math.toRadians((minutesWithSeconds * 6f - 90f).toDouble())
    val secondAngleRad: Double get() = Math.toRadians((secondsWithMillis * 6f - 90f).toDouble())

    companion object {
        fun now(): AnalogClockTimeState {
            val cal = Calendar.getInstance()
            return AnalogClockTimeState(
                hours = cal.get(Calendar.HOUR),
                minutes = cal.get(Calendar.MINUTE),
                seconds = cal.get(Calendar.SECOND),
                millis = cal.get(Calendar.MILLISECOND)
            )
        }
    }
}