package com.example.macmet.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Returns a Material Design ImageVector matching the WMO weather code and time of day.
 */
fun getWeatherIcon(weatherCode: Int, isDay: Boolean = true): ImageVector {
    return when (weatherCode) {
        0, 1 -> if (isDay) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay
        2 -> if (isDay) Icons.Rounded.WbCloudy else Icons.Rounded.NightsStay
        3 -> Icons.Rounded.Cloud
        45, 48 -> Icons.Rounded.Cloud
        51, 53, 55, 56, 57 -> Icons.Rounded.Grain
        61, 63, 65, 66, 67, 80, 81, 82 -> Icons.Rounded.WaterDrop
        71, 73, 75, 77, 85, 86 -> Icons.Rounded.AcUnit
        95, 96, 99 -> Icons.Rounded.Thunderstorm
        else -> Icons.AutoMirrored.Rounded.HelpOutline
    }
}

/**
 * Returns a vibrant color matching the weather condition for spectacular UI styling.
 */
fun getWeatherIconColor(weatherCode: Int, isDay: Boolean = true): Color {
    return when (weatherCode) {
        0, 1 -> if (isDay) Color(0xFFFFB300) else Color(0xFF7986CB) // Sunny Amber / Night Indigo
        2 -> if (isDay) Color(0xFF90A4AE) else Color(0xFF7986CB) // Cloudy Blue Grey
        3, 45, 48 -> Color(0xFF78909C) // Heavy Cloud / Fog
        51, 53, 55, 56, 57 -> Color(0xFF4FC3F7) // Drizzle Light Blue
        61, 63, 65, 66, 67, 80, 81, 82 -> Color(0xFF0288D1) // Rain Blue
        71, 73, 75, 77, 85, 86 -> Color(0xFF26A69A) // Snow Teal
        95, 96, 99 -> Color(0xFF7E57C2) // Thunderstorm Purple
        else -> Color(0xFF757575)
    }
}
