package com.altusix.slate.widgets.photos

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock

object PhotosAlarmScheduler {

    fun scheduleRotation(context: Context, receiverClass: Class<*>, widgetId: Int, intervalMinutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, receiverClass).apply {
            action = BasePhotosReceiver.ACTION_CYCLE_PHOTO
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            putExtra(BasePhotosReceiver.EXTRA_DELTA, 1)
            data = Uri.parse("slate_photos://$widgetId/cycle_timer")
        }

        val pi = PendingIntent.getBroadcast(
            context,
            widgetId * 53 + 99,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pi)

        if (intervalMinutes > 0) {
            val intervalMs = intervalMinutes * 60 * 1000L
            val firstTrigger = SystemClock.elapsedRealtime() + intervalMs
            alarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME,
                firstTrigger,
                intervalMs,
                pi
            )
        }
    }
}