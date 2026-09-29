package com.example.macmet.data.api

import com.example.macmet.data.model.api.SenseBoxDto
import com.example.macmet.data.model.api.SenseBoxLocationDto
import com.example.macmet.data.model.api.SenseBoxMeasurementDto
import com.example.macmet.data.model.api.SenseBoxSensorDto
import com.example.macmet.data.model.domain.StationProviderType
import com.example.macmet.data.repository.WeatherRepositoryImpl
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class OpenSenseMapParsingTest {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun parseOpenSenseMapJson_mapsCorrectlyToDto() {
        val jsonStr = """
            [
              {
                "_id": "58e123456789abcdef001122",
                "name": "Budapest Belváros Station",
                "currentLocation": {
                  "type": "Point",
                  "coordinates": [19.05, 47.50]
                },
                "sensors": [
                  {
                    "_id": "s1",
                    "title": "Temperatur",
                    "unit": "°C",
                    "lastMeasurement": {
                      "value": "18.5",
                      "createdAt": "2025-02-18T12:00:00.000Z"
                    }
                  },
                  {
                    "_id": "s2",
                    "title": "Rel. Luftfeuchte",
                    "unit": "%",
                    "lastMeasurement": {
                      "value": "62.0",
                      "createdAt": "2025-02-18T12:00:00.000Z"
                    }
                  }
                ]
              }
            ]
        """.trimIndent()

        val type = Types.newParameterizedType(List::class.java, SenseBoxDto::class.java)
        val adapter = moshi.adapter<List<SenseBoxDto>>(type)
        val boxes = adapter.fromJson(jsonStr)

        assertNotNull(boxes)
        assertEquals(1, boxes!!.size)

        val box = boxes[0]
        assertEquals("58e123456789abcdef001122", box.id)
        assertEquals("Budapest Belváros Station", box.name)
        assertEquals(19.05, box.currentLocation?.coordinates?.get(0))
        assertEquals(47.50, box.currentLocation?.coordinates?.get(1))
        assertEquals(2, box.sensors?.size)
        assertEquals("18.5", box.sensors!![0].lastMeasurement?.value)
    }

    @Test
    fun fetchOpenSenseMapStations_mapsBoxesToWeatherStations() = runTest {
        val fakeApiService = object : OpenSenseMapApiService {
            override suspend fun getBoxes(near: String, radius: Int, format: String): List<SenseBoxDto> {
                return listOf(
                    SenseBoxDto(
                        id = "box-001",
                        name = "Budapest Amateur Station",
                        currentLocation = SenseBoxLocationDto(
                            type = "Point",
                            coordinates = listOf(19.05, 47.50)
                        ),
                        sensors = listOf(
                            SenseBoxSensorDto(
                                title = "Temperatur",
                                unit = "°C",
                                lastMeasurement = SenseBoxMeasurementDto(value = "20.2")
                            ),
                            SenseBoxSensorDto(
                                title = "Humidity",
                                unit = "%",
                                lastMeasurement = SenseBoxMeasurementDto(value = "55.0")
                            )
                        )
                    )
                )
            }
        }

        val repository = WeatherRepositoryImpl(
            openSenseMapApiService = fakeApiService
        )

        val stations = repository.fetchOpenSenseMapStations(47.4979, 19.0402)
        assertEquals(1, stations.size)

        val st = stations[0]
        assertEquals("box-001", st.id)
        assertEquals("Budapest Amateur Station", st.name)
        assertEquals(StationProviderType.AMATEUR_OPENSENSEMAP, st.providerType)
        assertEquals(20.2, st.temperature!!, 0.01)
        assertEquals(55.0, st.relativeHumidity!!, 0.01)
    }
}
