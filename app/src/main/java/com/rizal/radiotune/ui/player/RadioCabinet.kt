package com.rizal.radiotune.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rizal.radiotune.playback.PlayerUiState
import com.rizal.radiotune.ui.theme.PlayerSkin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Photo mode: the whole display is one 1950s wood tabletop set. Every part is
 * drawn — cabinet and grain, woven speaker cloth, a slide-rule FM dial with a red
 * needle behind glass, knurled knobs and piano keys — so a photo of the phone
 * reads as a real radio. Long-press ON/OFF to leave.
 */
@Composable
internal fun RadioCabinet(
    skin: PlayerSkin,
    state: PlayerUiState,
    favoriteIds: Set<String>,
    actions: PlayerActions,
    onTogglePhotoMode: () -> Unit,
) {
    val station = state.current
    val poweredOn = station != null
    val isFavorite = station != null && station.id in favoriteIds
    val needle = remember(station?.id) { radioNeedle(station?.id) }

    Box(Modifier.fillMaxSize()) {
        CabinetCanvas(skin = skin, modifier = Modifier.fillMaxSize())

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(GRILLE_WEIGHT).fillMaxHeight()) {
                BrandPlate(skin = skin)
                Spacer(Modifier.height(10.dp))
                SpeakerCloth(skin = skin, modifier = Modifier.weight(1f).fillMaxWidth())
            }

            Spacer(Modifier.width(20.dp))

            Column(
                modifier = Modifier.weight(CONTROL_WEIGHT).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SlideRuleDial(
                    skin = skin,
                    needle = needle,
                    title = state.nowPlayingTitle ?: station?.name ?: "POWERED OFF",
                    subtitle = if (state.nowPlayingTitle != null) {
                        station?.name.orEmpty()
                    } else {
                        station?.location.orEmpty()
                    },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    KnurledKnob(
                        label = "VOLUME",
                        value = state.volume,
                        skin = skin,
                        onDrag = actions.onSetVolume,
                    )
                    KnurledKnob(
                        label = "TUNING",
                        value = null,
                        skin = skin,
                        onClick = actions.onScan,
                        enabled = poweredOn,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PianoKey(
                        label = "ON/OFF",
                        active = poweredOn,
                        skin = skin,
                        modifier = Modifier.weight(1f),
                        onClick = actions.onTogglePower,
                        onLongClick = onTogglePhotoMode,
                    )
                    PianoKey(
                        label = if (state.isPlaying) "PAUSE" else "PLAY",
                        active = state.isPlaying,
                        skin = skin,
                        modifier = Modifier.weight(1f),
                        onClick = actions.onPlayPause,
                        enabled = poweredOn,
                    )
                    PianoKey(
                        label = "FAV",
                        active = isFavorite,
                        skin = skin,
                        modifier = Modifier.weight(1f),
                        onClick = { station?.let(actions.onToggleFavorite) },
                        enabled = poweredOn,
                    )
                    PianoKey(
                        label = state.sleepTimerMinutes?.let { "SLEEP ${it}M" } ?: "SLEEP",
                        active = state.sleepTimerMinutes != null,
                        skin = skin,
                        modifier = Modifier.weight(1f),
                        onClick = actions.onCycleSleepTimer,
                    )
                }
            }
        }
    }
}

/** Walnut cabinet: three-stop wood gradient, grain, bevel, inset shadow, screws. */
@Composable
private fun CabinetCanvas(skin: PlayerSkin, modifier: Modifier = Modifier) {
    val top = skin.cassette.shellTop
    val mid = skin.cassette.shellMid ?: skin.cassette.shellBottom
    val bottom = skin.cassette.shellBottom
    val grain = skin.cassette.edge
    val metal = skin.outline

    Canvas(modifier) {
        val corner = CornerRadius(CABINET_CORNER.toPx())
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(top, mid, bottom)),
            cornerRadius = corner,
        )

        // Grain: gently bowed lines across the cabinet.
        val rows = 44
        repeat(rows) { index ->
            val y = size.height * index / rows.toFloat()
            val wave = size.width * 0.015f
            val path = Path().apply {
                moveTo(0f, y)
                cubicTo(
                    size.width * 0.33f, y - wave,
                    size.width * 0.66f, y + wave,
                    size.width, y + wave * 0.35f,
                )
            }
            drawPath(path, grain.copy(alpha = 0.12f), style = Stroke(1.2.dp.toPx()))
        }

        // Bevel: lit top-left edge, deep bottom-right edge.
        val inset = 2.dp.toPx()
        drawRoundRect(
            color = Color.White.copy(alpha = 0.12f),
            topLeft = Offset(inset, inset),
            size = Size(size.width - inset * 2, size.height - inset * 2),
            cornerRadius = corner,
            style = Stroke(2.dp.toPx()),
        )
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.45f),
            topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
            size = Size(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()),
            cornerRadius = corner,
            style = Stroke(3.dp.toPx()),
        )

        val screws = 14.dp.toPx()
        listOf(
            Offset(screws, screws),
            Offset(size.width - screws, screws),
            Offset(screws, size.height - screws),
            Offset(size.width - screws, size.height - screws),
        ).forEach { center -> drawScrew(center, metal, grain) }
    }
}

