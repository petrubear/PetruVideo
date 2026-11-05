package com.petruvideo.feature.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.model.Keyframe
import com.petruvideo.feature.editor.EditorEvent
import com.petruvideo.feature.editor.EditorState
import com.petruvideo.feature.editor.TimelineGeometry
import kotlin.math.abs
import androidx.compose.ui.res.stringResource
import com.petruvideo.feature.editor.R

private val TARGET = 48.dp
private val DIAMOND = 14.dp
private const val SNAP_DP = 8f
private const val MS = 1000f

/** What the lanes need to draw and edit keyframe diamonds on the selected clip. */
data class KeyframeLaneState(
    val selectedMs: Long? = null,
    val playheadMs: Long = 0,
    val onTap: (clipId: String, relMs: Long) -> Unit = { _, _ -> },
    val onMove: (clipId: String, fromRelMs: Long, toRelMs: Long) -> Unit = { _, _, _ -> },
)

/** The lane state for the editor: the selected keyframe, the playhead, and the events a tap or drag sends. */
fun keyframeLaneState(state: EditorState, onEvent: (EditorEvent) -> Unit) = KeyframeLaneState(
    selectedMs = state.selectedKeyframeMs,
    playheadMs = state.playback.positionMs,
    onTap = { _, rel -> onEvent(EditorEvent.KeyframeSelect(rel)) },
    onMove = { id, from, to -> onEvent(EditorEvent.KeyframeMove(id, from, to)) },
)

/** A diamond for each keyframe of the clip; tap selects (and seeks), drag moves it, snapping to the playhead. */
@Composable
fun BoxScope.KeyframeDiamonds(
    clipId: String,
    clipStartMs: Long,
    durationMs: Long,
    frames: List<Keyframe>,
    geometry: TimelineGeometry,
    state: KeyframeLaneState,
    alignment: Alignment = Alignment.CenterStart,
) {
    frames.forEach { frame ->
        Diamond(clipId, clipStartMs, durationMs, frame.timeMs, geometry, state, alignment)
    }
}

@Composable
private fun BoxScope.Diamond(
    clipId: String,
    clipStartMs: Long,
    durationMs: Long,
    timeMs: Long,
    geometry: TimelineGeometry,
    state: KeyframeLaneState,
    alignment: Alignment,
) {
    val density = LocalDensity.current.density
    var dx by remember(timeMs) { mutableFloatStateOf(0f) }
    val isSelected = state.selectedMs == timeMs
    val x = geometry.timeToPx(timeMs) + dx / density
    val description = stringResource(
        if (isSelected) R.string.editor_keyframe_at_selected else R.string.editor_keyframe_at,
        "%.1f".format(timeMs / MS),
    )
    Box(
        Modifier
            .align(alignment)
            .offset(x = x.dp - TARGET / 2)
            .size(TARGET)
            .clickable(role = Role.Button) { state.onTap(clipId, timeMs) }
            .pointerInput(clipId, timeMs, durationMs, state.playheadMs) {
                detectDragGestures(
                    onDragStart = { state.onTap(clipId, timeMs) },
                    onDrag = { change, amount -> change.consume(); dx += amount.x },
                    onDragEnd = {
                        var target = (timeMs + geometry.pxToTime(dx / density)).coerceIn(0L, durationMs)
                        val playheadRel = state.playheadMs - clipStartMs
                        if (abs(geometry.timeToPx(target - playheadRel)) <= SNAP_DP) target = playheadRel
                        dx = 0f
                        if (target != timeMs) state.onMove(clipId, timeMs, target.coerceIn(0L, durationMs))
                    },
                    onDragCancel = { dx = 0f },
                )
            }
            .semantics {
                this.selected = isSelected
                contentDescription = description
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(DIAMOND)
                .graphicsLayer { rotationZ = 45f }
                .clip(RoundedCornerShape(2.dp))
                .background(if (isSelected) PetruColors.Accent else PetruColors.OnBackground),
        )
    }
}
