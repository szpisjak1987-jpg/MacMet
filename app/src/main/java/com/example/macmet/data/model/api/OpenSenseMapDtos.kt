package com.example.macmet.data.model.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SenseBoxDto(
    @Json(name = "_id") val id: String,
    val name: String? = null,
    val currentLocation: SenseBoxLocationDto? = null,
    val sensors: List<SenseBoxSensorDto>? = null
)

@JsonClass(generateAdapter = true)
data class SenseBoxLocationDto(
    val type: String? = null,
    val coordinates: List<Double>? = null // [longitude, latitude]
)

@JsonClass(generateAdapter = true)
data class SenseBoxSensorDto(
    @Json(name = "_id") val id: String? = null,
    val title: String? = null,
    val unit: String? = null,
    val sensorType: String? = null,
    val lastMeasurement: SenseBoxMeasurementDto? = null
)

@JsonClass(generateAdapter = true)
data class SenseBoxMeasurementDto(
    val value: String? = null,
    val createdAt: String? = null
)
