package com.rizal.radiotune.playback

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.rizal.radiotune.data.model.Station
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json

data class PlayerUiState(
    val connected: Boolean = false,
    val current: Station? = null,
    /** Kept after [PlayerController.stop] so a power switch can resume it. */
    val lastStation: Station? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val volume: Float = 1f,
    val errorMessage: String? = null,
    val sleepTimerMinutes: Int? = null,
)

/**
 * Single app-scoped bridge to [RadioPlayerService]. The activity connects it in
 * `onStart` and releases it in `onStop`; playback keeps running in the service.
 */
@UnstableApi
class PlayerController(
    private val context: Context,
    private val json: Json,
) {

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val mainExecutor = ContextCompat.getMainExecutor(context)
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val listener = object : Player.Listener {

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying, isBuffering = false) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _state.update {
                it.copy(
                    isBuffering = playbackState == Player.STATE_BUFFERING,
                    errorMessage = if (playbackState == Player.STATE_READY) null else it.errorMessage,
                )
            }
        }

        override fun onVolumeChanged(volume: Float) {
            _state.update { it.copy(volume = volume) }
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            mediaMetadata.toStation(json)?.let { station ->
                _state.update { it.copy(current = station) }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _state.update { it.copy(isBuffering = false, errorMessage = error.toUserMessage()) }
        }
    }

    fun connect() {
        if (controllerFuture != null) return
        val token = SessionToken(context, ComponentName(context, RadioPlayerService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                val connected = runCatching { future.get() }.getOrNull() ?: return@addListener
                controller = connected
                connected.addListener(listener)
                syncFrom(connected)
            },
            mainExecutor,
        )
    }

    fun release() {
        controller?.removeListener(listener)
        controller = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        _state.update { it.copy(connected = false, isPlaying = false, isBuffering = false) }
    }

    fun play(station: Station, url: String = station.playbackUrl) {
        val connected = controller ?: return
        if (url.isBlank()) {
            _state.update { it.copy(errorMessage = "This station has no playable stream") }
            return
        }
        _state.update {
            it.copy(
                current = station,
                lastStation = station,
                errorMessage = null,
                isBuffering = true,
            )
        }
        connected.setMediaItem(station.toMediaItem(json, url))
        connected.prepare()
        connected.play()
    }

    fun togglePlayPause() {
        val connected = controller ?: return
        if (connected.isPlaying) {
            connected.pause()
        } else {
            if (connected.playbackState == Player.STATE_IDLE) connected.prepare()
            connected.play()
        }
    }

    fun stop() {
        controller?.stop()
        controller?.clearMediaItems()
        _state.update {
            it.copy(current = null, isPlaying = false, isBuffering = false, errorMessage = null)
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun setVolume(fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        controller?.volume = clamped
        _state.update { it.copy(volume = clamped) }
    }

    fun setSleepTimer(minutes: Int?) {
        val connected = controller ?: return
        if (minutes == null || minutes <= 0) {
            connected.sendCustomCommand(PlayerCommands.CancelSleepTimer, Bundle.EMPTY)
            _state.update { it.copy(sleepTimerMinutes = null) }
        } else {
            connected.sendCustomCommand(
                PlayerCommands.SetSleepTimer,
                bundleOf(PlayerCommands.ARG_MINUTES to minutes),
            )
            _state.update { it.copy(sleepTimerMinutes = minutes) }
        }
    }

    private fun syncFrom(connected: MediaController) {
        val station = connected.currentMediaItem?.toStation(json)
        _state.update {
            it.copy(
                connected = true,
                current = station ?: it.current,
                lastStation = station ?: it.lastStation,
                isPlaying = connected.isPlaying,
                isBuffering = connected.playbackState == Player.STATE_BUFFERING,
                volume = connected.volume,
            )
        }
    }
}

private fun PlaybackException.toUserMessage(): String = when (errorCode) {
    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
    PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
    -> "Stream is unavailable right now"

    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
    -> "Network problem — check your connection"

    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
    PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
    PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
    PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED,
    -> "This stream format is not supported"

    else -> "Could not play this station"
}
