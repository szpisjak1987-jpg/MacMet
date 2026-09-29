package com.example.macmet.data.model.domain

data class EstimationDetails(
    val stationWeight: Double,
    val modelWeight: Double,
    val isStationDataAvailable: Boolean,
    val rawModelTemperature: Double,
    val stationTemperature: Double? = null,
    val confidenceIndex: Int,
    val stationCount: Int = 0
)

data class JelenidoData(
    val temperature: Double,
    val apparentTemperature: Double,
    val relativeHumidity: Int,
    val precipitation: Double,
    val weatherCode: Int,
    val weatherCondition: WeatherCondition,
    val windSpeed: Double,
    val windDirection: Double,
    val windDirectionCardinal: String,
    val isDay: Boolean,
    val timestamp: String,
    val locationName: String = "Budapest",
    val estimationDetails: EstimationDetails,
    val pressure: Double = 1013.25,
    val nearbyStations: List<WeatherStation> = emptyList()
) {
    companion object {
        fun degreesToCardinal(degrees: Double): String {
            val directions = arrayOf("É", "ÉK", "K", "DK", "D", "DNY", "NY", "ÉNY")
            val normalized = ((degrees % 360) + 360) % 360
            val index = ((normalized + 22.5) / 45.0).toInt() % 8
            return directions[index]
        }
    }
}
