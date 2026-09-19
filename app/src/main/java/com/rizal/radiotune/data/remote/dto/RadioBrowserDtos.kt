package com.rizal.radiotune.data.remote.dto

import com.rizal.radiotune.data.model.Country
import com.rizal.radiotune.data.model.Station
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CountryDto(
    @SerialName("name") val name: String = "",
    @SerialName("iso_3166_1") val code: String = "",
    @SerialName("stationcount") val stationCount: Int = 0,
)

@Serializable
data class StationDto(
    @SerialName("stationuuid") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("url") val url: String = "",
    @SerialName("url_resolved") val resolvedUrl: String = "",
    @SerialName("homepage") val homepage: String = "",
    @SerialName("favicon") val favicon: String = "",
    @SerialName("tags") val tags: String = "",
    @SerialName("country") val country: String = "",
    @SerialName("countrycode") val countryCode: String = "",
    @SerialName("state") val state: String = "",
    @SerialName("language") val language: String = "",
    @SerialName("votes") val votes: Int = 0,
    @SerialName("codec") val codec: String = "",
    @SerialName("bitrate") val bitrate: Int = 0,
    @SerialName("lastcheckok") val lastCheckOk: Int = 1,
)

@Serializable
data class StationUrlDto(
    @SerialName("ok") val ok: Boolean = false,
    @SerialName("message") val message: String = "",
    @SerialName("stationuuid") val stationUuid: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("url") val url: String = "",
)

fun CountryDto.toCountry(): Country = Country(
    name = name.trim(),
    code = code.trim().uppercase(),
    stationCount = stationCount,
)

fun StationDto.toStation(): Station = Station(
    id = id,
    name = name.trim().ifBlank { "Unknown station" },
    streamUrl = url.trim(),
    resolvedUrl = resolvedUrl.trim(),
    homepage = homepage.trim(),
    faviconUrl = favicon.trim(),
    tags = tags.split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .take(8),
    country = country.trim(),
    countryCode = countryCode.trim().uppercase(),
    state = state.trim(),
    language = language.trim(),
    votes = votes,
    codec = codec.trim(),
    bitrate = bitrate,
)
