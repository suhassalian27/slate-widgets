package com.altusix.slate.widgets.battery

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class BatteryUpdateWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            updateAllBatteryWidgets(appContext)

            if (IS_DEV_MODE) {
                scheduleDevLoop(appContext)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "SlateBatteryUpdateWork"
        private const val DEV_WORK_NAME = "SlateDevLoopWork"

        // 🛠️ true = 1-minute loop for development | false = 15-minute standard release
        private const val IS_DEV_MODE = false

        fun schedule(context: Context) {
            if (IS_DEV_MODE) {
                scheduleDevLoop(context)
            } else {
                val workRequest = PeriodicWorkRequestBuilder<BatteryUpdateWorker>(
                    15, TimeUnit.MINUTES
                ).build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
            }
        }

        private fun scheduleDevLoop(context: Context) {
            val devWorkRequest = OneTimeWorkRequestBuilder<BatteryUpdateWorker>()
                .setInitialDelay(1, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                DEV_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                devWorkRequest
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            WorkManager.getInstance(context).cancelUniqueWork(DEV_WORK_NAME)
        }
    }
}