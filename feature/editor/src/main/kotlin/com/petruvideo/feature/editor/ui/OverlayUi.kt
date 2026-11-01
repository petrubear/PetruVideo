package com.petruvideo.feature.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.designsystem.components.PetruChip
import com.petruvideo.core.designsystem.components.PillButton
import com.petruvideo.core.designsystem.components.SegmentedControl
import com.petruvideo.core.designsystem.components.SheetSurface
import com.petruvideo.core.model.CHROMA_BLUE
import com.petruvideo.core.model.CHROMA_GREEN
import com.petruvideo.core.model.ChromaKey
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.OVERLAY_MIN_MS
import com.petruvideo.core.model.OVERLAY_WIDTH_MAX
import com.petruvideo.core.model.OVERLAY_WIDTH_MIN
import com.petruvideo.core.model.OverlayClip
import com.petruvideo.core.model.TEXT_PERMILLE
import com.petruvideo.core.model.engine.TextRows
import com.petruvideo.core.model.TextClip
import com.petruvideo.feature.editor.EditorEvent
import com.petruvideo.feature.editor.OverlayPanelState
import com.petruvideo.feature.editor.TimelineGeometry
import kotlin.math.abs
import kotlin.math.roundToInt

private val ROW_HEIGHT = 36.dp
private val ROW_GAP = 4.dp
private val HANDLE_WIDTH = 24.dp
private val CLIP_SHAPE = RoundedCornerShape(6.dp)
private val SNAP_DP = 8.dp
private val MIN_HIT = 64.dp
private const val MS = 1_000L

/** The overlay panel: opacity (the place and size are set by dragging and pinching on the preview). */
@Composable
fun OverlayPanel(panel: OverlayPanelState?, onEvent: (EditorEvent) -> Unit, modifier: Modifier = Modifier) {
    if (panel == null) return
    SheetSurface(modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Overlay",
                    style = PetruType.Title,
                    color = PetruColors.OnBackground,
                    modifier = Modifier.weight(1f),
                )
                PillButton("Done", onClick = { onEvent(EditorEvent.OverlayDismiss) })
            }
            Text(panel.name, style = PetruType.Caption, color = PetruColors.OnSurfaceMuted, maxLines = 1)
            OverlayControls(panel.clip, onEvent, checker = panel.chromaChecker, picking = panel.chromaPicking)
        }
    }
}

@Composable
fun OverlayControls(
    clip: OverlayClip,
    onEvent: (EditorEvent) -> Unit,
    modifier: Modifier = Modifier,
    checker: Boolean = false,
    picking: Boolean = false,
    startOnChroma: Boolean = false,
) {
    var tab by remember { mutableIntStateOf(if (startOnChroma) 1 else 0) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SegmentedControl(listOf("Opacity", "Chroma key"), tab, {
            tab = it
            if (it == 0) onEvent(EditorEvent.ChromaPicking(false))
        })
        if (tab == 0) OpacityControls(clip, onEvent) else ChromaControls(clip, checker, picking, onEvent)
    }
}

@Composable
private fun OpacityControls(clip: OverlayClip, onEvent: (EditorEvent) -> Unit) {
    var shown by remember(clip) { mutableStateOf(clip) }
    Column(Modifier.fillMaxWidth()) {
        SettingSlider(
            "Opacity", "${shown.opacityPercent}%", shown.opacityPercent.toFloat(), 0f..PERCENT_MAX,
            {
                shown = shown.copy(opacityPercent = it.roundToInt())
                onEvent(EditorEvent.OverlayPreview(shown))
            },
            { onEvent(EditorEvent.OverlayCommit(shown)) },
        )
        Text(
            "Drag the picture in the preview to move it and pinch to resize.",
            style = PetruType.Caption,
            color = PetruColors.OnSurfaceMuted,
        )
    }
}

