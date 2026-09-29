package com.example.macmet.data.api

import com.example.macmet.data.api.netatmo.NetatmoApiService
import com.example.macmet.data.api.netatmo.NetatmoAuthInterceptor
import com.example.macmet.data.model.api.FlexibleDoubleAdapter
import com.example.macmet.data.model.api.FlexibleLongAdapter
import com.example.macmet.data.model.api.FlexibleStringAdapter
import com.example.macmet.data.model.api.MetarDtoAdapter
import com.example.macmet.data.updater.GithubApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(MetarDtoAdapter())
            .add(FlexibleDoubleAdapter)
            .add(FlexibleLongAdapter)
            .add(FlexibleStringAdapter)
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    val apiService: OpenMeteoApiService by lazy {
        Retrofit.Builder()
            .baseUrl(OpenMeteoApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoApiService::class.java)
    }

    val openSenseMapApiService: OpenSenseMapApiService by lazy {
        Retrofit.Builder()
            .baseUrl(OpenSenseMapApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenSenseMapApiService::class.java)
    }

    val aviationWeatherApiService: AviationWeatherApiService by lazy {
        Retrofit.Builder()
            .baseUrl(AviationWeatherApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AviationWeatherApiService::class.java)
    }

    val openMeteoGeocodingApiService: OpenMeteoGeocodingApiService by lazy {
        Retrofit.Builder()
            .baseUrl(OpenMeteoGeocodingApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoGeocodingApiService::class.java)
    }

    val netatmoApiService: NetatmoApiService by lazy {
        val netatmoOkHttpClient = okHttpClient.newBuilder()
            .addInterceptor(
                NetatmoAuthInterceptor(
                    clientId = "6abbdce8465369b4da0ac0d5",
                    clientSecret = "P3YiJBSjNIyH2e6QCkmujImCzEWcy",
                    accessToken = "6ab7c4545b1369e0e90bcd9d|eaf23a590e03f46cd9e3b3539aa3a60d",
                    refreshToken = "6ab7c4545b1369e0e90bcd9d|41d8d9e9010bd358de534171164df134",
                    tokenRefreshAction = { refresh, clientId, secret ->
                        try {
                            val tempRetrofit = Retrofit.Builder()
                                .baseUrl(NetatmoApiService.BASE_URL)
                                .client(okHttpClient)
                                .addConverterFactory(MoshiConverterFactory.create(moshi))
                                .build()
                                .create(NetatmoApiService::class.java)
                                
                            val resp = tempRetrofit.refreshToken(
                                refreshToken = refresh,
                                clientId = clientId,
                                clientSecret = secret
                            )
                            val body = resp.body()
                            if (body?.accessToken != null && body.refreshToken != null) {
                                Pair(body.accessToken, body.refreshToken)
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }
                )
            )
            .build()
            
        Retrofit.Builder()
            .baseUrl(NetatmoApiService.BASE_URL)
            .client(netatmoOkHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(NetatmoApiService::class.java)
    }

    val githubApiService: GithubApiService by lazy {
        Retrofit.Builder()
            .baseUrl(GithubApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GithubApiService::class.java)
    }
}
