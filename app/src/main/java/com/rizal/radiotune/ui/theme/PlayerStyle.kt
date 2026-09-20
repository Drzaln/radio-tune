package com.rizal.radiotune.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Look of the now-playing screen. A style owns the whole screen (backdrop, text,
 * accent, control shape) plus the cassette drawing, so it reads as one skin.
 * Other screens stay on the Material theme.
 *
 * The stored preference keeps the enum *name*, so removing or reordering styles
 * is safe: an unknown name falls back to [PlayerStyle.CLASSIC].
 */
enum class PlayerStyle(val displayName: String) {
    CLASSIC("Classic"),
    MINIMAL("Minimal"),
    REALISM("Realism"),
    SEMI_REALISM("Semi-realism"),
}

/** One printed stripe of a label. Weights are relative, not absolute. */
@Immutable
data class LabelBand(val color: Color, val weight: Float = 1f)

@Immutable
data class CassetteLook(
    val shellTop: Color,
    val shellBottom: Color,
    val edge: Color,
    val label: Color,
    val stripe: Color,
    val recess: Color,
    val tape: Color,
    val hub: Color,
    val spoke: Color,
    val line: Color,
    val cornerScale: Float = 1f,
    val strokeScale: Float = 1f,
    val useGradient: Boolean = true,
    val hardShadow: Color? = null,
    /** Optional middle stop for a three-stop shell gradient. */
    val shellMid: Color? = null,
    /** Specular streak across the shell when non-null. */
    val sheen: Color? = null,
    /** Inner lip just inside the shell edge when non-null. */
    val bevel: Color? = null,
    /** Reflection across the tape window when non-null. */
    val glass: Color? = null,
    /** Draws a moulded, stepped shell lit from the top-left. */
    val steppedShell: Boolean = false,
    /** Broad highlight blob on the shell when non-null. */
    val specular: Color? = null,
    /** Ambient-occlusion shadows around recesses and under the label. */
    val occlusion: Color? = null,
    // Layout, as fractions of the cassette height/width.
    val labelTop: Float = 0.085f,
    val labelHeight: Float = 0.40f,
    /** Chamfers the label's top corners when > 0 (fraction of the width). */
    val labelChamfer: Float = 0f,
    val windowTop: Float = 0.79f,
    val windowWidth: Float = 0.56f,
    val windowHeight: Float = 0.145f,
    val spoolY: Float = 0.665f,
    val spoolRadius: Float = 0.145f,
    val hubSpokes: Int = 6,
    // Detail switches.
    val showNotches: Boolean = true,
    val showScrews: Boolean = true,
    val showStripe: Boolean = true,
    val showRuledLines: Boolean = true,
    val showLabelOutline: Boolean = true,
    val showTapeWindow: Boolean = true,
    val showPinchRollers: Boolean = true,
    // Extra finishes.
    /** When set, replaces the solid label + stripe with printed bands. */
    val labelBands: List<LabelBand> = emptyList(),
    val ribs: Color? = null,
    val speckle: Color? = null,
    val crossScrews: Boolean = false,
    val bottomPlate: Boolean = false,
)

@Immutable
data class PlayerSkin(
    val cassette: CassetteLook,
    val backdrop: Brush,
    /** Panels and dialogs belonging to the player. */
    val surface: Color,
    val content: Color,
    val mutedContent: Color,
    val accent: Color,
    val onAccent: Color,
    val outline: Color,
    /** Shape shared by the play button and the stop button. */
    val controlShape: Shape,
    val panelShape: Shape,
    val borderWidth: Dp,
    val shadowElevation: Dp,
    val darkStatusBarIcons: Boolean,
)

@Composable
fun PlayerStyle.skin(): PlayerSkin = when (this) {
    PlayerStyle.CLASSIC -> if (isSystemInDarkTheme()) ClassicDark else ClassicLight
    PlayerStyle.MINIMAL -> minimalSkin(MaterialTheme.colorScheme, isSystemInDarkTheme())
    PlayerStyle.REALISM -> if (isSystemInDarkTheme()) RealismDark else RealismLight
    PlayerStyle.SEMI_REALISM -> if (isSystemInDarkTheme()) SemiRealismDark else SemiRealismLight
}

