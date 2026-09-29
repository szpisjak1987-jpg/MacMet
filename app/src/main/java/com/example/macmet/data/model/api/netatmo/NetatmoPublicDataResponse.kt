package com.example.macmet.data.model.api.netatmo

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NetatmoPublicDataResponse(
    val body: List<NetatmoStation>?
)

@JsonClass(generateAdapter = true)
data class NetatmoStation(
    @Json(name = "_id") val id: String?,
    val place: NetatmoPlace?,
    val measures: Map<String, NetatmoMeasure>?
)

@JsonClass(generateAdapter = true)
data class NetatmoPlace(
    val location: List<Double>?, // [longitude, latitude]
    val altitude: Double?,
    val city: String?,
    val street: String?
)

@JsonClass(generateAdapter = true)
data class NetatmoMeasure(
    val res: Map<String, List<Double>>?,
    val type: List<String>?
)
