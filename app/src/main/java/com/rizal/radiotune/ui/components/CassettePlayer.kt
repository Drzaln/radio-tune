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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlin.math.cos
import kotlin.math.sin

/**
 * Classic Compact Cassette drawn with Canvas — no assets. The two reels spin
 * while [playing], slow down while buffering and freeze in place when paused.
 * Station artwork fills the label sticker when available.
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

    val shellColor = MaterialTheme.colorScheme.surfaceVariant
    val edgeColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.surface
    val recessColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.16f)
    val tapeColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    val hubColor = if (playing) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    }
    val spokeColor = MaterialTheme.colorScheme.surface

    BoxWithConstraints(
        modifier = modifier.size(width = width, height = width * CASSETTE_ASPECT),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val hairline = h * 0.009f
            val shellCorner = h * 0.075f

            drawRoundRect(
                color = shellColor,
                cornerRadius = CornerRadius(shellCorner, shellCorner),
            )
            drawRoundRect(
                color = edgeColor,
                cornerRadius = CornerRadius(shellCorner, shellCorner),
                style = Stroke(width = hairline),
            )

            // Label sticker. Artwork is composited over this exact rect.
            val labelLeft = w * 0.07f
            val labelTop = h * 0.085f
            val labelWidth = w * 0.86f
            val labelHeight = h * 0.40f
            val labelCorner = h * 0.03f
            drawRoundRect(
                color = labelColor,
                topLeft = Offset(labelLeft, labelTop),
                size = Size(labelWidth, labelHeight),
                cornerRadius = CornerRadius(labelCorner, labelCorner),
            )
            drawRoundRect(
                color = edgeColor,
                topLeft = Offset(labelLeft, labelTop),
                size = Size(labelWidth, labelHeight),
                cornerRadius = CornerRadius(labelCorner, labelCorner),
                style = Stroke(width = hairline),
            )

            val spoolY = h * 0.665f
            val spoolRadius = h * 0.145f
            val left = Offset(w * 0.30f, spoolY)
            val right = Offset(w * 0.70f, spoolY)

            // Tape path running under both reels.
            val tapePath = Path().apply {
                moveTo(left.x, left.y)
                lineTo(left.x, h * 0.86f)
                lineTo(right.x, h * 0.86f)
                lineTo(right.x, right.y)
            }
            drawPath(
                path = tapePath,
                color = tapeColor,
                style = Stroke(
                    width = h * 0.030f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )

            val currentAngle = angle.value
            drawReel(left, spoolRadius, currentAngle, recessColor, edgeColor, tapeColor, hubColor, spokeColor)
            drawReel(
                right,
                spoolRadius,
                currentAngle * TAKE_UP_RATIO,
                recessColor,
                edgeColor,
                tapeColor,
                hubColor,
                spokeColor,
            )

            // Head opening along the bottom edge.
            drawRoundRect(
                color = recessColor,
                topLeft = Offset(w * 0.42f, h * 0.90f),
                size = Size(w * 0.16f, h * 0.06f),
                cornerRadius = CornerRadius(h * 0.02f, h * 0.02f),
            )
        }

        if (!artworkUrl.isNullOrBlank()) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = maxHeight * 0.085f)
                    .size(width = maxWidth * 0.86f, height = maxHeight * 0.40f)
                    .clip(RoundedCornerShape(maxHeight * 0.03f)),
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
) {
    drawCircle(recessColor, radius, center)
    drawCircle(edgeColor, radius, center, style = Stroke(width = radius * 0.06f))
    drawCircle(tapeColor, radius * 0.68f, center, style = Stroke(width = radius * 0.26f))
    drawCircle(hubColor, radius * 0.34f, center)

    rotate(angleDegrees, center) {
        repeat(3) { index ->
            val radians = Math.toRadians((index * 120).toDouble())
            val inner = radius * 0.10f
            val outer = radius * 0.28f
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
                strokeWidth = radius * 0.11f,
                cap = StrokeCap.Round,
            )
        }
    }
}

private const val CASSETTE_ASPECT = 0.63f
private const val PLAYING_PERIOD_MS = 1600
private const val BUFFERING_PERIOD_MS = 5200
private const val TAKE_UP_RATIO = 1.55f
