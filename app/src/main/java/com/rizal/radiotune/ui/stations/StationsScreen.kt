package com.rizal.radiotune.ui.stations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.rizal.radiotune.data.model.StationSort
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
    var showSortMenu by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    val searching = state.query.isNotEmpty() || state.selectedTags.isNotEmpty() || searchFocused
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
            actions = {
                Box {
                    TextButton(onClick = { showSortMenu = true }) {
                        Text("${state.sort.label} ▾")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                    ) {
                        StationSort.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    viewModel.onSortChange(option)
                                    showSortMenu = false
                                },
                                trailingIcon = {
                                    if (option == state.sort) {
                                        Icon(
                                            painterResource(R.drawable.ic_check),
                                            contentDescription = null,
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            },
            scrollBehavior = scrollBehavior,
        )

        // Collapses with the bar unless the user is actually searching.
        AnimatedVisibility(visible = !barCollapsed || searching) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SearchField(
                        value = state.query,
                        onValueChange = viewModel::onQueryChange,
                        placeholder = "Search stations",
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { searchFocused = it.isFocused },
                    )
                    BadgedBox(
                        badge = {
                            if (state.selectedTags.isNotEmpty()) {
                                Badge { Text("${state.selectedTags.size}") }
                            }
                        },
                    ) {
                        IconButton(onClick = { showTagDialog = true }) {
                            Icon(
                                painterResource(R.drawable.ic_filter),
                                contentDescription = "Filter by genre",
                            )
                        }
                    }
                }

                if (state.selectedTags.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.selectedTags.forEach { tag ->
                            FilterChip(
                                selected = true,
                                onClick = { viewModel.onTagToggle(tag) },
                                label = { Text(tag) },
                            )
                        }
                    }
                }
            }
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

    if (showTagDialog) {
        TagFilterDialog(
            tags = state.availableTags,
            selected = state.selectedTags,
            onToggle = viewModel::onTagToggle,
            onClear = viewModel::clearTags,
            onDismiss = { showTagDialog = false },
        )
    }
}

@Composable
private fun TagFilterDialog(
    tags: List<String>,
    selected: List<String>,
    onToggle: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter by genre") },
        text = {
            if (tags.isEmpty()) {
                Text("No genres found for this country yet.")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(tags, key = { it }) { tag ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggle(tag) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = tag in selected,
                                onCheckedChange = { onToggle(tag) },
                            )
                            Text(tag, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
        dismissButton = {
            if (selected.isNotEmpty()) {
                TextButton(onClick = onClear) { Text("Clear") }
            }
        },
    )
}
