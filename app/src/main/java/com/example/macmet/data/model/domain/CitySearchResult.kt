package com.example.macmet.data.model.domain

data class CitySearchResult(
    val id: Long,
    val name: String,
    val country: String?,
    val admin1: String?,
    val latitude: Double,
    val longitude: Double,
    val countryCode: String?
)