/** The chroma key of an overlay: on or off, the colour to remove, and how wide, soft, and clean the result is. */
@Composable
private fun ChromaControls(
    clip: OverlayClip,
    checker: Boolean,
    picking: Boolean,
    onEvent: (EditorEvent) -> Unit,
) {
    var shown by remember(clip.chroma) { mutableStateOf(clip.chroma) }
    val commit = { next: ChromaKey -> onEvent(EditorEvent.ChromaCommit(next)) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            PetruChip("Off", selected = !shown.enabled, onClick = { commit(shown.copy(enabled = false)) })
            PetruChip("On", selected = shown.enabled, onClick = { commit(shown.copy(enabled = true)) })
            PetruChip("Green", selected = shown.color == CHROMA_GREEN, onClick = {
                commit(shown.copy(enabled = true, color = CHROMA_GREEN))
            })
            PetruChip("Blue", selected = shown.color == CHROMA_BLUE, onClick = {
                commit(shown.copy(enabled = true, color = CHROMA_BLUE))
            })
            Box(
                Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(androidx.compose.ui.graphics.Color(shown.color))
                    .border(1.dp, PetruColors.OnSurfaceMuted, RoundedCornerShape(12.dp))
                    .semantics { contentDescription = "Key colour" },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PetruChip(
                if (picking) "Tap the preview" else "Pick colour", selected = picking,
                onClick = { onEvent(EditorEvent.ChromaPicking(!picking)) },
            )
            PetruChip("Checkerboard", selected = checker, onClick = { onEvent(EditorEvent.ChromaChecker(!checker)) })
        }
        ChromaSlider("Strength", shown.strength, { shown = shown.copy(strength = it) }, shown, onEvent)
        ChromaSlider("Smoothness", shown.smoothness, { shown = shown.copy(smoothness = it) }, shown, onEvent)
        ChromaSlider("Spill", shown.spill, { shown = shown.copy(spill = it) }, shown, onEvent)
    }
}

@Composable
private fun ChromaSlider(
    label: String,
    value: Int,
    set: (Int) -> Unit,
    key: ChromaKey,
    onEvent: (EditorEvent) -> Unit,
) {
    SettingSlider(
        label, "$value", value.toFloat(), 0f..PERCENT_MAX,
        {
            set(it.roundToInt())
            onEvent(EditorEvent.ChromaPreview(key.copy(enabled = true)))
        },
        { onEvent(EditorEvent.ChromaCommit(key.copy(enabled = true))) },
    )
}

private const val PERCENT_MAX = 100f

/** The Overlay lane: each clip on the first free row, trim handles when selected, long press to move. */
@Composable
fun OverlayLaneView(
    clips: List<OverlayClip>,
    media: Map<String, MediaItem>,
    geometry: TimelineGeometry,
    selectedClipId: String?,
    onSelect: (String) -> Unit,
    onMove: (clipId: String, startMs: Long) -> Unit,
    onTrim: (clipId: String, startMs: Long, endMs: Long) -> Unit,
    modifier: Modifier = Modifier,
    keyframes: KeyframeLaneState = KeyframeLaneState(),
) {
    val rows = TextRows.assign(clips.map { TextClip(it.id, "", it.startMs, it.durationMs) })
    val rowCount = (rows.values.maxOrNull() ?: -1) + 1
    val height = (ROW_HEIGHT + ROW_GAP) * rowCount - ROW_GAP
    Box(modifier.height(height.coerceAtLeast(ROW_HEIGHT))) {
        clips.forEach { clip ->
            OverlayClipBox(
                clip, media[clip.mediaItemId]?.displayName.orEmpty(), rows.getValue(clip.id), geometry,
                clip.id == selectedClipId, onSelect, onMove, onTrim, keyframes,
            )
        }
    }
}

@Composable
private fun OverlayClipBox(
    clip: OverlayClip,
    name: String,
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
        .coerceAtLeast(geometry.timeToPx(OVERLAY_MIN_MS))
    val edge = if (selected) PetruColors.Accent else PetruColors.SurfaceHighest
    val description = "Overlay, $name, starts at ${clip.startMs / MS} seconds, lasts ${clip.durationMs / MS} seconds"
    Box(
        Modifier
            .offset(x = startDp.dp, y = (ROW_HEIGHT + ROW_GAP) * row)
            .width(widthDp.dp)
            .height(ROW_HEIGHT)
            .background(PetruColors.Surface, CLIP_SHAPE)
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
            name.ifBlank { "Overlay" },
            style = PetruType.Caption,
            color = PetruColors.OnBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = 8.dp),
        )
        if (selected) {
            KeyframeDiamonds(clip.id, clip.startMs, clip.durationMs, clip.keyframes, geometry, keyframes)
            OverlayHandle(Alignment.CenterStart, "Trim overlay start", { trimStartDx += it }) {
                val start = (clip.startMs + geometry.pxToTime(trimStartDx / density)).coerceAtLeast(0)
                trimStartDx = 0f
                onTrim(clip.id, start.coerceAtMost(clip.endMs - OVERLAY_MIN_MS), clip.endMs)
            }
            OverlayHandle(Alignment.CenterEnd, "Trim overlay end", { trimEndDx += it }) {
                val end = clip.endMs + geometry.pxToTime(trimEndDx / density)
                trimEndDx = 0f
                onTrim(clip.id, clip.startMs, end.coerceAtLeast(clip.startMs + OVERLAY_MIN_MS))
            }
        }
    }
}

