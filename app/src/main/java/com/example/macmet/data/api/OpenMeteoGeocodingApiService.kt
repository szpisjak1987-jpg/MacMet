package com.example.macmet.data.api

import com.example.macmet.data.model.api.OpenMeteoGeocodingResponseDto
import com.example.macmet.data.model.api.OpenMeteoGeocodingSearchResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoGeocodingApiService {

    @GET("v1/reverse")
    suspend fun reverseGeocode(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("language") language: String = "hu",
        @Query("format") format: String = "json"
    ): OpenMeteoGeocodingResponseDto

    @GET("v1/search")
    suspend fun search(
        @Query("name") name: String,
        @Query("language") language: String = "hu",
        @Query("count") count: Int = 1,
        @Query("format") format: String = "json"
    ): OpenMeteoGeocodingResponseDto

    @GET("v1/search")
    suspend fun searchLocations(
        @Query("name") name: String,
        @Query("count") count: Int = 10,
        @Query("language") language: String = "hu",
        @Query("format") format: String = "json"
    ): Response<OpenMeteoGeocodingSearchResponseDto>

    companion object {
        const val BASE_URL = "https://geocoding-api.open-meteo.com/"
    }
}
