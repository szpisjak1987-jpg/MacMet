package com.example.macmet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.macmet.data.model.domain.CitySearchResult
import com.example.macmet.data.model.domain.DailyForecast
import com.example.macmet.data.model.domain.EstimationDetails
import com.example.macmet.data.model.domain.HourlyForecast
import com.example.macmet.data.model.domain.JelenidoData
import com.example.macmet.data.model.domain.StationProviderType
import com.example.macmet.data.model.domain.WeatherCondition
import com.example.macmet.data.model.domain.WeatherForecast7Days
import com.example.macmet.data.model.domain.WeatherStation
import com.example.macmet.data.estimation.SafetyMonitorAnalyzer
import com.example.macmet.ui.safety.SafetyMonitorSection
import com.example.macmet.ui.theme.MacóMetTheme
import com.example.macmet.ui.weather.WeatherUiState
import com.example.macmet.ui.weather.WeatherViewModel

@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel,
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit = {},
    onUseDefaultLocation: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val selectedCity by viewModel.selectedCity.collectAsStateWithLifecycle()

    var showSearchDialog by remember { mutableStateOf(false) }

    WeatherScreenContent(
        uiState = uiState,
        searchQuery = searchQuery,
        searchResults = searchResults,
        isSearching = isSearching,
        selectedCity = selectedCity,
        showSearchDialog = showSearchDialog,
        onShowSearchDialog = { showSearchDialog = true },
        onDismissSearchDialog = { showSearchDialog = false },
        onQueryChanged = { viewModel.onSearchQueryChanged(it) },
        onSelectCity = { viewModel.selectCity(it) },
        onResetToGps = { viewModel.resetToGpsLocation() },
        onRefresh = { viewModel.refreshWeather() },
        onRequestPermission = onRequestPermission,
        onUseDefaultLocation = {
            viewModel.loadWeatherForCoordinates(
                latitude = 47.4979,
                longitude = 19.0402,
                locationName = "Budapest"
            )
            onUseDefaultLocation()
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreenContent(
    uiState: WeatherUiState,
    searchQuery: String,
    searchResults: List<CitySearchResult>,
    isSearching: Boolean,
    selectedCity: CitySearchResult?,
    showSearchDialog: Boolean,
    onShowSearchDialog: () -> Unit,
    onDismissSearchDialog: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onSelectCity: (CitySearchResult) -> Unit,
    onResetToGps: () -> Unit,
    onRefresh: () -> Unit,
    onRequestPermission: () -> Unit,
    onUseDefaultLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (showSearchDialog) {
        CitySearchDialog(
            searchQuery = searchQuery,
            searchResults = searchResults,
            isSearching = isSearching,
            onQueryChanged = onQueryChanged,
            onSelectCity = onSelectCity,
            onResetToGps = onResetToGps,
            onDismiss = onDismissSearchDialog
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "MacóMet Időjárás",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState is WeatherUiState.Success) {
                            Text(
                                text = uiState.locationName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onShowSearchDialog) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Település keresése"
                        )
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Frissítés"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState) {
                is WeatherUiState.Loading -> {
                    WeatherLoadingView()
                }

                is WeatherUiState.Error -> {
                    WeatherErrorView(
                        message = uiState.message,
                        onRetry = onRefresh
                    )
                }

                is WeatherUiState.Success -> {
                    WeatherSuccessView(
                        state = uiState,
                        selectedCity = selectedCity,
                        onRequestPermission = onRequestPermission,
                        onUseDefaultLocation = onUseDefaultLocation
                    )
                }
            }
        }
    }
}

@Composable
private fun WeatherLoadingView(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Időjárási adatok betöltése...",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun WeatherErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.ErrorOutline,
                    contentDescription = "Hiba",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(56.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Hiba történt az adatok letöltésekor",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = "Újrapróbálás")
                }
            }
        }
    }
}

@Composable
private fun WeatherSuccessView(
    state: WeatherUiState.Success,
    selectedCity: CitySearchResult?,
    onRequestPermission: () -> Unit,
    onUseDefaultLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Location Permission Prompt Card if permission is not granted
        if (!state.isPermissionGranted) {
            LocationPermissionCard(
                onRequestPermission = onRequestPermission,
                onUseDefaultLocation = onUseDefaultLocation
            )
        }

        // Current Weather Jelenidő Card
        JelenidoCard(
            jelenido = state.jelenido,
            selectedCity = selectedCity
        )

        // Nearby Weather Stations Section
        NearbyStationsSection(stations = state.jelenido.nearbyStations)

        // Safety Monitor Section
        val safetyData = remember(state.forecast) {
            SafetyMonitorAnalyzer.analyze(state.forecast)
        }
        SafetyMonitorSection(safetyData = safetyData)

        // Hourly Forecast Row
        if (state.forecast.hourlyForecasts.isNotEmpty()) {
            HourlyForecastRow(hourlyForecasts = state.forecast.hourlyForecasts)
        }

        // 7-Day Daily Forecast List
        if (state.forecast.dailyForecasts.isNotEmpty()) {
            DailyForecastList(dailyForecasts = state.forecast.dailyForecasts)
        }

        // Footer attribution
        Text(
            text = "Adatforrás: Open-Meteo & MacóMet Állomási Modell",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        )
    }
}

// --- Previews ---

