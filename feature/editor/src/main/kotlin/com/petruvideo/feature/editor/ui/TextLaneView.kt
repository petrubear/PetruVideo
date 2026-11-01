package com.petruvideo.feature.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.model.TEXT_MAX_MS
import com.petruvideo.core.model.TEXT_MIN_MS
import com.petruvideo.core.model.TextClip
import com.petruvideo.core.model.engine.TextRows
import com.petruvideo.feature.editor.TimelineGeometry

private val ROW_HEIGHT = 36.dp
private val ROW_GAP = 4.dp
private val HANDLE_WIDTH = 24.dp
private val CLIP_SHAPE = RoundedCornerShape(6.dp)

/** The Text lane: each clip on the first free row, trim handles when selected, long press to move. */
@Composable
fun TextLaneView(
    clips: List<TextClip>,
    geometry: TimelineGeometry,
    selectedClipId: String?,
    onSelect: (String) -> Unit,
    onMove: (clipId: String, startMs: Long) -> Unit,
    onTrim: (clipId: String, startMs: Long, endMs: Long) -> Unit,
    modifier: Modifier = Modifier,
    keyframes: KeyframeLaneState = KeyframeLaneState(),
) {
    val rows = TextRows.assign(clips)
    val rowCount = TextRows.rowCount(clips)
    val height = (ROW_HEIGHT + ROW_GAP) * rowCount - ROW_GAP
    Box(modifier.height(height.coerceAtLeast(ROW_HEIGHT))) {
        clips.forEach { clip ->
            TextClipBox(
                clip, rows.getValue(clip.id), geometry, clip.id == selectedClipId, onSelect, onMove, onTrim, keyframes,
            )
        }
    }
}

@Composable
private fun TextClipBox(
    clip: TextClip,
    row: Int,
    geometry: TimelineGeometry,
    selected: Boolean,
    onSelect: (String) -> Unit,
    onMove: (String, Long) -> Unit,
    onTrim: (String, Long, Long) -> Unit,
    keyframes: KeyframeLaneState,
) {
    val density = LocalDensity.current.density
    var moveDx by remember { mutableFloatStateOf(0f) }
    var trimStartDx by remember { mutableFloatStateOf(0f) }
    var trimEndDx by remember { mutableFloatStateOf(0f) }
    val startDp = geometry.timeToPx(clip.startMs) + (moveDx + trimStartDx) / density
    val widthDp = (geometry.timeToPx(clip.durationMs) - trimStartDx / density + trimEndDx / density)
        .coerceAtLeast(geometry.timeToPx(TEXT_MIN_MS))
    val first = clip.text.lineSequence().firstOrNull().orEmpty().ifBlank { "Empty text" }
    val edge = if (selected) PetruColors.Accent else PetruColors.SurfaceHighest
    val description = "Text, $first, starts at ${clip.startMs / MS} seconds, lasts ${clip.durationMs / MS} seconds"
    Box(
        Modifier
            .offset(x = startDp.dp, y = (ROW_HEIGHT + ROW_GAP) * row)
            .width(widthDp.dp)
            .height(ROW_HEIGHT)
            .background(PetruColors.Accent.copy(alpha = 0.25f), CLIP_SHAPE)
            .border(if (selected) 2.dp else 1.dp, edge, CLIP_SHAPE)
            .clickable { onSelect(clip.id) }
            .pointerInput(clip.id, clip.startMs) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onSelect(clip.id) },
                    onDrag = { change, amount -> change.consume(); moveDx += amount.x },
                    onDragEnd = {
                        val start = (clip.startMs + geometry.pxToTime(moveDx / density)).coerceAtLeast(0)
                        moveDx = 0f
                        if (start != clip.startMs) onMove(clip.id, start)
                    },
                    onDragCancel = { moveDx = 0f },
                )
            }
            .semantics {
                role = Role.Button
                this.selected = selected
                contentDescription = description
            },
    ) {
        Text(
            first,
            style = PetruType.Caption,
            color = PetruColors.OnBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = 8.dp),
        )
        if (selected) {
            KeyframeDiamonds(clip.id, clip.startMs, clip.durationMs, clip.keyframes, geometry, keyframes)
            Handle(Alignment.CenterStart, "Trim text start", { trimStartDx += it }) {
                val start = (clip.startMs + geometry.pxToTime(trimStartDx / density)).coerceAtLeast(0)
                trimStartDx = 0f
                onTrim(clip.id, start.coerceAtMost(clip.endMs - TEXT_MIN_MS), clip.endMs)
            }
            Handle(Alignment.CenterEnd, "Trim text end", { trimEndDx += it }) {
                val end = (clip.endMs + geometry.pxToTime(trimEndDx / density)).coerceAtMost(clip.startMs + TEXT_MAX_MS)
                trimEndDx = 0f
                onTrim(clip.id, clip.startMs, end.coerceAtLeast(clip.startMs + TEXT_MIN_MS))
            }
        }
    }
}

private const val MS = 1_000L

@Composable
private fun androidx.compose.foundation.layout.BoxScope.Handle(
    alignment: Alignment,
    description: String,
    onDrag: (Float) -> Unit,
    onEnd: () -> Unit,
) {
    Box(
        Modifier
            .align(alignment)
            .width(HANDLE_WIDTH)
            .fillMaxHeight()
            .pointerInput(description) {
                detectDragGestures(
                    onDrag = { change, amount -> change.consume(); onDrag(amount.x) },
                    onDragEnd = onEnd,
                    onDragCancel = onEnd,
                )
            }
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.width(4.dp).height(18.dp).background(PetruColors.Accent, RoundedCornerShape(2.dp)))
    }
}
