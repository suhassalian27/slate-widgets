package com.altusix.slate.widgets.weather

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class WeatherSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // 1. Fetch fresh weather from your repository/network source
            val city = WeatherPreferences.getSelectedCity(applicationContext)
            val freshData = WeatherRepository.fetchWeather(city.latitude, city.longitude, city.name)

            if (freshData != null) {
                // 2. Save fresh data to local cache
                WeatherPreferences.setCachedWeatherData(applicationContext, freshData)

                // 3. Re-render all placed weather widgets with fresh data
                updateAllWeatherWidgets(applicationContext)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "slate_weather_periodic_sync"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // Android enforces a 15-minute minimum for periodic background work
            val syncRequest = PeriodicWorkRequestBuilder<WeatherSyncWorker>(30, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }
    }
}