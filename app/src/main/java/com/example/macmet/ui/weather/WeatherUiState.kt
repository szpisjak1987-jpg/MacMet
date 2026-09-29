package com.example.macmet.ui.weather

import com.example.macmet.data.model.domain.JelenidoData
import com.example.macmet.data.model.domain.WeatherForecast7Days

sealed interface WeatherUiState {
    data object Loading : WeatherUiState

    data class Success(
        val jelenido: JelenidoData,
        val forecast: WeatherForecast7Days,
        val locationName: String,
        val isPermissionGranted: Boolean
    ) : WeatherUiState

    data class Error(
        val message: String
    ) : WeatherUiState
}
