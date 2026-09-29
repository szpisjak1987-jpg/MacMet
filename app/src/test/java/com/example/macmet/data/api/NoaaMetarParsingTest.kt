package com.example.macmet.data.api

import com.example.macmet.data.model.api.FlexibleDoubleAdapter
import com.example.macmet.data.model.api.FlexibleLongAdapter
import com.example.macmet.data.model.api.FlexibleStringAdapter
import com.example.macmet.data.model.api.MetarDto
import com.example.macmet.data.model.api.MetarDtoAdapter
import com.example.macmet.data.model.domain.StationProviderType
import com.example.macmet.data.repository.WeatherRepositoryImpl
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Response

class NoaaMetarParsingTest {

    private val moshi: Moshi = Moshi.Builder()
        .add(MetarDtoAdapter())
        .add(FlexibleDoubleAdapter)
        .add(FlexibleLongAdapter)
        .add(FlexibleStringAdapter)
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun parseNoaaMetarJson_withStandardTypesAndTypeVariations_succeedsWithoutJsonDataException() {
        val sampleJson = """
            [
              {
                "icaoId": "LHBP",
                "name": "Budapest / Ferihegy",
                "lat": 47.43,
                "lon": 19.26,
                "elev": 151,
                "reportTime": "2025-02-23 21:00:00",
                "obsTime": 1740344400,
                "temp": 8,
                "dewp": 2,
                "wdir": 310,
                "wspd": 7,
                "wgst": null,
                "visib": "10+",
                "altim": 1021,
                "slp": 1021.2,
                "pres": 1018.5,
                "rawOb": "LHBP 232100Z 31007KT 9999 FEW025 08/02 Q1021 NOSIG"
              },
              {
                "icaoId": "LHNY",
                "name": "Nyiregyhaza",
                "lat": "47.98",
                "lon": "21.69",
                "elev": "103",
                "reportTime": "2025-02-23 21:00:00",
                "obsTime": "1740344400",
                "temp": "7.5",
                "dewp": "1.0",
                "wdir": "280",
                "wspd": "5",
                "wgst": "12",
                "visib": 10,
                "altim": "1020",
                "slp": "1020.5",
                "pres": "1017.8",
                "rawOb": "LHNY 232100Z 28005KT 9999 FEW030 07/01 Q1020"
              },
              {
                "icaoId": "LHSM",
                "name": "Sarmellek",
                "lat": 46.68,
                "lon": 17.15,
                "temp": "M02",
                "dewp": "M05",
                "wdir": "VRB",
                "wspd": "3",
                "obsTime": "2025-02-23T21:00:00Z"
              }
            ]
        """.trimIndent()

        val type = Types.newParameterizedType(List::class.java, MetarDto::class.java)
        val adapter = moshi.adapter<List<MetarDto>>(type)
        val list = adapter.fromJson(sampleJson)

        assertNotNull(list)
        assertEquals(3, list!!.size)

        // Item 0: LHBP (standard numbers and strings)
        val lhbp = list[0]
        assertEquals("LHBP", lhbp.icaoId)
        assertEquals("Budapest / Ferihegy", lhbp.name)
        assertEquals(47.43, lhbp.lat!!, 0.001)
        assertEquals(19.26, lhbp.lon!!, 0.001)
        assertEquals(151.0, lhbp.elev!!, 0.001)
        assertEquals(1740344400L, lhbp.obsTime)
        assertEquals(8.0, lhbp.temp!!, 0.001)
        assertEquals(2.0, lhbp.dewp!!, 0.001)
        assertEquals(310.0, lhbp.wdir!!, 0.001)
        assertEquals(7.0, lhbp.wspd!!, 0.001)
        assertNull(lhbp.wgst)
        assertEquals("10+", lhbp.visib)
        assertEquals(1021.0, lhbp.altim!!, 0.001)
        assertEquals(1021.2, lhbp.slp!!, 0.001)
        assertEquals(1018.5, lhbp.pres!!, 0.001)

        // Item 1: LHNY (strings representing numbers)
        val lhny = list[1]
        assertEquals("LHNY", lhny.icaoId)
        assertEquals("Nyiregyhaza", lhny.name)
        assertEquals(47.98, lhny.lat!!, 0.001)
        assertEquals(21.69, lhny.lon!!, 0.001)
        assertEquals(103.0, lhny.elev!!, 0.001)
        assertEquals(1740344400L, lhny.obsTime)
        assertEquals(7.5, lhny.temp!!, 0.001)
        assertEquals(1.0, lhny.dewp!!, 0.001)
        assertEquals(280.0, lhny.wdir!!, 0.001)
        assertEquals(5.0, lhny.wspd!!, 0.001)
        assertEquals(12.0, lhny.wgst!!, 0.001)
        assertEquals("10", lhny.visib)
        assertEquals(1020.0, lhny.altim!!, 0.001)
        assertEquals(1020.5, lhny.slp!!, 0.001)

        // Item 2: LHSM (METAR "M" negative temp notation & non-numeric wdir "VRB")
        val lhsm = list[2]
        assertEquals("LHSM", lhsm.icaoId)
        assertEquals(-2.0, lhsm.temp!!, 0.001)
        assertEquals(-5.0, lhsm.dewp!!, 0.001)
        assertNull(lhsm.wdir) // "VRB" cannot be parsed to Double, should gracefully be null
        assertEquals(3.0, lhsm.wspd!!, 0.001)
        assertEquals(1740344400L, lhsm.obsTime)
    }

