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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.rizal.radiotune.ui.theme.CassetteLook
import com.rizal.radiotune.ui.theme.LabelBand
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Compact Cassette drawn with Canvas — no assets. The reels spin while [playing],
 * slow down while buffering and freeze in place when paused.
 *
 * Everything visual comes from [look], so a [com.rizal.radiotune.ui.theme.PlayerStyle]
 * can reshape the cassette — geometry, palette and finishes — without touching
 * this composable.
 *
 * The shell is a static Canvas and only the reels layer reads the animated angle,
 * so heavy shading is not re-rendered on every frame.
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
            drawShell(look, shellWidth.toPx(), shellHeight.toPx())
        }

        Canvas(Modifier.fillMaxSize()) {
            drawReels(angle.value, look, shellWidth.toPx(), shellHeight.toPx())
        }

        // Printed labels carry their own design, so artwork only goes on plain ones.
        if (!artworkUrl.isNullOrBlank() && look.labelBands.isEmpty()) {
            val stripeOffset = if (look.showStripe) STRIPE_HEIGHT else 0f
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .offset(x = shellWidth * LABEL_LEFT, y = shellHeight * (look.labelTop + stripeOffset))
                    .size(
                        width = shellWidth * LABEL_WIDTH,
                        height = shellHeight * (look.labelHeight - stripeOffset),
                    )
                    .clip(
                        RoundedCornerShape(
                            topStart = if (look.showStripe) 0.dp else shellHeight * LABEL_CORNER * look.cornerScale,
                            topEnd = if (look.showStripe) 0.dp else shellHeight * LABEL_CORNER * look.cornerScale,
                            bottomEnd = shellHeight * LABEL_CORNER * look.cornerScale,
                            bottomStart = shellHeight * LABEL_CORNER * look.cornerScale,
                        ),
                    ),
            )
        }
    }
}

