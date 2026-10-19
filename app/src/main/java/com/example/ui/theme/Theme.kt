package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GlassmorphicColorScheme = darkColorScheme(
    primary = GeoPrimary,
    onPrimary = Color.White,
    primaryContainer = GeoPrimaryContainer,
    onPrimaryContainer = GeoOnPrimaryContainer,
    secondary = GeoSecondary,
    onSecondary = Color.White,
    secondaryContainer = GeoSecondaryContainer,
    onSecondaryContainer = GeoOnSecondaryContainer,
    tertiary = GeoTertiary,
    onTertiary = Color.White,
    tertiaryContainer = GeoTertiaryContainer,
    onTertiaryContainer = GeoOnTertiaryContainer,
    background = GeoCanvas,
    onBackground = GeoTextPrimary,
    surface = GeoSurface,
    onSurface = GeoTextPrimary,
    surfaceVariant = GeoSurfaceVariant,
    onSurfaceVariant = GeoTextSecondary,
    outline = GeoOutline,
    outlineVariant = GeoOutlineVariant,
    error = GeoError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GlassmorphicColorScheme,
        typography = Typography,
        content = content
    )
}