    @Test
    fun fetchNoaaStations_queriesBboxAndHungarianIds_mapsToWeatherStations() = runTest {
        val fakeApiService = object : AviationWeatherApiService {
            override suspend fun getMetar(bbox: String?, ids: String?, format: String): Response<List<MetarDto>> {
                val list = when {
                    bbox != null -> listOf(
                        MetarDto(
                            icaoId = "LHBP",
                            name = "Budapest Airport",
                            lat = 47.43,
                            lon = 19.26,
                            temp = 8.0,
                            dewp = 2.0,
                            wspd = 10.0, // knots
                            wdir = 310.0,
                            slp = 1021.2,
                            obsTime = 1740344400L
                        )
                    )
                    ids != null -> listOf(
                        MetarDto(
                            icaoId = "LHBP",
                            name = "Budapest Airport",
                            lat = 47.43,
                            lon = 19.26,
                            temp = 8.0,
                            dewp = 2.0,
                            wspd = 10.0,
                            wdir = 310.0,
                            slp = 1021.2,
                            obsTime = 1740344400L
                        ),
                        MetarDto(
                            icaoId = "LHNY",
                            name = "Nyiregyhaza Station",
                            lat = 47.98,
                            lon = 21.69,
                            temp = 7.0,
                            dewp = 1.0,
                            wspd = 5.0,
                            wdir = 270.0,
                            altim = 1020.0,
                            obsTime = 1740344400L
                        )
                    )
                    else -> emptyList()
                }
                return Response.success(list)
            }
        }

        val repository = WeatherRepositoryImpl(
            aviationWeatherApiService = fakeApiService
        )

        val stations = repository.fetchNoaaStations(47.4979, 19.0402)

        // Should return combined, distinct stations (LHBP and LHNY)
        assertEquals(2, stations.size)

        val lhbpStation = stations.find { it.id == "LHBP" }
        assertNotNull(lhbpStation)
        assertEquals("LHBP / Budapest Airport", lhbpStation!!.name)
        assertEquals(StationProviderType.OFFICIAL_NOAA_SYNOP, lhbpStation.providerType)
        assertEquals(8.0, lhbpStation.temperature!!, 0.001)
        assertEquals(2.0, lhbpStation.dewPoint!!, 0.001)
        assertEquals(1021.2, lhbpStation.pressure!!, 0.001)
        assertEquals(18.52, lhbpStation.windSpeed!!, 0.01) // 10 knots * 1.852 = 18.52 km/h
        assertEquals(310.0, lhbpStation.windDirection!!, 0.001)
        assertEquals(1740344400000L, lhbpStation.timestampMs)

        val lhnyStation = stations.find { it.id == "LHNY" }
        assertNotNull(lhnyStation)
        assertEquals("LHNY / Nyiregyhaza Station", lhnyStation!!.name)
    }
}
