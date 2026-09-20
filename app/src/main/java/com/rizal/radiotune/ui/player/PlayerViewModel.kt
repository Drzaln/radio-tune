package com.rizal.radiotune.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizal.radiotune.data.model.Station
import com.rizal.radiotune.data.repository.FavoritesRepository
import com.rizal.radiotune.data.repository.RadioRepository
import com.rizal.radiotune.playback.PlayerController
import com.rizal.radiotune.playback.PlayerUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val radioRepository: RadioRepository,
    private val favoritesRepository: FavoritesRepository,
    private val controller: PlayerController,
) : ViewModel() {

    val playerState: StateFlow<PlayerUiState> = controller.state

    val favorites: StateFlow<List<Station>> = favoritesRepository.favorites
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun play(station: Station) {
        radioRepository.registerClick(station.id)
        viewModelScope.launch {
            val url = radioRepository.resolvePlayableUrl(station)
            controller.play(station, url)
        }
    }

    fun togglePlayPause() = controller.togglePlayPause()

    fun stop() = controller.stop()

    fun setSleepTimer(minutes: Int?) = controller.setSleepTimer(minutes)

    fun toggleFavorite(station: Station) {
        viewModelScope.launch { favoritesRepository.toggle(station) }
    }

    fun dismissError() = controller.clearError()
}
