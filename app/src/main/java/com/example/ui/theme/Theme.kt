package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SophisticatedDarkColorScheme = darkColorScheme(
    primary = SophisticatedPrimary,
    onPrimary = SophisticatedOnPrimary,
    primaryContainer = SophisticatedPrimaryContainer,
    onPrimaryContainer = SophisticatedOnPrimaryContainer,
    secondary = SophisticatedSecondary,
    onSecondary = SophisticatedOnSecondary,
    secondaryContainer = SophisticatedSecondaryContainer,
    onSecondaryContainer = SophisticatedOnSecondaryContainer,
    tertiary = SophisticatedPrecisionGreen,
    onTertiary = Color(0xFF003915),
    error = SophisticatedError,
    onError = Color.White,
    background = SophisticatedBg,
    onBackground = SophisticatedTextPrimary,
    surface = SophisticatedSurface,
    onSurface = SophisticatedTextPrimary,
    surfaceVariant = SophisticatedSurfaceVariant,
    onSurfaceVariant = SophisticatedTextSecondary,
    outline = SophisticatedBorder,
    outlineVariant = SophisticatedBorder.copy(alpha = 0.5f)
)

private val NightColorScheme = darkColorScheme(
    primary = NightPrimary,
    onPrimary = NightOnPrimary,
    primaryContainer = NightPrimaryContainer,
    onPrimaryContainer = NightOnPrimaryContainer,
    secondary = NightPrimary,
    onSecondary = NightOnPrimary,
    secondaryContainer = NightSurfaceVariant,
    onSecondaryContainer = NightTextPrimary,
    tertiary = NightPrimary,
    onTertiary = Color.Black,
    error = NightPrimary,
    onError = Color.Black,
    background = NightBg,
    onBackground = NightTextPrimary,
    surface = NightSurface,
    onSurface = NightTextPrimary,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = NightTextSecondary,
    outline = NightBorder,
    outlineVariant = NightBorder.copy(alpha = 0.5f)
)

@Composable
fun OfflineCompassTheme(
    nightMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (nightMode) NightColorScheme else SophisticatedDarkColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Keep backwards-compatibility alias for tests
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    OfflineCompassTheme(nightMode = false, content = content)
}
