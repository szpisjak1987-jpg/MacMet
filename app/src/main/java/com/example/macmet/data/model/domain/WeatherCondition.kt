package com.example.macmet.data.model.domain

enum class WeatherCategory {
    CLEAR,
    PARTLY_CLOUDY,
    CLOUDY,
    FOG,
    DRIZZLE,
    RAIN,
    SNOW,
    THUNDERSTORM,
    UNKNOWN
}

data class WeatherCondition(
    val code: Int,
    val descriptionHu: String,
    val descriptionEn: String,
    val category: WeatherCategory,
    val iconName: String
) {
    companion object {
        fun fromWmoCode(code: Int): WeatherCondition {
            return when (code) {
                0 -> WeatherCondition(0, "Derült", "Clear sky", WeatherCategory.CLEAR, "wb_sunny")
                1 -> WeatherCondition(1, "Túlnyomóan napos", "Mainly clear", WeatherCategory.CLEAR, "wb_sunny")
                2 -> WeatherCondition(2, "Részben felhős", "Partly cloudy", WeatherCategory.PARTLY_CLOUDY, "wb_cloudy")
                3 -> WeatherCondition(3, "Borult", "Overcast", WeatherCategory.CLOUDY, "cloud")
                45 -> WeatherCondition(45, "Sűrű köd", "Dense fog", WeatherCategory.FOG, "cloud")
                48 -> WeatherCondition(48, "Zúzmarás köd", "Depositing rime fog", WeatherCategory.FOG, "cloud")
                51 -> WeatherCondition(51, "Enyhe szitálás", "Light drizzle", WeatherCategory.DRIZZLE, "grain")
                53 -> WeatherCondition(53, "Mérsékelt szitálás", "Moderate drizzle", WeatherCategory.DRIZZLE, "grain")
                55 -> WeatherCondition(55, "Sűrű szitálás", "Dense drizzle", WeatherCategory.DRIZZLE, "grain")
                56, 57 -> WeatherCondition(code, "Ónos szitálás", "Freezing drizzle", WeatherCategory.DRIZZLE, "grain")
                61 -> WeatherCondition(61, "Enyhe eső", "Slight rain", WeatherCategory.RAIN, "water_drop")
                63 -> WeatherCondition(63, "Mérsékelt eső", "Moderate rain", WeatherCategory.RAIN, "water_drop")
                65 -> WeatherCondition(65, "Heves eső", "Heavy rain", WeatherCategory.RAIN, "water_drop")
                66, 67 -> WeatherCondition(code, "Ónos eső", "Freezing rain", WeatherCategory.RAIN, "water_drop")
                71 -> WeatherCondition(71, "Enyhe havazás", "Slight snow", WeatherCategory.SNOW, "ac_unit")
                73 -> WeatherCondition(73, "Mérsékelt havazás", "Moderate snow", WeatherCategory.SNOW, "ac_unit")
                75 -> WeatherCondition(75, "Heves havazás", "Heavy snow", WeatherCategory.SNOW, "ac_unit")
                77 -> WeatherCondition(77, "Hószállingózás", "Snow grains", WeatherCategory.SNOW, "ac_unit")
                80 -> WeatherCondition(80, "Gomolyerfős, zápor", "Rain showers", WeatherCategory.RAIN, "water_drop")
                81, 82 -> WeatherCondition(code, "Záporeső", "Rain showers", WeatherCategory.RAIN, "water_drop")
                85, 86 -> WeatherCondition(code, "Hózápor", "Snow showers", WeatherCategory.SNOW, "ac_unit")
                95 -> WeatherCondition(95, "Zivatar", "Thunderstorm", WeatherCategory.THUNDERSTORM, "thunderstorm")
                96, 99 -> WeatherCondition(code, "Jégesős zivatar", "Thunderstorm with hail", WeatherCategory.THUNDERSTORM, "thunderstorm")
                else -> WeatherCondition(code, "Ismeretlen", "Unknown", WeatherCategory.UNKNOWN, "help_outline")
            }
        }
    }
}