private fun DrawScope.drawShell(look: CassetteLook, w: Float, h: Float) {
    val corner = h * 0.075f * look.cornerScale
    val stroke = h * 0.010f * look.strokeScale
    val shellPath = Path().apply {
        addRoundRect(RoundRect(Rect(0f, 0f, w, h), CornerRadius(corner, corner)))
    }

    look.hardShadow?.let { shadowColor ->
        drawRoundRect(
            color = shadowColor,
            topLeft = Offset(h * 0.05f, h * 0.06f),
            size = Size(w, h),
            cornerRadius = CornerRadius(corner, corner),
        )
    }

    if (look.steppedShell) {
        drawSteppedShell(look, w, h, corner, stroke)
    } else {
        if (look.useGradient) {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOfNotNull(look.shellTop, look.shellMid, look.shellBottom),
                    startY = 0f,
                    endY = h,
                ),
                cornerRadius = CornerRadius(corner, corner),
            )
        } else {
            drawRoundRect(color = look.shellTop, cornerRadius = CornerRadius(corner, corner))
        }
        drawRoundRect(
            color = look.edge,
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = stroke),
        )

        // Moulded inner lip.
        look.bevel?.let { bevel ->
            val inset = stroke * 1.4f
            drawRoundRect(
                color = bevel,
                topLeft = Offset(inset, inset),
                size = Size(w - inset * 2f, h - inset * 2f),
                cornerRadius = CornerRadius(
                    (corner - inset).coerceAtLeast(0f),
                    (corner - inset).coerceAtLeast(0f),
                ),
                style = Stroke(width = stroke),
            )
        }
    }

    look.ribs?.let { ribs -> clipPath(shellPath) { drawRibs(ribs, w, h, stroke) } }

    // Specular streak across the plastic.
    look.sheen?.let { sheen ->
        clipPath(shellPath) {
            rotate(-18f, Offset(w / 2f, h / 2f)) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, sheen, Color.Transparent),
                        startX = -w * 0.2f,
                        endX = w,
                    ),
                    topLeft = Offset(-w * 0.4f, -h),
                    size = Size(w * 1.8f, h * 3f),
                )
            }
        }
    }

    // Broad highlight where the light falls across the moulding.
    look.specular?.let { spec ->
        clipPath(shellPath) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(spec, Color.Transparent),
                    center = Offset(w * 0.30f, h * 0.24f),
                    radius = w * 0.42f,
                ),
                radius = w * 0.42f,
                center = Offset(w * 0.30f, h * 0.24f),
            )
        }
    }

    if (look.showNotches) {
        listOf(0.10f, 0.845f).forEach { x ->
            drawRoundRect(
                color = look.recess,
                topLeft = Offset(w * x, h * 0.025f),
                size = Size(w * 0.055f, h * 0.045f),
                cornerRadius = CornerRadius(h * 0.012f, h * 0.012f),
            )
        }
    }

    if (look.showScrews) {
        val screwRadius = h * 0.021f
        listOf(
            Offset(w * 0.06f, h * 0.075f),
            Offset(w * 0.94f, h * 0.075f),
            Offset(w * 0.06f, h * 0.925f),
            Offset(w * 0.94f, h * 0.925f),
        ).forEach { center -> drawScrew(center, screwRadius, look, stroke) }
    }

    // Label sticker. Artwork is composited over this rect.
    val labelLeft = w * LABEL_LEFT
    val labelWidth = w * LABEL_WIDTH
    val labelTop = h * look.labelTop
    val labelHeight = h * look.labelHeight
    val labelCorner = h * LABEL_CORNER * look.cornerScale
    val labelPath = buildLabelPath(
        left = labelLeft,
        top = labelTop,
        width = labelWidth,
        height = labelHeight,
        corner = labelCorner,
        chamfer = w * look.labelChamfer,
    )

    // Contact shadow so the sticker sits on the shell.
    look.occlusion?.let { ao ->
        drawPath(path = labelPath, color = ao, style = Stroke(width = stroke * 2.6f))
    }

    if (look.labelBands.isEmpty()) {
        drawPath(path = labelPath, color = look.label)
    } else {
        clipPath(labelPath) {
            drawBands(look.labelBands, labelLeft, labelTop, labelWidth, labelHeight)
        }
    }

    if (look.showLabelOutline) {
        drawPath(path = labelPath, color = look.edge, style = Stroke(width = stroke))
    }

    if (look.showStripe) {
        clipPath(labelPath) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(look.stripe, look.stripe.copy(alpha = 0.72f)),
                ),
                topLeft = Offset(labelLeft, labelTop),
                size = Size(labelWidth, h * STRIPE_HEIGHT),
            )
        }
    }

    if (look.showRuledLines) {
        repeat(2) { index ->
            val y = labelTop + labelHeight * (0.46f + index * 0.20f)
            drawLine(
                color = look.line,
                start = Offset(w * 0.12f, y),
                end = Offset(w * 0.88f, y),
                strokeWidth = stroke * 0.8f,
            )
        }
    }

    look.speckle?.let { speckle ->
        clipPath(labelPath) {
            drawSpeckle(speckle, labelLeft, labelTop, labelWidth, labelHeight, h)
        }
    }

    val windowLeft = (w - w * look.windowWidth) / 2f
    val windowTop = h * look.windowTop
    val windowW = w * look.windowWidth
    val windowH = h * look.windowHeight
    val windowCorner = h * 0.03f

    if (look.showTapeWindow) {
        // Tape window opening.
        drawRoundRect(
            color = look.recess,
            topLeft = Offset(windowLeft, windowTop),
            size = Size(windowW, windowH),
            cornerRadius = CornerRadius(windowCorner, windowCorner),
        )
        drawRoundRect(
            color = look.edge,
            topLeft = Offset(windowLeft, windowTop),
            size = Size(windowW, windowH),
            cornerRadius = CornerRadius(windowCorner, windowCorner),
            style = Stroke(width = stroke),
        )

        // Inner shadow around the opening.
        look.occlusion?.let { ao ->
            drawRoundRect(
                color = ao,
                topLeft = Offset(windowLeft, windowTop),
                size = Size(windowW, windowH),
                cornerRadius = CornerRadius(windowCorner, windowCorner),
                style = Stroke(width = stroke * 2.4f),
            )
        }
    }

    // Tape running under both reels. Drawn after the window opening so it shows
    // through it instead of being painted over by the recess.
    val spoolY = h * look.spoolY
    val tapeY = if (look.showTapeWindow) windowTop + windowH * 0.5f else h * 0.865f
    val tapePath = Path().apply {
        moveTo(w * SPOOL_LEFT, spoolY)
        lineTo(w * SPOOL_LEFT, tapeY)
        lineTo(w * SPOOL_RIGHT, tapeY)
        lineTo(w * SPOOL_RIGHT, spoolY)
    }
    drawPath(
        path = tapePath,
        color = look.tape,
        style = Stroke(width = h * 0.028f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )

    if (look.showTapeWindow) {
        // Reflection across the glass.
        look.glass?.let { glass ->
            val windowClip = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(windowLeft, windowTop, windowLeft + windowW, windowTop + windowH),
                        cornerRadius = CornerRadius(windowCorner, windowCorner),
                    ),
                )
            }
            clipPath(windowClip) {
                rotate(-22f, Offset(w / 2f, windowTop + windowH / 2f)) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, glass, Color.Transparent),
                            startX = windowLeft,
                            endX = windowLeft + windowW,
                        ),
                        topLeft = Offset(windowLeft - w * 0.2f, windowTop - h * 0.1f),
                        size = Size(windowW + w * 0.4f, windowH + h * 0.2f),
                    )
                }
            }
        }
    }

    if (look.showPinchRollers) {
        listOf(0.36f, 0.64f).forEach { x ->
            drawCircle(look.recess, h * 0.024f, Offset(w * x, h * 0.905f))
        }
    }

    if (look.bottomPlate) {
        drawBottomPlate(look, w, h, stroke)
    }
}

