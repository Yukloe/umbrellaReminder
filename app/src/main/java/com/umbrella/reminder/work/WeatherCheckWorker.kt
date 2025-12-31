package com.umbrella.reminder.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.umbrella.reminder.data.WeatherRepository
import com.umbrella.reminder.location.LocationManager
import com.umbrella.reminder.notification.NotificationHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class WeatherCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val weatherRepository = WeatherRepository()
    private val locationManager = LocationManager(applicationContext)
    private val notificationHelper = NotificationHelper(applicationContext)

    override suspend fun doWork(): Result {
        return try {
            // Get current location
            val locationResult = locationManager.getLastKnownLocation()
            if (locationResult.isFailure) {
                return Result.failure()
            }

            val location = locationResult.getOrThrow()
            
            // Get weather forecast
            val weatherResult = weatherRepository.getWeatherForecast(
                location.latitude,
                location.longitude
            )
            
            if (weatherResult.isFailure) {
                return Result.failure()
            }

            val weatherResponse = weatherResult.getOrThrow()
            
            // Check if umbrella is needed
            val shouldTakeUmbrella = weatherRepository.shouldTakeUmbrella(weatherResponse)
            
            // Send notification
            notificationHelper.showUmbrellaNotification(shouldTakeUmbrella)
            
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "WeatherCheckWorker"
    }
}
