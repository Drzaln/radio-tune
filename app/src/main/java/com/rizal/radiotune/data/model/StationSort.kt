package com.rizal.radiotune.data.model

/** Maps to Radio Browser's `order` / `reverse` query parameters. */
enum class StationSort(
    val label: String,
    val apiOrder: String,
    val reverse: Boolean,
) {
    POPULARITY("Popularity", "clickcount", true),
    BITRATE("Bitrate", "bitrate", true),
    CODEC("Codec", "codec", false),
    NAME("Name", "name", false),
}
