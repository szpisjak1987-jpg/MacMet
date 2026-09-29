package com.example.macmet.data.api

import com.example.macmet.data.model.api.SenseBoxDto
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenSenseMapApiService {

    @GET("boxes")
    suspend fun getBoxes(
        @Query("near", encoded = true) near: String, // format: "{longitude},{latitude}"
        @Query("radius") radius: Int = 50000,
        @Query("format") format: String = "json"
    ): List<SenseBoxDto>

    companion object {
        const val BASE_URL = "https://api.opensensemap.org/"
    }
}
