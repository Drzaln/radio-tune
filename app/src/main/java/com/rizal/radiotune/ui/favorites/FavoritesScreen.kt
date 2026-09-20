package com.rizal.radiotune.ui.favorites

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
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

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Column(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
    ) {
        TopAppBar(title = { Text("Favorites") }, scrollBehavior = scrollBehavior)

        if (favorites.isEmpty()) {
            EmptyView("No favorites yet.\nTap the heart on any station to save it.")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = FAVORITE_COLUMN_MIN_WIDTH),
                modifier = Modifier.fillMaxSize(),
            ) {
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
                }
            }
        }
    }
}

private val FAVORITE_COLUMN_MIN_WIDTH = 320.dp