private fun DrawScope.drawScrew(center: Offset, metal: Color, shadow: Color) {
    val radius = 3.4.dp.toPx()
    drawCircle(shadow.copy(alpha = 0.55f), radius * 1.2f, center + Offset(0f, radius * 0.3f))
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.45f), metal, shadow),
            center = center - Offset(radius * 0.4f, radius * 0.4f),
            radius = radius * 1.8f,
        ),
        radius = radius,
        center = center,
    )
    drawLine(
        color = shadow.copy(alpha = 0.85f),
        start = center - Offset(radius * 0.7f, 0f),
        end = center + Offset(radius * 0.7f, 0f),
        strokeWidth = radius * 0.28f,
    )
}

/** Woven speaker cloth: dense two-row weave, recessed and framed. */
@Composable
private fun SpeakerCloth(skin: PlayerSkin, modifier: Modifier = Modifier) {
    val cloth = skin.cassette.recess
    val dot = skin.cassette.line
    val frame = skin.outline
    val shape = RoundedCornerShape(10.dp)

    Canvas(modifier.clip(shape)) {
        drawRect(cloth)

        val step = 7.dp.toPx()
        val radius = 1.15.dp.toPx()
        var row = 0
        var y = step * 0.5f
        while (y < size.height) {
            var x = if (row % 2 == 0) step * 0.5f else step
            while (x < size.width) {
                drawCircle(dot.copy(alpha = 0.32f), radius, Offset(x, y))
                x += step
            }
            y += step * 0.72f
            row++
        }

        drawRect(
            Brush.verticalGradient(
                listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent, Color.Black.copy(alpha = 0.45f)),
            ),
        )
        drawRect(
            Brush.horizontalGradient(
                listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent, Color.Black.copy(alpha = 0.5f)),
            ),
        )
        drawRoundRect(
            color = frame.copy(alpha = 0.9f),
            cornerRadius = CornerRadius(10.dp.toPx()),
            style = Stroke(2.5.dp.toPx()),
        )
    }
}

