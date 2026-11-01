package com.petruvideo.core.model

/** How strongly the blurred background is blurred: the blur radius as a share of the frame height. */
enum class BlurStrength(val label: String, val radiusPermille: Int) {
    Light("Light", 20),
    Medium("Medium", 40),
    Strong("Strong", 80),
}

/** What fills the part of the frame a clip does not cover (feature 021). */
sealed interface CanvasBackground {
    data object Black : CanvasBackground

    data class Colour(val argb: Int) : CanvasBackground

    data class Blur(val strength: BlurStrength) : CanvasBackground
}
