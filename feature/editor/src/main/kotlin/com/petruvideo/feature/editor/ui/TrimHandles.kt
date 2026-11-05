package com.petruvideo.feature.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.feature.editor.TimelineGeometry
import com.petruvideo.feature.editor.TrimTarget
import androidx.compose.ui.res.stringResource
import com.petruvideo.feature.editor.R

private val HANDLE_WIDTH = 16.dp
private val HANDLE_SHAPE = RoundedCornerShape(4.dp)

/**
 * White trim handles on both edges of the selected clip, with the live length badge above it. Dragging only
 * moves the handle and updates the badge; the trim is committed once, on release, so each drag is one undo step.
 * [target] maps a drag distance in timeline ms to where that handle would land, already clamped to the limits.
 */
@Composable
fun TrimHandles(
    geometry: TimelineGeometry,
    currentLengthMs: Long,
    target: (start: Boolean, deltaMs: Long) -> TrimTarget,
    onPreview: (start: Boolean, TrimTarget) -> Unit,
    onCommit: (start: Boolean, TrimTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current.density
    var startDx by remember { mutableFloatStateOf(0f) }
    var endDx by remember { mutableFloatStateOf(0f) }
    val currentTarget by rememberUpdatedState(target)
    val preview by rememberUpdatedState(onPreview)
    val commit by rememberUpdatedState(onCommit)

    fun delta(dx: Float) = geometry.pxToTime(dx / density)

    /** How far the handle really moved (dp) once the limits are applied; start handles move right as they shorten. */
    fun shown(start: Boolean, dx: Float): Float {
        val length = currentTarget(start, delta(dx)).lengthMs
        return geometry.timeToPx(if (start) currentLengthMs - length else length - currentLengthMs)
    }

    Box(modifier) {
        Handle(
            description = stringResource(R.string.editor_trim_start),
            alignment = Alignment.CenterStart,
            offsetDp = shown(true, startDx),
            onDrag = { startDx += it; preview(true, currentTarget(true, delta(startDx))) },
            onEnd = { commit(true, currentTarget(true, delta(startDx))); startDx = 0f },
            onCancel = { startDx = 0f },
        )
        Handle(
            description = stringResource(R.string.editor_trim_end),
            alignment = Alignment.CenterEnd,
            offsetDp = shown(false, endDx),
            onDrag = { endDx += it; preview(false, currentTarget(false, delta(endDx))) },
            onEnd = { commit(false, currentTarget(false, delta(endDx))); endDx = 0f },
            onCancel = { endDx = 0f },
        )
    }
}

@Composable
private fun BoxScope.Handle(
    description: String,
    alignment: Alignment,
    offsetDp: Float,
    onDrag: (Float) -> Unit,
    onEnd: () -> Unit,
    onCancel: () -> Unit,
) {
    Box(
        Modifier
            .align(alignment)
            .offset(x = offsetDp.dp)
            .width(HANDLE_WIDTH)
            .fillMaxHeight()
            .background(PetruColors.OnBackground, HANDLE_SHAPE)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = onEnd,
                    onDragCancel = onCancel,
                    onHorizontalDrag = { change, amount -> change.consume(); onDrag(amount) },
                )
            }
            .semantics { contentDescription = description },
    )
}