/**
 * Moulded shell lit from the top-left: body, beveled rim and a raised face plate.
 * This is what makes the object read as thick plastic instead of a flat rectangle.
 */
private fun DrawScope.drawSteppedShell(
    look: CassetteLook,
    w: Float,
    h: Float,
    corner: Float,
    stroke: Float,
) {
    val inset = h * 0.045f
    val faceCorner = (corner - inset).coerceAtLeast(0f)
    val light = look.bevel ?: Color.White
    val shade = look.occlusion ?: look.edge

    // Body.
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOfNotNull(look.shellTop, look.shellMid, look.shellBottom),
            start = Offset(0f, 0f),
            end = Offset(w, h),
        ),
        cornerRadius = CornerRadius(corner, corner),
    )

    // Beveled rim: lit top-left, shaded bottom-right.
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(light, Color.Transparent, shade),
            start = Offset(0f, 0f),
            end = Offset(w, h),
        ),
        cornerRadius = CornerRadius(corner, corner),
        style = Stroke(width = stroke * 1.8f),
    )

    // Raised face plate.
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                lerp(look.shellTop, Color.White, 0.12f),
                look.shellMid ?: look.shellTop,
                lerp(look.shellBottom, Color.Black, 0.10f),
            ),
            start = Offset(inset, inset),
            end = Offset(w - inset, h - inset),
        ),
        topLeft = Offset(inset, inset),
        size = Size(w - inset * 2f, h - inset * 2f),
        cornerRadius = CornerRadius(faceCorner, faceCorner),
    )

    // Step edge: highlight above, shadow below.
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(light, Color.Transparent, shade),
            start = Offset(inset, inset),
            end = Offset(w - inset, h - inset),
        ),
        topLeft = Offset(inset, inset),
        size = Size(w - inset * 2f, h - inset * 2f),
        cornerRadius = CornerRadius(faceCorner, faceCorner),
        style = Stroke(width = stroke * 0.9f),
    )
}

private fun buildLabelPath(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    corner: Float,
    chamfer: Float,
): Path = Path().apply {
    if (chamfer > 0f) {
        moveTo(left + chamfer, top)
        lineTo(left + width - chamfer, top)
        lineTo(left + width, top + chamfer)
        lineTo(left + width, top + height)
        lineTo(left, top + height)
        lineTo(left, top + chamfer)
        close()
    } else {
        addRoundRect(
            RoundRect(
                rect = Rect(left, top, left + width, top + height),
                cornerRadius = CornerRadius(corner, corner),
            ),
        )
    }
}

private fun DrawScope.drawBands(
    bands: List<LabelBand>,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
) {
    val total = bands.sumOf { it.weight.toDouble() }.toFloat()
    if (total <= 0f) return

    var y = top
    bands.forEach { band ->
        val bandHeight = height * (band.weight / total)
        drawRect(
            color = band.color,
            topLeft = Offset(left, y),
            size = Size(width, bandHeight + 1f),
        )
        y += bandHeight
    }
}

private fun DrawScope.drawRibs(color: Color, w: Float, h: Float, stroke: Float) {
    val margin = w * 0.06f
    val spacing = h * 0.022f
    var x = spacing
    while (x < margin) {
        drawLine(color, Offset(x, h * 0.10f), Offset(x, h * 0.90f), strokeWidth = stroke * 0.9f)
        drawLine(color, Offset(w - x, h * 0.10f), Offset(w - x, h * 0.90f), strokeWidth = stroke * 0.9f)
        x += spacing
    }
    var bx = margin
    while (bx < w - margin) {
        drawLine(color, Offset(bx, h * 0.965f), Offset(bx, h * 0.995f), strokeWidth = stroke * 0.9f)
        bx += spacing * 0.8f
    }
}

private fun DrawScope.drawSpeckle(
    color: Color,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    h: Float,
) {
    val random = Random(0x5EED)
    repeat(170) {
        drawCircle(
            color = color,
            radius = h * (0.0015f + random.nextFloat() * 0.0035f),
            center = Offset(
                left + random.nextFloat() * width,
                top + random.nextFloat() * height,
            ),
        )
    }
}

