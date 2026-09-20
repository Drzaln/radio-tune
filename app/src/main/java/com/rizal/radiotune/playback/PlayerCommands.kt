package com.rizal.radiotune.playback

import android.os.Bundle
import androidx.media3.session.SessionCommand

object PlayerCommands {

    const val ACTION_SET_SLEEP_TIMER = "com.rizal.radiotune.action.SET_SLEEP_TIMER"
    const val ACTION_CANCEL_SLEEP_TIMER = "com.rizal.radiotune.action.CANCEL_SLEEP_TIMER"
    const val ARG_MINUTES = "minutes"

    /**
     * Session extras published by [RadioPlayerService] so the controller can
     * rebuild the active sleep timer after reconnecting (or process death).
     */
    const val EXTRA_SLEEP_TIMER_MINUTES = "sleep_timer_minutes"
    const val EXTRA_SLEEP_TIMER_DEADLINE_MS = "sleep_timer_deadline_ms"

    val SetSleepTimer = SessionCommand(ACTION_SET_SLEEP_TIMER, Bundle.EMPTY)
    val CancelSleepTimer = SessionCommand(ACTION_CANCEL_SLEEP_TIMER, Bundle.EMPTY)
}
