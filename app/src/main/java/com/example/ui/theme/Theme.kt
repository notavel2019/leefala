package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StudioLightColorScheme = lightColorScheme(
    primary = StudioTeal,
    onPrimary = Color.White,
    primaryContainer = StudioTealLight,
    onPrimaryContainer = StudioTealDark,
    secondary = StudioSky,
    onSecondary = Color.White,
    secondaryContainer = StudioSkyContainer,
    onSecondaryContainer = InkPrimary,
    tertiary = RouteAmber,
    onTertiary = Color.White,
    tertiaryContainer = RouteAmberContainer,
    onTertiaryContainer = InkPrimary,
    background = CanvasLight,
    onBackground = InkPrimary,
    surface = SurfacePure,
    onSurface = InkPrimary,
    surfaceVariant = SurfaceSubtle,
    onSurfaceVariant = InkSecondary,
    outline = SurfaceOutline,
    outlineVariant = SurfaceSubtle,
    error = RecCrimson,
    onError = Color.White,
    errorContainer = RecCrimsonSoft,
    onErrorContainer = RecCrimson,
)

private val StudioDarkColorScheme = darkColorScheme(
    primary = StudioTealOnDark,
    onPrimary = StudioDarkCanvas,
    primaryContainer = StudioTealContainerDark,
    onPrimaryContainer = StudioTealLight,
    secondary = StudioSkyContainer,
    onSecondary = StudioDarkCanvas,
    secondaryContainer = StudioDarkSurfaceHigh,
    onSecondaryContainer = StudioTextOnDark,
    tertiary = Color(0xFFFBBF24),
    onTertiary = StudioDarkCanvas,
    tertiaryContainer = Color(0xFF451A03),
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = StudioDarkCanvas,
    onBackground = StudioTextOnDark,
    surface = StudioDarkSurface,
    onSurface = StudioTextOnDark,
    surfaceVariant = StudioDarkSurfaceHigh,
    onSurfaceVariant = StudioMutedOnDark,
    outline = Color(0xFF334155),
    outlineVariant = StudioDarkSurfaceHigh,
    error = Color(0xFFF87171),
    onError = StudioDarkCanvas,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
)

@Composable
fun EscutaEGravaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) StudioDarkColorScheme else StudioLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
