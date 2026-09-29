package com.example.macmet.data.repository

import com.example.macmet.data.estimation.StationObservation
import com.example.macmet.data.location.LocationData
import com.example.macmet.data.model.domain.CitySearchResult
import com.example.macmet.data.model.domain.WeatherForecast7Days

interface WeatherRepository {
    suspend fun get7DayForecast(
        location: LocationData? = null,
        stationObservation: StationObservation? = null
    ): Result<WeatherForecast7Days>

    suspend fun get7DayForecastForCoordinates(
        latitude: Double,
        longitude: Double,
        locationName: String = "Budapest",
        stationObservation: StationObservation? = null
    ): Result<WeatherForecast7Days>

    suspend fun searchCities(query: String): Result<List<CitySearchResult>>
}
