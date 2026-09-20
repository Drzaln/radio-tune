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
import androidx.compose.ui.graphics.StrokeCap
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
 * Flat-retro cassette whose reels spin while [playing]. The station artwork fills
 * the label area when available, otherwise the drawn label shows through.
 *
 * The reels are driven by an [Animatable] (not an infinite transition) so that
 * pausing freezes them at their current angle instead of snapping back.
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
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    val tapeColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
    val screwColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
    val hubColor = if (playing) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
    }
    val spokeColor = MaterialTheme.colorScheme.surface

    BoxWithConstraints(
        modifier = modifier.size(width = width, height = width * CASSETTE_ASPECT),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val corner = h * 0.10f

            drawRoundRect(color = shellColor, cornerRadius = CornerRadius(corner, corner))
            drawRoundRect(
                color = edgeColor,
                cornerRadius = CornerRadius(corner, corner),
                style = Stroke(width = h * 0.014f),
            )

            drawRoundRect(
                color = labelColor,
                topLeft = Offset(w * 0.09f, h * 0.10f),
                size = Size(w * 0.82f, h * 0.38f),
                cornerRadius = CornerRadius(h * 0.035f, h * 0.035f),
            )

            val hubY = h * 0.70f
            val hubRadius = h * 0.15f
            val left = Offset(w * 0.34f, hubY)
            val right = Offset(w * 0.66f, hubY)

            drawRoundRect(
                color = tapeColor,
                topLeft = Offset(left.x, hubY - h * 0.025f),
                size = Size(right.x - left.x, h * 0.05f),
                cornerRadius = CornerRadius(h * 0.025f, h * 0.025f),
            )

            val currentAngle = angle.value
            drawReel(left, hubRadius, currentAngle, tapeColor, hubColor, spokeColor)
            drawReel(right, hubRadius, currentAngle * TAKE_UP_RATIO, tapeColor, hubColor, spokeColor)

            val screwRadius = h * 0.018f
            listOf(
                Offset(w * 0.055f, h * 0.09f),
                Offset(w * 0.945f, h * 0.09f),
                Offset(w * 0.055f, h * 0.91f),
                Offset(w * 0.945f, h * 0.91f),
            ).forEach { drawCircle(screwColor, screwRadius, it) }
        }

        if (!artworkUrl.isNullOrBlank()) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = maxHeight * 0.10f)
                    .size(width = maxWidth * 0.82f, height = maxHeight * 0.38f)
                    .clip(RoundedCornerShape(maxHeight * 0.035f)),
            )
        }
    }
}

private fun DrawScope.drawReel(
    center: Offset,
    radius: Float,
    angleDegrees: Float,
    tapeColor: Color,
    hubColor: Color,
    spokeColor: Color,
) {
    drawCircle(tapeColor, radius, center)
    drawCircle(hubColor, radius * 0.42f, center)
    rotate(angleDegrees, center) {
        repeat(3) { index ->
            val radians = Math.toRadians((index * 120).toDouble())
            val inner = radius * 0.12f
            val outer = radius * 0.36f
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
                strokeWidth = radius * 0.13f,
                cap = StrokeCap.Round,
            )
        }
    }
}

private const val CASSETTE_ASPECT = 0.63f
private const val PLAYING_PERIOD_MS = 1600
private const val BUFFERING_PERIOD_MS = 5200
private const val TAKE_UP_RATIO = 1.55f
