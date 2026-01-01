package com.umbrellareminder.app.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

class WeatherRepository {
    private val weatherApiService: WeatherApiService
    private val geocodingApiService: GeocodingApiService

    init {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()

        // Weather API
        val weatherRetrofit = Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/v1/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        weatherApiService = weatherRetrofit.create(WeatherApiService::class.java)

        // Geocoding API (Nominatim)
        val geocodingOkHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("User-Agent", "UmbrellaReminder App (https://github.com/Yukloe/umbrellaReminder)")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(loggingInterceptor)
            .build()

        val geocodingRetrofit = Retrofit.Builder()
            .baseUrl("https://nominatim.openstreetmap.org/")
            .client(geocodingOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        geocodingApiService = geocodingRetrofit.create(GeocodingApiService::class.java)
    }

    suspend fun getWeatherForecast(latitude: Double, longitude: Double): Result<WeatherResponse> {
        return try {
            // Get user's local timezone
            val localZone = java.time.ZoneId.systemDefault()
            val timezoneId = localZone.id
            
            android.util.Log.d("WeatherRepository", "Fetching weather for lat: $latitude, lon: $longitude, timezone: $timezoneId")
            
            val response = weatherApiService.getWeatherForecast(latitude, longitude, timezone = timezoneId)
            android.util.Log.d("WeatherRepository", "Weather API response received")
            Result.success(response)
        } catch (e: Exception) {
            android.util.Log.e("WeatherRepository", "Weather API failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getLocationName(latitude: Double, longitude: Double): Result<String> {
        return try {
            android.util.Log.d("WeatherRepository", "Getting location name for lat: $latitude, lon: $longitude")
            
            val response = geocodingApiService.reverseGeocode(latitude, longitude)
            android.util.Log.d("WeatherRepository", "Geocoding response received")
            
            // Extract location name from Nominatim response
            val locationName = if (response.displayName != null) {
                // Use display_name which is already formatted nicely
                response.displayName.split(",").take(2).joinToString(",").trim()
            } else if (response.address != null) {
                val address = response.address
                val city = address.city ?: address.town ?: address.village
                val state = address.state ?: address.county
                if (city != null && state != null && city != state) {
                    "$city, $state"
                } else if (city != null) {
                    city
                } else {
                    "Your Location"
                }
            } else {
                "Your Location"
            }
            
            android.util.Log.d("WeatherRepository", "Final location name: $locationName")
            Result.success(locationName)
        } catch (e: Exception) {
            android.util.Log.e("WeatherRepository", "Geocoding failed: ${e.message}", e)
            Result.success("Your Location") // Fallback
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
