package com.example.macmet.data.model.api

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import java.time.Instant

data class MetarDto(
    val icaoId: String? = null,
    val name: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val temp: Double? = null,
    val dewp: Double? = null,
    val wspd: Double? = null,
    val wgst: Double? = null,
    val wdir: Double? = null,
    val pres: Double? = null,
    val visib: String? = null,
    val altim: Double? = null,
    val slp: Double? = null,
    val elev: Double? = null,
    val reportTime: String? = null,
    val obsTime: Long? = null,
)

class MetarDtoAdapter : JsonAdapter<MetarDto>() {

    @FromJson
    override fun fromJson(reader: JsonReader): MetarDto {
        var icaoId: String? = null
        var name: String? = null
        var lat: Double? = null
        var lon: Double? = null
        var temp: Double? = null
        var dewp: Double? = null
        var wspd: Double? = null
        var wgst: Double? = null
        var wdir: Double? = null
        var pres: Double? = null
        var visib: String? = null
        var altim: Double? = null
        var slp: Double? = null
        var elev: Double? = null
        var reportTime: String? = null
        var obsTime: Long? = null

        if (reader.peek() == JsonReader.Token.NULL) {
            reader.nextNull<Any>()
            return MetarDto()
        }

        reader.beginObject()
        while (reader.hasNext()) {
            val fieldName = reader.nextName()
            if (reader.peek() == JsonReader.Token.NULL) {
                reader.nextNull<Any>()
                continue
            }
            when (fieldName) {
                "icaoId", "icao", "id" -> icaoId = readStringSafely(reader)
                "name" -> name = readStringSafely(reader)
                "lat" -> lat = readDoubleSafely(reader)
                "lon" -> lon = readDoubleSafely(reader)
                "temp" -> temp = readDoubleSafely(reader)
                "dewp" -> dewp = readDoubleSafely(reader)
                "wspd" -> wspd = readDoubleSafely(reader)
                "wgst" -> wgst = readDoubleSafely(reader)
                "wdir" -> wdir = readDoubleSafely(reader)
                "pres" -> pres = readDoubleSafely(reader)
                "visib" -> visib = readStringSafely(reader)
                "altim" -> altim = readDoubleSafely(reader)
                "slp" -> slp = readDoubleSafely(reader)
                "elev" -> elev = readDoubleSafely(reader)
                "reportTime" -> reportTime = readStringSafely(reader)
                "obsTime" -> obsTime = readLongSafely(reader)
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return MetarDto(
            icaoId = icaoId,
            name = name,
            lat = lat,
            lon = lon,
            temp = temp,
            dewp = dewp,
            wspd = wspd,
            wgst = wgst,
            wdir = wdir,
            pres = pres,
            visib = visib,
            altim = altim,
            slp = slp,
            elev = elev,
            reportTime = reportTime,
            obsTime = obsTime,
        )
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: MetarDto?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.beginObject()
        value.icaoId?.let { writer.name("icaoId").value(it) }
        value.name?.let { writer.name("name").value(it) }
        value.lat?.let { writer.name("lat").value(it) }
        value.lon?.let { writer.name("lon").value(it) }
        value.temp?.let { writer.name("temp").value(it) }
        value.dewp?.let { writer.name("dewp").value(it) }
        value.wspd?.let { writer.name("wspd").value(it) }
        value.wgst?.let { writer.name("wgst").value(it) }
        value.wdir?.let { writer.name("wdir").value(it) }
        value.pres?.let { writer.name("pres").value(it) }
        value.visib?.let { writer.name("visib").value(it) }
        value.altim?.let { writer.name("altim").value(it) }
        value.slp?.let { writer.name("slp").value(it) }
        value.elev?.let { writer.name("elev").value(it) }
        value.reportTime?.let { writer.name("reportTime").value(it) }
        value.obsTime?.let { writer.name("obsTime").value(it) }
        writer.endObject()
    }

    companion object {
        fun readDoubleSafely(reader: JsonReader): Double? {
            return try {
                when (reader.peek()) {
                    JsonReader.Token.NUMBER -> reader.nextDouble()
                    JsonReader.Token.STRING -> {
                        val str = reader.nextString().trim()
                        if (str.isEmpty()) return null
                        val isNegativeMetar = str.startsWith("M", ignoreCase = true) && str.length > 1 && (str[1].isDigit() || str[1] == '.')
                        val normalized = if (isNegativeMetar) {
                            "-" + str.substring(1)
                        } else {
                            str
                        }
                        normalized.toDoubleOrNull()
                    }
                    JsonReader.Token.NULL -> {
                        reader.nextNull<Any>()
                        null
                    }
                    else -> {
                        reader.skipValue()
                        null
                    }
                }
            } catch (_: Exception) {
                try { reader.skipValue() } catch (_: Exception) {}
                null
            }
        }

        fun readLongSafely(reader: JsonReader): Long? {
            return try {
                when (reader.peek()) {
                    JsonReader.Token.NUMBER -> {
                        try {
                            reader.nextLong()
                        } catch (_: Exception) {
                            reader.nextDouble().toLong()
                        }
                    }
                    JsonReader.Token.STRING -> {
                        val str = reader.nextString().trim()
                        if (str.isEmpty()) return null
                        str.toLongOrNull() ?: str.toDoubleOrNull()?.toLong() ?: try {
                            val isoStr = if (!str.contains("T") && str.contains(" ")) str.replace(" ", "T") else str
                            val normalizedIso = if (!isoStr.endsWith("Z") && !isoStr.contains("+") && !isoStr.contains("-", ignoreCase = false)) "${isoStr}Z" else isoStr
                            Instant.parse(normalizedIso).epochSecond
                        } catch (_: Exception) {
                            null
                        }
                    }
                    JsonReader.Token.NULL -> {
                        reader.nextNull<Any>()
                        null
                    }
                    else -> {
                        reader.skipValue()
                        null
                    }
                }
            } catch (_: Exception) {
                try { reader.skipValue() } catch (_: Exception) {}
                null
            }
        }

        fun readStringSafely(reader: JsonReader): String? {
            return try {
                when (reader.peek()) {
                    JsonReader.Token.STRING -> reader.nextString()
                    JsonReader.Token.NUMBER -> {
                        try {
                            reader.nextString()
                        } catch (_: Exception) {
                            val d = reader.nextDouble()
                            if (d == d.toLong().toDouble()) d.toLong().toString() else d.toString()
                        }
                    }
                    JsonReader.Token.BOOLEAN -> reader.nextBoolean().toString()
                    JsonReader.Token.NULL -> {
                        reader.nextNull<Any>()
                        null
                    }
                    else -> {
                        reader.skipValue()
                        null
                    }
                }
            } catch (_: Exception) {
                try { reader.skipValue() } catch (_: Exception) {}
                null
            }
        }
    }
}

object FlexibleDoubleAdapter {
    @FromJson
    fun fromJson(reader: JsonReader): Double? = MetarDtoAdapter.readDoubleSafely(reader)

    @ToJson
    fun toJson(writer: JsonWriter, value: Double?) {
        if (value == null) writer.nullValue() else writer.value(value)
    }
}

object FlexibleLongAdapter {
    @FromJson
    fun fromJson(reader: JsonReader): Long? = MetarDtoAdapter.readLongSafely(reader)

    @ToJson
    fun toJson(writer: JsonWriter, value: Long?) {
        if (value == null) writer.nullValue() else writer.value(value)
    }
}

object FlexibleStringAdapter {
    @FromJson
    fun fromJson(reader: JsonReader): String? = MetarDtoAdapter.readStringSafely(reader)

    @ToJson
    fun toJson(writer: JsonWriter, value: String?) {
        if (value == null) writer.nullValue() else writer.value(value)
    }
}
