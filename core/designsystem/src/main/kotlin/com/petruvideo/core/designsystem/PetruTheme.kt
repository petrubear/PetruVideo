package com.petruvideo.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = PetruColors.Accent,
    onPrimary = PetruColors.OnAccent,
    background = PetruColors.Background,
    onBackground = PetruColors.OnBackground,
    surface = PetruColors.Surface,
    onSurface = PetruColors.OnBackground,
    surfaceVariant = PetruColors.SurfaceHighest,
    onSurfaceVariant = PetruColors.OnSurfaceMuted,
    surfaceContainer = PetruColors.Surface,
    surfaceContainerHigh = PetruColors.SurfaceHigh,
    surfaceContainerHighest = PetruColors.SurfaceHighest,
    outline = PetruColors.Outline,
    outlineVariant = PetruColors.OutlineDashed,
    error = PetruColors.Danger,
)

/** The app is dark-only, as designed. */
@Composable
fun PetruTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = MaterialTypography,
        shapes = MaterialShapes,
        content = content,
    )
}
