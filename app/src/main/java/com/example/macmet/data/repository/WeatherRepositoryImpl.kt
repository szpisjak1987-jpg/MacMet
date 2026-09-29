package com.example.macmet.data.repository

import android.util.Log
import com.example.macmet.data.api.AviationWeatherApiService
import com.example.macmet.data.api.OpenMeteoApiService
import com.example.macmet.data.api.OpenMeteoGeocodingApiService
import com.example.macmet.data.api.OpenSenseMapApiService
import com.example.macmet.data.api.RetrofitClient
import com.example.macmet.data.api.netatmo.NetatmoApiService
import com.example.macmet.data.estimation.JelenidoEstimator
import com.example.macmet.data.estimation.StationObservation
import com.example.macmet.data.location.LocationData
import com.example.macmet.data.location.LocationNameResolver
import com.example.macmet.data.location.LocationNameResolverImpl
import com.example.macmet.data.location.LocationTracker
import com.example.macmet.data.model.api.DailyWeatherDto
import com.example.macmet.data.model.api.HourlyWeatherDto
import com.example.macmet.data.model.domain.CitySearchResult
import com.example.macmet.data.model.domain.DailyForecast
import com.example.macmet.data.model.domain.HourlyForecast
import com.example.macmet.data.model.domain.StationProviderType
import com.example.macmet.data.model.domain.WeatherCondition
import com.example.macmet.data.model.domain.WeatherForecast7Days
import com.example.macmet.data.model.domain.WeatherStation
import com.example.macmet.data.util.Haversine
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import retrofit2.HttpException
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt

