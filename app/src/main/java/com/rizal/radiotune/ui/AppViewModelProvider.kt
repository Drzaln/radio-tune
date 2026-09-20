package com.rizal.radiotune.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rizal.radiotune.RadioTuneApp
import com.rizal.radiotune.ui.countries.CountriesViewModel
import com.rizal.radiotune.ui.favorites.FavoritesViewModel
import com.rizal.radiotune.ui.player.PlayerViewModel
import com.rizal.radiotune.ui.stations.StationsViewModel
import com.rizal.radiotune.ui.update.UpdateViewModel

object AppViewModelProvider {

    val Factory = viewModelFactory {
        initializer {
            val container = app().container
            PlayerViewModel(
                radioRepository = container.radioRepository,
                favoritesRepository = container.favoritesRepository,
                settingsRepository = container.settingsRepository,
                controller = container.playerController,
            )
        }
        initializer {
            CountriesViewModel(app().container.radioRepository)
        }
        initializer {
            FavoritesViewModel(app().container.favoritesRepository)
        }
        initializer {
            StationsViewModel(app().container.radioRepository, createSavedStateHandle())
        }
        initializer {
            UpdateViewModel(app().container.updateRepository)
        }
    }
}

private fun CreationExtras.app(): RadioTuneApp =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as RadioTuneApp
