package com.rizal.radiotune.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rizal.radiotune.R
import com.rizal.radiotune.data.model.Station
import com.rizal.radiotune.playback.PlayerUiState
import com.rizal.radiotune.ui.components.CassettePlayer
import com.rizal.radiotune.ui.components.EmptyView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    state: PlayerUiState,
    favoriteIds: Set<String>,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onStop: () -> Unit,
    onToggleFavorite: (Station) -> Unit,
    onSetSleepTimer: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSleepDialog by remember { mutableStateOf(false) }
    val station = state.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        TopAppBar(
            title = { Text("Now playing") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back")
                }
            },
            windowInsets = WindowInsets(0, 0, 0, 0),
        )

        if (station == null) {
            EmptyView("Nothing is playing right now")
            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CassettePlayer(
                playing = state.isPlaying,
                buffering = state.isBuffering,
                artworkUrl = station.faviconUrl,
                width = 264.dp,
            )
            Spacer(Modifier.height(24.dp))

            Text(
                text = station.name,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            if (station.location.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = station.location,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            if (station.qualityLabel.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = station.qualityLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (station.tags.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = station.tags.joinToString("  ·  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(36.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                IconButton(onClick = { onToggleFavorite(station) }) {
                    val isFavorite = station.id in favoriteIds
                    Icon(
                        painter = painterResource(
                            if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border,
                        ),
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (isFavorite) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }

                FilledIconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.size(72.dp),
                ) {
                    if (state.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(
                            painter = painterResource(
                                if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                            ),
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }

                IconButton(onClick = { showSleepDialog = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_sleep),
                        contentDescription = "Sleep timer",
                        tint = if (state.sleepTimerMinutes != null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            if (state.sleepTimerMinutes != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Sleep timer · ${state.sleepTimerMinutes} min",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.height(24.dp))

            TextButton(onClick = onStop) {
                Text("Stop playback")
            }
        }
    }

    if (showSleepDialog) {
        SleepTimerDialog(
            currentMinutes = state.sleepTimerMinutes,
            onSelect = { minutes ->
                onSetSleepTimer(minutes)
                showSleepDialog = false
            },
            onDismiss = { showSleepDialog = false },
        )
    }
}

@Composable
private fun SleepTimerDialog(
    currentMinutes: Int?,
    onSelect: (Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep timer") },
        text = {
            Column {
                SleepTimerOptions.forEach { minutes ->
                    SleepTimerRow(
                        label = "$minutes minutes",
                        selected = currentMinutes == minutes,
                        onClick = { onSelect(minutes) },
                    )
                }
                SleepTimerRow(
                    label = "Off",
                    selected = currentMinutes == null,
                    onClick = { onSelect(null) },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}

@Composable
private fun SleepTimerRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

private val SleepTimerOptions = listOf(5, 10, 15, 30, 45, 60, 90, 120)
