package com.rizal.radiotune.ui.stations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rizal.radiotune.R
import com.rizal.radiotune.data.model.Station
import com.rizal.radiotune.playback.PlayerUiState
import com.rizal.radiotune.ui.AppViewModelProvider
import com.rizal.radiotune.ui.components.EmptyView
import com.rizal.radiotune.ui.components.ErrorView
import com.rizal.radiotune.ui.components.LoadingView
import com.rizal.radiotune.ui.components.SearchField
import com.rizal.radiotune.ui.components.StationListItem

private val STATION_COLUMN_MIN_WIDTH = 320.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationsScreen(
    playerState: PlayerUiState,
    favoriteIds: Set<String>,
    onBack: () -> Unit,
    onPlay: (Station) -> Unit,
    onTogglePlayPause: () -> Unit,
    onToggleFavorite: (Station) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: StationsViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()

    val nearEnd by remember {
        derivedStateOf {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            state.stations.isNotEmpty() && lastVisible >= state.stations.size - 4
        }
    }

    LaunchedEffect(nearEnd, state.stations.size) {
        if (nearEnd) viewModel.loadMore()
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var searchFocused by remember { mutableStateOf(false) }
    val searching = state.query.isNotEmpty() || searchFocused
    val barCollapsed = scrollBehavior.state.collapsedFraction >= 1f

    Column(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
    ) {
        TopAppBar(
            title = {
                Text(viewModel.countryName, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back")
                }
            },
            scrollBehavior = scrollBehavior,
        )

        // Collapses with the bar unless the user is actually searching.
        AnimatedVisibility(visible = !barCollapsed || searching) {
            SearchField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = "Search stations",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .onFocusChanged { searchFocused = it.isFocused },
            )
        }

        when {
            state.isLoading -> LoadingView()

            state.error != null && state.stations.isEmpty() ->
                ErrorView(state.error.orEmpty(), onRetry = viewModel::retry)

            state.stations.isEmpty() ->
                EmptyView("No stations found in ${viewModel.countryName}")

            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = STATION_COLUMN_MIN_WIDTH),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.stations, key = { it.id }) { station ->
                    StationListItem(
                        station = station,
                        isFavorite = station.id in favoriteIds,
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

                if (state.isLoadingMore) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}
