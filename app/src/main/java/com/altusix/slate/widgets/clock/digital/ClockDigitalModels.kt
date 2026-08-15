package com.altusix.slate.widgets.clock.digital

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class DigitalClockTimeState(
    val hour12: String,
    val hour24: String,
    val minute: String,
    val second: String,
    val amPm: String,
    val dayOfWeek: String,
    val monthName: String,
    val dayOfMonth: String,
    val year: String
) {
    companion object {
        fun now(): DigitalClockTimeState {
            val cal = Calendar.getInstance()
            val sdf12 = SimpleDateFormat("hh", Locale.getDefault())
            val sdf24 = SimpleDateFormat("HH", Locale.getDefault())
            val sdfMin = SimpleDateFormat("mm", Locale.getDefault())
            val sdfSec = SimpleDateFormat("ss", Locale.getDefault())
            val sdfAmPm = SimpleDateFormat("a", Locale.getDefault())
            val sdfDay = SimpleDateFormat("EEE", Locale.getDefault())
            val sdfMonth = SimpleDateFormat("MMM", Locale.getDefault())
            val sdfDate = SimpleDateFormat("d", Locale.getDefault())
            val sdfYear = SimpleDateFormat("yyyy", Locale.getDefault())

            return DigitalClockTimeState(
                hour12 = sdf12.format(cal.time),
                hour24 = sdf24.format(cal.time),
                minute = sdfMin.format(cal.time),
                second = sdfSec.format(cal.time),
                amPm = sdfAmPm.format(cal.time).uppercase(Locale.getDefault()),
                dayOfWeek = sdfDay.format(cal.time).uppercase(Locale.getDefault()),
                monthName = sdfMonth.format(cal.time).uppercase(Locale.getDefault()),
                dayOfMonth = sdfDate.format(cal.time),
                year = sdfYear.format(cal.time)
            )
        }
    }
}