/** Printed brass nameplate. */
@Composable
private fun BrandPlate(skin: PlayerSkin, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .background(
                Brush.verticalGradient(
                    listOf(skin.accent, skin.accent.copy(alpha = 0.72f)),
                ),
            )
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "RADIOTUNE",
            fontFamily = FontFamily.Serif,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp,
            color = skin.onAccent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Backlit slide-rule dial: printed FM scale, glass, red needle, station card. */
@Composable
private fun SlideRuleDial(
    skin: PlayerSkin,
    needle: Float,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    val face = skin.surface
    val print = skin.mutedContent
    val needleColor = skin.cassette.stripe
    val glass = skin.cassette.glass ?: Color.White.copy(alpha = 0.12f)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.verticalGradient(
                    listOf(face, face.copy(alpha = 0.88f), skin.cassette.recess.copy(alpha = 0.18f)),
                ),
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                fontFamily = FontFamily.Serif,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = skin.content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "FM",
                fontFamily = FontFamily.Serif,
                fontSize = 9.sp,
                letterSpacing = 1.sp,
                color = print,
            )
        }

        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                fontFamily = FontFamily.Serif,
                fontSize = 9.sp,
                color = print,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Canvas(Modifier.weight(1f).fillMaxWidth().padding(top = 2.dp)) {
            val baseY = size.height * 0.70f
            val ticks = 41
            repeat(ticks) { index ->
                val x = size.width * index / (ticks - 1).toFloat()
                val major = index % 5 == 0
                val tickHeight = if (major) size.height * 0.34f else size.height * 0.18f
                drawLine(
                    color = print.copy(alpha = if (major) 0.85f else 0.5f),
                    start = Offset(x, baseY - tickHeight),
                    end = Offset(x, baseY),
                    strokeWidth = 1.2.dp.toPx(),
                )
            }
            drawLine(
                color = print.copy(alpha = 0.6f),
                start = Offset(0f, baseY),
                end = Offset(size.width, baseY),
                strokeWidth = 1.5.dp.toPx(),
            )

            // Second (AM) row, printed smaller.
            val amY = size.height * 0.88f
            drawLine(
                color = print.copy(alpha = 0.35f),
                start = Offset(0f, amY),
                end = Offset(size.width, amY),
                strokeWidth = 1.dp.toPx(),
            )
            repeat(ticks) { index ->
                if (index % 5 == 0) {
                    val x = size.width * index / (ticks - 1).toFloat()
                    drawLine(
                        color = print.copy(alpha = 0.4f),
                        start = Offset(x, amY),
                        end = Offset(x, amY - size.height * 0.10f),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
            }

            // Warm backlight and a glass streak.
            drawRect(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, skin.accent.copy(alpha = 0.12f), Color.Transparent),
                ),
            )
            drawRect(
                Brush.linearGradient(
                    colors = listOf(glass, Color.Transparent),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height),
                ),
            )

            val x = size.width * needle.coerceIn(0.03f, 0.97f)
            drawLine(
                color = needleColor,
                start = Offset(x, size.height * 0.05f),
                end = Offset(x, size.height * 0.92f),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawCircle(needleColor, 3.5.dp.toPx(), Offset(x, size.height * 0.92f))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            FM_FREQUENCIES.forEach { frequency ->
                Text(
                    text = frequency.toString(),
                    fontFamily = FontFamily.Serif,
                    fontSize = 9.sp,
                    color = print.copy(alpha = 0.9f),
                )
            }
        }
    }
}

/** Knurled metal-and-bakelite knob with a cast shadow and a printed label. */
@Composable
private fun KnurledKnob(
    label: String,
    value: Float?,
    skin: PlayerSkin,
    onDrag: ((Float) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(KNOB_SIZE)
                .then(
                    if (onDrag != null) {
                        Modifier.pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { position -> onDrag(knobVolume(position, size)) },
                                onDrag = { change, _ ->
                                    change.consume()
                                    onDrag(knobVolume(change.position, size))
                                },
                            )
                        }
                    } else {
                        Modifier
                    },
                )
                .then(
                    if (onClick != null) {
                        Modifier.clickable(enabled = enabled, onClick = onClick)
                    } else {
                        Modifier
                    },
                ),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)
                val alpha = if (enabled) 1f else 0.5f

                drawCircle(
                    Color.Black.copy(alpha = 0.45f * alpha),
                    radius * 0.86f,
                    center + Offset(0f, radius * 0.16f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.55f),
                            skin.outline,
                            skin.cassette.edge,
                        ),
                        center = center - Offset(radius * 0.35f, radius * 0.35f),
                        radius = radius * 1.5f,
                    ),
                    radius = radius * 0.86f,
                    center = center,
                    alpha = alpha,
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(skin.cassette.shellTop, skin.cassette.shellBottom),
                        center = center - Offset(radius * 0.30f, radius * 0.30f),
                        radius = radius * 1.35f,
                    ),
                    radius = radius * 0.70f,
                    center = center,
                    alpha = alpha,
                )

                repeat(KNURL_TEETH) { index ->
                    val radians = Math.toRadians(index * 360.0 / KNURL_TEETH)
                    drawLine(
                        color = skin.cassette.edge.copy(alpha = 0.55f * alpha),
                        start = Offset(
                            center.x + cos(radians).toFloat() * radius * 0.60f,
                            center.y + sin(radians).toFloat() * radius * 0.60f,
                        ),
                        end = Offset(
                            center.x + cos(radians).toFloat() * radius * 0.70f,
                            center.y + sin(radians).toFloat() * radius * 0.70f,
                        ),
                        strokeWidth = 1.6.dp.toPx(),
                    )
                }

                drawCircle(skin.accent.copy(alpha = 0.92f * alpha), radius * 0.26f, center)
                drawCircle(
                    color = skin.cassette.edge.copy(alpha = 0.75f),
                    radius = radius * 0.26f,
                    center = center,
                    style = Stroke(1.dp.toPx()),
                )

                if (value != null) {
                    val radians = Math.toRadians(KNOB_START_DEGREES + value.coerceIn(0f, 1f) * KNOB_SWEEP)
                    drawLine(
                        color = skin.cassette.label.copy(alpha = alpha),
                        start = center,
                        end = Offset(
                            center.x + cos(radians).toFloat() * radius * 0.62f,
                            center.y + sin(radians).toFloat() * radius * 0.62f,
                        ),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }

                drawCircle(
                    Color.White.copy(alpha = 0.10f * alpha),
                    radius * 0.46f,
                    center - Offset(radius * 0.30f, radius * 0.34f),
                )
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            fontFamily = FontFamily.Serif,
            fontSize = 9.sp,
            letterSpacing = 1.5.sp,
            color = skin.cassette.label.copy(alpha = if (enabled) 0.9f else 0.45f),
            maxLines = 1,
        )
    }
}

