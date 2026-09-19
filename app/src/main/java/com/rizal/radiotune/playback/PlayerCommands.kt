package com.rizal.radiotune.playback

import android.os.Bundle
import androidx.media3.session.SessionCommand

object PlayerCommands {

    const val ACTION_SET_SLEEP_TIMER = "com.rizal.radiotune.action.SET_SLEEP_TIMER"
    const val ACTION_CANCEL_SLEEP_TIMER = "com.rizal.radiotune.action.CANCEL_SLEEP_TIMER"
    const val ARG_MINUTES = "minutes"

    val SetSleepTimer = SessionCommand(ACTION_SET_SLEEP_TIMER, Bundle.EMPTY)
    val CancelSleepTimer = SessionCommand(ACTION_CANCEL_SLEEP_TIMER, Bundle.EMPTY)
}
