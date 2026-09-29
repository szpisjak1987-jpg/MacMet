package com.example.macmet.di

import android.content.Context
import com.example.macmet.data.api.AviationWeatherApiService
import com.example.macmet.data.api.OpenMeteoApiService
import com.example.macmet.data.api.OpenSenseMapApiService
import com.example.macmet.data.api.RetrofitClient
import com.example.macmet.data.api.netatmo.NetatmoApiService
import com.example.macmet.data.location.FusedLocationTracker
import com.example.macmet.data.location.LocationTracker
import com.example.macmet.data.repository.WeatherRepository
import com.example.macmet.data.repository.WeatherRepositoryImpl

interface AppContainer {
    val apiService: OpenMeteoApiService
    val openSenseMapApiService: OpenSenseMapApiService
    val aviationWeatherApiService: AviationWeatherApiService
    val netatmoApiService: NetatmoApiService
    val locationTracker: LocationTracker
    val weatherRepository: WeatherRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    override val apiService: OpenMeteoApiService by lazy {
        RetrofitClient.apiService
    }

    override val openSenseMapApiService: OpenSenseMapApiService by lazy {
        RetrofitClient.openSenseMapApiService
    }

    override val aviationWeatherApiService: AviationWeatherApiService by lazy {
        RetrofitClient.aviationWeatherApiService
    }

    override val netatmoApiService: NetatmoApiService by lazy {
        RetrofitClient.netatmoApiService
    }

    override val locationTracker: LocationTracker by lazy {
        FusedLocationTracker(context)
    }

    override val weatherRepository: WeatherRepository by lazy {
        WeatherRepositoryImpl(
            apiService = apiService,
            openSenseMapApiService = openSenseMapApiService,
            aviationWeatherApiService = aviationWeatherApiService,
            netatmoApiService = netatmoApiService,
            locationTracker = locationTracker
        )
    }
}

