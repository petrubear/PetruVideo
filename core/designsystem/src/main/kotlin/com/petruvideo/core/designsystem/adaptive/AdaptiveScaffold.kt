package com.petruvideo.core.designsystem.adaptive

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

/**
 * Renders the compact, expanded, or tabletop slot for the resolved [arrangement]. Slots receive the hinge
 * bounds so layouts can keep interactive content out of them. In tabletop the scaffold itself splits the window
 * into the area above the hinge, the hinge gap, and the area below, so nothing is ever placed under the hinge.
 */
@Composable
fun AdaptiveScaffold(
    arrangement: Arrangement,
    modifier: Modifier = Modifier,
    compact: @Composable (wideContent: Boolean) -> Unit,
    expanded: @Composable (hinge: HingeBounds?) -> Unit,
    tabletopTop: @Composable () -> Unit,
    tabletopBottom: @Composable () -> Unit,
) {
    when (arrangement) {
        is Arrangement.Compact -> Box(modifier.fillMaxSize()) { compact(arrangement.wideContent) }
        is Arrangement.Expanded -> Box(modifier.fillMaxSize()) { expanded(arrangement.hinge) }
        is Arrangement.Tabletop -> {
            val density = LocalDensity.current
            val hingeTop = with(density) { arrangement.hinge.top.toDp() }
            val hingeHeight = with(density) { arrangement.hinge.height.toDp() }
            Column(modifier.fillMaxSize()) {
                Region(hingeTop) { tabletopTop() }
                Spacer(Modifier.fillMaxWidth().height(hingeHeight))
                Box(Modifier.fillMaxWidth().weight(1f)) { tabletopBottom() }
            }
        }
    }
}

@Composable
private fun Region(height: Dp, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().height(height)) { content() }
}
