package com.rizal.radiotune.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Station(
    val id: String,
    val name: String,
    val streamUrl: String,
    val resolvedUrl: String = "",
    val homepage: String = "",
    val faviconUrl: String = "",
    val tags: List<String> = emptyList(),
    val country: String = "",
    val countryCode: String = "",
    val state: String = "",
    val language: String = "",
    val votes: Int = 0,
    val codec: String = "",
    val bitrate: Int = 0,
) {
    val playbackUrl: String
        get() = resolvedUrl.ifBlank { streamUrl }

    val location: String
        get() = listOf(state, country)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(", ")

    val qualityLabel: String
        get() = buildList {
            if (codec.isNotBlank()) add(codec.uppercase())
            if (bitrate > 0) add("$bitrate kbps")
        }.joinToString(" · ")
}
