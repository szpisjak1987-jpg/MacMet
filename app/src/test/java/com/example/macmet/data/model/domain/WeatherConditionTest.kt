package com.example.macmet.data.model.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherConditionTest {

    @Test
    fun fromWmoCode_mapsCorrectly() {
        val clear = WeatherCondition.fromWmoCode(0)
        assertEquals("Derült", clear.descriptionHu)
        assertEquals(WeatherCategory.CLEAR, clear.category)

        val rain = WeatherCondition.fromWmoCode(63)
        assertEquals("Mérsékelt eső", rain.descriptionHu)
        assertEquals(WeatherCategory.RAIN, rain.category)

        val thunderstorm = WeatherCondition.fromWmoCode(95)
        assertEquals("Zivatar", thunderstorm.descriptionHu)
        assertEquals(WeatherCategory.THUNDERSTORM, thunderstorm.category)

        val unknown = WeatherCondition.fromWmoCode(999)
        assertEquals("Ismeretlen", unknown.descriptionHu)
        assertEquals(WeatherCategory.UNKNOWN, unknown.category)
    }
}
