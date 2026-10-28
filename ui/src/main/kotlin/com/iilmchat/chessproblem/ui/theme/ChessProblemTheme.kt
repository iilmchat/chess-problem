package com.iilmchat.chessproblem.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkScheme = darkColorScheme(
    primary = Emerald,
    onPrimary = OnEmerald,
    primaryContainer = InkSurfaceHigh,
    onPrimaryContainer = Parchment,
    secondary = Brass,
    onSecondary = OnBrass,
    secondaryContainer = InkSurfaceHigh,
    onSecondaryContainer = Parchment,
    tertiary = Violet,
    onTertiary = OnViolet,
    background = InkBackground,
    onBackground = Parchment,
    surface = InkSurface,
    onSurface = Parchment,
    surfaceVariant = InkSurfaceVariant,
    onSurfaceVariant = ParchmentMuted,
    outline = InkOutline,
    error = ErrorRed,
    onError = OnErrorRed,
    scrim = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f),
)

private val LightScheme = lightColorScheme(
    primary = EmeraldDeep,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = PaperSurfaceVariant,
    onPrimaryContainer = PaperOnBackground,
    secondary = PaperBrass,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = PaperSurfaceVariant,
    onSecondaryContainer = PaperOnBackground,
    tertiary = Violet,
    onTertiary = OnViolet,
    background = PaperBackground,
    onBackground = PaperOnBackground,
    surface = PaperSurface,
    onSurface = PaperOnBackground,
    surfaceVariant = PaperSurfaceVariant,
    onSurfaceVariant = PaperOnSurfaceVariant,
    outline = PaperOutline,
    error = PaperError,
    onError = androidx.compose.ui.graphics.Color.White,
)

@Composable
fun ChessProblemTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = ChessTypography,
        shapes = ChessShapes,
        content = content,
    )
}
