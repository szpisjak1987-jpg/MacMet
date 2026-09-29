package com.example.macmet.data.estimation

import com.example.macmet.data.model.api.CurrentWeatherDto
import com.example.macmet.data.model.domain.EstimationDetails
import com.example.macmet.data.model.domain.JelenidoData
import com.example.macmet.data.model.domain.StationProviderType
import com.example.macmet.data.model.domain.WeatherCondition
import com.example.macmet.data.model.domain.WeatherStation
import kotlin.math.abs
import kotlin.math.roundToInt

data class StationObservation(
    val stationId: String,
    val temperature: Double,
    val relativeHumidity: Double? = null,
    val windSpeed: Double? = null,
    val timestampMs: Long = System.currentTimeMillis()
)

object JelenidoEstimator {

    /**
     * Estimates Jelenidő (Current Weather) using weighted model and station inputs.
     * Filter stations within 50 km radius.
     * Weights stations inversely proportional to distance squared (1 / (distance + 1)^2)
     * and weights official stations higher than amateur stations.
     */
    fun estimate(
        currentDto: CurrentWeatherDto,
        locationName: String = "Budapest",
        stations: List<WeatherStation> = emptyList(),
        stationObservation: StationObservation? = null,
        targetStationWeight: Double = 0.7
    ): JelenidoData {
        val rawModelTemp = currentDto.temperature2m
        val rawModelHum = currentDto.relativeHumidity2m
        val rawModelWind = currentDto.windSpeed10m
        val rawModelPress = 1013.25

        // Filter stations within 20 km radius
        val nearbyStations = stations.filter { it.distanceKm <= 20.0 }

        // If stations list is empty but stationObservation is provided (legacy support)
        val validStations = if (nearbyStations.isNotEmpty()) {
            nearbyStations
        } else if (stationObservation != null) {
            listOf(
                WeatherStation(
                    id = stationObservation.stationId,
                    name = "Station ${stationObservation.stationId}",
                    providerType = StationProviderType.AMATEUR_OPENSENSEMAP,
                    latitude = 0.0,
                    longitude = 0.0,
                    distanceKm = 0.0,
                    temperature = stationObservation.temperature,
                    relativeHumidity = stationObservation.relativeHumidity,
                    windSpeed = stationObservation.windSpeed,
                    timestampMs = stationObservation.timestampMs
                )
            )
        } else {
            emptyList()
        }

        val tempStations = validStations.filter { it.temperature != null }
        val isStationAvailable = tempStations.isNotEmpty()

        val stationWeight = if (isStationAvailable) targetStationWeight.coerceIn(0.0, 1.0) else 0.0
        val modelWeight = 1.0 - stationWeight

        val (finalTemp, stationTempAvg) = if (isStationAvailable) {
            val totalWeight = tempStations.sumOf { computeStationWeight(it) }
            val weightedTempSum = tempStations.sumOf { computeStationWeight(it) * (it.temperature ?: rawModelTemp) }
            val stationAvg = if (totalWeight > 0) weightedTempSum / totalWeight else rawModelTemp
            val combined = (stationAvg * stationWeight) + (rawModelTemp * modelWeight)
            combined to stationAvg
        } else {
            rawModelTemp to null
        }

        val humStations = validStations.filter { it.relativeHumidity != null }
        val finalHum = if (humStations.isNotEmpty() && isStationAvailable) {
            val totalWeight = humStations.sumOf { computeStationWeight(it) }
            val weightedHumSum = humStations.sumOf { computeStationWeight(it) * (it.relativeHumidity ?: rawModelHum) }
            val stationHumAvg = if (totalWeight > 0) weightedHumSum / totalWeight else rawModelHum
            ((stationHumAvg * stationWeight) + (rawModelHum * modelWeight)).roundToInt()
        } else {
            rawModelHum.roundToInt()
        }

        val windStations = validStations.filter { it.windSpeed != null }
        val finalWind = if (windStations.isNotEmpty() && isStationAvailable) {
            val totalWeight = windStations.sumOf { computeStationWeight(it) }
            val weightedWindSum = windStations.sumOf { computeStationWeight(it) * (it.windSpeed ?: rawModelWind) }
            val stationWindAvg = if (totalWeight > 0) weightedWindSum / totalWeight else rawModelWind
            (stationWindAvg * stationWeight) + (rawModelWind * modelWeight)
        } else {
            rawModelWind
        }

        val pressStations = validStations.filter { it.pressure != null }
        val finalPress = if (pressStations.isNotEmpty() && isStationAvailable) {
            val totalWeight = pressStations.sumOf { computeStationWeight(it) }
            val weightedPressSum = pressStations.sumOf { computeStationWeight(it) * (it.pressure ?: rawModelPress) }
            val stationPressAvg = if (totalWeight > 0) weightedPressSum / totalWeight else rawModelPress
            (stationPressAvg * stationWeight) + (rawModelPress * modelWeight)
        } else {
            rawModelPress
        }

        val confidenceIndex = calculateConfidenceScore(
            isStationAvailable = isStationAvailable,
            tempStations = tempStations,
            stationTempAvg = stationTempAvg,
            rawModelTemp = rawModelTemp
        )

        val estimationDetails = EstimationDetails(
            stationWeight = stationWeight,
            modelWeight = modelWeight,
            isStationDataAvailable = isStationAvailable,
            rawModelTemperature = rawModelTemp,
            stationTemperature = stationTempAvg?.let { (it * 10.0).roundToInt() / 10.0 },
            confidenceIndex = confidenceIndex,
            stationCount = validStations.size
        )

        val roundedTemp = (finalTemp * 10.0).roundToInt() / 10.0
        val roundedApparent = (currentDto.apparentTemperature * 10.0).roundToInt() / 10.0
        val roundedWind = (finalWind * 10.0).roundToInt() / 10.0
        val roundedPress = (finalPress * 10.0).roundToInt() / 10.0
        val windDir = currentDto.windDirection10m ?: 0.0

        return JelenidoData(
            temperature = roundedTemp,
            apparentTemperature = roundedApparent,
            relativeHumidity = finalHum,
            precipitation = currentDto.precipitation,
            weatherCode = currentDto.weatherCode,
            weatherCondition = WeatherCondition.fromWmoCode(currentDto.weatherCode),
            windSpeed = roundedWind,
            windDirection = windDir,
            windDirectionCardinal = JelenidoData.degreesToCardinal(windDir),
            isDay = currentDto.isDay == 1,
            timestamp = currentDto.time,
            locationName = locationName,
            estimationDetails = estimationDetails,
            pressure = roundedPress,
            nearbyStations = validStations
        )
    }