/** Ivory piano key that dips into brass when active. */
@Composable
private fun PianoKey(
    label: String,
    active: Boolean,
    skin: PlayerSkin,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val faceTop = if (active) skin.accent else skin.cassette.label
    val faceBottom = if (active) skin.accent.copy(alpha = 0.74f) else skin.cassette.label.copy(alpha = 0.80f)
    val textColor = if (active) skin.onAccent else skin.cassette.edge
    val clickModifier = if (onLongClick != null) {
        Modifier.combinedClickable(enabled = enabled, onClick = onClick, onLongClick = onLongClick)
    } else {
        Modifier.clickable(enabled = enabled, onClick = onClick)
    }

    Box(
        modifier = modifier
            .height(KEY_HEIGHT)
            .clip(RoundedCornerShape(4.dp))
            .then(clickModifier)
            .background(Brush.verticalGradient(listOf(faceTop, faceBottom))),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.35f),
                topLeft = Offset(0f, size.height - 3.dp.toPx()),
                size = Size(size.width, 3.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx()),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.28f),
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = Size(size.width - 2.dp.toPx(), size.height * 0.42f),
                cornerRadius = CornerRadius(3.dp.toPx()),
            )
            drawRoundRect(
                color = skin.cassette.edge.copy(alpha = 0.8f),
                cornerRadius = CornerRadius(4.dp.toPx()),
                style = Stroke(1.2.dp.toPx()),
            )
        }
        Text(
            text = label,
            fontFamily = FontFamily.Serif,
            fontSize = 9.sp,
            letterSpacing = 0.5.sp,
            color = textColor.copy(alpha = if (enabled) 1f else 0.5f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

/** Volume follows the finger around the dial; the gap at the bottom snaps to an end. */
private fun knobVolume(position: Offset, size: IntSize): Float {
    val degrees = Math.toDegrees(
        atan2(
            (position.y - size.height / 2f).toDouble(),
            (position.x - size.width / 2f).toDouble(),
        ),
    )
    val angle = if (degrees < KNOB_START_DEGREES) degrees + 360.0 else degrees
    val end = KNOB_START_DEGREES + KNOB_SWEEP
    return when {
        angle <= KNOB_START_DEGREES -> 0f
        angle <= end -> ((angle - KNOB_START_DEGREES) / KNOB_SWEEP).toFloat()
        angle < end + (360.0 - KNOB_SWEEP) / 2.0 -> 1f
        else -> 0f
    }
}

/** Stable pseudo-frequency for the needle, derived from the station id. */
private fun radioNeedle(seed: String?): Float {
    if (seed.isNullOrBlank()) return 0.5f
    val bucket = (seed.hashCode().toLong() and 0x7FFFFFFFL) % 100L
    return 0.06f + bucket / 100f * 0.88f
}

private val FM_FREQUENCIES = listOf(88, 92, 96, 100, 104, 108)

private val KNOB_SIZE = 62.dp
private val KEY_HEIGHT = 34.dp
private val CABINET_CORNER = 10.dp
private const val GRILLE_WEIGHT = 1.05f
private const val CONTROL_WEIGHT = 1.0f
private const val KNURL_TEETH = 28
private const val KNOB_SWEEP = 270.0
private const val KNOB_START_DEGREES = 135.0
