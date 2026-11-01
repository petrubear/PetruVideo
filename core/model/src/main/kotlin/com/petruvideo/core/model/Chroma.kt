package com.petruvideo.core.model

const val CHROMA_GREEN = 0xFF00B140.toInt()
const val CHROMA_BLUE = 0xFF0047BB.toInt()
const val CHROMA_STRENGTH_DEFAULT = 40
const val CHROMA_SMOOTHNESS_DEFAULT = 20
const val CHROMA_SPILL_DEFAULT = 50

/** Makes the picture of an overlay clip transparent where it is close to [color] (feature 022). */
data class ChromaKey(
    val enabled: Boolean = false,
    val color: Int = CHROMA_GREEN,
    val strength: Int = CHROMA_STRENGTH_DEFAULT,
    val smoothness: Int = CHROMA_SMOOTHNESS_DEFAULT,
    val spill: Int = CHROMA_SPILL_DEFAULT,
) {
    fun normalized() = copy(
        color = color or OPAQUE,
        strength = strength.coerceIn(0, 100),
        smoothness = smoothness.coerceIn(0, 100),
        spill = spill.coerceIn(0, 100),
    )

    val inRange: Boolean get() = normalized() == this

    private companion object {
        const val OPAQUE = 0xFF000000.toInt()
    }
}