@Preview(showBackground = true)
@Composable
fun WeatherScreenLoadingPreview() {
    MacóMetTheme {
        WeatherScreenContent(
            uiState = WeatherUiState.Loading,
            searchQuery = "",
            searchResults = emptyList(),
            isSearching = false,
            selectedCity = null,
            showSearchDialog = false,
            onShowSearchDialog = {},
            onDismissSearchDialog = {},
            onQueryChanged = {},
            onSelectCity = {},
            onResetToGps = {},
            onRefresh = {},
            onRequestPermission = {},
            onUseDefaultLocation = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WeatherScreenErrorPreview() {
    MacóMetTheme {
        WeatherScreenContent(
            uiState = WeatherUiState.Error("Nincs internetkapcsolat."),
            searchQuery = "",
            searchResults = emptyList(),
            isSearching = false,
            selectedCity = null,
            showSearchDialog = false,
            onShowSearchDialog = {},
            onDismissSearchDialog = {},
            onQueryChanged = {},
            onSelectCity = {},
            onResetToGps = {},
            onRefresh = {},
            onRequestPermission = {},
            onUseDefaultLocation = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WeatherScreenSuccessPreview() {
    val sampleStations = listOf(
        WeatherStation(
            id = "noaa_12345",
            name = "Budapest Pestszentlőrinc / OMSZ",
            providerType = StationProviderType.OFFICIAL_NOAA_SYNOP,
            latitude = 47.43,
            longitude = 19.18,
            distanceKm = 3.2,
            temperature = 18.2,
            relativeHumidity = 62.0,
            pressure = 1014.5,
            windSpeed = 14.5,
            timestampMs = System.currentTimeMillis() - 12 * 60 * 1000
        ),
        WeatherStation(
            id = "sensebox_6789",
            name = "Újlipótváros senseBox",
            providerType = StationProviderType.AMATEUR_OPENSENSEMAP,
            latitude = 47.52,
            longitude = 19.05,
            distanceKm = 5.8,
            temperature = 18.8,
            relativeHumidity = 60.0,
            pressure = 1013.8,
            timestampMs = System.currentTimeMillis() - 5 * 60 * 1000
        )
    )

    val sampleJelenido = JelenidoData(
        temperature = 18.5,
        apparentTemperature = 19.0,
        relativeHumidity = 65,
        precipitation = 0.0,
        weatherCode = 1,
        weatherCondition = WeatherCondition.fromWmoCode(1),
        windSpeed = 12.0,
        windDirection = 315.0,
        windDirectionCardinal = "ÉNY",
        isDay = true,
        timestamp = "2025-02-18T14:00",
        locationName = "Budapest",
        estimationDetails = EstimationDetails(
            stationWeight = 0.7,
            modelWeight = 0.3,
            isStationDataAvailable = true,
            rawModelTemperature = 17.8,
            stationTemperature = 18.8,
            confidenceIndex = 95,
            stationCount = sampleStations.size
        ),
        nearbyStations = sampleStations
    )

    val sampleHourly = listOf(
        HourlyForecast(
            time = "14:00",
            formattedHour = "14:00",
            temperature = 18.5,
            relativeHumidity = 60,
            precipitation = 0.0,
            precipitationProbability = 10,
            weatherCode = 1,
            weatherCondition = WeatherCondition.fromWmoCode(1),
            windSpeed = 10.0
        ),
        HourlyForecast(
            time = "15:00",
            formattedHour = "15:00",
            temperature = 19.0,
            relativeHumidity = 55,
            precipitation = 0.0,
            precipitationProbability = 0,
            weatherCode = 0,
            weatherCondition = WeatherCondition.fromWmoCode(0),
            windSpeed = 11.0
        )
    )

    val sampleDaily = listOf(
        DailyForecast(
            date = "2025-05-15",
            dayOfWeek = "Csütörtök",
            tempMax = 24.0,
            tempMin = 12.0,
            precipitationSum = 0.0,
            precipitationProbabilityMax = 10,
            weatherCode = 1,
            weatherCondition = WeatherCondition.fromWmoCode(1)
        ),
        DailyForecast(
            date = "2025-05-16",
            dayOfWeek = "Péntek",
            tempMax = 22.5,
            tempMin = 13.1,
            precipitationSum = 2.5,
            precipitationProbabilityMax = 60,
            weatherCode = 61,
            weatherCondition = WeatherCondition.fromWmoCode(61)
        )
    )

    val sampleForecast = WeatherForecast7Days(
        currentWeather = sampleJelenido,
        dailyForecasts = sampleDaily,
        hourlyForecasts = sampleHourly,
        latitude = 47.4979,
        longitude = 19.0402,
        locationName = "Budapest"
    )

    MacóMetTheme {
        WeatherScreenContent(
            uiState = WeatherUiState.Success(
                jelenido = sampleJelenido,
                forecast = sampleForecast,
                locationName = "Budapest",
                isPermissionGranted = false
            ),
            searchQuery = "",
            searchResults = emptyList(),
            isSearching = false,
            selectedCity = null,
            showSearchDialog = false,
            onShowSearchDialog = {},
            onDismissSearchDialog = {},
            onQueryChanged = {},
            onSelectCity = {},
            onResetToGps = {},
            onRefresh = {},
            onRequestPermission = {},
            onUseDefaultLocation = {}
        )
    }
}
