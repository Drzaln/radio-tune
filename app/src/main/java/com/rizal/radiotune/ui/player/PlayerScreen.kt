package com.rizal.radiotune.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.rizal.radiotune.R
import com.rizal.radiotune.data.model.Station
import com.rizal.radiotune.playback.PlayerUiState
import com.rizal.radiotune.ui.components.CassettePlayer
import com.rizal.radiotune.ui.components.EmptyView
import com.rizal.radiotune.ui.theme.PlayerSkin
import com.rizal.radiotune.ui.theme.PlayerStyle
import com.rizal.radiotune.ui.theme.skin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    state: PlayerUiState,
    favoriteIds: Set<String>,
    playerStyle: PlayerStyle,
    onSelectStyle: (PlayerStyle) -> Unit,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onStop: () -> Unit,
    onToggleFavorite: (Station) -> Unit,
    onSetSleepTimer: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val skin = playerStyle.skin()
    var showSleepDialog by remember { mutableStateOf(false) }
    var showStylePicker by remember { mutableStateOf(false) }
    val station = state.current

    // The skinned backdrop can be light or dark regardless of the system theme,
    // so the status bar icons have to follow the style.
    val view = LocalView.current
    val systemDark = isSystemInDarkTheme()
    DisposableEffect(skin.darkStatusBarIcons) {
        val controller = view.context.findActivity()
            ?.window
            ?.let { WindowCompat.getInsetsController(it, view) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = skin.darkStatusBarIcons
        onDispose {
            controller?.isAppearanceLightStatusBars = previous ?: !systemDark
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(skin.backdrop)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        TopAppBar(
            title = { Text("Now playing") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { showStylePicker = true }) {
                    Icon(painterResource(R.drawable.ic_palette), contentDescription = "Player style")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = skin.content,
                navigationIconContentColor = skin.content,
                actionIconContentColor = skin.content,
            ),
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
                look = skin.cassette,
                playing = state.isPlaying,
                buffering = state.isBuffering,
                artworkUrl = station.faviconUrl,
                width = 264.dp,
                elevation = skin.shadowElevation,
            )
            Spacer(Modifier.height(24.dp))

            Text(
                text = station.name,
                style = MaterialTheme.typography.headlineSmall,
                color = skin.content,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            if (station.location.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = station.location,
                    style = MaterialTheme.typography.bodyLarge,
                    color = skin.mutedContent,
                    textAlign = TextAlign.Center,
                )
            }

            if (station.qualityLabel.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = station.qualityLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = skin.mutedContent,
                )
            }

            if (station.tags.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = station.tags.joinToString("  ·  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = skin.mutedContent,
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
                val isFavorite = station.id in favoriteIds
                IconButton(onClick = { onToggleFavorite(station) }) {
                    Icon(
                        painter = painterResource(
                            if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border,
                        ),
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (isFavorite) skin.accent else skin.mutedContent,
                    )
                }

                FilledIconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.size(72.dp),
                    shape = skin.controlShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = skin.accent,
                        contentColor = skin.onAccent,
                    ),
                ) {
                    if (state.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp,
                            color = skin.onAccent,
                        )
                    } else {
                        Icon(
                            painter = painterResource(
                                if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                            ),
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(36.dp),
                            tint = skin.onAccent,
                        )
                    }
                }

                IconButton(onClick = { showSleepDialog = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_sleep),
                        contentDescription = "Sleep timer",
                        tint = if (state.sleepTimerMinutes != null) skin.accent else skin.mutedContent,
                    )
                }
            }

            if (state.sleepTimerMinutes != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Sleep timer · ${state.sleepTimerMinutes} min",
                    style = MaterialTheme.typography.labelLarge,
                    color = skin.accent,
                )
            }

            Spacer(Modifier.height(24.dp))

            OutlinedButton(
                onClick = onStop,
                shape = skin.controlShape,
                border = BorderStroke(skin.borderWidth, skin.outline),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = skin.content),
            ) {
                Text("Stop playback")
            }
        }
    }

    if (showSleepDialog) {
        SleepTimerDialog(
            skin = skin,
            currentMinutes = state.sleepTimerMinutes,
            onSelect = { minutes ->
                onSetSleepTimer(minutes)
                showSleepDialog = false
            },
            onDismiss = { showSleepDialog = false },
        )
    }

    if (showStylePicker) {
        StylePickerDialog(
            current = playerStyle,
            onSelect = { style ->
                onSelectStyle(style)
                showStylePicker = false
            },
            onDismiss = { showStylePicker = false },
        )
    }
}

@Composable
private fun SleepTimerDialog(
    skin: PlayerSkin,
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
                        skin = skin,
                        onClick = { onSelect(minutes) },
                    )
                }
                SleepTimerRow(
                    label = "Off",
                    selected = currentMinutes == null,
                    skin = skin,
                    onClick = { onSelect(null) },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = skin.accent),
            ) {
                Text("Close")
            }
        },
        containerColor = skin.surface,
        titleContentColor = skin.content,
        textContentColor = skin.content,
        shape = skin.panelShape,
    )
}

@Composable
private fun SleepTimerRow(
    label: String,
    selected: Boolean,
    skin: PlayerSkin,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = skin.accent,
                unselectedColor = skin.mutedContent,
            ),
        )
        Spacer(Modifier.width(8.dp))
        Text(label, color = skin.content, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun StylePickerDialog(
    current: PlayerStyle,
    onSelect: (PlayerStyle) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Player style") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                PlayerStyle.entries.forEach { style ->
                    val preview = style.skin()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(style) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CassettePlayer(
                            look = preview.cassette,
                            playing = style == current,
                            buffering = false,
                            artworkUrl = null,
                            width = 96.dp,
                            elevation = preview.shadowElevation,
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = style.displayName,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Spacer(Modifier.weight(1f))
                        if (style == current) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private val SleepTimerOptions = listOf(5, 10, 15, 30, 45, 60, 90, 120)
