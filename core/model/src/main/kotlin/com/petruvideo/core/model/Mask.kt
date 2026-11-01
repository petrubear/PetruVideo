package com.petruvideo.core.model

const val MASK_SIZE_DEFAULT = 70
const val MASK_FEATHER_DEFAULT = 20

enum class MaskKind(val label: String) {
    None("None"),
    Circle("Circle"),
    Rectangle("Rectangle"),
    Linear("Linear"),
}

/** Shows only part of an overlay clip: inside a shape (outside it with [invert]) with a soft edge (feature 024). */
data class OverlayMask(
    val kind: MaskKind = MaskKind.None,
    val size: Int = MASK_SIZE_DEFAULT,
    val feather: Int = MASK_FEATHER_DEFAULT,
    val invert: Boolean = false,
) {
    fun normalized() = copy(size = size.coerceIn(0, 100), feather = feather.coerceIn(0, 100))

    val inRange: Boolean get() = normalized() == this

    val active: Boolean get() = kind != MaskKind.None
}
