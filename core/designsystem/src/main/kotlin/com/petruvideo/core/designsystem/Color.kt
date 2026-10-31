package com.petruvideo.core.designsystem

import androidx.compose.ui.graphics.Color

/** Palette taken from the designer's PetruVideo UI Reference canvas. */
object PetruColors {
    val Background = Color(0xFF0B0C10)
    val SurfaceLow = Color(0xFF0F1015)
    val Surface = Color(0xFF14161C)
    val SurfaceHigh = Color(0xFF1D2028)
    val SurfaceHighest = Color(0xFF292D38)
    val Outline = Color(0xFF2C303B)
    val OutlineDashed = Color(0xFF3A3F4D)

    val OnBackground = Color(0xFFF2F4F8)
    val OnSurfaceMuted = Color(0xFF9AA1B2)

    val Accent = Color(0xFF35F0C0)
    val OnAccent = Color(0xFF0B0C10)
    val Danger = Color(0xFFFF6B8A)
}

/** Fixed lane colors: border and label color, with the matching dark fill. */
object TrackColors {
    val Video = Color(0xFF35F0C0)
    val VideoFill = Color(0xFF0F2F28)
    val Audio = Color(0xFFFFB547)
    val AudioFill = Color(0xFF33260F)
    val Music = Color(0xFF4CC9F0)
    val MusicFill = Color(0xFF0F2A33)
    val Pip = Color(0xFFFF6B8A)
    val PipFill = Color(0xFF3A1F2A)
    val Effects = Color(0xFFA58BFF)
    val EffectsFill = Color(0xFF2A2250)
}
