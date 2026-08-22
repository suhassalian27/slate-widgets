package com.altusix.slate.widgets.compass

import android.content.Context

data class CompassState(
    val azimuthDegrees: Float = 0f,
    val cardinalDirection: String = "N",
    val isActive: Boolean = false
) {
    companion object {
        fun getCardinal(degrees: Float): String {
            val deg = (degrees % 360 + 360) % 360
            return when {
                deg >= 337.5 || deg < 22.5 -> "N"
                deg >= 22.5 && deg < 67.5 -> "NE"
                deg >= 67.5 && deg < 112.5 -> "E"
                deg >= 112.5 && deg < 157.5 -> "SE"
                deg >= 157.5 && deg < 202.5 -> "S"
                deg >= 202.5 && deg < 247.5 -> "SW"
                deg >= 247.5 && deg < 292.5 -> "W"
                deg >= 292.5 && deg < 337.5 -> "NW"
                else -> "N"
            }
        }

        fun readCurrentHeading(context: Context, widgetId: Int): CompassState {
            val prefs = context.getSharedPreferences("slate_compass_prefs", Context.MODE_PRIVATE)
            val heading = prefs.getFloat("last_azimuth", 0f)
            val activeUntil = prefs.getLong("widget_${widgetId}_active_until", 0L)
            val isActive = System.currentTimeMillis() < activeUntil
            return CompassState(heading, getCardinal(heading), isActive)
        }
    }
}