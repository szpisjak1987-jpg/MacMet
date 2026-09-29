package com.example.macmet.data.util

import org.junit.Assert.assertEquals
import org.junit.Test

class HaversineTest {

    @Test
    fun calculateDistanceKm_sameLocation_returnsZero() {
        val dist = Haversine.calculateDistanceKm(47.4979, 19.0402, 47.4979, 19.0402)
        assertEquals(0.0, dist, 0.001)
    }

    @Test
    fun calculateDistanceKm_budapestToVienna_returnsApprox214km() {
        // Budapest (47.4979, 19.0402) to Vienna (48.2082, 16.3738)
        val dist = Haversine.calculateDistanceKm(47.4979, 19.0402, 48.2082, 16.3738)
        assertEquals(214.0, dist, 5.0)
    }

    @Test
    fun calculateDistanceKm_budapestParliamentToKeleti_returnsApprox3km() {
        // Parliament (47.5071, 19.0458) to Keleti Station (47.5003, 19.0839)
        val dist = Haversine.calculateDistanceKm(47.5071, 19.0458, 47.5003, 19.0839)
        assertEquals(2.9, dist, 0.5)
    }
}
