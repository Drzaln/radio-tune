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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import com.rizal.radiotune.ui.theme.CassetteLook
import kotlin.math.cos
import kotlin.math.sin

/**
 * Compact Cassette drawn with Canvas — no assets. The reels spin while [playing],
 * slow down while buffering and freeze in place when paused.
 *
 * Everything visual comes from [look], so a [com.rizal.radiotune.ui.theme.PlayerStyle]
 * can reshape the cassette without touching this composable.
 */
@Composable
fun CassettePlayer(
    look: CassetteLook,
    playing: Boolean,
    buffering: Boolean,
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    width: Dp = 240.dp,
    elevation: Dp = 8.dp,
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

    val height = width * CASSETTE_ASPECT
    val shape = RoundedCornerShape(height * 0.075f * look.cornerScale)
    val sized = modifier.size(width = width, height = height)
    val boxModifier = if (elevation > 0.dp) sized.shadow(elevation, shape, clip = false) else sized

    BoxWithConstraints(boxModifier) {
        // Room for the hard offset shadow, so it is not clipped by the canvas.
        val room = if (look.hardShadow != null) maxHeight * 0.08f else 0.dp
        val shellWidth = maxWidth - room
        val shellHeight = maxHeight - room

        Canvas(Modifier.fillMaxSize()) {
            val w = shellWidth.toPx()
            val h = shellHeight.toPx()
            val corner = h * 0.075f * look.cornerScale
            val stroke = h * 0.010f * look.strokeScale

            look.hardShadow?.let { shadowColor ->
                drawRoundRect(
                    color = shadowColor,
                    topLeft = Offset(h * 0.05f, h * 0.06f),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(corner, corner),
                )
            }

            if (look.useGradient) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(look.shellTop, look.shellBottom),
                        startY = 0f,
                        endY = h,
                    ),
                    cornerRadius = CornerRadius(corner, corner),
                )
            } else {
                drawRoundRect(
                    color = look.shellTop,
                    cornerRadius = CornerRadius(corner, corner),
                )
            }
            drawRoundRect(
                color = look.edge,
                cornerRadius = CornerRadius(corner, corner),
                style = Stroke(width = stroke),
            )

            if (look.detailed) {
                // Write-protect notches.
                listOf(0.10f, 0.845f).forEach { x ->
                    drawRoundRect(
                        color = look.recess,
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
                    drawCircle(look.shellBottom, screwRadius, center)
                    drawCircle(look.edge, screwRadius, center, style = Stroke(width = stroke))
                    drawLine(
                        color = look.line,
                        start = Offset(center.x - screwRadius * 0.55f, center.y),
                        end = Offset(center.x + screwRadius * 0.55f, center.y),
                        strokeWidth = screwRadius * 0.28f,
                        cap = StrokeCap.Round,
                    )
                }
            }

            // Label sticker. Artwork is composited over this rect.
            val labelLeft = w * 0.07f
            val labelTop = h * 0.085f
            val labelWidth = w * 0.86f
            val labelHeight = h * 0.40f
            val labelCorner = h * 0.03f * look.cornerScale
            drawRoundRect(
                color = look.label,
                topLeft = Offset(labelLeft, labelTop),
                size = Size(labelWidth, labelHeight),
                cornerRadius = CornerRadius(labelCorner, labelCorner),
            )
            if (look.detailed) {
                drawRoundRect(
                    color = look.edge,
                    topLeft = Offset(labelLeft, labelTop),
                    size = Size(labelWidth, labelHeight),
                    cornerRadius = CornerRadius(labelCorner, labelCorner),
                    style = Stroke(width = stroke),
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
                            colors = listOf(look.stripe, look.stripe.copy(alpha = 0.72f)),
                        ),
                        topLeft = Offset(labelLeft, labelTop),
                        size = Size(labelWidth, h * 0.05f),
                    )
                }

                // Ruled writing lines, visible when there is no artwork.
                repeat(2) { index ->
                    val y = h * (0.27f + index * 0.075f)
                    drawLine(
                        color = look.line,
                        start = Offset(w * 0.12f, y),
                        end = Offset(w * 0.88f, y),
                        strokeWidth = stroke * 0.8f,
                    )
                }

                // Tape window.
                drawRoundRect(
                    color = look.recess,
                    topLeft = Offset(w * 0.22f, h * 0.79f),
                    size = Size(w * 0.56f, h * 0.145f),
                    cornerRadius = CornerRadius(h * 0.03f, h * 0.03f),
                )
                drawRoundRect(
                    color = look.edge,
                    topLeft = Offset(w * 0.22f, h * 0.79f),
                    size = Size(w * 0.56f, h * 0.145f),
                    cornerRadius = CornerRadius(h * 0.03f, h * 0.03f),
                    style = Stroke(width = stroke),
                )
            }

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
                color = look.tape,
                style = Stroke(
                    width = h * 0.028f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )

            val a = angle.value
            val spokes = if (look.detailed) 6 else 3
            drawReel(left, spoolRadius, a, look, stroke, spokes, tapePackScale = 0.70f)
            drawReel(right, spoolRadius, a * TAKE_UP_RATIO, look, stroke, spokes, tapePackScale = 0.58f)

            if (look.detailed) {
                // Pinch roller cut-outs either side of the head opening.
                listOf(0.36f, 0.64f).forEach { x ->
                    drawCircle(look.recess, h * 0.024f, Offset(w * x, h * 0.905f))
                }
            }
        }

        if (!artworkUrl.isNullOrBlank()) {
            val artTop = if (look.detailed) 0.135f else 0.085f
            val artHeight = if (look.detailed) 0.35f else 0.40f
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .offset(x = shellWidth * 0.07f, y = shellHeight * artTop)
                    .size(width = shellWidth * 0.86f, height = shellHeight * artHeight)
                    .clip(
                        RoundedCornerShape(
                            topStart = if (look.detailed) 0.dp else shellHeight * 0.03f * look.cornerScale,
                            topEnd = if (look.detailed) 0.dp else shellHeight * 0.03f * look.cornerScale,
                            bottomEnd = shellHeight * 0.03f * look.cornerScale,
                            bottomStart = shellHeight * 0.03f * look.cornerScale,
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
    look: CassetteLook,
    stroke: Float,
    spokes: Int,
    tapePackScale: Float,
) {
    drawCircle(look.recess, radius, center)
    drawCircle(look.edge, radius, center, style = Stroke(width = stroke))
    drawCircle(
        color = look.tape,
        radius = radius * tapePackScale,
        center = center,
        style = Stroke(width = radius * (tapePackScale - 0.30f)),
    )
    drawCircle(look.hub, radius * 0.32f, center)

    rotate(angleDegrees, center) {
        repeat(spokes) { index ->
            val radians = Math.toRadians(index * (360.0 / spokes))
            val inner = radius * 0.09f
            val outer = radius * 0.26f
            drawLine(
                color = look.spoke,
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