// Warm veneer and cream label, like a 1980s cassette deck.
private val ClassicLight = PlayerSkin(
    cassette = CassetteLook(
        shellTop = Color(0xFFFDF7EA),
        shellBottom = Color(0xFFEBDDC1),
        edge = Color(0xFFC6B18C),
        label = Color(0xFFFFFDF7),
        stripe = Color(0xFFB4532A),
        recess = Color(0xFFDCCAA6),
        tape = Color(0xFF6E5D45),
        hub = Color(0xFFB4532A),
        spoke = Color(0xFFFFFDF7),
        line = Color(0xFFC6B18C),
    ),
    backdrop = Brush.verticalGradient(listOf(Color(0xFFF5EAD4), Color(0xFFE6D6B6))),
    surface = Color(0xFFFFFBF2),
    content = Color(0xFF3B2E1E),
    mutedContent = Color(0xFF6E5D45),
    accent = Color(0xFFB4532A),
    onAccent = Color(0xFFFFF8EE),
    outline = Color(0xFFC6B18C),
    controlShape = CircleShape,
    panelShape = RoundedCornerShape(20.dp),
    borderWidth = 1.dp,
    shadowElevation = 12.dp,
    darkStatusBarIcons = true,
)

private val ClassicDark = PlayerSkin(
    cassette = CassetteLook(
        shellTop = Color(0xFF3B2F22),
        shellBottom = Color(0xFF2C2318),
        edge = Color(0xFF5A4A35),
        label = Color(0xFFF2E7D3),
        stripe = Color(0xFFE08A4E),
        recess = Color(0xFF241C13),
        tape = Color(0xFF93805F),
        hub = Color(0xFFE08A4E),
        spoke = Color(0xFF2C2318),
        line = Color(0xFFA38F6C),
    ),
    backdrop = Brush.verticalGradient(listOf(Color(0xFF2B2218), Color(0xFF1B1610))),
    surface = Color(0xFF2E251A),
    content = Color(0xFFF2E7D3),
    mutedContent = Color(0xFFBEAC8F),
    accent = Color(0xFFE08A4E),
    onAccent = Color(0xFF2B1A0E),
    outline = Color(0xFF5A4A35),
    controlShape = CircleShape,
    panelShape = RoundedCornerShape(20.dp),
    borderWidth = 1.dp,
    shadowElevation = 12.dp,
    darkStatusBarIcons = false,
)

// Flat and quiet: inherits the Material theme colours.
@Composable
private fun minimalSkin(scheme: ColorScheme, dark: Boolean) = PlayerSkin(
    cassette = CassetteLook(
        shellTop = scheme.surfaceVariant,
        shellBottom = scheme.surfaceVariant,
        edge = scheme.outlineVariant,
        label = scheme.surface,
        stripe = scheme.primary,
        recess = scheme.onSurfaceVariant.copy(alpha = 0.12f),
        tape = scheme.onSurfaceVariant.copy(alpha = 0.50f),
        hub = scheme.primary,
        spoke = scheme.surface,
        line = scheme.onSurfaceVariant.copy(alpha = 0.25f),
        useGradient = false,
        hubSpokes = 3,
        showNotches = false,
        showScrews = false,
        showStripe = false,
        showRuledLines = false,
        showLabelOutline = false,
        showTapeWindow = false,
        showPinchRollers = false,
    ),
    backdrop = SolidColor(scheme.background),
    surface = scheme.surfaceContainerHigh,
    content = scheme.onBackground,
    mutedContent = scheme.onSurfaceVariant,
    accent = scheme.primary,
    onAccent = scheme.onPrimary,
    outline = scheme.outlineVariant,
    controlShape = CircleShape,
    panelShape = RoundedCornerShape(20.dp),
    borderWidth = 1.dp,
    shadowElevation = 0.dp,
    darkStatusBarIcons = !dark,
)

// Dark moulded plastic lit from the top-left: stepped rim, recessed wells, paper
// label. The cassette is a physical object, so both themes share it.
private val RealismCassette = CassetteLook(
    shellTop = Color(0xFF4C4C52),
    shellMid = Color(0xFF34343B),
    shellBottom = Color(0xFF1D1D22),
    edge = Color(0xFF121216),
    label = Color(0xFFF3F0EA),
    stripe = Color(0xFFB23A2E),
    recess = Color(0xFF0A0A0D),
    tape = Color(0xFF26201B),
    hub = Color(0xFFE8E6E1),
    spoke = Color(0xFF6E6E74),
    line = Color(0xFF8A857C),
    bevel = Color(0x73FFFFFF),
    sheen = Color(0x1AFFFFFF),
    glass = Color(0x21FFFFFF),
    specular = Color(0x33FFFFFF),
    occlusion = Color(0x5C000000),
    steppedShell = true,
)

