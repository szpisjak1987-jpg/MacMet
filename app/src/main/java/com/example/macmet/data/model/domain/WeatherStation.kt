package com.example.macmet.data.model.domain

enum class StationProviderType(val displayName: String, val baseWeightMultiplier: Double) {
    OFFICIAL_NOAA_SYNOP("NOAA / WMO", 1.5),
    AMATEUR_OPENSENSEMAP("OpenSenseMap", 1.0),
    AMATEUR_NETATMO("Netatmo", 1.2)
}

data class WeatherStation(
    val id: String,
    val name: String,
    val providerType: StationProviderType,
    val latitude: Double,
    val longitude: Double,
    val distanceKm: Double,
    val temperature: Double? = null,
    val relativeHumidity: Double? = null,
    val pressure: Double? = null,
    val windSpeed: Double? = null,
    val windDirection: Double? = null,
    val dewPoint: Double? = null,
    val timestampMs: Long? = null
)
