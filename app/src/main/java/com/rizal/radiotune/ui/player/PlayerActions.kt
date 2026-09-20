package com.rizal.radiotune.ui.player

import androidx.compose.runtime.Immutable
import com.rizal.radiotune.data.model.Station
import com.rizal.radiotune.ui.theme.PlayerStyle

/**
 * Everything the now-playing screen can ask the app to do. Bundled so the
 * portrait and landscape layouts share one parameter.
 */
@Immutable
data class PlayerActions(
    val onBack: () -> Unit,
    val onSelectStyle: (PlayerStyle) -> Unit,
    val onPlayPause: () -> Unit,
    val onTogglePower: () -> Unit,
    val onToggleFavorite: (Station) -> Unit,
    val onScan: () -> Unit,
    val onSetSleepTimer: (Int?) -> Unit,
    val onCycleSleepTimer: () -> Unit,
    val onSetVolume: (Float) -> Unit,
)
