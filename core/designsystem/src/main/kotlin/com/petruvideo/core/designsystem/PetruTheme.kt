package com.petruvideo.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private fun PetruPalette.dark() = darkColorScheme(
    primary = accent,
    onPrimary = onAccent,
    background = background,
    onBackground = onBackground,
    surface = surface,
    onSurface = onBackground,
    surfaceVariant = surfaceHighest,
    onSurfaceVariant = onSurfaceMuted,
    surfaceContainer = surface,
    surfaceContainerHigh = surfaceHigh,
    surfaceContainerHighest = surfaceHighest,
    outline = outline,
    outlineVariant = outlineDashed,
    error = danger,
)

private fun PetruPalette.light() = lightColorScheme(
    primary = accent,
    onPrimary = onAccent,
    background = background,
    onBackground = onBackground,
    surface = surface,
    onSurface = onBackground,
    surfaceVariant = surfaceHighest,
    onSurfaceVariant = onSurfaceMuted,
    surfaceContainer = surface,
    surfaceContainerHigh = surfaceHigh,
    surfaceContainerHighest = surfaceHighest,
    outline = outline,
    outlineVariant = outlineDashed,
    error = danger,
)

private fun scheme(darkTheme: Boolean): ColorScheme =
    if (darkTheme) DarkPalette.dark() else LightPalette.light()

/** The app's look: dark or light, following the phone's setting unless [darkTheme] says otherwise. */
@Composable
fun PetruTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPetruPalette provides if (darkTheme) DarkPalette else LightPalette) {
        MaterialTheme(
            colorScheme = scheme(darkTheme),
            typography = MaterialTypography,
            shapes = MaterialShapes,
            content = content,
        )
    }
}
