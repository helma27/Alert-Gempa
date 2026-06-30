package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EarthquakeResponse(
    @Json(name = "Infogempa") val infoGempa: InfoGempa
)

@JsonClass(generateAdapter = true)
data class InfoGempa(
    @Json(name = "gempa") val gempa: Gempa
)

@JsonClass(generateAdapter = true)
data class EarthquakeListResponse(
    @Json(name = "Infogempa") val infoGempa: InfoGempaList
)

@JsonClass(generateAdapter = true)
data class InfoGempaList(
    @Json(name = "gempa") val gempa: List<Gempa>
)

@JsonClass(generateAdapter = true)
data class Gempa(
    @Json(name = "Tanggal") val tanggal: String,
    @Json(name = "Jam") val jam: String,
    @Json(name = "DateTime") val dateTime: String,
    @Json(name = "Coordinates") val coordinates: String,
    @Json(name = "Lintang") val lintang: String,
    @Json(name = "Bujur") val bujur: String,
    @Json(name = "Magnitude") val magnitude: String,
    @Json(name = "Kedalaman") val kedalaman: String,
    @Json(name = "Wilayah") val wilayah: String,
    @Json(name = "Potensi") val potensi: String,
    @Json(name = "Dirasakan") val dirasakan: String?,
    @Json(name = "Shakemap") val shakemap: String?
)
