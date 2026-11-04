package com.petruvideo.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** One lane's border and label color with its fill. */
data class LaneColors(val name: String, val border: Color, val fill: Color)

/** Every named color of one look: [DarkPalette] is the designer's reference, [LightPalette] its light twin. */
data class PetruPalette(
    val background: Color,
    val surfaceLow: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val surfaceHighest: Color,
    val outline: Color,
    val outlineDashed: Color,
    val onBackground: Color,
    val onSurfaceMuted: Color,
    val accent: Color,
    val onAccent: Color,
    val danger: Color,
    /** The volume range above 100%. */
    val boost: Color,
    val lanes: List<LaneColors>,
) {
    fun lane(name: String): LaneColors = lanes.first { it.name == name }
}

val DarkPalette = PetruPalette(
    background = Color(0xFF0B0C10),
    surfaceLow = Color(0xFF0F1015),
    surface = Color(0xFF14161C),
    surfaceHigh = Color(0xFF1D2028),
    surfaceHighest = Color(0xFF292D38),
    outline = Color(0xFF2C303B),
    outlineDashed = Color(0xFF3A3F4D),
    onBackground = Color(0xFFF2F4F8),
    onSurfaceMuted = Color(0xFF9AA1B2),
    accent = Color(0xFF35F0C0),
    onAccent = Color(0xFF0B0C10),
    danger = Color(0xFFFF6B8A),
    boost = Color(0xFFFFB547),
    lanes = listOf(
        LaneColors("Video", Color(0xFF35F0C0), Color(0xFF0F2F28)),
        LaneColors("Audio", Color(0xFFFFB547), Color(0xFF33260F)),
        LaneColors("Music", Color(0xFF4CC9F0), Color(0xFF0F2A33)),
        LaneColors("Pip", Color(0xFFFF6B8A), Color(0xFF3A1F2A)),
        LaneColors("Effects", Color(0xFFA58BFF), Color(0xFF2A2250)),
    ),
)

/** Same accent and lane hues as the dark look, on light surfaces with dark text and light lane tints. */
val LightPalette = PetruPalette(
    background = Color(0xFFF6F7FA),
    surfaceLow = Color(0xFFEFF1F5),
    surface = Color(0xFFFFFFFF),
    surfaceHigh = Color(0xFFE8EBF1),
    surfaceHighest = Color(0xFFDCE0E9),
    outline = Color(0xFF7D8599),
    outlineDashed = Color(0xFF727B8F),
    onBackground = Color(0xFF14161C),
    onSurfaceMuted = Color(0xFF4F5668),
    accent = Color(0xFF007A63),
    onAccent = Color(0xFFFFFFFF),
    danger = Color(0xFFC21F4A),
    boost = Color(0xFF9A5A00),
    lanes = listOf(
        LaneColors("Video", Color(0xFF007A63), Color(0xFFCFF3E9)),
        LaneColors("Audio", Color(0xFF9A5A00), Color(0xFFFBE8C8)),
        LaneColors("Music", Color(0xFF2268C7), Color(0xFFD2E5F8)),
        LaneColors("Pip", Color(0xFFC21F4A), Color(0xFFF9D6DF)),
        LaneColors("Effects", Color(0xFF6244D6), Color(0xFFE3DDFB)),
    ),
)

/** The palette of the current look; set by [PetruTheme]. */
val LocalPetruPalette = staticCompositionLocalOf { DarkPalette }

/** The app's colors in the current look. Read them inside a composable. */
object PetruColors {
    val Background: Color @Composable @ReadOnlyComposable get() = LocalPetruPalette.current.background
    val SurfaceLow: Color @Composable @ReadOnlyComposable get() = LocalPetruPalette.current.surfaceLow
    val Surface: Color @Composable @ReadOnlyComposable get() = LocalPetruPalette.current.surface
    val SurfaceHigh: Color @Composable @ReadOnlyComposable get() = LocalPetruPalette.current.surfaceHigh
    val SurfaceHighest: Color @Composable @ReadOnlyComposable get() = LocalPetruPalette.current.surfaceHighest
    val Outline: Color @Composable @ReadOnlyComposable get() = LocalPetruPalette.current.outline
    val OutlineDashed: Color @Composable @ReadOnlyComposable get() = LocalPetruPalette.current.outlineDashed

    val OnBackground: Color @Composable @ReadOnlyComposable get() = LocalPetruPalette.current.onBackground
    val OnSurfaceMuted: Color @Composable @ReadOnlyComposable get() = LocalPetruPalette.current.onSurfaceMuted

    val Accent: Color @Composable @ReadOnlyComposable get() = LocalPetruPalette.current.accent
    val OnAccent: Color @Composable @ReadOnlyComposable get() = LocalPetruPalette.current.onAccent
    val Danger: Color @Composable @ReadOnlyComposable get() = LocalPetruPalette.current.danger

    /** The volume range above 100%. */
    val Boost: Color @Composable @ReadOnlyComposable get() = LocalPetruPalette.current.boost
}

/** Lane colors in the current look: border and label color, with the matching fill. */
object TrackColors {
    val Video: Color @Composable @ReadOnlyComposable get() = LocalPetruPalette.current.lane("Video").border
    val VideoFill: Color @Composable @ReadOnlyComposable get() = LocalPetruPalette.current.lane("Video").fill
    val Audio: Color @Composable @ReadOnlyComposable get() = LocalPetruPalette.current.lane("Audio").border
    val AudioFill: Color @Composable @ReadOnlyComposable get() = LocalPetruPalette.current.lane("Audio").fill
    val Music: Color @Composable @ReadOnlyComposable get() = LocalPetruPalette.current.lane("Music").border
    val MusicFill: Color @Composable @ReadOnlyComposable get() = LocalPetruPalette.current.lane("Music").fill
    val Pip: Color @Composable @ReadOnlyComposable get() = LocalPetruPalette.current.lane("Pip").border
    val PipFill: Color @Composable @ReadOnlyComposable get() = LocalPetruPalette.current.lane("Pip").fill
    val Effects: Color @Composable @ReadOnlyComposable get() = LocalPetruPalette.current.lane("Effects").border
    val EffectsFill: Color @Composable @ReadOnlyComposable get() = LocalPetruPalette.current.lane("Effects").fill
}
