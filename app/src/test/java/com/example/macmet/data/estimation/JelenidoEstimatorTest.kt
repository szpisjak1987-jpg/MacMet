package com.example.macmet.data.estimation

import com.example.macmet.data.model.api.CurrentWeatherDto
import com.example.macmet.data.model.domain.JelenidoData
import com.example.macmet.data.model.domain.StationProviderType
import com.example.macmet.data.model.domain.WeatherStation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JelenidoEstimatorTest {

    @Test
    fun estimate_withoutStation_usesModelValues() {
        val dto = CurrentWeatherDto(
            time = "2025-02-18T12:00",
            temperature2m = 10.0,
            relativeHumidity2m = 50.0,
            apparentTemperature = 8.5,
            isDay = 1,
            precipitation = 0.0,
            weatherCode = 1,
            windSpeed10m = 15.0,
            windDirection10m = 180.0
        )

        val result = JelenidoEstimator.estimate(
            currentDto = dto,
            locationName = "Budapest"
        )

        assertEquals(10.0, result.temperature, 0.01)
        assertEquals(50, result.relativeHumidity)
        assertFalse(result.estimationDetails.isStationDataAvailable)
        assertEquals(0.0, result.estimationDetails.stationWeight, 0.01)
        assertEquals(1.0, result.estimationDetails.modelWeight, 0.01)
        assertEquals(85, result.estimationDetails.confidenceIndex)
    }

    @Test
    fun estimate_withStation_appliesWeightedCalculation() {
        val dto = CurrentWeatherDto(
            time = "2025-02-18T12:00",
            temperature2m = 10.0,
            relativeHumidity2m = 50.0,
            apparentTemperature = 8.5,
            isDay = 1,
            precipitation = 0.0,
            weatherCode = 1,
            windSpeed10m = 10.0,
            windDirection10m = 90.0
        )

        val stationObs = StationObservation(
            stationId = "ST-001",
            temperature = 14.0,
            relativeHumidity = 70.0,
            windSpeed = 20.0
        )

        val result = JelenidoEstimator.estimate(
            currentDto = dto,
            locationName = "Budapest",
            stationObservation = stationObs
        )

        assertTrue(result.temperature in 12.0..15.0)
        assertEquals(62, result.relativeHumidity)
        assertTrue(result.estimationDetails.isStationDataAvailable)
    }

    @Test
    fun estimate_filtersStationsBeyond50kmRadius() {
        val dto = CurrentWeatherDto(
            time = "2025-02-18T12:00",
            temperature2m = 15.0,
            relativeHumidity2m = 60.0,
            apparentTemperature = 15.0,
            isDay = 1,
            precipitation = 0.0,
            weatherCode = 0,
            windSpeed10m = 10.0,
            windDirection10m = 0.0
        )

        val stationNear = WeatherStation(
            id = "near",
            name = "Near Station (10km)",
            providerType = StationProviderType.OFFICIAL_NOAA_SYNOP,
            latitude = 47.5,
            longitude = 19.1,
            distanceKm = 10.0,
            temperature = 20.0
        )

        val stationFar = WeatherStation(
            id = "far",
            name = "Far Station (60km)",
            providerType = StationProviderType.OFFICIAL_NOAA_SYNOP,
            latitude = 48.0,
            longitude = 20.0,
            distanceKm = 60.0,
            temperature = 30.0
        )

        val result = JelenidoEstimator.estimate(
            currentDto = dto,
            stations = listOf(stationNear, stationFar)
        )

        assertEquals(1, result.nearbyStations.size)
        assertEquals("near", result.nearbyStations[0].id)
        assertTrue(result.temperature in 15.0..20.0)
    }

    @Test
    fun computeStationWeight_calculatesInverselyProportionalToDistanceSquared() {
        val officialStation = WeatherStation(
            id = "st1",
            name = "Official 0km",
            providerType = StationProviderType.OFFICIAL_NOAA_SYNOP, // multiplier 1.5
            latitude = 0.0,
            longitude = 0.0,
            distanceKm = 0.0
        )

        val amateurStation = WeatherStation(
            id = "st2",
            name = "Amateur 1km",
            providerType = StationProviderType.AMATEUR_OPENSENSEMAP, // multiplier 1.0
            latitude = 0.0,
            longitude = 0.0,
            distanceKm = 1.0
        )

        // dist 0km: 1.5 / (0 + 1)^2 = 1.5
        val weightOfficial = JelenidoEstimator.computeStationWeight(officialStation)
        assertEquals(1.5, weightOfficial, 0.001)

        // dist 1km: 1.0 / (1 + 1)^2 = 0.25
        val weightAmateur = JelenidoEstimator.computeStationWeight(amateurStation)
        assertEquals(0.25, weightAmateur, 0.001)
    }

    @Test
    fun degreesToCardinal_returnsCorrectCardinalDirection() {
        assertEquals("É", JelenidoData.degreesToCardinal(0.0))
        assertEquals("K", JelenidoData.degreesToCardinal(90.0))
        assertEquals("D", JelenidoData.degreesToCardinal(180.0))
        assertEquals("NY", JelenidoData.degreesToCardinal(270.0))
        assertEquals("ÉK", JelenidoData.degreesToCardinal(45.0))
    }
}
