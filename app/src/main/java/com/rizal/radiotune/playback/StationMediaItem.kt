package com.rizal.radiotune.playback

import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.rizal.radiotune.data.model.Station
import kotlinx.serialization.json.Json

private const val EXTRA_STATION = "com.rizal.radiotune.extra.STATION"

/**
 * The station is embedded in the media item so the player state can be rebuilt
 * from the session alone after the UI (or the whole process) reconnects.
 */
fun Station.toMediaItem(json: Json, url: String = playbackUrl): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(name)
        .setArtist(location.ifBlank { countryCode })
        .setArtworkUri(faviconUrl.takeIf { it.isNotBlank() }?.toUri())
        .setIsBrowsable(false)
        .setIsPlayable(true)
        .setExtras(bundleOf(EXTRA_STATION to json.encodeToString(this)))
        .build()

    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(url)
        .setMediaMetadata(metadata)
        .build()
}

fun MediaItem.toStation(json: Json): Station? = mediaMetadata.toStation(json)

fun MediaMetadata.toStation(json: Json): Station? {
    val raw = extras?.getString(EXTRA_STATION) ?: return null
    return runCatching { json.decodeFromString<Station>(raw) }.getOrNull()
}
