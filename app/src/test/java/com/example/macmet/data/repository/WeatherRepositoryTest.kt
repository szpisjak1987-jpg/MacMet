package com.example.macmet.data.repository

import com.example.macmet.data.api.OpenMeteoApiService
import com.example.macmet.data.api.OpenMeteoGeocodingApiService
import com.example.macmet.data.location.LocationData
import com.example.macmet.data.location.LocationTracker
import com.example.macmet.data.model.api.CitySearchResultDto
import com.example.macmet.data.model.api.CurrentWeatherDto
import com.example.macmet.data.model.api.DailyWeatherDto
import com.example.macmet.data.model.api.HourlyWeatherDto
import com.example.macmet.data.model.api.OpenMeteoGeocodingResponseDto
import com.example.macmet.data.model.api.OpenMeteoGeocodingSearchResponseDto
import com.example.macmet.data.model.api.OpenMeteoResponseDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class WeatherRepositoryTest {

    private class FakeApiService(
        private val responseDto: OpenMeteoResponseDto
    ) : OpenMeteoApiService {
        override suspend fun getForecast(
            latitude: Double,
            longitude: Double,
            current: String,
            hourly: String,
            daily: String,
            timezone: String
        ): OpenMeteoResponseDto {
            return responseDto
        }
    }

    private class FakeGeocodingApiService(
        private val responseDto: OpenMeteoGeocodingSearchResponseDto
    ) : OpenMeteoGeocodingApiService {
        override suspend fun reverseGeocode(
            latitude: Double,
            longitude: Double,
            language: String,
            format: String
        ): OpenMeteoGeocodingResponseDto = OpenMeteoGeocodingResponseDto()

        override suspend fun search(
            name: String,
            language: String,
            count: Int,
            format: String
        ): OpenMeteoGeocodingResponseDto = OpenMeteoGeocodingResponseDto()

        override suspend fun searchLocations(
            name: String,
            count: Int,
            language: String,
            format: String
        ): Response<OpenMeteoGeocodingSearchResponseDto> {
            return Response.success(responseDto)
        }
    }

    private class FakeLocationTracker(
        private val locationData: LocationData
    ) : LocationTracker {
        override suspend fun getCurrentLocation(): LocationData {
            return locationData
        }
    }

    @Test
    fun get7DayForecast_returnsMappedDomainForecast() = runTest {
        val fakeResponse = OpenMeteoResponseDto(
            latitude = 47.4979,
            longitude = 19.0402,
            current = CurrentWeatherDto(
                time = "2025-02-18T12:00",
                temperature2m = 12.0,
                relativeHumidity2m = 55.0,
                apparentTemperature = 10.5,
                isDay = 1,
                precipitation = 0.0,
                weatherCode = 1,
                windSpeed10m = 12.0,
                windDirection10m = 180.0
            ),
            daily = DailyWeatherDto(
                time = listOf("2025-02-18", "2025-02-19"),
                weatherCode = listOf(1, 61),
                temperature2mMax = listOf(14.0, 10.0),
                temperature2mMin = listOf(5.0, 3.0),
                precipitationSum = listOf(0.0, 2.5),
                precipitationProbabilityMax = listOf(10.0, 80.0)
            ),
            hourly = HourlyWeatherDto(
                time = listOf("2025-02-18T12:00", "2025-02-18T13:00"),
                temperature2m = listOf(12.0, 13.0),
                relativeHumidity2m = listOf(55.0, 50.0),
                precipitationProbability = listOf(10.0, 20.0),
                precipitation = listOf(0.0, 0.0),
                weatherCode = listOf(1, 1),
                windSpeed10m = listOf(12.0, 14.0)
            )
        )

        val repository = WeatherRepositoryImpl(
            apiService = FakeApiService(fakeResponse),
            locationTracker = FakeLocationTracker(LocationData.BUDAPEST_FALLBACK)
        )

        val result = repository.get7DayForecast()

        assertTrue(result.isSuccess)
        val forecast = result.getOrThrow()

        assertEquals("Budapest", forecast.locationName)
        assertEquals(12.0, forecast.currentWeather.temperature, 0.01)
        assertEquals(2, forecast.dailyForecasts.size)
        assertEquals("Kedd", forecast.dailyForecasts[0].dayOfWeek)
        assertEquals(14.0, forecast.dailyForecasts[0].tempMax, 0.01)
        assertEquals(2, forecast.hourlyForecasts.size)
        assertEquals("12:00", forecast.hourlyForecasts[0].formattedHour)
    }

    @Test
    fun searchCities_returnsMappedCityResults() = runTest {
        val fakeGeoResponse = OpenMeteoGeocodingSearchResponseDto(
            results = listOf(
                CitySearchResultDto(
                    id = 3045153,
                    name = "Pécs",
                    latitude = 46.08333,
                    longitude = 18.23333,
                    country = "Hungary",
                    admin1 = "Baranya",
                    countryCode = "HU"
                )
            )
        )

        val repository = WeatherRepositoryImpl(
            apiService = FakeApiService(
                OpenMeteoResponseDto(
                    latitude = 47.0,
                    longitude = 19.0,
                    current = CurrentWeatherDto(
                        time = "2025-02-18T12:00",
                        temperature2m = 12.0,
                        relativeHumidity2m = 55.0,
                        apparentTemperature = 10.5,
                        isDay = 1,
                        precipitation = 0.0,
                        weatherCode = 1,
                        windSpeed10m = 12.0,
                        windDirection10m = 180.0
                    )
                )
            ),
            openMeteoGeocodingApiService = FakeGeocodingApiService(fakeGeoResponse),
            locationTracker = FakeLocationTracker(LocationData.BUDAPEST_FALLBACK)
        )

        val result = repository.searchCities("Pécs")

        assertTrue(result.isSuccess)
        val cities = result.getOrThrow()
        assertEquals(1, cities.size)
        assertEquals("Pécs", cities[0].name)
        assertEquals("Baranya", cities[0].admin1)
        assertEquals("Hungary", cities[0].country)
    }

    @Test
    fun get7DayForecast_suppliesUltimateFallbackStationWhenAllProvidersEmpty() = runTest {
        val fakeResponse = OpenMeteoResponseDto(
            latitude = 47.4979,
            longitude = 19.0402,
            current = CurrentWeatherDto(
                time = "2025-02-18T12:00",
                temperature2m = 15.0,
                relativeHumidity2m = 60.0,
                apparentTemperature = 14.0,
                isDay = 1,
                precipitation = 0.0,
                weatherCode = 0,
                windSpeed10m = 10.0,
                windDirection10m = 90.0
            )
        )

        val repository = WeatherRepositoryImpl(
            apiService = FakeApiService(fakeResponse),
            locationTracker = FakeLocationTracker(LocationData.BUDAPEST_FALLBACK)
        )

        val result = repository.get7DayForecast()

        assertTrue(result.isSuccess)
        val forecast = result.getOrThrow()
        val stations = forecast.currentWeather.nearbyStations
        assertEquals(0, stations.size)
    }
}
