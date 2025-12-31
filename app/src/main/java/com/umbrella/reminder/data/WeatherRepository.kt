package com.umbrella.reminder.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

class WeatherRepository {
    private val weatherApiService: WeatherApiService

    init {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/v1/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        weatherApiService = retrofit.create(WeatherApiService::class.java)
    }

    suspend fun getWeatherForecast(latitude: Double, longitude: Double): Result<WeatherResponse> {
        return try {
            val response = weatherApiService.getWeatherForecast(latitude, longitude)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun shouldTakeUmbrella(weatherResponse: WeatherResponse): Boolean {
        val hourlyData = weatherResponse.hourly
        
        // Check for rain between 8AM (8:00) and 6PM (18:00)
        val relevantHours = mutableListOf<Int>()
        
        for (i in hourlyData.times.indices) {
            val timeString = hourlyData.times[i]
            val hour = extractHourFromTimeString(timeString)
            
            if (hour in 8..17) { // 8AM to 5:59PM (6PM is exclusive)
                relevantHours.add(i)
            }
        }

        // Check if any relevant hour has precipitation probability > 30% or rain weather code
        for (hourIndex in relevantHours) {
            val precipitationProb = hourlyData.precipitationProbabilities[hourIndex]
            val weatherCode = hourlyData.weatherCodes[hourIndex]
            
            // Weather codes for rain: 51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82
            val rainCodes = setOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82)
            
            if (precipitationProb > 30 || weatherCode in rainCodes) {
                return true
            }
        }
        
        return false
    }

    private fun extractHourFromTimeString(timeString: String): Int {
        // Extract hour from ISO format time string like "2023-12-25T08:00"
        return try {
            val hourPart = timeString.substringAfter("T").substringBefore(":")
            hourPart.toInt()
        } catch (e: Exception) {
            0
        }
    }
}
