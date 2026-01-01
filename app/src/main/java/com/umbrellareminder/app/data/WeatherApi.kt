package com.umbrellareminder.app.data

import retrofit2.http.GET
import retrofit2.http.Query
import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("timezone") val timezone: String,
    @SerializedName("current_weather") val currentWeather: CurrentWeather?,
    @SerializedName("hourly") val hourly: HourlyForecast
)

data class CurrentWeather(
    @SerializedName("temperature") val temperature: Double,
    @SerializedName("weathercode") val weatherCode: Int
)

data class HourlyForecast(
    @SerializedName("time") val times: List<String>,
    @SerializedName("temperature_2m") val temperatures: List<Double>,
    @SerializedName("precipitation_probability") val precipitationProbabilities: List<Int>,
    @SerializedName("weathercode") val weatherCodes: List<Int>
)

data class WeatherCondition(
    @SerializedName("id") val id: Int,
    @SerializedName("main") val main: String,
    @SerializedName("description") val description: String
)

data class GeocodingResponse(
    @SerializedName("name") val name: String?,
    @SerializedName("display_name") val displayName: String?,
    @SerializedName("address") val address: GeocodingAddress?
)

data class GeocodingAddress(
    @SerializedName("city") val city: String?,
    @SerializedName("town") val town: String?,
    @SerializedName("village") val village: String?,
    @SerializedName("county") val county: String?,
    @SerializedName("state") val state: String?,
    @SerializedName("country") val country: String?
)

data class GeocodingResult(
    @SerializedName("name") val name: String,
    @SerializedName("admin1") val admin1: String? = null,
    @SerializedName("country") val country: String? = null
)

interface WeatherApiService {
    @GET("forecast")
    suspend fun getWeatherForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("hourly") hourly: String = "temperature_2m,precipitation_probability,weathercode",
        @Query("current_weather") currentWeather: String = "true",
        @Query("forecast_hours") forecastHours: Int = 24,
        @Query("timezone") timezone: String
    ): WeatherResponse
}

interface GeocodingApiService {
    @GET("reverse")
    suspend fun reverseGeocode(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 1
    ): GeocodingResponse
}
