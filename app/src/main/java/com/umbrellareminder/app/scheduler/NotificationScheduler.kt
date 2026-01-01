package com.umbrellareminder.app.scheduler

import android.content.Context
import androidx.work.*
import com.umbrellareminder.app.work.WeatherCheckWorker
import java.time.*
import java.util.concurrent.TimeUnit

class NotificationScheduler(private val context: Context) {

    fun scheduleDailyNotification() {
        cancelExistingWork()
        
        val currentTime = LocalTime.now()
        val scheduledTime = LocalTime.of(7, 30) // 7:30 AM
        
        val initialDelay = calculateInitialDelay(currentTime, scheduledTime)
        
        val dailyWorkRequest = PeriodicWorkRequestBuilder<WeatherCheckWorker>(
            1, TimeUnit.DAYS
        ).setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
         .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(false)
                .setRequiresCharging(false)
                .build()
        )
         .addTag("weather_check")
         .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WeatherCheckWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            dailyWorkRequest
        )
    }

    fun cancelDailyNotification() {
        WorkManager.getInstance(context).cancelUniqueWork(WeatherCheckWorker.WORK_NAME)
    }

    private fun cancelExistingWork() {
        WorkManager.getInstance(context).cancelUniqueWork(WeatherCheckWorker.WORK_NAME)
    }

    private fun calculateInitialDelay(currentTime: LocalTime, scheduledTime: LocalTime): Long {
        val currentDate = LocalDate.now()
        var scheduledDateTime = LocalDateTime.of(currentDate, scheduledTime)
        
        // If the scheduled time has already passed today, schedule for tomorrow
        if (currentTime.isAfter(scheduledTime)) {
            scheduledDateTime = scheduledDateTime.plusDays(1)
        }
        
        // Use device's local timezone instead of Paris timezone
        val localZone = ZoneId.systemDefault()
        val currentZonedDateTime = ZonedDateTime.now(localZone)
        val scheduledZonedDateTime = scheduledDateTime.atZone(localZone)
        
        return Duration.between(currentZonedDateTime, scheduledZonedDateTime).toMillis()
    }

    fun isNotificationScheduled(): Boolean {
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(WeatherCheckWorker.WORK_NAME)
        
        return try {
            workInfos.get().any { !it.state.isFinished }
        } catch (e: Exception) {
            false
        }
    }
}