class WeatherRepositoryImpl(
    private val apiService: OpenMeteoApiService = RetrofitClient.apiService,
    private val openSenseMapApiService: OpenSenseMapApiService? = null,
    private val aviationWeatherApiService: AviationWeatherApiService? = null,
    private val netatmoApiService: NetatmoApiService? = null,
    private val openMeteoGeocodingApiService: OpenMeteoGeocodingApiService? = null,
    private val locationTracker: LocationTracker? = null,
    private val locationNameResolver: LocationNameResolver? = LocationNameResolverImpl()
) : WeatherRepository {


    override suspend fun get7DayForecast(
        location: LocationData?,
        stationObservation: StationObservation?
    ): Result<WeatherForecast7Days> {
        val targetLocation = location ?: locationTracker?.getCurrentLocation() ?: LocationData.BUDAPEST_FALLBACK
        return get7DayForecastForCoordinates(
            latitude = targetLocation.latitude,
            longitude = targetLocation.longitude,
            locationName = targetLocation.cityName,
            stationObservation = stationObservation
        )
    }

    override suspend fun get7DayForecastForCoordinates(
        latitude: Double,
        longitude: Double,
        locationName: String,
        stationObservation: StationObservation?
    ): Result<WeatherForecast7Days> = coroutineScope {
        try {
            val resolvedLocationName = if (locationName == "Jelenlegi helyszín" || locationName.isBlank()) {
                locationNameResolver?.resolveCityName(latitude, longitude) ?: "Budapest"
            } else {
                locationName
            }

            val openMeteoDeferred = async { apiService.getForecast(latitude, longitude) }
            val openSenseMapDeferred = async {
                try {
                    fetchOpenSenseMapStations(latitude, longitude)
                } catch (e: Exception) {
                    Log.e("WeatherRepository", "OpenSenseMap station fetch error handled safely", e)
                    emptyList()
                }
            }
            val netatmoDeferred = async {
                try {
                    fetchNetatmoStations(latitude, longitude)
                } catch (e: Exception) {
                    Log.e("WeatherRepository", "Netatmo station fetch error handled safely", e)
                    emptyList()
                }
            }
            val noaaDeferred = async {
                try {
                    fetchNoaaStations(latitude, longitude)
                } catch (e: Exception) {
                    Log.e("WeatherRepository", "NOAA station fetch error handled safely", e)
                    emptyList()
                }
            }
            val response = openMeteoDeferred.await()
            val openSenseMapStations = openSenseMapDeferred.await()
            val netatmoStations = netatmoDeferred.await()
            val noaaStations = noaaDeferred.await()

            val rawStations = (openSenseMapStations + netatmoStations + noaaStations)
                .filter { it.distanceKm <= 20.0 }
                .sortedBy { it.distanceKm }

            val allStations = rawStations

            val currentDto = response.current
                ?: return@coroutineScope Result.failure(IllegalStateException("API response missing 'current' weather data"))

            val currentWeather = JelenidoEstimator.estimate(
                currentDto = currentDto,
                locationName = resolvedLocationName,
                stations = allStations,
                stationObservation = stationObservation
            )

            val dailyForecasts = mapDailyForecasts(response.daily)
            val hourlyForecasts = mapHourlyForecasts(response.hourly)

            Result.success(
                WeatherForecast7Days(
                    currentWeather = currentWeather,
                    dailyForecasts = dailyForecasts,
                    hourlyForecasts = hourlyForecasts,
                    latitude = response.latitude,
                    longitude = response.longitude,
                    locationName = resolvedLocationName
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchCities(query: String): Result<List<CitySearchResult>> {
        return try {
            val service = openMeteoGeocodingApiService ?: RetrofitClient.openMeteoGeocodingApiService
            val response = service.searchLocations(name = query)
            if (response.isSuccessful) {
                val body = response.body()
                val resultsDto = body?.results ?: emptyList()
                val domainResults = resultsDto.mapNotNull { dto ->
                    val id = dto.id ?: return@mapNotNull null
                    val name = dto.name ?: return@mapNotNull null
                    val lat = dto.latitude ?: return@mapNotNull null
                    val lon = dto.longitude ?: return@mapNotNull null
                    CitySearchResult(
                        id = id,
                        name = name,
                        country = dto.country,
                        admin1 = dto.admin1,
                        latitude = lat,
                        longitude = lon,
                        countryCode = dto.countryCode
                    )
                }
                Result.success(domainResults)
            } else {
                Result.failure(Exception("City search failed with code ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    internal suspend fun fetchOpenSenseMapStations(lat: Double, lon: Double): List<WeatherStation> {
        val service = openSenseMapApiService ?: return emptyList()
        return try {
            val nearParam = String.format(Locale.US, "%.4f,%.4f", lon, lat)
            val boxes = service.getBoxes(near = nearParam, radius = 20000)
            boxes.mapNotNull { box ->
                val coords = box.currentLocation?.coordinates
                if ((coords == null) || (coords.size < 2)) return@mapNotNull null
                val boxLon = coords[0]
                val boxLat = coords[1]
                val dist = Haversine.calculateDistanceKm(lat, lon, boxLat, boxLon)

                var temp: Double? = null
                var hum: Double? = null
                var press: Double? = null
                var wind: Double? = null
                val timestampMs: Long? = null

                box.sensors?.forEach { sensor ->
                    val title = sensor.title?.lowercase(Locale.US) ?: ""
                    val unit = sensor.unit?.lowercase(Locale.US) ?: ""
                    val valStr = sensor.lastMeasurement?.value
                    val valNum = valStr?.toDoubleOrNull() ?: return@forEach

                    when {
                        title.contains("temp") || title.contains("hőmérséklet") || unit == "°c" || unit == "c" -> {
                            temp = valNum
                        }
                        title.contains("hum") || title.contains("feucht") || title.contains("pára") || unit == "%" -> {
                            hum = valNum
                        }
                        title.contains("press") || title.contains("druck") || title.contains("nyomás") || unit == "hpa" || unit == "mb" -> {
                            press = valNum
                        }
                        unit == "pa" -> {
                            press = valNum / 100.0
                        }
                        title.contains("wind") || title.contains("szél") || unit == "km/h" -> {
                            wind = valNum
                        }
                        unit == "m/s" -> {
                            wind = valNum * 3.6
                        }
                    }
                }

                WeatherStation(
                    id = box.id,
                    name = box.name ?: "OpenSenseMap Station",
                    providerType = StationProviderType.AMATEUR_OPENSENSEMAP,
                    latitude = boxLat,
                    longitude = boxLon,
                    distanceKm = dist,
                    temperature = temp,
                    relativeHumidity = hum,
                    pressure = press,
                    windSpeed = wind,
                    timestampMs = timestampMs
                )
            }
        } catch (e: Exception) {
            Log.e("WeatherRepository", "OpenSenseMap fetch error", e)
            emptyList()
        }
    }

    internal suspend fun fetchNetatmoStations(lat: Double, lon: Double): List<WeatherStation> {
        val service = netatmoApiService ?: return emptyList()
        return try {
            val minLat = lat - 0.45
            val maxLat = lat + 0.45
            val minLon = lon - 0.45
            val maxLon = lon + 0.45
            
            val response = service.getPublicData(
                latNe = maxLat,
                lonNe = maxLon,
                latSw = minLat,
                lonSw = minLon
            )

            if (!response.isSuccessful) {
                Log.e("WeatherRepository", "Netatmo fetch error: ${response.code()}")
                return emptyList()
            }

            val body = response.body()?.body ?: return emptyList()

            body.mapNotNull { station ->
                val id = station.id ?: return@mapNotNull null
                val place = station.place ?: return@mapNotNull null
                val coords = place.location ?: return@mapNotNull null
                if (coords.size < 2) return@mapNotNull null
                val sLon = coords[0]
                val sLat = coords[1]
                val dist = Haversine.calculateDistanceKm(lat, lon, sLat, sLon)

                val measures = station.measures
                var temp: Double? = null
                var hum: Double? = null
                var pressure: Double? = null
                var windSpeed: Double? = null
                var windDir: Double? = null

                if (measures != null) {
                    for ((_, measure) in measures) {
                        val types = measure.type
                        val resMap = measure.res
                        if (types != null && resMap != null && resMap.isNotEmpty()) {
                            // res is a map of timestamp -> List<Double>
                            val latestValues = resMap.entries.maxByOrNull { it.key.toLongOrNull() ?: 0L }?.value
                            if (latestValues != null) {
                                types.forEachIndexed { index, type ->
                                    val value = latestValues.getOrNull(index)
                                    if (value != null) {
                                        when (type.lowercase(Locale.US)) {
                                            "temperature" -> temp = value
                                            "humidity" -> hum = value
                                            "pressure" -> pressure = value
                                            "windstrength" -> windSpeed = value
                                            "windangle" -> windDir = value
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                val city = place.city
                val street = place.street
                
                val stationName = buildString {
                    if (!city.isNullOrBlank()) append(city)
                    if (!city.isNullOrBlank() && !street.isNullOrBlank()) append(", ")
                    if (!street.isNullOrBlank()) append(street)
                    if (isEmpty()) append("Netatmo Station")
                }

                WeatherStation(
                    id = id,
                    name = stationName,
                    providerType = StationProviderType.AMATEUR_NETATMO,
                    latitude = sLat,
                    longitude = sLon,
                    distanceKm = dist,
                    temperature = temp,
                    relativeHumidity = hum,
                    pressure = pressure,
                    windSpeed = windSpeed,
                    windDirection = windDir,
                    timestampMs = System.currentTimeMillis() // Approximate
                )
            }
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Netatmo fetch error", e)
            emptyList()
        }
    }

    internal suspend fun fetchNoaaStations(lat: Double, lon: Double): List<WeatherStation> {
        val service = aviationWeatherApiService ?: return emptyList()
        return try {
            val minLat = lat - 0.45
            val maxLat = lat + 0.45
            val minLon = lon - 0.45
            val maxLon = lon + 0.45
            val bboxStr = String.format(Locale.US, "%.2f,%.2f,%.2f,%.2f", minLat, minLon, maxLat, maxLon)

            val bboxMetars = try {
                service.getMetar(bbox = bboxStr).body() ?: emptyList()
            } catch (e: Exception) {
                Log.e("WeatherRepository", "NOAA fetch error", e)
                emptyList()
            }

            val idsMetars = try {
                service.getMetar(ids = AviationWeatherApiService.DEFAULT_HUNGARIAN_ICAO_IDS).body() ?: emptyList()
            } catch (e: Exception) {
                Log.e("WeatherRepository", "NOAA fetch error", e)
                emptyList()
            }

            val combinedMetars = (bboxMetars + idsMetars).distinctBy { it.icaoId ?: it.name ?: "" }

            combinedMetars.mapNotNull { metar ->
                val mLat = metar.lat ?: return@mapNotNull null
                val mLon = metar.lon ?: return@mapNotNull null
                val dist = Haversine.calculateDistanceKm(lat, lon, mLat, mLon)

                val timestampMs = metar.obsTime?.let {
                    if (it > 10_000_000_000L) it else it * 1000L
                }

                val stationName = buildString {
                    if (!metar.icaoId.isNullOrBlank()) append(metar.icaoId)
                    if (!metar.icaoId.isNullOrBlank() && !metar.name.isNullOrBlank()) append(" / ")
                    if (!metar.name.isNullOrBlank()) append(metar.name)
                    if (isEmpty()) append("NOAA Station")
                }

                WeatherStation(
                    id = metar.icaoId ?: "METAR",
                    name = stationName,
                    providerType = StationProviderType.OFFICIAL_NOAA_SYNOP,
                    latitude = mLat,
                    longitude = mLon,
                    distanceKm = dist,
                    temperature = metar.temp,
                    dewPoint = metar.dewp,
                    pressure = metar.slp ?: metar.altim,
                    windSpeed = metar.wspd?.let { it * 1.852 },
                    windDirection = metar.wdir,
                    timestampMs = timestampMs
                )
            }
        } catch (e: Exception) {
            Log.e("WeatherRepository", "NOAA fetch error", e)
            emptyList()
        }
    }

    private fun mapDailyForecasts(dailyDto: DailyWeatherDto?): List<DailyForecast> {
        if (dailyDto == null) return emptyList()

        val times = dailyDto.time
        val result = mutableListOf<DailyForecast>()

        for (i in times.indices) {
            val dateStr = times[i]
            val maxTemp = dailyDto.temperature2mMax.getOrNull(i) ?: 0.0
            val minTemp = dailyDto.temperature2mMin.getOrNull(i) ?: 0.0
            val weatherCode = dailyDto.weatherCode.getOrNull(i) ?: 0
            val precipSum = dailyDto.precipitationSum?.getOrNull(i) ?: 0.0
            val precipProbMax = dailyDto.precipitationProbabilityMax?.getOrNull(i)?.roundToInt() ?: 0

            result.add(
                DailyForecast(
                    date = dateStr,
                    dayOfWeek = getHungarianDayOfWeek(dateStr),
                    tempMax = (maxTemp * 10.0).roundToInt() / 10.0,
                    tempMin = (minTemp * 10.0).roundToInt() / 10.0,
                    precipitationSum = (precipSum * 10.0).roundToInt() / 10.0,
                    precipitationProbabilityMax = precipProbMax,
                    weatherCode = weatherCode,
                    weatherCondition = WeatherCondition.fromWmoCode(weatherCode)
                )
            )
        }
        return result
    }

    private fun mapHourlyForecasts(hourlyDto: HourlyWeatherDto?): List<HourlyForecast> {
        if (hourlyDto == null) return emptyList()

        val times = hourlyDto.time
        val result = mutableListOf<HourlyForecast>()

        for (i in times.indices) {
            val rawTime = times[i]
            val temp = hourlyDto.temperature2m.getOrNull(i) ?: 0.0
            val humidity = hourlyDto.relativeHumidity2m?.getOrNull(i)?.roundToInt() ?: 0
            val precip = hourlyDto.precipitation?.getOrNull(i) ?: 0.0
            val precipProb = hourlyDto.precipitationProbability?.getOrNull(i)?.roundToInt() ?: 0
            val weatherCode = hourlyDto.weatherCode?.getOrNull(i) ?: 0
            val windSpeed = hourlyDto.windSpeed10m?.getOrNull(i) ?: 0.0

            result.add(
                HourlyForecast(
                    time = rawTime,
                    formattedHour = formatHour(rawTime),
                    temperature = (temp * 10.0).roundToInt() / 10.0,
                    relativeHumidity = humidity,
                    precipitation = (precip * 10.0).roundToInt() / 10.0,
                    precipitationProbability = precipProb,
                    weatherCode = weatherCode,
                    weatherCondition = WeatherCondition.fromWmoCode(weatherCode),
                    windSpeed = (windSpeed * 10.0).roundToInt() / 10.0
                )
            )
        }
        return result
    }

    private fun getHungarianDayOfWeek(dateString: String): String {
        return try {
            val date = LocalDate.parse(dateString.take(10))
            when (date.dayOfWeek) {
                DayOfWeek.MONDAY -> "Hétfő"
                DayOfWeek.TUESDAY -> "Kedd"
                DayOfWeek.WEDNESDAY -> "Szerda"
                DayOfWeek.THURSDAY -> "Csütörtök"
                DayOfWeek.FRIDAY -> "Péntek"
                DayOfWeek.SATURDAY -> "Szombat"
                DayOfWeek.SUNDAY -> "Vasárnap"
                else -> dateString
            }
        } catch (_: Exception) {
            dateString
        }
    }

    private fun formatHour(timeString: String): String {
        return try {
            if (timeString.contains("T")) {
                timeString.substringAfter("T").take(5)
            } else {
                timeString
            }
        } catch (_: Exception) {
            timeString
        }
    }
}
