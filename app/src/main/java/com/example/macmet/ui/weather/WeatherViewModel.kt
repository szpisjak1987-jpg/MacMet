package com.example.macmet.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.macmet.data.estimation.StationObservation
import com.example.macmet.data.location.LocationData
import com.example.macmet.data.location.LocationTracker
import com.example.macmet.data.model.domain.CitySearchResult
import com.example.macmet.data.model.domain.EstimationDetails
import com.example.macmet.data.model.domain.JelenidoData
import com.example.macmet.data.model.domain.WeatherCondition
import com.example.macmet.data.repository.WeatherRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class StationWeightingInfo(
    val stationWeightPercentage: Int,
    val modelWeightPercentage: Int,
    val isStationDataAvailable: Boolean,
    val summaryHu: String,
    val stationTemperature: Double? = null,
    val rawModelTemperature: Double? = null,
    val confidenceIndex: Int = 85
)

class WeatherViewModel(
    private val weatherRepository: WeatherRepository,
    private val locationTracker: LocationTracker? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<CitySearchResult>>(emptyList())
    val searchResults: StateFlow<List<CitySearchResult>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _selectedCity = MutableStateFlow<CitySearchResult?>(null)
    val selectedCity: StateFlow<CitySearchResult?> = _selectedCity.asStateFlow()

    private var isPermissionGranted: Boolean = false
    private var lastLocationData: LocationData? = null
    private var lastStationObservation: StationObservation? = null
    private var searchJob: Job? = null

    init {
        loadWeather()
    }

    /**
     * Loads weather data based on location and station observations.
     * If location is null, fetches current device location from LocationTracker if available.
     */
    fun loadWeather(
        location: LocationData? = null,
        stationObservation: StationObservation? = null
    ) {
        lastStationObservation = stationObservation
        _uiState.value = WeatherUiState.Loading

        viewModelScope.launch {
            val targetLocation = location
                ?: locationTracker?.getCurrentLocation()
                ?: lastLocationData

            if (targetLocation != null) {
                lastLocationData = targetLocation
            }

            val result = weatherRepository.get7DayForecast(
                location = targetLocation,
                stationObservation = stationObservation
            )

            result.fold(
                onSuccess = { forecast ->
                    _uiState.value = WeatherUiState.Success(
                        jelenido = forecast.currentWeather,
                        forecast = forecast,
                        locationName = forecast.locationName,
                        isPermissionGranted = isPermissionGranted
                    )
                },
                onFailure = { throwable ->
                    _uiState.value = WeatherUiState.Error(
                        message = throwable.message ?: "Ismeretlen hiba történt az időjárás adatok letöltésekor."
                    )
                }
            )
        }
    }

    /**
     * Triggers a manual refresh of weather data using current settings.
     */
    fun refreshWeather() {
        val currentCity = _selectedCity.value
        if (currentCity != null) {
            selectCity(currentCity)
        } else {
            loadWeather(location = null, stationObservation = lastStationObservation)
        }
    }

    /**
     * Handles location permission status updates.
     * Triggers a weather refresh if permission was newly granted.
     */
    fun updatePermissionStatus(isGranted: Boolean) {
        val previousPermission = isPermissionGranted
        isPermissionGranted = isGranted

        val currentState = _uiState.value
        if (currentState is WeatherUiState.Success) {
            _uiState.value = currentState.copy(isPermissionGranted = isGranted)
        }

        if (isGranted && !previousPermission && _selectedCity.value == null) {
            loadWeather(location = null, stationObservation = lastStationObservation)
        }
    }

    /**
     * Alias for updatePermissionStatus for UI callbacks.
     */
    fun onPermissionResult(isGranted: Boolean) {
        updatePermissionStatus(isGranted)
    }

    /**
     * Loads weather data for specific coordinates manually.
     */
    fun loadWeatherForCoordinates(
        latitude: Double,
        longitude: Double,
        locationName: String = "Budapest",
        stationObservation: StationObservation? = null
    ) {
        lastStationObservation = stationObservation
        _uiState.value = WeatherUiState.Loading

        viewModelScope.launch {
            val result = weatherRepository.get7DayForecastForCoordinates(
                latitude = latitude,
                longitude = longitude,
                locationName = locationName,
                stationObservation = stationObservation
            )

            result.fold(
                onSuccess = { forecast ->
                    _uiState.value = WeatherUiState.Success(
                        jelenido = forecast.currentWeather,
                        forecast = forecast,
                        locationName = forecast.locationName,
                        isPermissionGranted = isPermissionGranted
                    )
                },
                onFailure = { throwable ->
                    _uiState.value = WeatherUiState.Error(
                        message = throwable.message ?: "Hiba történt a megadott koordináták időjárásának letöltésekor."
                    )
                }
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchCities(query)
    }

    fun searchCities(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (query.trim().length < 2) {
                _searchResults.value = emptyList()
                _isSearching.value = false
                return@launch
            }
            _isSearching.value = true
            val result = weatherRepository.searchCities(query.trim())
            _isSearching.value = false
            result.fold(
                onSuccess = { list ->
                    _searchResults.value = list
                },
                onFailure = {
                    _searchResults.value = emptyList()
                }
            )
        }
    }

    fun selectCity(city: CitySearchResult) {
        _selectedCity.value = city
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        val locationName = buildString {
            append(city.name)
            if (!city.admin1.isNullOrBlank()) {
                append(", ${city.admin1}")
            }
            if (!city.country.isNullOrBlank()) {
                append(", ${city.country}")
            }
        }
        loadWeatherForCoordinates(
            latitude = city.latitude,
            longitude = city.longitude,
            locationName = locationName,
            stationObservation = lastStationObservation
        )
    }

    fun resetToGpsLocation() {
        _selectedCity.value = null
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        loadWeather(location = null, stationObservation = lastStationObservation)
    }

    // --- Metric Unit Formatting Helpers ---

    fun formatTemperature(temperature: Double): String {
        return "${roundOneDecimal(temperature)} °C"
    }

    fun formatWindSpeed(windSpeed: Double): String {
        return "${roundOneDecimal(windSpeed)} km/h"
    }

    fun formatPrecipitation(precipitation: Double): String {
        return "${roundOneDecimal(precipitation)} mm"
    }

    fun formatPressure(pressure: Double): String {
        return "${roundOneDecimal(pressure)} hPa"
    }

    fun formatPressure(pressure: Int): String {
        return "$pressure hPa"
    }

    fun formatHumidity(humidity: Int): String {
        return "$humidity %"
    }

    fun formatHumidity(humidity: Double): String {
        return "${humidity.roundToInt()} %"
    }

    fun formatPrecipitationProbability(probability: Int): String {
        return "$probability %"
    }

    // --- Hungarian WMO Weather Code Translation ---

    fun translateWmoCode(code: Int): String {
        return WeatherCondition.fromWmoCode(code).descriptionHu
    }

    fun getWeatherDescriptionHu(code: Int): String {
        return translateWmoCode(code)
    }

    // --- Station Weighting & Transparency Helpers ---

    fun getStationWeightingInfo(jelenido: JelenidoData): StationWeightingInfo {
        return getStationWeightingInfo(jelenido.estimationDetails)
    }

    fun getStationWeightingInfo(details: EstimationDetails): StationWeightingInfo {
        val stationPct = (details.stationWeight * 100).roundToInt()
        val modelPct = (details.modelWeight * 100).roundToInt()
        val summary = if (details.isStationDataAvailable) {
            "$stationPct% állomási mérés + $modelPct% Open-Meteo modelladat"
        } else {
            "100% Open-Meteo modelladat (állomási mérés nem áll rendelkezésre)"
        }
        return StationWeightingInfo(
            stationWeightPercentage = stationPct,
            modelWeightPercentage = modelPct,
            isStationDataAvailable = details.isStationDataAvailable,
            summaryHu = summary,
            stationTemperature = details.stationTemperature,
            rawModelTemperature = details.rawModelTemperature,
            confidenceIndex = details.confidenceIndex
        )
    }

    fun formatStationWeighting(jelenido: JelenidoData): String {
        return getStationWeightingInfo(jelenido).summaryHu
    }

    fun formatStationWeighting(details: EstimationDetails): String {
        return getStationWeightingInfo(details).summaryHu
    }

    private fun roundOneDecimal(value: Double): String {
        val rounded = (value * 10.0).roundToInt() / 10.0
        return if (rounded % 1.0 == 0.0) {
            rounded.toInt().toString()
        } else {
            rounded.toString()
        }
    }
}

class WeatherViewModelFactory(
    private val weatherRepository: WeatherRepository,
    private val locationTracker: LocationTracker? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WeatherViewModel::class.java)) {
            return WeatherViewModel(weatherRepository, locationTracker) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
