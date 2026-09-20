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
 */
enum class PlayerStyle(val displayName: String) {
    CLASSIC("Classic"),
    MINIMAL("Minimal"),
    BRUTALIST("Brutalist"),
    REALISM("Realism"),
}

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
    val detailed: Boolean = true,
    val hardShadow: Color? = null,
    /** Optional middle stop for a three-stop shell gradient. */
    val shellMid: Color? = null,
    /** Specular streak across the shell when non-null. */
    val sheen: Color? = null,
    /** Inner lip just inside the shell edge when non-null. */
    val bevel: Color? = null,
    /** Reflection across the tape window when non-null. */
    val glass: Color? = null,
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
    PlayerStyle.BRUTALIST -> if (isSystemInDarkTheme()) BrutalistDark else BrutalistLight
    PlayerStyle.REALISM -> if (isSystemInDarkTheme()) RealismDark else RealismLight
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
        detailed = false,
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

// Moulded plastic under a studio backdrop: bevels, specular sheen, glass.
private val RealismLight = PlayerSkin(
    cassette = CassetteLook(
        shellTop = Color(0xFFF4F2EE),
        shellMid = Color(0xFFE4E0D9),
        shellBottom = Color(0xFFC8C2B8),
        edge = Color(0xFFA8A19A),
        label = Color(0xFFFBFAF7),
        stripe = Color(0xFF274690),
        recess = Color(0xFF55524D),
        tape = Color(0xFF2E2822),
        hub = Color(0xFFECECEC),
        spoke = Color(0xFF8C8C8C),
        line = Color(0xFF9A948C),
        bevel = Color(0x59FFFFFF),
        sheen = Color(0x29FFFFFF),
        glass = Color(0x24FFFFFF),
    ),
    backdrop = Brush.verticalGradient(listOf(Color(0xFFEFEFF0), Color(0xFFD6D6D9))),
    surface = Color(0xFFFFFFFF),
    content = Color(0xFF1B1B1E),
    mutedContent = Color(0xFF6B6B72),
    accent = Color(0xFF26262B),
    onAccent = Color(0xFFF6F6F8),
    outline = Color(0xFFC5C5C9),
    controlShape = CircleShape,
    panelShape = RoundedCornerShape(24.dp),
    borderWidth = 1.dp,
    shadowElevation = 18.dp,
    darkStatusBarIcons = true,
)

private val RealismDark = PlayerSkin(
    cassette = CassetteLook(
        shellTop = Color(0xFF3C3C40),
        shellMid = Color(0xFF2F2F33),
        shellBottom = Color(0xFF202024),
        edge = Color(0xFF56565B),
        label = Color(0xFFE9E7E2),
        stripe = Color(0xFFD9A05B),
        recess = Color(0xFF131315),
        tape = Color(0xFF0E0E10),
        hub = Color(0xFFC9C9CC),
        spoke = Color(0xFF77777D),
        line = Color(0xFF8A8A90),
        bevel = Color(0x29FFFFFF),
        sheen = Color(0x1AFFFFFF),
        glass = Color(0x14FFFFFF),
    ),
    backdrop = Brush.verticalGradient(listOf(Color(0xFF1D1D20), Color(0xFF111113))),
    surface = Color(0xFF26262A),
    content = Color(0xFFF1F1F3),
    mutedContent = Color(0xFFA2A2A9),
    accent = Color(0xFFECECEF),
    onAccent = Color(0xFF1A1A1C),
    outline = Color(0xFF4A4A50),
    controlShape = CircleShape,
    panelShape = RoundedCornerShape(24.dp),
    borderWidth = 1.dp,
    shadowElevation = 18.dp,
    darkStatusBarIcons = false,
)

// Raw blocks, fat outlines and a hard offset shadow.
private val BrutalistLight = PlayerSkin(
    cassette = CassetteLook(
        shellTop = Color(0xFFFFFFFF),
        shellBottom = Color(0xFFFFFFFF),
        edge = Color(0xFF000000),
        label = Color(0xFFFFFFFF),
        stripe = Color(0xFF000000),
        recess = Color(0xFFD9D9D9),
        tape = Color(0xFF000000),
        hub = Color(0xFF000000),
        spoke = Color(0xFFFFFFFF),
        line = Color(0xFF000000),
        cornerScale = 0.2f,
        strokeScale = 3f,
        useGradient = false,
        hardShadow = Color(0xFF000000),
    ),
    backdrop = SolidColor(Color(0xFFFFE24D)),
    surface = Color(0xFFFFFFFF),
    content = Color(0xFF000000),
    mutedContent = Color(0xFF000000).copy(alpha = 0.68f),
    accent = Color(0xFF000000),
    onAccent = Color(0xFFFFE24D),
    outline = Color(0xFF000000),
    controlShape = RoundedCornerShape(0.dp),
    panelShape = RoundedCornerShape(0.dp),
    borderWidth = 3.dp,
    shadowElevation = 0.dp,
    darkStatusBarIcons = true,
)

private val BrutalistDark = PlayerSkin(
    cassette = CassetteLook(
        shellTop = Color(0xFF1A1A1A),
        shellBottom = Color(0xFF1A1A1A),
        edge = Color(0xFFFFFFFF),
        label = Color(0xFF1A1A1A),
        stripe = Color(0xFFFFE24D),
        recess = Color(0xFF000000),
        tape = Color(0xFFFFFFFF),
        hub = Color(0xFFFFE24D),
        spoke = Color(0xFF1A1A1A),
        line = Color(0xFFFFFFFF),
        cornerScale = 0.2f,
        strokeScale = 3f,
        useGradient = false,
        hardShadow = Color(0xFFFFE24D),
    ),
    backdrop = SolidColor(Color(0xFF111111)),
    surface = Color(0xFF1A1A1A),
    content = Color(0xFFFFFFFF),
    mutedContent = Color(0xFFFFFFFF).copy(alpha = 0.65f),
    accent = Color(0xFFFFE24D),
    onAccent = Color(0xFF000000),
    outline = Color(0xFFFFFFFF),
    controlShape = RoundedCornerShape(0.dp),
    panelShape = RoundedCornerShape(0.dp),
    borderWidth = 3.dp,
    shadowElevation = 0.dp,
    darkStatusBarIcons = false,
)
