package com.rizal.radiotune.ui.stations

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val listState = rememberLazyListState()

    val nearEnd by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            state.stations.isNotEmpty() && lastVisible >= state.stations.size - 4
        }
    }

    LaunchedEffect(nearEnd, state.stations.size) {
        if (nearEnd) viewModel.loadMore()
    }

    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(viewModel.countryName, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back")
                }
            },
        )

        SearchField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            placeholder = "Search stations",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        when {
            state.isLoading -> LoadingView()

            state.error != null && state.stations.isEmpty() ->
                ErrorView(state.error.orEmpty(), onRetry = viewModel::retry)

            state.stations.isEmpty() ->
                EmptyView("No stations found in ${viewModel.countryName}")

            else -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
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
                    HorizontalDivider()
                }

                if (state.isLoadingMore) {
                    item {
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