    /**
     * Calculates station weight inversely proportional to distance squared
     * multiplied by provider base multiplier.
     * w = providerMultiplier / (distance + 1.0)^2
     */
    fun computeStationWeight(station: WeatherStation): Double {
        val distanceFactor = 1.0 / ((station.distanceKm + 1.0) * (station.distanceKm + 1.0))
        val providerMultiplier = station.providerType.baseWeightMultiplier
        return providerMultiplier * distanceFactor
    }

    /**
     * Calculates overall confidence score (%) based on station density, freshness, and agreement.
     */
    private fun calculateConfidenceScore(
        isStationAvailable: Boolean,
        tempStations: List<WeatherStation>,
        stationTempAvg: Double?,
        rawModelTemp: Double
    ): Int {
        if (!isStationAvailable) return 85

        val baseConfidence = 90

        // Station density bonus: up to +5%
        val densityBonus = when (tempStations.size) {
            1 -> 2
            2 -> 3
            else -> 5
        }

        // Freshness bonus: up to +3%
        val now = System.currentTimeMillis()
        val validTimestamps = tempStations.mapNotNull { it.timestampMs }
        val freshnessBonus = if (validTimestamps.isNotEmpty()) {
            val avgAgeMs = now - validTimestamps.average()
            when {
                avgAgeMs <= 15 * 60 * 1000 -> 3
                avgAgeMs <= 30 * 60 * 1000 -> 2
                avgAgeMs <= 60 * 60 * 1000 -> 1
                else -> 0
            }
        } else {
            3 // Default to recent if no explicit timestamp provided
        }

        // Agreement modifier: diff between station average and model prediction
        val agreementModifier = if (stationTempAvg != null) {
            val diff = abs(stationTempAvg - rawModelTemp)
            when {
                diff <= 1.0 -> 2
                diff <= 2.5 -> 0
                diff > 4.0 -> -3
                else -> 0
            }
        } else {
            0
        }

        return (baseConfidence + densityBonus + freshnessBonus + agreementModifier).coerceIn(50, 98)
    }
}
