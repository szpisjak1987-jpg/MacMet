package com.example.macmet.data.util

import com.example.macmet.data.api.AviationWeatherApiService
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

class CoordinateFormattingTest {

    private lateinit var originalLocale: Locale

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
        // Force Hungarian locale for test execution to ensure Hungarian decimal commas (",")
        Locale.setDefault(Locale.Builder().setLanguage("hu").setRegion("HU").build())
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun openSenseMapNearParam_usesUsLocaleDotAndLonLatOrder() {
        val lat = 47.4979
        val lon = 19.0402

        // OpenSenseMap format: lon,lat with US dots
        val nearParam = String.format(Locale.US, "%.4f,%.4f", lon, lat)

        assertEquals("19.0402,47.4979", nearParam)
        assertFalse("Near param must not contain Hungarian commas for decimal point", nearParam.contains("19,0402"))
        assertTrue("Near param must contain dot for decimals", nearParam.contains("19.0402"))
    }

    @Test
    fun noaaBboxParam_usesUsLocaleDotAndMinMaxCoordinates() {
        val lat = 47.4979
        val lon = 19.0402

        val minLat = lat - 0.45
        val maxLat = lat + 0.45
        val minLon = lon - 0.45
        val maxLon = lon + 0.45

        val bboxStr = String.format(Locale.US, "%.2f,%.2f,%.2f,%.2f", minLat, minLon, maxLat, maxLon)

        assertEquals("47.05,18.59,47.95,19.49", bboxStr)
        assertFalse("Bbox string must not use Hungarian commas as decimal point", bboxStr.contains("47,05"))
    }

    @Test
    fun noaaHungarianDefaultStationIds_containsKeyHungarianAirports() {
        val ids = AviationWeatherApiService.DEFAULT_HUNGARIAN_ICAO_IDS
        assertTrue(ids.contains("LHBP")) // Budapest
        assertTrue(ids.contains("LHDC")) // Debrecen
        assertTrue(ids.contains("LHPP")) // Pécs
        assertTrue(ids.contains("LHUD")) // Szeged
    }

}
