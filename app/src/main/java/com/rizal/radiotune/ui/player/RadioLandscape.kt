package com.rizal.radiotune.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.rizal.radiotune.R
import com.rizal.radiotune.data.model.Station
import com.rizal.radiotune.playback.PlayerUiState
import com.rizal.radiotune.ui.components.CASSETTE_ASPECT
import com.rizal.radiotune.ui.components.CassettePlayer
import com.rizal.radiotune.ui.theme.PlayerSkin
import com.rizal.radiotune.ui.theme.PlayerStyle
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Landscape now-playing screen: cassette on the left, a working retro radio
 * chassis on the right. Every control is wired — power, play/pause, favourite,
 * sleep (cycling), tuning (scan) and volume.
 */
@Composable
fun RadioLandscape(
    skin: PlayerSkin,
    state: PlayerUiState,
    favoriteIds: Set<String>,
    playerStyle: PlayerStyle,
    actions: PlayerActions,
) {
    var showStylePicker by remember { mutableStateOf(false) }
    val station = state.current

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = actions.onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "Back",
                    tint = skin.content,
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { showStylePicker = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_palette),
                    contentDescription = "Player style",
                    tint = skin.content,
                )
            }
        }

        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BoxWithConstraints(
                modifier = Modifier.weight(CASSETTE_PANE_WEIGHT).fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                val cassetteWidth = minOf(
                    maxWidth * CASSETTE_PANE_FILL,
                    maxHeight * CASSETTE_PANE_FILL / CASSETTE_ASPECT,
                )
                CassettePlayer(
                    look = skin.cassette,
                    playing = state.isPlaying,
                    buffering = state.isBuffering,
                    artworkUrl = station?.faviconUrl,
                    width = cassetteWidth,
                    elevation = skin.shadowElevation,
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(CONTROL_PANE_WEIGHT).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DialWindow(
                    skin = skin,
                    station = station,
                    live = state.isPlaying,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TuneKnob(skin = skin, enabled = station != null, onScan = actions.onScan)
                    VolumeKnob(
                        volume = state.volume,
                        skin = skin,
                        onSet = actions.onSetVolume,
                    )
                }

                ControlRow(
                    skin = skin,
                    state = state,
                    favoriteIds = favoriteIds,
                    actions = actions,
                )
            }
        }
    }

    if (showStylePicker) {
        StylePickerDialog(
            current = playerStyle,
            onSelect = { style ->
                actions.onSelectStyle(style)
                showStylePicker = false
            },
            onDismiss = { showStylePicker = false },
        )
    }
}

@Composable
private fun DialWindow(
    skin: PlayerSkin,
    station: Station?,
    live: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(skin.panelShape)
            .background(skin.surface)
            .border(skin.borderWidth, skin.outline, skin.panelShape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = station?.name ?: "POWERED OFF",
                    style = MaterialTheme.typography.titleMedium,
                    color = skin.content,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val meta = station?.let { current ->
                    listOf(current.location, current.qualityLabel)
                        .filter { it.isNotBlank() }
                        .joinToString(" · ")
                }.orEmpty()
                if (meta.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = skin.mutedContent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            TuningScale(skin = skin, seed = station?.id, live = live)
        }
    }
}

