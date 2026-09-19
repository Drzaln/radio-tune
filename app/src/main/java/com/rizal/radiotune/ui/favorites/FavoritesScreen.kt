package com.rizal.radiotune.ui.favorites

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rizal.radiotune.data.model.Station
import com.rizal.radiotune.playback.PlayerUiState
import com.rizal.radiotune.ui.AppViewModelProvider
import com.rizal.radiotune.ui.components.EmptyView
import com.rizal.radiotune.ui.components.StationListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    playerState: PlayerUiState,
    onPlay: (Station) -> Unit,
    onTogglePlayPause: () -> Unit,
    onToggleFavorite: (Station) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: FavoritesViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val favoriteIds = favorites.map { it.id }.toSet()

    Column(modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Favorites") })

        if (favorites.isEmpty()) {
            EmptyView("No favorites yet.\nTap the heart on any station to save it.")
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(favorites, key = { it.id }) { station ->
                    StationListItem(
                        station = station,
                        isFavorite = true,
                        isActive = playerState.current?.id == station.id,
                        onClick = {
                            if (playerState.current?.id == station.id) {
                                onTogglePlayPause()
                            } else {
                                onPlay(station)
                            }
                        },
                        onToggleFavorite = { onToggleFavorite(station) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
