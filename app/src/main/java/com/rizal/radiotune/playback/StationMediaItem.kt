package com.rizal.radiotune.playback

import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.rizal.radiotune.data.model.Station
import kotlinx.serialization.json.Json

private const val EXTRA_STATION = "com.rizal.radiotune.extra.STATION"

private const val LIVE_TARGET_OFFSET_MS = 5_000L
private const val LIVE_MIN_OFFSET_MS = 2_000L
private const val LIVE_MAX_OFFSET_MS = 15_000L

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
        .setLiveConfiguration(
            MediaItem.LiveConfiguration.Builder()
                .setTargetOffsetMs(LIVE_TARGET_OFFSET_MS)
                .setMinOffsetMs(LIVE_MIN_OFFSET_MS)
                .setMaxOffsetMs(LIVE_MAX_OFFSET_MS)
                .build(),
        )
        .build()
}

fun MediaItem.toStation(json: Json): Station? = mediaMetadata.toStation(json)

fun MediaMetadata.toStation(json: Json): Station? {
    val raw = extras?.getString(EXTRA_STATION) ?: return null
    return runCatching { json.decodeFromString<Station>(raw) }.getOrNull()
}