private val RealismLight = PlayerSkin(
    cassette = RealismCassette,
    backdrop = Brush.verticalGradient(listOf(Color(0xFFE9E9EC), Color(0xFFCFCFD4))),
    surface = Color(0xFFFFFFFF),
    content = Color(0xFF1B1B1F),
    mutedContent = Color(0xFF6B6B72),
    accent = Color(0xFF26262B),
    onAccent = Color(0xFFF6F6F8),
    outline = Color(0xFFC5C5C9),
    controlShape = CircleShape,
    panelShape = RoundedCornerShape(24.dp),
    borderWidth = 1.dp,
    shadowElevation = 20.dp,
    darkStatusBarIcons = true,
)

private val RealismDark = PlayerSkin(
    cassette = RealismCassette,
    backdrop = Brush.verticalGradient(listOf(Color(0xFF17171B), Color(0xFF0E0E11))),
    surface = Color(0xFF26262A),
    content = Color(0xFFF1F1F3),
    mutedContent = Color(0xFFA2A2A9),
    accent = Color(0xFFECECEF),
    onAccent = Color(0xFF1A1A1C),
    outline = Color(0xFF4A4A50),
    controlShape = CircleShape,
    panelShape = RoundedCornerShape(24.dp),
    borderWidth = 1.dp,
    shadowElevation = 20.dp,
    darkStatusBarIcons = false,
)

// Navy ribbed shell with a big printed sticker label that the window cuts into —
// illustrated rather than photographic.
private val SemiRealismCassette = CassetteLook(
    shellTop = Color(0xFF3C4658),
    shellMid = Color(0xFF313A4A),
    shellBottom = Color(0xFF232A38),
    edge = Color(0xFF161B24),
    label = Color(0xFFF1E7C6),
    stripe = Color(0xFFE8541E),
    recess = Color(0xFF12161E),
    tape = Color(0xFFDCD7C8),
    hub = Color(0xFFF7F5EF),
    spoke = Color(0xFF9AA0AB),
    line = Color(0xFF2A2F3A),
    sheen = Color(0x14FFFFFF),
    bevel = Color(0x1AFFFFFF),
    glass = Color(0x1FFFFFFF),
    labelTop = 0.10f,
    labelHeight = 0.60f,
    labelChamfer = 0.05f,
    windowTop = 0.32f,
    windowWidth = 0.58f,
    windowHeight = 0.24f,
    spoolY = 0.44f,
    spoolRadius = 0.12f,
    showNotches = false,
    showRuledLines = false,
    showStripe = false,
    showPinchRollers = false,
    labelBands = listOf(
        LabelBand(Color(0xFFA9C4A0), 0.10f),
        LabelBand(Color(0xFFF1E7C6), 0.20f),
        LabelBand(Color(0xFFE8541E), 0.46f),
        LabelBand(Color(0xFFF6C61C), 0.14f),
        LabelBand(Color(0xFFA9C4A0), 0.10f),
    ),
    ribs = Color(0xFF1B2130),
    speckle = Color(0x33161B24),
    crossScrews = true,
    bottomPlate = true,
)

private val SemiRealismLight = PlayerSkin(
    cassette = SemiRealismCassette,
    backdrop = Brush.verticalGradient(listOf(Color(0xFFF4F1EA), Color(0xFFE3DDD0))),
    surface = Color(0xFFFFFDF7),
    content = Color(0xFF232833),
    mutedContent = Color(0xFF6A7080),
    accent = Color(0xFFE8541E),
    onAccent = Color(0xFFFFF8F0),
    outline = Color(0xFFC9C3B4),
    controlShape = CircleShape,
    panelShape = RoundedCornerShape(18.dp),
    borderWidth = 1.dp,
    shadowElevation = 16.dp,
    darkStatusBarIcons = true,
)

private val SemiRealismDark = PlayerSkin(
    cassette = SemiRealismCassette,
    backdrop = Brush.verticalGradient(listOf(Color(0xFF1A1E26), Color(0xFF11141A))),
    surface = Color(0xFF232833),
    content = Color(0xFFF1EDE3),
    mutedContent = Color(0xFFA8AEBB),
    accent = Color(0xFFFF6B35),
    onAccent = Color(0xFF1A1008),
    outline = Color(0xFF3A4150),
    controlShape = CircleShape,
    panelShape = RoundedCornerShape(18.dp),
    borderWidth = 1.dp,
    shadowElevation = 16.dp,
    darkStatusBarIcons = false,
)
