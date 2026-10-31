package com.petruvideo.core.designsystem.adaptive

/** Hinge or fold occlusion rectangle in window pixels. */
data class HingeBounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

enum class FoldState { None, Flat, HalfOpened }

enum class FoldOrientation { Horizontal, Vertical }

/** Everything the layout choice depends on. Widths and heights are in dp. */
data class ArrangementInput(
    val widthDp: Int,
    val heightDp: Int,
    val fold: FoldState = FoldState.None,
    val foldOrientation: FoldOrientation? = null,
    val hinge: HingeBounds? = null,
    val hingeSeparates: Boolean = false,
)

sealed interface Arrangement {
    /** Phone layouts. [wideContent] is true on medium width (600 to 839 dp): same layouts, wider content. */
    data class Compact(val wideContent: Boolean) : Arrangement

    /** Multi-pane layouts, from 840 dp. [hinge] is set when a separating hinge must be avoided. */
    data class Expanded(val hinge: HingeBounds?) : Arrangement

    /** Half-folded with a horizontal hinge: preview above, controls below. */
    data class Tabletop(val hinge: HingeBounds) : Arrangement
}

object ArrangementResolver {
    const val MEDIUM_MIN_DP = 600
    const val EXPANDED_MIN_DP = 840

    /** A pure function of the window and fold state, never of the device model (FR-023). */
    fun resolve(input: ArrangementInput): Arrangement {
        val hinge = input.hinge.takeIf { input.hingeSeparates }
        if (input.fold == FoldState.HalfOpened &&
            input.foldOrientation == FoldOrientation.Horizontal &&
            hinge != null
        ) {
            return Arrangement.Tabletop(hinge)
        }
        return when {
            input.widthDp >= EXPANDED_MIN_DP -> Arrangement.Expanded(hinge)
            input.widthDp >= MEDIUM_MIN_DP -> Arrangement.Compact(wideContent = true)
            else -> Arrangement.Compact(wideContent = false)
        }
    }
}
