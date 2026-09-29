package com.example.macmet.data.api.netatmo

import com.example.macmet.data.model.api.netatmo.NetatmoPublicDataResponse
import com.example.macmet.data.model.api.netatmo.NetatmoTokenResponse
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface NetatmoApiService {

    @GET("api/getpublicdata")
    suspend fun getPublicData(
        @Query("lat_ne") latNe: Double,
        @Query("lon_ne") lonNe: Double,
        @Query("lat_sw") latSw: Double,
        @Query("lon_sw") lonSw: Double
    ): Response<NetatmoPublicDataResponse>

    @FormUrlEncoded
    @POST("oauth2/token")
    suspend fun refreshToken(
        @Field("grant_type") grantType: String = "refresh_token",
        @Field("refresh_token") refreshToken: String,
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String
    ): Response<NetatmoTokenResponse>

    companion object {
        const val BASE_URL = "https://api.netatmo.com/"
    }
}
