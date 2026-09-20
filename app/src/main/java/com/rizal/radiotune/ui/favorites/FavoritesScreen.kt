package com.rizal.radiotune.ui.favorites

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rizal.radiotune.R
import com.rizal.radiotune.data.model.Station
import com.rizal.radiotune.playback.PlayerUiState
import com.rizal.radiotune.ui.AppViewModelProvider
import com.rizal.radiotune.ui.components.EmptyView
import com.rizal.radiotune.ui.components.SearchField
import com.rizal.radiotune.ui.components.StationListItem

private val FAVORITE_COLUMN_MIN_WIDTH = 320.dp
private const val BACKUP_MIME = "application/json"

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
    val query by viewModel.query.collectAsStateWithLifecycle()
    val favoriteIds = favorites.map { it.id }.toSet()

    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BACKUP_MIME),
    ) { uri ->
        uri?.let { target ->
            context.contentResolver.openOutputStream(target)?.let(viewModel::exportTo)
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let { source ->
            context.contentResolver.openInputStream(source)?.let(viewModel::importFrom)
        }
    }

    val visible = remember(favorites, query) {
        if (query.isBlank()) favorites else favorites.filter { it.matches(query) }
    }

    val gridState = rememberLazyGridState()
    var draggingIndex by remember { mutableStateOf(-1) }
    var dragDelta by remember { mutableStateOf(Offset.Zero) }
    val reorderable = query.isBlank()

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Column(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
    ) {
        TopAppBar(
            title = { Text("Favorites") },
            actions = {
                IconButton(
                    onClick = {
                        exportLauncher.launch("radiotune-favorites.json")
                    },
                    enabled = favorites.isNotEmpty(),
                ) {
                    Icon(painterResource(R.drawable.ic_download), contentDescription = "Export favorites")
                }
                IconButton(onClick = { importLauncher.launch(arrayOf(BACKUP_MIME, "*/*")) }) {
                    Icon(painterResource(R.drawable.ic_upload), contentDescription = "Import favorites")
                }
            },
            scrollBehavior = scrollBehavior,
        )

        if (favorites.isNotEmpty()) {
            SearchField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                placeholder = "Search favorites",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        when {
            favorites.isEmpty() ->
                EmptyView("No favorites yet.\nTap the heart on any station to save it.")

            visible.isEmpty() ->
                EmptyView("No favorites match \"$query\"")

            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = FAVORITE_COLUMN_MIN_WIDTH),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(visible, key = { _, station -> station.id }) { index, station ->
                    val latestIndex by rememberUpdatedState(index)
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
                        modifier = Modifier
                            .zIndex(if (index == draggingIndex) 1f else 0f)
                            .graphicsLayer {
                                if (index == draggingIndex) {
                                    translationX = dragDelta.x
                                    translationY = dragDelta.y
                                }
                            }
                            .then(
                                if (reorderable) {
                                    Modifier.pointerInput(station.id) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                draggingIndex = latestIndex
                                                dragDelta = Offset.Zero
                                            },
                                            onDrag = { change, amount ->
                                                change.consume()
                                                dragDelta += amount
                                                val target = targetIndexAt(
                                                    gridState = gridState,
                                                    fromIndex = draggingIndex,
                                                    delta = dragDelta,
                                                )
                                                if (target != null && target != draggingIndex) {
                                                    viewModel.move(draggingIndex, target)
                                                    draggingIndex = target
                                                    dragDelta = Offset.Zero
                                                }
                                            },
                                            onDragEnd = {
                                                draggingIndex = -1
                                                dragDelta = Offset.Zero
                                            },
                                            onDragCancel = {
                                                draggingIndex = -1
                                                dragDelta = Offset.Zero
                                            },
                                        )
                                    }
                                } else {
                                    Modifier
                                },
                            ),
                    )
                }
            }
        }
    }
}

private fun Station.matches(query: String): Boolean {
    val needle = query.trim()
    if (needle.isBlank()) return true
    return name.contains(needle, ignoreCase = true) ||
        country.contains(needle, ignoreCase = true) ||
        tags.any { it.contains(needle, ignoreCase = true) }
}

/** Grid slot under the dragged item's centre, or null when it is off the screen. */
private fun targetIndexAt(gridState: LazyGridState, fromIndex: Int, delta: Offset): Int? {
    val info = gridState.layoutInfo
    val from = info.visibleItemsInfo.firstOrNull { it.index == fromIndex } ?: return null
    val centerX = from.offset.x + from.size.width / 2f + delta.x
    val centerY = from.offset.y + from.size.height / 2f + delta.y
    return info.visibleItemsInfo.firstOrNull { item ->
        centerX >= item.offset.x && centerX <= item.offset.x + item.size.width &&
            centerY >= item.offset.y && centerY <= item.offset.y + item.size.height
    }?.index
}
