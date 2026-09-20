package com.rizal.radiotune.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlin.math.cos
import kotlin.math.sin

/**
 * Retro Compact Cassette drawn with Canvas — no assets. The reels spin while
 * [playing], slow down while buffering and freeze in place when paused. Station
 * artwork fills the label sticker under the brand stripe.
 */
@Composable
fun CassettePlayer(
    playing: Boolean,
    buffering: Boolean,
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    width: Dp = 240.dp,
) {
    val angle = remember { Animatable(0f) }

    LaunchedEffect(playing, buffering) {
        val periodMs = when {
            playing -> PLAYING_PERIOD_MS
            buffering -> BUFFERING_PERIOD_MS
            else -> null
        } ?: return@LaunchedEffect

        while (true) {
            angle.animateTo(
                targetValue = angle.value + 360f,
                animationSpec = tween(periodMs, easing = LinearEasing),
            )
        }
    }

    val colors = MaterialTheme.colorScheme
    val shellTop = colors.surfaceContainerHighest
    val shellBottom = colors.surfaceContainerHigh
    val edge = colors.outlineVariant
    val label = colors.surface
    val stripe = colors.primary
    val recess = colors.onSurfaceVariant.copy(alpha = 0.18f)
    val tape = colors.onSurfaceVariant.copy(alpha = 0.55f)
    val line = colors.onSurfaceVariant.copy(alpha = 0.30f)
    val hub = if (playing) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.55f)
    val spoke = colors.surface

    val shellCorner = width * CASSETTE_ASPECT * 0.075f

    BoxWithConstraints(
        modifier = modifier
            .size(width = width, height = width * CASSETTE_ASPECT)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(shellCorner), clip = false),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val shellCornerPx = h * 0.075f
            val hairline = h * 0.010f

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(shellTop, shellBottom),
                    startY = 0f,
                    endY = h,
                ),
                cornerRadius = CornerRadius(shellCornerPx, shellCornerPx),
            )
            drawRoundRect(
                color = edge,
                cornerRadius = CornerRadius(shellCornerPx, shellCornerPx),
                style = Stroke(width = hairline),
            )

            // Write-protect notches inside the top edge.
            listOf(0.10f, 0.845f).forEach { x ->
                drawRoundRect(
                    color = recess,
                    topLeft = Offset(w * x, h * 0.025f),
                    size = Size(w * 0.055f, h * 0.045f),
                    cornerRadius = CornerRadius(h * 0.012f, h * 0.012f),
                )
            }

            // Corner screws.
            val screwRadius = h * 0.021f
            listOf(
                Offset(w * 0.06f, h * 0.075f),
                Offset(w * 0.94f, h * 0.075f),
                Offset(w * 0.06f, h * 0.925f),
                Offset(w * 0.94f, h * 0.925f),
            ).forEach { center ->
                drawCircle(shellBottom, screwRadius, center)
                drawCircle(edge, screwRadius, center, style = Stroke(width = hairline))
                drawLine(
                    color = line,
                    start = Offset(center.x - screwRadius * 0.55f, center.y),
                    end = Offset(center.x + screwRadius * 0.55f, center.y),
                    strokeWidth = screwRadius * 0.28f,
                    cap = StrokeCap.Round,
                )
            }

            // Label sticker with a brand stripe across the top.
            val labelLeft = w * 0.07f
            val labelTop = h * 0.085f
            val labelWidth = w * 0.86f
            val labelHeight = h * 0.40f
            val labelCorner = h * 0.03f
            drawRoundRect(
                color = label,
                topLeft = Offset(labelLeft, labelTop),
                size = Size(labelWidth, labelHeight),
                cornerRadius = CornerRadius(labelCorner, labelCorner),
            )
            drawRoundRect(
                color = edge,
                topLeft = Offset(labelLeft, labelTop),
                size = Size(labelWidth, labelHeight),
                cornerRadius = CornerRadius(labelCorner, labelCorner),
                style = Stroke(width = hairline),
            )
            val labelClip = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(labelLeft, labelTop, labelLeft + labelWidth, labelTop + labelHeight),
                        cornerRadius = CornerRadius(labelCorner, labelCorner),
                    ),
                )
            }
            clipPath(labelClip) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(stripe, stripe.copy(alpha = 0.72f)),
                    ),
                    topLeft = Offset(labelLeft, labelTop),
                    size = Size(labelWidth, h * 0.05f),
                )
            }

            // Ruled writing lines, visible when there is no artwork.
            repeat(2) { index ->
                val y = h * (0.27f + index * 0.075f)
                drawLine(
                    color = line,
                    start = Offset(w * 0.12f, y),
                    end = Offset(w * 0.88f, y),
                    strokeWidth = hairline,
                )
            }

            // Tape window.
            drawRoundRect(
                color = recess,
                topLeft = Offset(w * 0.22f, h * 0.79f),
                size = Size(w * 0.56f, h * 0.145f),
                cornerRadius = CornerRadius(h * 0.03f, h * 0.03f),
            )
            drawRoundRect(
                color = edge,
                topLeft = Offset(w * 0.22f, h * 0.79f),
                size = Size(w * 0.56f, h * 0.145f),
                cornerRadius = CornerRadius(h * 0.03f, h * 0.03f),
                style = Stroke(width = hairline),
            )

            val spoolY = h * 0.665f
            val spoolRadius = h * 0.145f
            val left = Offset(w * 0.30f, spoolY)
            val right = Offset(w * 0.70f, spoolY)

            val tapePath = Path().apply {
                moveTo(left.x, left.y)
                lineTo(left.x, h * 0.865f)
                lineTo(right.x, h * 0.865f)
                lineTo(right.x, right.y)
            }
            drawPath(
                path = tapePath,
                color = tape,
                style = Stroke(
                    width = h * 0.028f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )

            val a = angle.value
            drawReel(left, spoolRadius, a, recess, edge, tape, hub, spoke, 0.70f)
            drawReel(right, spoolRadius, a * TAKE_UP_RATIO, recess, edge, tape, hub, spoke, 0.58f)

            // Pinch roller cut-outs either side of the head opening.
            listOf(0.36f, 0.64f).forEach { x ->
                drawCircle(recess, h * 0.024f, Offset(w * x, h * 0.905f))
            }
        }

        if (!artworkUrl.isNullOrBlank()) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = maxHeight * 0.135f)
                    .size(width = maxWidth * 0.86f, height = maxHeight * 0.35f)
                    .clip(
                        RoundedCornerShape(
                            topStart = 0.dp,
                            topEnd = 0.dp,
                            bottomEnd = maxHeight * 0.03f,
                            bottomStart = maxHeight * 0.03f,
                        ),
                    ),
            )
        }
    }
}

