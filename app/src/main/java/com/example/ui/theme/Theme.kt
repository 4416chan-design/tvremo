package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val TvRemoteDarkColorScheme = darkColorScheme(
    primary = MintGreen,
    onPrimary = OnMintGreen,
    primaryContainer = MintGreenContainer,
    onPrimaryContainer = MintGreen,
    secondary = CharcoalSurfaceElevated,
    onSecondary = TextPrimary,
    secondaryContainer = CharcoalSurface,
    onSecondaryContainer = TextPrimary,
    tertiary = DpadCenter,
    onTertiary = TextPrimary,
    background = CharcoalBg,
    onBackground = TextPrimary,
    surface = CharcoalSurface,
    onSurface = TextPrimary,
    surfaceVariant = CharcoalWell,
    onSurfaceVariant = TextSecondary,
    outline = DividerSubtle,
    error = ErrorCoral,
    onError = CharcoalBgDeep
)

private val TvRemoteShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun TvRemoteTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TvRemoteDarkColorScheme,
        typography = TvRemoteTypography,
        shapes = TvRemoteShapes,
        content = content
    )
}