@Composable
private fun TuningScale(skin: PlayerSkin, seed: String?, live: Boolean) {
    val needle = remember(seed) { stableNeedle(seed) }
    Canvas(Modifier.fillMaxWidth().height(26.dp)) {
        val y = size.height * 0.62f
        val scaleColor = skin.mutedContent
        val needleColor = if (live) skin.accent else skin.mutedContent

        drawLine(
            color = scaleColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = size.height * 0.035f,
        )
        repeat(TICK_COUNT) { index ->
            val x = size.width * index / (TICK_COUNT - 1).toFloat()
            val tall = index % 5 == 0
            val half = if (tall) size.height * 0.16f else size.height * 0.085f
            drawLine(
                color = scaleColor,
                start = Offset(x, y - half),
                end = Offset(x, y + half),
                strokeWidth = size.height * 0.025f,
            )
        }

        val needleX = size.width * needle
        drawLine(
            color = needleColor,
            start = Offset(needleX, size.height * 0.06f),
            end = Offset(needleX, size.height * 0.94f),
            strokeWidth = size.height * 0.09f,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun TuneKnob(skin: PlayerSkin, enabled: Boolean, onScan: () -> Unit) {
    Box(Modifier.clickable(enabled = enabled, onClick = onScan)) {
        KnobFace(
            label = "SCAN",
            value = null,
            skin = skin,
            enabled = enabled,
        )
    }
}

/**
 * Rotary: the pointer follows the finger around the knob, so the control matches
 * the way it looks. Dragging through the gap at the bottom snaps to the nearest end.
 */
@Composable
private fun VolumeKnob(volume: Float, skin: PlayerSkin, onSet: (Float) -> Unit) {
    Box(
        Modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { position -> onSet(volumeFromPosition(position, size)) },
                onDrag = { change, _ ->
                    change.consume()
                    onSet(volumeFromPosition(change.position, size))
                },
            )
        },
    ) {
        KnobFace(label = "VOLUME", value = volume, skin = skin, enabled = true)
    }
}

private fun volumeFromPosition(position: Offset, size: IntSize): Float {
    val degrees = Math.toDegrees(
        atan2(
            (position.y - size.height / 2f).toDouble(),
            (position.x - size.width / 2f).toDouble(),
        ),
    )
    // Screen coordinates put 0 degrees at 3 o'clock and grow clockwise, so the
    // dial runs 135°..405° and the gap sits at the bottom (45°..135°).
    val angle = if (degrees < KNOB_START_DEGREES) degrees + 360.0 else degrees
    val end = KNOB_START_DEGREES + KNOB_SWEEP_DEGREES
    return when {
        angle <= KNOB_START_DEGREES -> 0f
        angle <= end -> ((angle - KNOB_START_DEGREES) / KNOB_SWEEP_DEGREES).toFloat()
        angle < end + (360.0 - KNOB_SWEEP_DEGREES) / 2.0 -> 1f
        else -> 0f
    }
}

@Composable
private fun KnobFace(
    label: String,
    value: Float?,
    skin: PlayerSkin,
    enabled: Boolean,
    knobSize: Dp = KNOB_SIZE,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(knobSize)) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val bodyAlpha = if (enabled) 1f else 0.45f

            drawCircle(skin.cassette.shellTop, radius * 0.80f, center, alpha = bodyAlpha)
            drawCircle(
                color = skin.cassette.shellBottom,
                radius = radius * 0.80f,
                center = center,
                style = Stroke(width = radius * 0.08f),
                alpha = bodyAlpha,
            )
            drawCircle(
                color = skin.outline,
                radius = radius * 0.80f,
                center = center,
                style = Stroke(width = radius * 0.03f),
                alpha = bodyAlpha,
            )

            repeat(KNOB_TICKS) { index ->
                val radians = Math.toRadians(KNOB_START_DEGREES + index * KNOB_STEP_DEGREES)
                val tall = index % 5 == 0
                val inner = radius * 0.86f
                val outer = if (tall) radius else radius * 0.95f
                drawLine(
                    color = skin.mutedContent,
                    start = Offset(
                        center.x + cos(radians).toFloat() * inner,
                        center.y + sin(radians).toFloat() * inner,
                    ),
                    end = Offset(
                        center.x + cos(radians).toFloat() * outer,
                        center.y + sin(radians).toFloat() * outer,
                    ),
                    strokeWidth = radius * 0.05f,
                    alpha = bodyAlpha,
                )
            }

            if (value != null) {
                val radians = Math.toRadians(KNOB_START_DEGREES + value.coerceIn(0f, 1f) * 270.0)
                drawLine(
                    color = skin.accent,
                    start = center,
                    end = Offset(
                        center.x + cos(radians).toFloat() * radius * 0.62f,
                        center.y + sin(radians).toFloat() * radius * 0.62f,
                    ),
                    strokeWidth = radius * 0.14f,
                    cap = StrokeCap.Round,
                    alpha = bodyAlpha,
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = skin.mutedContent,
        )
    }
}

@Composable
private fun ControlRow(
    skin: PlayerSkin,
    state: PlayerUiState,
    favoriteIds: Set<String>,
    actions: PlayerActions,
) {
    val station = state.current
    val isFavorite = station != null && station.id in favoriteIds
    val poweredOn = station != null

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = actions.onTogglePower) {
            Icon(
                painter = painterResource(R.drawable.ic_power),
                contentDescription = if (poweredOn) "Power off" else "Power on",
                tint = if (poweredOn) skin.accent else skin.mutedContent,
            )
        }

        FilledIconButton(
            onClick = actions.onPlayPause,
            enabled = poweredOn,
            modifier = Modifier.size(56.dp),
            shape = skin.controlShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = skin.accent,
                contentColor = skin.onAccent,
            ),
        ) {
            if (state.isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = skin.onAccent,
                )
            } else {
                Icon(
                    painter = painterResource(
                        if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                    ),
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(28.dp),
                    tint = skin.onAccent,
                )
            }
        }

        IconButton(
            onClick = { station?.let(actions.onToggleFavorite) },
            enabled = poweredOn,
        ) {
            Icon(
                painter = painterResource(
                    if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border,
                ),
                contentDescription = "Favorite",
                tint = if (isFavorite) skin.accent else skin.mutedContent,
            )
        }

        TextButton(
            onClick = actions.onCycleSleepTimer,
            colors = ButtonDefaults.textButtonColors(contentColor = skin.content),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_sleep),
                contentDescription = "Sleep timer",
                tint = if (state.sleepTimerMinutes != null) skin.accent else skin.mutedContent,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = state.sleepTimerMinutes?.let { "$it m" } ?: "SLEEP",
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

/** Stable pseudo-frequency for the dial needle, derived from the station id. */
private fun stableNeedle(seed: String?): Float {
    if (seed.isNullOrBlank()) return 0.5f
    val bucket = (seed.hashCode().toLong() and 0x7FFFFFFFL) % 100L
    return 0.06f + bucket / 100f * 0.88f
}

/** The controls get a little more room than the cassette, which is centred. */
private const val CASSETTE_PANE_WEIGHT = 0.85f
private const val CONTROL_PANE_WEIGHT = 1.15f
private const val CASSETTE_PANE_FILL = 0.84f

private val KNOB_SIZE = 64.dp
private const val KNOB_TICKS = 11

/** Dial sweeps clockwise from 7:30 to 4:30, leaving the gap at the bottom. */
private const val KNOB_SWEEP_DEGREES = 270.0
private const val KNOB_START_DEGREES = 135.0
private const val KNOB_STEP_DEGREES = KNOB_SWEEP_DEGREES / (KNOB_TICKS - 1)
private const val TICK_COUNT = 21
