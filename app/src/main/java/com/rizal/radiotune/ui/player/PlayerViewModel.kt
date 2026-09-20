package com.rizal.radiotune.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizal.radiotune.data.model.Station
import com.rizal.radiotune.data.repository.FavoritesRepository
import com.rizal.radiotune.data.repository.RadioRepository
import com.rizal.radiotune.data.repository.SettingsRepository
import com.rizal.radiotune.playback.PlayerController
import com.rizal.radiotune.playback.PlayerUiState
import com.rizal.radiotune.ui.theme.PlayerStyle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val radioRepository: RadioRepository,
    private val favoritesRepository: FavoritesRepository,
    private val settingsRepository: SettingsRepository,
    private val controller: PlayerController,
) : ViewModel() {

    val playerState: StateFlow<PlayerUiState> = controller.state

    val favorites: StateFlow<List<Station>> = favoritesRepository.favorites
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val playerStyle: StateFlow<PlayerStyle> = settingsRepository.playerStyleName
        .map { name -> PlayerStyle.entries.firstOrNull { it.name == name } ?: PlayerStyle.CLASSIC }
        .stateIn(viewModelScope, SharingStarted.Eagerly, PlayerStyle.CLASSIC)

    fun play(station: Station) {
        radioRepository.registerClick(station.id)
        viewModelScope.launch {
            val url = radioRepository.resolvePlayableUrl(station)
            controller.play(station, url)
        }
    }

    fun togglePlayPause() = controller.togglePlayPause()

    fun stop() = controller.stop()

    /** Power switch: off stops playback, on resumes the last station. */
    fun togglePower() {
        val state = controller.state.value
        if (state.current != null) {
            controller.stop()
        } else {
            state.lastStation?.let { play(it) }
        }
    }

    /** Tuning: hop to another station in the same country. */
    fun scan() {
        val countryCode = controller.state.value.current?.countryCode.orEmpty()
        if (countryCode.isBlank()) return
        viewModelScope.launch {
            radioRepository.randomStation(countryCode)?.let { station -> play(station) }
        }
    }

    fun setVolume(fraction: Float) = controller.setVolume(fraction)

    fun cycleSleepTimer() {
        val current = controller.state.value.sleepTimerMinutes
        val next = SLEEP_CYCLE.getOrNull(SLEEP_CYCLE.indexOf(current) + 1) ?: SLEEP_CYCLE.first()
        controller.setSleepTimer(next)
    }

    fun setSleepTimer(minutes: Int?) = controller.setSleepTimer(minutes)

    fun toggleFavorite(station: Station) {
        viewModelScope.launch { favoritesRepository.toggle(station) }
    }

    fun dismissError() = controller.clearError()

    fun retryConnection() = controller.retryConnect()

    fun setPlayerStyle(style: PlayerStyle) {
        viewModelScope.launch { settingsRepository.setPlayerStyleName(style.name) }
    }

    private companion object {
        val SLEEP_CYCLE = listOf(null, 5, 15, 30, 60, 90)
    }
}
