package com.petruvideo.feature.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.feature.editor.TimelineGeometry

/** Time ruler with a label on each major tick; the label of the current second is highlighted. */
@Composable
fun TimelineRuler(
    geometry: TimelineGeometry,
    totalMs: Long,
    currentSecond: Int,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth().height(22.dp)) {
        geometry.rulerTicks(totalMs).forEach { tick ->
            val x = geometry.timeToPx(tick.timeMs).dp
            if (tick.label != null) {
                val current = tick.timeMs / MS_PER_SECOND == currentSecond.toLong()
                Text(
                    tick.label,
                    style = PetruType.MonoSmall.copy(fontSize = 9.sp),
                    color = if (current) PetruColors.OnBackground else PetruColors.OnSurfaceMuted,
                    modifier = Modifier.offset(x = x - LABEL_HALF_WIDTH, y = 6.dp),
                )
            } else {
                Box(Modifier.offset(x = x, y = 11.dp).width(1.dp).height(4.dp).tick())
            }
        }
    }
}

private fun Modifier.tick(): Modifier =
    this.then(Modifier.background(PetruColors.OnSurfaceMuted.copy(alpha = 0.5f)))

private val LABEL_HALF_WIDTH = 12.dp
private const val MS_PER_SECOND = 1_000L
