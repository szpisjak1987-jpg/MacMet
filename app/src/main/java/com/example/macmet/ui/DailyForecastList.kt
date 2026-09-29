package com.example.macmet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.macmet.data.model.domain.DailyForecast
import com.example.macmet.data.model.domain.WeatherCondition
import com.example.macmet.ui.theme.MacóMetTheme

@Composable
fun DailyForecastList(
    dailyForecasts: List<DailyForecast>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "7 napos előrejelzés",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            dailyForecasts.forEach { forecast ->
                DailyItemCard(forecast = forecast)
            }
        }
    }
}

@Composable
private fun DailyItemCard(
    forecast: DailyForecast,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Day name & Hungarian Date
            Column(
                modifier = Modifier.weight(1.1f)
            ) {
                Text(
                    text = forecast.dayOfWeek,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatHungarianDate(forecast.date),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Middle: Icon & Condition Description
            Row(
                modifier = Modifier.weight(1.5f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = getWeatherIcon(forecast.weatherCode, isDay = true),
                    contentDescription = forecast.weatherCondition.descriptionHu,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = forecast.weatherCondition.descriptionHu,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (forecast.precipitationProbabilityMax > 0 || forecast.precipitationSum > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.WaterDrop,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${forecast.precipitationSum} mm (${forecast.precipitationProbabilityMax}%)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Right: Temperatures (Min / Max)
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${forecast.tempMax} °C",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${forecast.tempMin} °C",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

private fun formatHungarianDate(dateString: String): String {
    return try {
        val parts = dateString.split("-")
        if (parts.size == 3) {
            val month = parts[1].toInt()
            val day = parts[2].toInt()
            val monthName = when (month) {
                1 -> "Jan."
                2 -> "Feb."
                3 -> "Már."
                4 -> "Ápr."
                5 -> "Máj."
                6 -> "Jún."
                7 -> "Júl."
                8 -> "Aug."
                9 -> "Szept."
                10 -> "Okt."
                11 -> "Nov."
                12 -> "Dec."
                else -> "$month."
            }
            "$monthName $day."
        } else {
            dateString
        }
    } catch (_: Exception) {
        dateString
    }
}

@Preview(showBackground = true)
@Composable
fun DailyForecastListPreview() {
    val sampleDaily = listOf(
        DailyForecast(
            date = "2025-05-15",
            dayOfWeek = "Csütörtök",
            tempMax = 24.0,
            tempMin = 12.0,
            precipitationSum = 0.0,
            precipitationProbabilityMax = 10,
            weatherCode = 1,
            weatherCondition = WeatherCondition.fromWmoCode(1)
        ),
        DailyForecast(
            date = "2025-05-16",
            dayOfWeek = "Péntek",
            tempMax = 22.5,
            tempMin = 13.1,
            precipitationSum = 2.5,
            precipitationProbabilityMax = 60,
            weatherCode = 61,
            weatherCondition = WeatherCondition.fromWmoCode(61)
        ),
        DailyForecast(
            date = "2025-05-17",
            dayOfWeek = "Szombat",
            tempMax = 25.0,
            tempMin = 14.0,
            precipitationSum = 0.0,
            precipitationProbabilityMax = 5,
            weatherCode = 0,
            weatherCondition = WeatherCondition.fromWmoCode(0)
        )
    )

    MacóMetTheme {
        DailyForecastList(
            dailyForecasts = sampleDaily,
            modifier = Modifier.padding(16.dp)
        )
    }
}
