package com.rizal.radiotune.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val Teal40 = Color(0xFF006874)
private val Teal80 = Color(0xFF82D3E0)
private val TealContainerLight = Color(0xFF9EEFFD)
private val TealContainerDark = Color(0xFF004F58)

private val Sand40 = Color(0xFF4A6267)
private val Sand80 = Color(0xFFB1CBD0)

internal val LightColors = lightColorScheme(
    primary = Teal40,
    onPrimary = Color.White,
    primaryContainer = TealContainerLight,
    onPrimaryContainer = Color(0xFF001F24),
    secondary = Sand40,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDE7EC),
    onSecondaryContainer = Color(0xFF051F23),
    background = Color(0xFFFAFDFD),
    surface = Color(0xFFFAFDFD),
)

internal val DarkColors = darkColorScheme(
    primary = Teal80,
    onPrimary = Color(0xFF00363D),
    primaryContainer = TealContainerDark,
    onPrimaryContainer = Color(0xFF9EEFFD),
    secondary = Sand80,
    onSecondary = Color(0xFF1B3439),
    secondaryContainer = Color(0xFF324B50),
    onSecondaryContainer = Color(0xFFCDE7EC),
    background = Color(0xFF191C1D),
    surface = Color(0xFF191C1D),
)
