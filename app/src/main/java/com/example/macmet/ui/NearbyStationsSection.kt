package com.example.macmet.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.macmet.data.model.domain.StationProviderType
import com.example.macmet.data.model.domain.WeatherStation
import com.example.macmet.ui.theme.MacóMetTheme
import kotlin.math.roundToInt

@Composable
fun NearbyStationsSection(
    stations: List<WeatherStation>,
    modifier: Modifier = Modifier,
) {
    if (stations.isEmpty()) return
    
    var isExpanded by remember { mutableStateOf(value = false) }
    val closestStation = stations.first()
    val otherStations = stations.drop(1)

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header: Legközelebbi mérőállomás
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Radar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Legközelebbi mérőállomás",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Mindig mutatjuk a legközelebbi állomást
            NearbyStationItemCard(station = closestStation)

            // További állomások (ha vannak)
            if (otherStations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Kinyitható szekció fejléce
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isExpanded = !isExpanded }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "További ${otherStations.size} állomás a közelben",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Icon(
                        imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = if (isExpanded) "Összecsukás" else "Kinyitás",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        otherStations.forEach { station ->
                            NearbyStationItemCard(station = station)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NearbyStationItemCard(
    station: WeatherStation,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Station Name & Distance Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = formatDistance(station.distanceKm),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Provider Badge & Freshness Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProviderTagBadge(providerType = station.providerType)

                // Freshness Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatDataFreshness(station.timestampMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Live Measurements Row / Grid
            val hasMetrics = (station.temperature != null) ||
                    (station.relativeHumidity != null) ||
                    (station.pressure != null) ||
                    (station.windSpeed != null)

            if (!hasMetrics) {
                Text(
                    text = "Nincs friss adat",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    station.temperature?.let { temp ->
                        StationMetricChip(
                            icon = Icons.Rounded.Thermostat,
                            value = "${roundOneDecimal(temp)} °C"
                        )
                    }

                    station.relativeHumidity?.let { hum ->
                        StationMetricChip(
                            icon = Icons.Rounded.WaterDrop,
                            value = "${hum.roundToInt()} %"
                        )
                    }

                    station.pressure?.let { press ->
                        StationMetricChip(
                            icon = Icons.Rounded.Compress,
                            value = "${roundOneDecimal(press)} hPa"
                        )
                    }

                    station.windSpeed?.let { wind ->
                        StationMetricChip(
                            icon = Icons.Rounded.Air,
                            value = "${roundOneDecimal(wind)} km/h"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderTagBadge(providerType: StationProviderType) {
    val (text, containerColor, contentColor) = when (providerType) {
        StationProviderType.OFFICIAL_NOAA_SYNOP -> Triple(
            "Hivatalos (NOAA/SYNOP)",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        StationProviderType.AMATEUR_OPENSENSEMAP -> Triple(
            "Amatőr (OpenSenseMap)",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        StationProviderType.AMATEUR_NETATMO -> Triple(
            "Amatőr (Netatmo)",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun StationMetricChip(
    icon: ImageVector,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun formatDistance(distanceKm: Double): String {
    val rounded = (distanceKm * 10.0).roundToInt() / 10.0
    return "$rounded km-re"
}

private fun formatDataFreshness(timestampMs: Long?): String {
    if (timestampMs == null || timestampMs <= 0L) {
        return "Frissítve: ismeretlen"
    }
    val diffMs = System.currentTimeMillis() - timestampMs
    if (diffMs < 0) return "Frissítve: épp most"
    val minutes = diffMs / (1000 * 60)
    return when {
        minutes < 1 -> "Frissítve: épp most"
        minutes < 60 -> "Frissítve: $minutes perce"
        minutes < 1440 -> {
            val hours = minutes / 60
            "Frissítve: $hours órája"
        }
        else -> {
            val days = minutes / 1440
            "Frissítve: $days napja"
        }
    }
}

private fun roundOneDecimal(value: Double): String {
    val rounded = (value * 10.0).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) {
        rounded.toInt().toString()
    } else {
        rounded.toString()
    }
}

@Preview(showBackground = true)
@Composable
fun NearbyStationsSectionPreview() {
    val sampleStations = listOf(
        WeatherStation(
            id = "noaa_12345",
            name = "Budapest Pestszentlőrinc / OMSZ",
            providerType = StationProviderType.OFFICIAL_NOAA_SYNOP,
            latitude = 47.43,
            longitude = 19.18,
            distanceKm = 3.2,
            temperature = 18.2,
            relativeHumidity = 62.0,
            pressure = 1014.5,
            windSpeed = 14.5,
            timestampMs = System.currentTimeMillis() - (12 * 60 * 1000)
        ),
        WeatherStation(
            id = "sensebox_6789",
            name = "Újlipótváros senseBox",
            providerType = StationProviderType.AMATEUR_OPENSENSEMAP,
            latitude = 47.52,
            longitude = 19.05,
            distanceKm = 5.8,
            temperature = 18.8,
            relativeHumidity = 60.0,
            pressure = 1013.8,
            timestampMs = System.currentTimeMillis() - (5 * 60 * 1000)
        )
    )

    MacóMetTheme {
        NearbyStationsSection(
            stations = sampleStations,
            modifier = Modifier.padding(16.dp)
        )
    }
}
