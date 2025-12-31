package com.umbrella.reminder.scheduler

import android.content.Context
import androidx.work.*
import com.umbrella.reminder.work.WeatherCheckWorker
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
                .build()
        )
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
        
        // Convert to Paris timezone
        val parisZone = ZoneId.of("Europe/Paris")
        val currentZonedDateTime = ZonedDateTime.now(parisZone)
        val scheduledZonedDateTime = scheduledDateTime.atZone(parisZone)
        
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
