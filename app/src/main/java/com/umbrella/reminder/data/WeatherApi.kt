package com.umbrella.reminder.data

import retrofit2.http.GET
import retrofit2.http.Query
import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double,
    @SerializedName("timezone") val timezone: String,
    @SerializedName("current") val current: CurrentWeather,
    @SerializedName("hourly") val hourly: HourlyForecast
)

data class CurrentWeather(
    @SerializedName("temp") val temperature: Double,
    @SerializedName("weather") val weather: List<WeatherCondition>
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

interface WeatherApiService {
    @GET("forecast")
    suspend fun getWeatherForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("hourly") hourly: String = "temperature_2m,precipitation_probability,weathercode",
        @Query("forecast_hours") forecastHours: Int = 24,
        @Query("timezone") timezone: String = "Europe/Paris"
    ): WeatherResponse
}