private fun DrawScope.drawScrew(center: Offset, radius: Float, look: CassetteLook, stroke: Float) {
    drawCircle(
        color = look.occlusion
            ?.let { lerp(look.shellBottom, Color.White, 0.20f) }
            ?: look.shellBottom,
        radius = radius,
        center = center,
    )
    drawCircle(look.edge, radius, center, style = Stroke(width = stroke))

    val slot = radius * 0.55f
    val slotWidth = radius * 0.28f
    drawLine(
        color = look.line,
        start = Offset(center.x - slot, center.y),
        end = Offset(center.x + slot, center.y),
        strokeWidth = slotWidth,
        cap = StrokeCap.Round,
    )
    if (look.crossScrews) {
        drawLine(
            color = look.line,
            start = Offset(center.x, center.y - slot),
            end = Offset(center.x, center.y + slot),
            strokeWidth = slotWidth,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawBottomPlate(look: CassetteLook, w: Float, h: Float, stroke: Float) {
    val top = h * 0.80f
    val bottom = h * 0.95f
    val inset = w * 0.19f
    val shoulder = w * 0.055f
    val path = Path().apply {
        moveTo(inset + shoulder, top)
        lineTo(w - inset - shoulder, top)
        lineTo(w - inset, bottom)
        lineTo(inset, bottom)
        close()
    }
    drawPath(path, look.shellMid ?: look.shellTop)
    drawPath(path, look.edge, style = Stroke(width = stroke))

    listOf(0.31f, 0.69f).forEach { fx ->
        drawCircle(look.tape, h * 0.024f, Offset(w * fx, h * 0.875f))
        drawCircle(
            color = look.edge,
            radius = h * 0.024f,
            center = Offset(w * fx, h * 0.875f),
            style = Stroke(width = stroke * 0.8f),
        )
    }
    drawCircle(look.edge, h * 0.016f, Offset(w * 0.5f, h * 0.875f))
}

private fun DrawScope.drawReels(angleDegrees: Float, look: CassetteLook, w: Float, h: Float) {
    val stroke = h * 0.010f * look.strokeScale
    val spoolY = h * look.spoolY
    val spoolRadius = h * look.spoolRadius

    drawReel(
        center = Offset(w * SPOOL_LEFT, spoolY),
        radius = spoolRadius,
        angleDegrees = angleDegrees,
        look = look,
        stroke = stroke,
        tapePackScale = 0.70f,
    )
    drawReel(
        center = Offset(w * SPOOL_RIGHT, spoolY),
        radius = spoolRadius,
        angleDegrees = angleDegrees * TAKE_UP_RATIO,
        look = look,
        stroke = stroke,
        tapePackScale = 0.58f,
    )
}

private fun DrawScope.drawReel(
    center: Offset,
    radius: Float,
    angleDegrees: Float,
    look: CassetteLook,
    stroke: Float,
    tapePackScale: Float,
) {
    drawCircle(look.recess, radius, center)
    // Inner shadow so the well reads as recessed into the shell.
    look.occlusion?.let { ao ->
        drawCircle(ao, radius, center, style = Stroke(width = radius * 0.16f))
    }
    drawCircle(look.edge, radius, center, style = Stroke(width = stroke))
    drawCircle(
        color = look.tape,
        radius = radius * tapePackScale,
        center = center,
        style = Stroke(width = radius * (tapePackScale - 0.30f)),
    )

    // Highlight along the wound tape edge.
    look.sheen?.let { sheen ->
        drawArc(
            color = sheen,
            startAngle = 190f,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = Offset(
                center.x - radius * tapePackScale,
                center.y - radius * tapePackScale,
            ),
            size = Size(radius * tapePackScale * 2f, radius * tapePackScale * 2f),
            style = Stroke(width = radius * 0.09f, cap = StrokeCap.Round),
        )
    }

    drawCircle(look.hub, radius * 0.32f, center)
    look.bevel?.let { bevel ->
        drawCircle(bevel, radius * 0.36f, center, style = Stroke(width = radius * 0.05f))
    }

    rotate(angleDegrees, center) {
        repeat(look.hubSpokes) { index ->
            val radians = Math.toRadians(index * (360.0 / look.hubSpokes))
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
private const val LABEL_LEFT = 0.07f
private const val LABEL_WIDTH = 0.86f
private const val LABEL_CORNER = 0.03f
private const val STRIPE_HEIGHT = 0.05f
private const val SPOOL_LEFT = 0.30f
private const val SPOOL_RIGHT = 0.70f
