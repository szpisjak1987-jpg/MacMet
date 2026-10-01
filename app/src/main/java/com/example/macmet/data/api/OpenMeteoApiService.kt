package com.example.macmet.data.api

import com.example.macmet.data.model.api.OpenMeteoResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApiService {

    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = DEFAULT_CURRENT_METRICS,
        @Query("hourly") hourly: String = DEFAULT_HOURLY_METRICS,
        @Query("daily") daily: String = DEFAULT_DAILY_METRICS,
        @Query("timezone") timezone: String = "auto"
    ): OpenMeteoResponseDto

    companion object {
        const val BASE_URL = "https://api.open-meteo.com/"
        const val DEFAULT_CURRENT_METRICS = "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,wind_speed_10m,wind_direction_10m"
        const val DEFAULT_HOURLY_METRICS = "temperature_2m,relative_humidity_2m,precipitation_probability,precipitation,weather_code,wind_speed_10m,visibility,wind_gusts_10m"
        const val DEFAULT_DAILY_METRICS = "weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max"
    }
}
