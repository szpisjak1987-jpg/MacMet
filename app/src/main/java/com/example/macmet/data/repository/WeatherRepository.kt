package com.example.macmet.data.repository

import com.example.macmet.data.local.AppDatabase
import com.example.macmet.data.local.CachedWeatherEntity
import com.example.macmet.data.local.SavedLocationEntity
import com.example.macmet.data.model.AlertSeverity
import com.example.macmet.data.model.AlertType
import com.example.macmet.data.model.CompleteWeatherData
import com.example.macmet.data.model.CurrentAirQualityDto
import com.example.macmet.data.model.DailyForecastItem
import com.example.macmet.data.model.EuropeanAqiBand
import com.example.macmet.data.model.GeocodingLocation
import com.example.macmet.data.model.HourlyForecastItem
import com.example.macmet.data.model.OpenMeteoAirQualityResponse
import com.example.macmet.data.model.OpenMeteoForecastResponse
import com.example.macmet.data.model.RainViewerResponse
import com.example.macmet.data.model.SevereWeatherAlert
import com.example.macmet.data.remote.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class WeatherRepository(
    private val database: AppDatabase
) {
    private val forecastApi = NetworkClient.forecastApi
    private val airQualityApi = NetworkClient.airQualityApi
    private val geocodingApi = NetworkClient.geocodingApi
    private val rainViewerApi = NetworkClient.rainViewerApi
    private val dao = database.weatherDao()

    val savedLocations: Flow<List<SavedLocationEntity>> = dao.getAllSavedLocations()

    suspend fun getCompleteWeather(
        lat: Double,
        lon: Double,
        locationName: String,
        countryName: String? = null
    ): Result<CompleteWeatherData> = withContext(Dispatchers.IO) {
        val cacheKey = "${String.format(Locale.US, "%.2f", lat)}_${String.format(Locale.US, "%.2f", lon)}"
        try {
            // Concurrently fetch forecast & air quality
            val (forecastResp, airQualityResp) = coroutineScope {
                val forecastDeferred = async {
                    forecastApi.getForecast(latitude = lat, longitude = lon)
                }
                val airQualityDeferred = async {
                    try {
                        airQualityApi.getAirQuality(latitude = lat, longitude = lon)
                    } catch (e: Exception) {
                        null
                    }
                }
                Pair(forecastDeferred.await(), airQualityDeferred.await())
            }

            val completeData = mapToCompleteData(
                locationName = locationName,
                country = countryName ?: "Közép-Európa",
                forecast = forecastResp,
                airQuality = airQualityResp?.current
            )

            // Cache successfully downloaded data
            try {
                val json = NetworkClient.moshi.adapter(OpenMeteoForecastResponse::class.java).toJson(forecastResp)
                dao.saveCachedWeather(
                    CachedWeatherEntity(
                        locationKey = cacheKey,
                        locationName = locationName,
                        weatherJson = json
                    )
                )
            } catch (_: Exception) {}

            Result.success(completeData)
        } catch (e: Exception) {
            // Check Room cache as fallback
            val cached = dao.getCachedWeather(cacheKey)
            if (cached != null) {
                try {
                    val adapter = NetworkClient.moshi.adapter(OpenMeteoForecastResponse::class.java)
                    val cachedForecast = adapter.fromJson(cached.weatherJson)
                    if (cachedForecast != null) {
                        val fallback = mapToCompleteData(
                            locationName = cached.locationName,
                            country = countryName ?: "Offline mentés",
                            forecast = cachedForecast,
                            airQuality = null
                        )
                        return@withContext Result.success(fallback)
                    }
                } catch (_: Exception) {}
            }
            Result.failure(e)
        }
    }

    suspend fun searchLocations(query: String): Result<List<GeocodingLocation>> = withContext(Dispatchers.IO) {
        try {
            val response = geocodingApi.searchCity(name = query)
            Result.success(response.results ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRadarData(): Result<RainViewerResponse> = withContext(Dispatchers.IO) {
        try {
            val resp = rainViewerApi.getRadarMaps()
            Result.success(resp)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveLocation(location: SavedLocationEntity) = withContext(Dispatchers.IO) {
        dao.insertLocation(location)
    }

    suspend fun removeLocation(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteLocationById(id)
    }

    suspend fun isLocationSaved(lat: Double, lon: Double): Boolean = withContext(Dispatchers.IO) {
        dao.findLocation(lat, lon) != null
    }

    private fun mapToCompleteData(
        locationName: String,
        country: String,
        forecast: OpenMeteoForecastResponse,
        airQuality: CurrentAirQualityDto?
    ): CompleteWeatherData {
        val current = forecast.current ?: throw IllegalStateException("Hiányzó jelenlegi időjárás adat")
        val hourly = forecast.hourly
        val daily = forecast.daily

        // Format hourly (first 36-48 hours)
        val hourlyItems = mutableListOf<HourlyForecastItem>()
        if (hourly != null && hourly.time.isNotEmpty()) {
            val count = minOf(36, hourly.time.size)
            val isoParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
            val hourFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

            for (i in 0 until count) {
                val timeStr = hourly.time.getOrNull(i) ?: ""
                val hourLabel = try {
                    val date = isoParser.parse(timeStr)
                    if (i == 0) "Most" else (if (date != null) hourFormatter.format(date) else timeStr)
                } catch (e: Exception) {
                    timeStr
                }

                // Check isNight for each hour:
                val hourOfDay = try {
                    val cal = Calendar.getInstance()
                    val date = isoParser.parse(timeStr)
                    if (date != null) {
                        cal.time = date
                        cal.get(Calendar.HOUR_OF_DAY)
                    } else 12
                } catch (e: Exception) {
                    12
                }
                val isHourNight = hourOfDay < 6 || hourOfDay >= 20

                hourlyItems.add(
                    HourlyForecastItem(
                        timeLabel = hourLabel,
                        temperature = hourly.temperature.getOrNull(i) ?: 0.0,
                        apparentTemperature = hourly.apparentTemperature?.getOrNull(i) ?: hourly.temperature.getOrNull(i) ?: 0.0,
                        precipitationProbability = hourly.precipitationProbability?.getOrNull(i) ?: 0,
                        precipitationMm = hourly.precipitation?.getOrNull(i) ?: 0.0,
                        weatherCode = hourly.weatherCode.getOrNull(i) ?: 0,
                        isNight = isHourNight,
                        windSpeed = hourly.windSpeed?.getOrNull(i) ?: 0.0,
                        uvIndex = hourly.uvIndex?.getOrNull(i) ?: 0.0,
                        cape = hourly.cape?.getOrNull(i) ?: 0.0
                    )
                )
            }
        }

        // Format daily
        val dailyItems = mutableListOf<DailyForecastItem>()
        var weekMin = current.temperature
        var weekMax = current.temperature

        if (daily != null && daily.time.isNotEmpty()) {
            val dateParser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val dayFormatter = SimpleDateFormat("EE", Locale("hu", "HU"))
            val labelFormatter = SimpleDateFormat("MM.dd", Locale.getDefault())

            for (i in daily.time.indices) {
                val dateStr = daily.time[i]
                val (dayName, dateLabel) = try {
                    val date = dateParser.parse(dateStr)
                    if (date != null) {
                        val cal = Calendar.getInstance()
                        val today = Calendar.getInstance()
                        cal.time = date
                        val isToday = cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) &&
                                cal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                        val name = if (isToday) "Ma" else dayFormatter.format(date).replaceFirstChar { it.uppercase() }
                        Pair(name, labelFormatter.format(date))
                    } else Pair(dateStr, dateStr)
                } catch (e: Exception) {
                    Pair(dateStr, dateStr)
                }

                val minT = daily.temperatureMin.getOrNull(i) ?: 0.0
                val maxT = daily.temperatureMax.getOrNull(i) ?: 0.0
                if (i == 0) {
                    weekMin = minT
                    weekMax = maxT
                } else {
                    weekMin = minOf(weekMin, minT)
                    weekMax = maxOf(weekMax, maxT)
                }

                val sunriseTime = daily.sunrise?.getOrNull(i)?.takeLast(5) ?: "06:00"
                val sunsetTime = daily.sunset?.getOrNull(i)?.takeLast(5) ?: "19:00"

                dailyItems.add(
                    DailyForecastItem(
                        dateLabel = dateLabel,
                        dayOfWeekHu = dayName,
                        weatherCode = daily.weatherCode.getOrNull(i) ?: 0,
                        minTemp = minT,
                        maxTemp = maxT,
                        precipitationProbability = daily.precipitationProbabilityMax?.getOrNull(i) ?: 0,
                        precipitationSumMm = daily.precipitationSum?.getOrNull(i) ?: 0.0,
                        uvIndexMax = daily.uvIndexMax?.getOrNull(i) ?: 0.0,
                        windSpeedMax = daily.windSpeedMax?.getOrNull(i) ?: 0.0,
                        evapotranspirationMm = daily.evapotranspiration?.getOrNull(i),
                        sunrise = sunriseTime,
                        sunset = sunsetTime
                    )
                )
            }
        }

        val aqiBand = EuropeanAqiBand.fromAqi(airQuality?.europeanAqi)

        // Generate MeteoAlarm / CAP style severe warnings based on meteorology parameters
        val activeAlerts = mutableListOf<SevereWeatherAlert>()
        val maxCape = hourlyItems.maxOfOrNull { it.cape } ?: 0.0
        val maxGust = current.windGusts ?: current.windSpeed
        val maxRainProb = hourlyItems.maxOfOrNull { it.precipitationProbability } ?: 0

        if (maxCape > 1200 || current.weatherCode in listOf(95, 96, 99)) {
            activeAlerts.add(
                SevereWeatherAlert(
                    id = "cape_storm_1",
                    title = "Zivatar és felhőszakadás riasztás",
                    severity = if (maxCape > 2000 || current.weatherCode in listOf(96, 99)) AlertSeverity.EXTREME else AlertSeverity.SEVERE,
                    type = AlertType.THUNDERSTORM,
                    description = "A légkörben magas konvektív instabilitási energia (CAPE: ${maxCape.roundToInt()} J/kg) mérhető. Hirtelen lezúduló csapadék és jégeső kialakulhat.",
                    advice = "Vihar esetén keressen biztonságos menedéket, kerülje a magányos fákat és a nyílt vízpartokat.",
                    validUntil = "Következő 12 órában"
                )
            )
        }

        if (maxGust > 70) {
            activeAlerts.add(
                SevereWeatherAlert(
                    id = "wind_gust_1",
                    title = "Viharos széllökés figyelmeztetés",
                    severity = if (maxGust > 90) AlertSeverity.EXTREME else AlertSeverity.MODERATE,
                    type = AlertType.WIND,
                    description = "Erős, viharos széllökések várhatók (${maxGust.roundToInt()} km/h), amelyek faágakat törhetnek le.",
                    advice = "Rögzítse a laza kerti tárgyakat, vezessen óvatosan nyílt szakaszokon.",
                    validUntil = "Ma estig"
                )
            )
        }

        if (current.temperature > 34) {
            activeAlerts.add(
                SevereWeatherAlert(
                    id = "heat_wave_1",
                    title = "Hőségriasztás",
                    severity = if (current.temperature > 37) AlertSeverity.EXTREME else AlertSeverity.SEVERE,
                    type = AlertType.EXTREME_HEAT,
                    description = "Tartós kánikula magas hőmérséklettel (${current.temperature.roundToInt()} °C).",
                    advice = "Fogyasszon bőséges folyadékot, 11 és 15 óra között kerülje a közvetlen napsugárzást.",
                    validUntil = "Holnap 20:00-ig"
                )
            )
        } else if (current.temperature < -10) {
            activeAlerts.add(
                SevereWeatherAlert(
                    id = "frost_1",
                    title = "Extrém fagy figyelmeztetés",
                    severity = AlertSeverity.SEVERE,
                    type = AlertType.FROST,
                    description = "Erős éjszakai lehűlés várható (${current.temperature.roundToInt()} °C). Fagyási sérülések veszélye.",
                    advice = "Öltözzön rétegesen, óvja a kisállatokat és a fagyérzékeny vízhálózatot.",
                    validUntil = "Kedd reggelig"
                )
            )
        }

        val bestModelName = when {
            forecast.latitude in 45.0..49.0 && forecast.longitude in 15.0..23.0 -> "DWD ICON-D2 (1.2 km felbontás)"
            forecast.latitude in 40.0..55.0 && forecast.longitude in -5.0..30.0 -> "ECMWF IFS / AROME (Közép-Európa)"
            else -> "Open-Meteo Best-Match (Automatikus mezoskálájú)"
        }

        val lastUpdatedFormatted = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        return CompleteWeatherData(
            locationName = locationName,
            country = country,
            latitude = forecast.latitude,
            longitude = forecast.longitude,
            elevation = forecast.elevation,
            bestMatchModel = bestModelName,
            current = current,
            hourlyItems = hourlyItems,
            dailyItems = dailyItems,
            airQuality = airQuality,
            aqiBand = aqiBand,
            activeAlerts = activeAlerts,
            weekMinTemp = weekMin,
            weekMaxTemp = weekMax,
            lastUpdated = lastUpdatedFormatted
        )
    }
}