private fun DrawScope.drawReel(
    center: Offset,
    radius: Float,
    angleDegrees: Float,
    recessColor: Color,
    edgeColor: Color,
    tapeColor: Color,
    hubColor: Color,
    spokeColor: Color,
    tapePackScale: Float,
) {
    drawCircle(recessColor, radius, center)
    drawCircle(edgeColor, radius, center, style = Stroke(width = radius * 0.06f))
    drawCircle(
        color = tapeColor,
        radius = radius * tapePackScale,
        center = center,
        style = Stroke(width = radius * (tapePackScale - 0.30f)),
    )
    drawCircle(hubColor, radius * 0.32f, center)

    // Classic six-slot hub.
    rotate(angleDegrees, center) {
        repeat(6) { index ->
            val radians = Math.toRadians((index * 60).toDouble())
            val inner = radius * 0.09f
            val outer = radius * 0.26f
            drawLine(
                color = spokeColor,
                start = Offset(
                    center.x + cos(radians).toFloat() * inner,
                    center.y + sin(radians).toFloat() * inner,
                ),
                end = Offset(
                    center.x + cos(radians).toFloat() * outer,
                    center.y + sin(radians).toFloat() * outer,
                ),
                strokeWidth = radius * 0.075f,
                cap = StrokeCap.Round,
            )
        }
    }
}

private const val CASSETTE_ASPECT = 0.63f
private const val PLAYING_PERIOD_MS = 1600
private const val BUFFERING_PERIOD_MS = 5200
private const val TAKE_UP_RATIO = 1.55f
