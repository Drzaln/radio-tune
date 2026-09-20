package com.rizal.radiotune.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rizal.radiotune.R
import com.rizal.radiotune.playback.PlayerUiState

@Composable
fun MiniPlayer(
    state: PlayerUiState,
    onTogglePlayPause: () -> Unit,
    onOpen: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val station = state.current ?: return

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clickable(onClick = onOpen)
                .padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StationAvatar(
                size = 42.dp,
                highlighted = state.isPlaying,
                imageUrl = station.faviconUrl,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = state.errorMessage ?: station.location.ifBlank { station.qualityLabel },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(22.dp)
                        .padding(2.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                IconButton(onClick = onTogglePlayPause) {
                    Icon(
                        painter = painterResource(
                            if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                        ),
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                    )
                }
            }
            IconButton(onClick = onStop) {
                Icon(painterResource(R.drawable.ic_close), contentDescription = "Stop")
            }
        }
    }
}
