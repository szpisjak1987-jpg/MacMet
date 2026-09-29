package com.example.macmet.ui.weather

import com.example.macmet.data.estimation.StationObservation
import com.example.macmet.data.location.LocationData
import com.example.macmet.data.model.domain.CitySearchResult
import com.example.macmet.data.model.domain.WeatherForecast7Days
import com.example.macmet.data.repository.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeWeatherRepository : WeatherRepository {
        var searchResultToReturn: Result<List<CitySearchResult>> = Result.success(emptyList())

        override suspend fun get7DayForecast(
            location: LocationData?,
            stationObservation: StationObservation?
        ): Result<WeatherForecast7Days> {
            return Result.failure(NotImplementedError())
        }

        override suspend fun get7DayForecastForCoordinates(
            latitude: Double,
            longitude: Double,
            locationName: String,
            stationObservation: StationObservation?
        ): Result<WeatherForecast7Days> {
            return Result.failure(NotImplementedError())
        }

        override suspend fun searchCities(query: String): Result<List<CitySearchResult>> {
            return searchResultToReturn
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun searchCities_updatesSearchResultsState() = runTest {
        val repo = FakeWeatherRepository()
        val city = CitySearchResult(
            id = 1L,
            name = "Budapest",
            country = "Hungary",
            admin1 = "Budapest",
            latitude = 47.0,
            longitude = 19.0,
            countryCode = "HU"
        )
        repo.searchResultToReturn = Result.success(listOf(city))

        val viewModel = WeatherViewModel(weatherRepository = repo)

        viewModel.onSearchQueryChanged("Budapest")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Budapest", viewModel.searchQuery.value)
        assertEquals(1, viewModel.searchResults.value.size)
        assertEquals("Budapest", viewModel.searchResults.value[0].name)
    }

    @Test
    fun selectCity_updatesSelectedCityState() = runTest {
        val repo = FakeWeatherRepository()
        val city = CitySearchResult(
            id = 2L,
            name = "Pécs",
            country = "Hungary",
            admin1 = "Baranya",
            latitude = 46.0,
            longitude = 18.0,
            countryCode = "HU"
        )

        val viewModel = WeatherViewModel(weatherRepository = repo)

        viewModel.selectCity(city)
        assertEquals(city, viewModel.selectedCity.value)

        viewModel.resetToGpsLocation()
        assertEquals(null, viewModel.selectedCity.value)
    }
}
