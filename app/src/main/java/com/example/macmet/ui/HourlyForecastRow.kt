package com.example.macmet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.macmet.data.model.domain.HourlyForecast
import com.example.macmet.data.model.domain.WeatherCondition
import com.example.macmet.ui.theme.MacóMetTheme

@Composable
fun HourlyForecastRow(
    hourlyForecasts: List<HourlyForecast>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Órás előrejelzés",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(hourlyForecasts) { forecast ->
                HourlyItemCard(forecast = forecast)
            }
        }
    }
}

@Composable
private fun HourlyItemCard(
    forecast: HourlyForecast,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.width(90.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = forecast.formattedHour,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Icon(
                imageVector = getWeatherIcon(forecast.weatherCode),
                contentDescription = forecast.weatherCondition.descriptionHu,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${forecast.temperature} °C",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (forecast.precipitationProbability > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WaterDrop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${forecast.precipitationProbability}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HourlyForecastRowPreview() {
    val sampleForecasts = listOf(
        HourlyForecast(
            time = "2025-02-18T14:00",
            formattedHour = "14:00",
            temperature = 18.5,
            relativeHumidity = 60,
            precipitation = 0.0,
            precipitationProbability = 10,
            weatherCode = 1,
            weatherCondition = WeatherCondition.fromWmoCode(1),
            windSpeed = 10.0
        ),
        HourlyForecast(
            time = "2025-02-18T15:00",
            formattedHour = "15:00",
            temperature = 19.2,
            relativeHumidity = 58,
            precipitation = 0.0,
            precipitationProbability = 0,
            weatherCode = 0,
            weatherCondition = WeatherCondition.fromWmoCode(0),
            windSpeed = 12.0
        ),
        HourlyForecast(
            time = "2025-02-18T16:00",
            formattedHour = "16:00",
            temperature = 18.8,
            relativeHumidity = 62,
            precipitation = 0.5,
            precipitationProbability = 40,
            weatherCode = 61,
            weatherCondition = WeatherCondition.fromWmoCode(61),
            windSpeed = 15.0
        ),
        HourlyForecast(
            time = "2025-02-18T17:00",
            formattedHour = "17:00",
            temperature = 17.0,
            relativeHumidity = 70,
            precipitation = 1.2,
            precipitationProbability = 70,
            weatherCode = 63,
            weatherCondition = WeatherCondition.fromWmoCode(63),
            windSpeed = 14.0
        )
    )

    MacóMetTheme {
        HourlyForecastRow(
            hourlyForecasts = sampleForecasts,
            modifier = Modifier.padding(16.dp)
        )
    }
}
