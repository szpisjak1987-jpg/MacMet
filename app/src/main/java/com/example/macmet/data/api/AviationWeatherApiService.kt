package com.example.macmet.data.api

import com.example.macmet.data.model.api.MetarDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface AviationWeatherApiService {

    @GET("metar")
    suspend fun getMetar(
        @Query("bbox", encoded = true) bbox: String? = null, // "minLat,minLon,maxLat,maxLon"
        @Query("ids", encoded = true) ids: String? = null,
        @Query("format") format: String = "json"
    ): Response<List<MetarDto>>

    companion object {
        const val BASE_URL = "https://aviationweather.gov/api/data/"
        const val DEFAULT_HUNGARIAN_ICAO_IDS = "LHBP,LHNY,LHDC,LHSM,LHPR,LHUD,LHSN,LHKV,LHKA,LHPP,LHPPA"
    }
}
