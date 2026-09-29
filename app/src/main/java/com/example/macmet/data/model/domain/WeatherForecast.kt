package com.example.macmet.data.model.domain

data class HourlyForecast(
    val time: String, // e.g. "2025-02-18T14:00" or "14:00"
    val formattedHour: String, // e.g. "14:00"
    val temperature: Double,
    val relativeHumidity: Int,
    val precipitation: Double,
    val precipitationProbability: Int,
    val weatherCode: Int,
    val weatherCondition: WeatherCondition,
    val windSpeed: Double
)

data class DailyForecast(
    val date: String, // e.g. "2025-02-18"
    val dayOfWeek: String, // e.g. "Kedd" / "Tuesday"
    val tempMax: Double,
    val tempMin: Double,
    val precipitationSum: Double,
    val precipitationProbabilityMax: Int,
    val weatherCode: Int,
    val weatherCondition: WeatherCondition
)

data class WeatherForecast7Days(
    val currentWeather: JelenidoData,
    val dailyForecasts: List<DailyForecast>,
    val hourlyForecasts: List<HourlyForecast>,
    val latitude: Double,
    val longitude: Double,
    val locationName: String = "Budapest"
)