@Composable
private fun BoxScope.OverlayHandle(
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

/** The selection box of the selected overlay on the preview frame; drag moves it, pinch resizes it. */
@Composable
internal fun OverlaySelectionBox(
    clip: OverlayClip,
    aspect: Float,
    frameWidth: Dp,
    frameHeight: Dp,
    onEvent: (EditorEvent) -> Unit,
) {
    val density = LocalDensity.current
    val frameW = with(density) { frameWidth.toPx() }.coerceAtLeast(1f)
    val frameH = with(density) { frameHeight.toPx() }.coerceAtLeast(1f)
    var live by remember(clip) { mutableStateOf(clip) }
    var guideX by remember { mutableStateOf(false) }
    var guideY by remember { mutableStateOf(false) }
    val boxW = with(density) { (frameW * live.widthPermille / TEXT_PERMILLE).toDp() }
    val boxH = boxW / aspect
    val hitW = maxOf(boxW, MIN_HIT)
    val hitH = maxOf(boxH, MIN_HIT)
    val left = frameWidth * live.xPermille / TEXT_PERMILLE - hitW / 2
    val top = frameHeight * live.yPermille / TEXT_PERMILLE - hitH / 2
    val line = Modifier.background(PetruColors.Accent)
    if (guideX) Box(Modifier.offset(x = frameWidth / 2).width(1.dp).height(frameHeight).then(line))
    if (guideY) Box(Modifier.offset(y = frameHeight / 2).width(frameWidth).height(1.dp).then(line))
    Box(
        Modifier
            .offset(x = left, y = top)
            .width(hitW)
            .height(hitH)
            .border(2.dp, PetruColors.Accent, RoundedCornerShape(4.dp))
            .pointerInput(clip.id) {
                val snapPx = SNAP_DP.toPx()
                awaitEachGesture {
                    awaitFirstDown()
                    var current = live
                    var rawX = current.xPermille.toFloat()
                    var rawY = current.yPermille.toFloat()
                    do {
                        val event = awaitPointerEvent()
                        val pan = event.calculatePan()
                        val zoom = event.calculateZoom()
                        if (pan.x != 0f || pan.y != 0f || zoom != 1f) {
                            val step = overlayDragStep(
                                current, rawX + pan.x / frameW * TEXT_PERMILLE, rawY + pan.y / frameH * TEXT_PERMILLE,
                                zoom, frameW, frameH, snapPx,
                            )
                            rawX = step.rawX
                            rawY = step.rawY
                            guideX = step.guideX
                            guideY = step.guideY
                            current = step.clip
                            live = current
                            onEvent(EditorEvent.OverlayPreview(current))
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                    guideX = false
                    guideY = false
                    onEvent(EditorEvent.OverlayCommit(current))
                }
            }
            .semantics { contentDescription = "Selected overlay, drag to move, pinch to resize" },
    )
}

internal class OverlayDragStep(
    val rawX: Float,
    val rawY: Float,
    val guideX: Boolean,
    val guideY: Boolean,
    val clip: OverlayClip,
)

/** Snaps to the frame's center within [snapPx] pixels and keeps the width inside its limits. */
internal fun overlayDragStep(
    clip: OverlayClip,
    rawX: Float,
    rawY: Float,
    zoom: Float,
    frameW: Float,
    frameH: Float,
    snapPx: Float,
): OverlayDragStep {
    val half = TEXT_PERMILLE / 2f
    val guideX = abs(rawX - half) / TEXT_PERMILLE * frameW < snapPx
    val guideY = abs(rawY - half) / TEXT_PERMILLE * frameH < snapPx
    val x = if (guideX) half else rawX
    val y = if (guideY) half else rawY
    val width = (clip.widthPermille * zoom).toInt().coerceIn(OVERLAY_WIDTH_MIN, OVERLAY_WIDTH_MAX)
    val moved = clip.copy(
        xPermille = x.toInt().coerceIn(0, TEXT_PERMILLE),
        yPermille = y.toInt().coerceIn(0, TEXT_PERMILLE),
        widthPermille = width,
    )
    return OverlayDragStep(rawX, rawY, guideX, guideY, moved)
}
