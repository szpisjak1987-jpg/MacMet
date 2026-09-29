package com.example.macmet.data.location

import android.content.Context
import android.location.Geocoder
import android.os.Build
import com.example.macmet.data.api.OpenMeteoGeocodingApiService
import com.example.macmet.data.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

interface LocationNameResolver {
    suspend fun resolveCityName(latitude: Double, longitude: Double): String
}

class LocationNameResolverImpl(
    private val context: Context? = null,
    private val geocodingApiService: OpenMeteoGeocodingApiService? = RetrofitClient.openMeteoGeocodingApiService
) : LocationNameResolver {

    override suspend fun resolveCityName(latitude: Double, longitude: Double): String {
        // 1. Try Android Geocoder with Hungarian locale
        val androidCityName = resolveWithAndroidGeocoder(latitude, longitude)
        if (!androidCityName.isNullOrBlank() && androidCityName != "Jelenlegi helyszín") {
            return androidCityName
        }

        // 2. Fallback to Open-Meteo Geocoding API
        val apiCityName = resolveWithOpenMeteoGeocoding(latitude, longitude)
        if (!apiCityName.isNullOrBlank()) {
            return apiCityName
        }

        return "Budapest"
    }

    internal suspend fun resolveWithAndroidGeocoder(latitude: Double, longitude: Double): String? {
        val ctx = context ?: return null
        return try {
            val geocoder = Geocoder(ctx, Locale.Builder().setLanguage("hu").setRegion("HU").build())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        val address = addresses.firstOrNull()
                        val city = address?.locality
                            ?: address?.subAdminArea
                            ?: address?.adminArea
                            ?: address?.subLocality
                        if (continuation.isActive) {
                            continuation.resume(city)
                        }
                    }
                }
            } else {
                withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    val address = addresses?.firstOrNull()
                    address?.locality
                        ?: address?.subAdminArea
                        ?: address?.adminArea
                        ?: address?.subLocality
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    internal suspend fun resolveWithOpenMeteoGeocoding(latitude: Double, longitude: Double): String? {
        val api = geocodingApiService ?: return null
        return try {
            val response = api.reverseGeocode(latitude = latitude, longitude = longitude, language = "hu")
            val firstResult = response.results?.firstOrNull()
            firstResult?.name ?: firstResult?.admin1
        } catch (_: Exception) {
            null
        }
    }
}
