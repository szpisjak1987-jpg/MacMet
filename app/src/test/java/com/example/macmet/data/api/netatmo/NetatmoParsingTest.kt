package com.example.macmet.data.api.netatmo

import com.example.macmet.data.model.api.netatmo.NetatmoPublicDataResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class NetatmoParsingTest {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun testNetatmoJsonParsing() {
        val json = """
            {
              "status": "ok",
              "time_server": 1790697862,
              "body": [
                {
                  "_id": "70:ee:50:1c:85:ae",
                  "place": {
                    "location": [21.446728, 47.046996],
                    "city": "Komádi"
                  },
                  "measures": {
                    "02:00:00:1c:78:6e": {
                      "res": {
                        "1790697544": [28.9, 30]
                      },
                      "type": ["temperature", "humidity"]
                    },
                    "05:00:00:03:45:18": {
                      "rain_60min": 0,
                      "rain_24h": 0,
                      "rain_live": 0,
                      "rain_timeutc": 1790697576
                    }
                  }
                }
              ]
            }
        """.trimIndent()

        val adapter = moshi.adapter(NetatmoPublicDataResponse::class.java)
        val response = adapter.fromJson(json)

        assertNotNull(response)
        assertNotNull(response?.body)
        assertEquals(1, response?.body?.size)
        
        val station = response?.body?.get(0)
        assertEquals("70:ee:50:1c:85:ae", station?.id)
        assertEquals("Komádi", station?.place?.city)
        assertEquals(2, station?.place?.location?.size)
        
        val measures = station?.measures
        assertNotNull(measures)
        val mainMeasure = measures?.get("02:00:00:1c:78:6e")
        assertNotNull(mainMeasure)
        assertEquals(listOf("temperature", "humidity"), mainMeasure?.type)
        assertEquals(listOf(28.9, 30.0), mainMeasure?.res?.get("1790697544"))
    }
}
