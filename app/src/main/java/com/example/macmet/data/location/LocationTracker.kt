package com.example.macmet.data.location

interface LocationTracker {
    suspend fun getCurrentLocation(): LocationData
}
