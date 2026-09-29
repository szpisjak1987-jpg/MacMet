package com.example.macmet.data.location

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val isFallback: Boolean = false,
    val cityName: String = "Budapest"
) {
    companion object {
        val BUDAPEST_FALLBACK = LocationData(
            latitude = 47.4979,
            longitude = 19.0402,
            isFallback = true,
            cityName = "Budapest"
        )
    }
}
