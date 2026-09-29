package com.example.macmet.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class FusedLocationTracker(
    private val context: Context,
    private val locationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context),
    private val locationNameResolver: LocationNameResolver = LocationNameResolverImpl(context)
) : LocationTracker {

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): LocationData {
        val hasFineLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarseLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val isGpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true

        if ((!hasFineLocationPermission && !hasCoarseLocationPermission) || !isGpsEnabled) {
            return LocationData.BUDAPEST_FALLBACK
        }

        return suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()

            locationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location: Location? ->
                if (location != null && continuation.isActive) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val cityName = locationNameResolver.resolveCityName(location.latitude, location.longitude)
                        if (continuation.isActive) {
                            continuation.resume(
                                LocationData(
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                    isFallback = false,
                                    cityName = cityName
                                )
                            )
                        }
                    }
                } else if (continuation.isActive) {
                    locationClient.lastLocation.addOnSuccessListener { lastLocation: Location? ->
                        if (lastLocation != null && continuation.isActive) {
                            CoroutineScope(Dispatchers.IO).launch {
                                val cityName = locationNameResolver.resolveCityName(lastLocation.latitude, lastLocation.longitude)
                                if (continuation.isActive) {
                                    continuation.resume(
                                        LocationData(
                                            latitude = lastLocation.latitude,
                                            longitude = lastLocation.longitude,
                                            isFallback = false,
                                            cityName = cityName
                                        )
                                    )
                                }
                            }
                        } else if (continuation.isActive) {
                            continuation.resume(LocationData.BUDAPEST_FALLBACK)
                        }
                    }.addOnFailureListener {
                        if (continuation.isActive) {
                            continuation.resume(LocationData.BUDAPEST_FALLBACK)
                        }
                    }
                }
            }.addOnFailureListener {
                if (continuation.isActive) {
                    continuation.resume(LocationData.BUDAPEST_FALLBACK)
                }
            }

            continuation.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }
        }
    }
}
