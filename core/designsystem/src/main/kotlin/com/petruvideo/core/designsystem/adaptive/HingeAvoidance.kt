package com.petruvideo.core.designsystem.adaptive

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.Box

/** Pure geometry for keeping interactive content out of a separating hinge (FR-025). Values are window pixels. */
object HingeAvoidance {
    /** A hinge that runs top to bottom, splitting the window into a left and a right pane. */
    fun isVertical(hinge: HingeBounds) = hinge.height > hinge.width

    /** True when a rectangle overlaps the hinge. */
    fun intersects(left: Int, top: Int, right: Int, bottom: Int, hinge: HingeBounds): Boolean =
        left < hinge.right && right > hinge.left && top < hinge.bottom && bottom > hinge.top

    /** Widths of the left pane, the hinge gap, and the right pane for a window [windowWidthPx] wide. */
    fun verticalSplit(hinge: HingeBounds, windowWidthPx: Int): Triple<Int, Int, Int> {
        val left = hinge.left.coerceIn(0, windowWidthPx)
        val gap = hinge.width.coerceIn(0, windowWidthPx - left)
        return Triple(left, gap, windowWidthPx - left - gap)
    }
}

/**
 * Lays out [first] and [second] side by side with a gap exactly as wide as a vertical hinge, so neither pane is
 * under it. Without a vertical hinge the panes share the width with the given [firstWeight].
 */
@Composable
fun HingeSplit(
    hinge: HingeBounds?,
    modifier: Modifier = Modifier,
    firstWeight: Float = 1f,
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val vertical = hinge?.takeIf { HingeAvoidance.isVertical(it) }
    Row(modifier.fillMaxSize()) {
        if (vertical == null) {
            Box(Modifier.weight(firstWeight).fillMaxHeight()) { first() }
            Box(Modifier.weight(1f).fillMaxHeight()) { second() }
        } else {
            Box(Modifier.width(with(density) { vertical.left.toDp() }).fillMaxHeight()) { first() }
            Spacer(Modifier.width(with(density) { vertical.width.toDp() }).fillMaxHeight())
            Box(Modifier.weight(1f).fillMaxHeight()) { second() }
        }
    }
}
