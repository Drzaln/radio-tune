package com.rizal.radiotune.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.core.os.bundleOf
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.rizal.radiotune.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException

@UnstableApi
class RadioPlayerService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var sleepTimerJob: Job? = null

    override fun onCreate() {
        super.onCreate()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(this)
                    .setLoadErrorHandlingPolicy(RadioLoadErrorHandlingPolicy()),
            )
            .setLoadControl(
                DefaultLoadControl.Builder()
                    // Live radio: start on a small buffer, grow while playing.
                    .setBufferDurationsMs(
                        MIN_BUFFER_MS,
                        MAX_BUFFER_MS,
                        BUFFER_FOR_PLAYBACK_MS,
                        BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
                    )
                    .setPrioritizeTimeOverSizeThresholds(true)
                    .build(),
            )
            .build()

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .setCallback(SessionCallback())
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (player.mediaItemCount == 0 || !player.playWhenReady) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        sleepTimerJob?.cancel()
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    private inner class SessionCallback : MediaSession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                .buildUpon()
                .add(PlayerCommands.SetSleepTimer)
                .add(PlayerCommands.CancelSleepTimer)
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                PlayerCommands.ACTION_SET_SLEEP_TIMER -> {
                    val minutes = args.getInt(PlayerCommands.ARG_MINUTES, 0)
                    scheduleSleepTimer(minutes)
                }

                PlayerCommands.ACTION_CANCEL_SLEEP_TIMER -> {
                    sleepTimerJob?.cancel()
                    sleepTimerJob = null
                    player.volume = 1f
                    publishSleepTimer(minutes = null, deadlineMs = null)
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    private fun scheduleSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        val clamped = minutes.coerceAtMost(MAX_SLEEP_MINUTES)
        if (clamped <= 0) {
            publishSleepTimer(minutes = null, deadlineMs = null)
            return
        }

        val durationMs = clamped * 60_000L
        publishSleepTimer(clamped, SystemClock.elapsedRealtime() + durationMs)

        sleepTimerJob = serviceScope.launch {
            delay(durationMs)
            fadeOutAndPause()
            publishSleepTimer(minutes = null, deadlineMs = null)
        }
    }

    /**
     * Publishes the running timer as session extras. The controller reads them
     * on connect, so the countdown survives UI reconnects and process death.
     */
    private fun publishSleepTimer(minutes: Int?, deadlineMs: Long?) {
        mediaSession?.setSessionExtras(
            bundleOf(
                PlayerCommands.EXTRA_SLEEP_TIMER_MINUTES to (minutes ?: 0),
                PlayerCommands.EXTRA_SLEEP_TIMER_DEADLINE_MS to (deadlineMs ?: 0L),
            ),
        )
    }

    private suspend fun fadeOutAndPause() {
        for (step in FADE_STEPS downTo 1) {
            player.volume = step / FADE_STEPS.toFloat()
            delay(FADE_TICK_MS)
        }
        player.pause()
        player.volume = 1f
    }

    private companion object {
        const val FADE_STEPS = 20
        const val FADE_TICK_MS = 500L
        const val MAX_SLEEP_MINUTES = 24 * 60

        const val MIN_BUFFER_MS = 1_500
        const val MAX_BUFFER_MS = 30_000
        const val BUFFER_FOR_PLAYBACK_MS = 1_000
        const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 2_000
    }
}

/**
 * Retries transient network/IO failures with exponential backoff instead of
 * giving up on the first hiccup, so a live stream that drops briefly resumes
 * on its own. [PlayerController] only reports an error once these run out.
 */
@UnstableApi
private class RadioLoadErrorHandlingPolicy :
    DefaultLoadErrorHandlingPolicy(RETRY_COUNT) {

    override fun getRetryDelayMsFor(
        loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo,
    ): Long {
        if (loadErrorInfo.exception !is IOException) return C.TIME_UNSET
        val backoff = INITIAL_DELAY_MS shl loadErrorInfo.errorCount.coerceAtMost(MAX_BACKOFF_SHIFT)
        return backoff.coerceAtMost(MAX_DELAY_MS)
    }

    private companion object {
        const val RETRY_COUNT = 6
        const val INITIAL_DELAY_MS = 1_000L
        const val MAX_DELAY_MS = 30_000L
        const val MAX_BACKOFF_SHIFT = 4
    }
}
