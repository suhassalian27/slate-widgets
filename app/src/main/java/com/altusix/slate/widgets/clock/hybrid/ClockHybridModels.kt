package com.altusix.slate.widgets.clock.hybrid

import java.util.Calendar

data class HybridClockTimeState(
    val hour24: Int,
    val hour12: Int,
    val minute: Int,
    val second: Int,
    val dayOfWeek: String,
    val dayOfMonth: String,
    val monthName: String,
    val year: String
) {
    companion object {
        fun now(): HybridClockTimeState {
            val cal = Calendar.getInstance()
            val dayOfWeek = java.text.SimpleDateFormat("EEE", java.util.Locale.ENGLISH).format(cal.time).uppercase()
            val monthName = java.text.SimpleDateFormat("MMM", java.util.Locale.ENGLISH).format(cal.time).uppercase()

            return HybridClockTimeState(
                hour24 = cal.get(Calendar.HOUR_OF_DAY),
                hour12 = cal.get(Calendar.HOUR).let { if (it == 0) 12 else it },
                minute = cal.get(Calendar.MINUTE),
                second = cal.get(Calendar.SECOND),
                dayOfWeek = dayOfWeek,
                dayOfMonth = cal.get(Calendar.DAY_OF_MONTH).toString(),
                monthName = monthName,
                year = cal.get(Calendar.YEAR).toString()
            )
        }
    }
}