package com.example.macmet.data.location

import com.example.macmet.data.api.OpenMeteoGeocodingApiService
import com.example.macmet.data.model.api.GeocodingResultDto
import com.example.macmet.data.model.api.OpenMeteoGeocodingResponseDto
import com.example.macmet.data.model.api.OpenMeteoGeocodingSearchResponseDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response

class LocationNameResolverTest {

    private class FakeGeocodingApiService(
        private val resultName: String? = null,
        private val shouldThrow: Boolean = false
    ) : OpenMeteoGeocodingApiService {

        override suspend fun reverseGeocode(
            latitude: Double,
            longitude: Double,
            language: String,
            format: String
        ): OpenMeteoGeocodingResponseDto {
            if (shouldThrow) throw RuntimeException("Network error")
            val results = if (resultName != null) {
                listOf(
                    GeocodingResultDto(
                        id = 123L,
                        name = resultName,
                        latitude = latitude,
                        longitude = longitude,
                        country = "Hungary",
                        admin1 = resultName
                    )
                )
            } else {
                emptyList()
            }
            return OpenMeteoGeocodingResponseDto(results = results)
        }

        override suspend fun search(
            name: String,
            language: String,
            count: Int,
            format: String
        ): OpenMeteoGeocodingResponseDto {
            return reverseGeocode(0.0, 0.0, language, format)
        }

        override suspend fun searchLocations(
            name: String,
            count: Int,
            language: String,
            format: String
        ): Response<OpenMeteoGeocodingSearchResponseDto> {
            return Response.success(OpenMeteoGeocodingSearchResponseDto())
        }
    }

    @Test
    fun resolveWithOpenMeteoGeocoding_returnsCityNameWhenApiSucceeds() = runTest {
        val fakeApi = FakeGeocodingApiService(resultName = "Pécs")
        val resolver = LocationNameResolverImpl(geocodingApiService = fakeApi)

        val city = resolver.resolveWithOpenMeteoGeocoding(46.0727, 18.2323)
        assertEquals("Pécs", city)
    }

    @Test
    fun resolveWithOpenMeteoGeocoding_returnsCityNameForDebrecen() = runTest {
        val fakeApi = FakeGeocodingApiService(resultName = "Debrecen")
        val resolver = LocationNameResolverImpl(geocodingApiService = fakeApi)

        val city = resolver.resolveWithOpenMeteoGeocoding(47.5316, 21.6273)
        assertEquals("Debrecen", city)
    }

    @Test
    fun resolveCityName_fallsBackToBudapestOnApiFailure() = runTest {
        val fakeApi = FakeGeocodingApiService(shouldThrow = true)
        val resolver = LocationNameResolverImpl(geocodingApiService = fakeApi)

        val city = resolver.resolveCityName(46.0727, 18.2323)
        assertEquals("Budapest", city)
    }
}
