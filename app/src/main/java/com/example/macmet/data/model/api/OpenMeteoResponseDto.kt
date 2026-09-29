package com.example.macmet.data.model.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenMeteoResponseDto(
    @field:Json(name = "latitude") val latitude: Double,
    @field:Json(name = "longitude") val longitude: Double,
    @field:Json(name = "generationtime_ms") val generationtimeMs: Double? = null,
    @field:Json(name = "utc_offset_seconds") val utcOffsetSeconds: Int? = null,
    @field:Json(name = "timezone") val timezone: String? = null,
    @field:Json(name = "timezone_abbreviation") val timezoneAbbreviation: String? = null,
    @field:Json(name = "elevation") val elevation: Double? = null,
    @field:Json(name = "current") val current: CurrentWeatherDto? = null,
    @field:Json(name = "hourly") val hourly: HourlyWeatherDto? = null,
    @field:Json(name = "daily") val daily: DailyWeatherDto? = null
)

@JsonClass(generateAdapter = true)
data class CurrentWeatherDto(
    @field:Json(name = "time") val time: String,
    @field:Json(name = "interval") val interval: Int? = null,
    @field:Json(name = "temperature_2m") val temperature2m: Double,
    @field:Json(name = "relative_humidity_2m") val relativeHumidity2m: Double,
    @field:Json(name = "apparent_temperature") val apparentTemperature: Double,
    @field:Json(name = "is_day") val isDay: Int? = 1,
    @field:Json(name = "precipitation") val precipitation: Double = 0.0,
    @field:Json(name = "weather_code") val weatherCode: Int,
    @field:Json(name = "wind_speed_10m") val windSpeed10m: Double,
    @field:Json(name = "wind_direction_10m") val windDirection10m: Double? = 0.0
)

@JsonClass(generateAdapter = true)
data class HourlyWeatherDto(
    @field:Json(name = "time") val time: List<String>,
    @field:Json(name = "temperature_2m") val temperature2m: List<Double>,
    @field:Json(name = "relative_humidity_2m") val relativeHumidity2m: List<Double>? = null,
    @field:Json(name = "precipitation_probability") val precipitationProbability: List<Double>? = null,
    @field:Json(name = "precipitation") val precipitation: List<Double>? = null,
    @field:Json(name = "weather_code") val weatherCode: List<Int>? = null,
    @field:Json(name = "wind_speed_10m") val windSpeed10m: List<Double>? = null
)

@JsonClass(generateAdapter = true)
data class DailyWeatherDto(
    @field:Json(name = "time") val time: List<String>,
    @field:Json(name = "weather_code") val weatherCode: List<Int>,
    @field:Json(name = "temperature_2m_max") val temperature2mMax: List<Double>,
    @field:Json(name = "temperature_2m_min") val temperature2mMin: List<Double>,
    @field:Json(name = "precipitation_sum") val precipitationSum: List<Double>? = null,
    @field:Json(name = "precipitation_probability_max") val precipitationProbabilityMax: List<Double>? = null
)
