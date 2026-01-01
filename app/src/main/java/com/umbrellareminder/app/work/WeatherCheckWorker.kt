package com.umbrellareminder.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.umbrellareminder.app.data.WeatherRepository
import com.umbrellareminder.app.location.LocationManager
import com.umbrellareminder.app.notification.NotificationHelper
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
            
            // Get location name
            val locationNameResult = weatherRepository.getLocationName(
                location.latitude,
                location.longitude
            )
            val locationName = locationNameResult.getOrNull()
            
            // Get current temperature
            val currentTemperature = weatherResponse.currentWeather?.temperature 
                ?: weatherResponse.hourly.temperatures.firstOrNull()
            
            // Check if umbrella is needed
            val shouldTakeUmbrella = weatherRepository.shouldTakeUmbrella(weatherResponse)
            
            // Send notification with location and temperature
            notificationHelper.showUmbrellaNotification(
                shouldTakeUmbrella,
                locationName,
                currentTemperature
            )
            
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "WeatherCheckWorker"
    }
}
