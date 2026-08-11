package com.altusix.slate.widgets.calendar

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CalendarPillState(
    val dayOfMonth: String,
    val monthShort: String,
    val dayOfWeekFull: String
)

data class CalendarDay(
    val dayNumber: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isSunday: Boolean
)

data class CalendarMonthState(
    val monthShort: String,
    val year: String,
    val daysGrid: List<CalendarDay>
)

data class CalendarDateState(
    val monthShort: String,
    val year: String,
    val dayOfMonth: String,
    val dayOfWeekShort: String
)

object CalendarEngine {

    fun getPillState(): CalendarPillState {
        val now = Date()
        val dayOfMonth = SimpleDateFormat("d", Locale.US).format(now)
        val monthShort = SimpleDateFormat("MMM", Locale.US).format(now).uppercase(Locale.US)
        val dayOfWeekFull = SimpleDateFormat("EEEE", Locale.US).format(now)
        return CalendarPillState(dayOfMonth, monthShort, dayOfWeekFull)
    }

    fun getDateState(): CalendarDateState {
        val now = Date()
        return CalendarDateState(
            monthShort = SimpleDateFormat("MMM", Locale.US).format(now).uppercase(Locale.US),
            year = SimpleDateFormat("yyyy", Locale.US).format(now),
            dayOfMonth = SimpleDateFormat("d", Locale.US).format(now),
            dayOfWeekShort = SimpleDateFormat("EEE", Locale.US).format(now).uppercase(Locale.US)
        )
    }

    fun getMonthGridState(): CalendarMonthState {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val today = cal.get(Calendar.DAY_OF_MONTH)

        val monthShort = SimpleDateFormat("MMM", Locale.US).format(cal.time).uppercase(Locale.US)
        val yearStr = year.toString()

        val days = mutableListOf<CalendarDay>()

        // Reset to 1st of the month
        cal.set(Calendar.DAY_OF_MONTH, 1)
        var firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 2
        if (firstDayOfWeek < 0) firstDayOfWeek = 6 // Make Monday = 0, Sunday = 6

        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Previous month padding
        val prevCal = cal.clone() as Calendar
        prevCal.add(Calendar.MONTH, -1)
        val prevMaxDays = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (i in firstDayOfWeek - 1 downTo 0) {
            days.add(CalendarDay(prevMaxDays - i, isCurrentMonth = false, isToday = false, isSunday = false))
        }

        // Current month
        for (d in 1..maxDays) {
            cal.set(Calendar.DAY_OF_MONTH, d)
            val isSunday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
            days.add(CalendarDay(d, isCurrentMonth = true, isToday = (d == today), isSunday = isSunday))
        }

        // Next month padding to complete 42 cells (6 rows)
        var nextDay = 1
        while (days.size < 42) {
            days.add(CalendarDay(nextDay++, isCurrentMonth = false, isToday = false, isSunday = false))
        }

        return CalendarMonthState(monthShort, yearStr, days)
    }
}