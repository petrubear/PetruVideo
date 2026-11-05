package com.petruvideo.feature.editor.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.media.text.TextRenderer
import com.petruvideo.core.model.FRAMING_ZOOM_MIN
import com.petruvideo.core.model.Framing
import com.petruvideo.core.model.OverlayClip
import com.petruvideo.core.model.Pose
import com.petruvideo.core.model.TEXT_PERMILLE
import com.petruvideo.core.model.TEXT_SIZE_MAX
import com.petruvideo.core.model.TEXT_SIZE_MIN
import com.petruvideo.core.model.TextClip
import com.petruvideo.feature.editor.EditorEvent
import com.petruvideo.feature.editor.EditorState
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import androidx.compose.ui.res.stringResource
import com.petruvideo.feature.editor.R
import com.petruvideo.core.designsystem.described

private val SNAP_DP = 8.dp
private val MIN_HIT = 96.dp

/** The player with the selected text clip's selection box on top; drag moves it, pinch scales it. */
@Composable
fun PreviewWithText(
    state: EditorState,
    player: Player?,
    onEvent: (EditorEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        PreviewSurface(player, Modifier.fillMaxSize())
        val overlay = state.selectedOverlay
        val clip = state.selectedText
        val format = state.timeline?.format
        val playhead = state.playback.positionMs
        if (clip != null && format != null && playhead in clip.startMs until clip.endMs) {
            TextPreviewLayer(clip, format.width.toFloat() / format.height, onEvent)
        }
        val framing = state.keyframePanel?.framing
        if (framing != null && format != null) {
            FramingGestureLayer(framing, format.width.toFloat() / format.height, onEvent)
        }
        if (overlay != null && format != null && playhead in overlay.startMs until overlay.endMs) {
            OverlayPreviewLayer(overlay, state.selectedOverlayAspect, format.width.toFloat() / format.height, onEvent)
        }
        if (state.chromaPicking && format != null) {
            ChromaPickLayer(format.width.toFloat() / format.height, onEvent)
        }
    }
}

/** While picking a key colour, a tap on the frame sends its place as permille of the frame. */
@Composable
private fun ChromaPickLayer(aspect: Float, onEvent: (EditorEvent) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val frameWidth: Dp = min(maxWidth.value, maxHeight.value * aspect).dp
        val frameHeight: Dp = frameWidth / aspect
        Box(
            Modifier
                .align(androidx.compose.ui.Alignment.Center)
                .width(frameWidth)
                .height(frameHeight)
                .border(2.dp, PetruColors.Accent)
                .described(R.string.editor_tap_the_colour_to_remove)
                .pointerInput(Unit) {
                    detectTapGestures { tap ->
                        onEvent(
                            EditorEvent.ChromaPick(
                                (tap.x / size.width * TEXT_PERMILLE).roundToInt(),
                                (tap.y / size.height * TEXT_PERMILLE).roundToInt(),
                            ),
                        )
                    }
                },
        )
    }
}

/** The video frame is fitted inside this layer; the selected overlay's box is placed on that frame. */
@Composable
private fun OverlayPreviewLayer(
    clip: OverlayClip,
    overlayAspect: Float,
    aspect: Float,
    onEvent: (EditorEvent) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val frameWidth: Dp = min(maxWidth.value, maxHeight.value * aspect).dp
        val frameHeight: Dp = frameWidth / aspect
        Box(Modifier.align(androidx.compose.ui.Alignment.Center).width(frameWidth).height(frameHeight)) {
            OverlaySelectionBox(clip, overlayAspect, frameWidth, frameHeight, onEvent)
        }
    }
}

/** The video frame is fitted inside this layer; the selection box is placed on that frame. */
@Composable
private fun TextPreviewLayer(clip: TextClip, aspect: Float, onEvent: (EditorEvent) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val frameWidth: Dp = min(maxWidth.value, maxHeight.value * aspect).dp
        val frameHeight: Dp = frameWidth / aspect
        Box(Modifier.align(androidx.compose.ui.Alignment.Center).width(frameWidth).height(frameHeight)) {
            SelectionBox(clip, frameWidth, frameHeight, onEvent)
        }
    }
}

@Composable
private fun SelectionBox(clip: TextClip, frameWidth: Dp, frameHeight: Dp, onEvent: (EditorEvent) -> Unit) {
    val density = LocalDensity.current
    val frameW = with(density) { frameWidth.toPx() }.toInt().coerceAtLeast(1)
    val frameH = with(density) { frameHeight.toPx() }.toInt().coerceAtLeast(1)
    var live by remember(clip) { mutableStateOf(clip) }
    var guideX by remember { mutableStateOf(false) }
    var guideY by remember { mutableStateOf(false) }
    val size = remember(live.text, live.style, live.sizePermille, frameW, frameH) {
        TextRenderer.render(live, frameW, frameH).let { it.width to it.height }
    }
    val box = TextRenderer.place(live, size.first, size.second, frameW, frameH)
    val boxWidth = with(density) { maxOf(size.first.toDp(), MIN_HIT) }
    val boxHeight = with(density) { maxOf(size.second.toDp(), MIN_HIT) }
    val left = with(density) { box.centerX.toDp() } - boxWidth / 2
    val top = with(density) { box.centerY.toDp() } - boxHeight / 2
    CenterGuides(guideX, guideY, frameWidth, frameHeight)
    Box(
        Modifier
            .offset(x = left, y = top)
            .width(boxWidth)
            .height(boxHeight)
            .border(2.dp, PetruColors.Accent, RoundedCornerShape(4.dp))
            .pointerInput(clip.id) {
                val snapPx = SNAP_DP.toPx()
                awaitEachGesture {
                    awaitFirstDown()
                    var current = live
                    // The finger's own position; snapping to the center only changes what is shown.
                    var rawX = current.xPermille.toFloat()
                    var rawY = current.yPermille.toFloat()
                    do {
                        val event = awaitPointerEvent()
                        val pan = event.calculatePan()
                        val zoom = event.calculateZoom()
                        if (pan.x != 0f || pan.y != 0f || zoom != 1f) {
                            val nextX = rawX + pan.x / frameW * TEXT_PERMILLE
                            val nextY = rawY + pan.y / frameH * TEXT_PERMILLE
                            val step = dragStep(current, nextX, nextY, zoom, frameW, frameH, snapPx)
                            rawX = step.rawX
                            rawY = step.rawY
                            guideX = step.guideX
                            guideY = step.guideY
                            current = step.clip
                            live = current
                            onEvent(EditorEvent.TextPreview(current))
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                    guideX = false
                    guideY = false
                    onEvent(EditorEvent.TextCommit(current))
                }
            }
            .described(R.string.editor_selected_text_drag_to_move),
    )
}

@Composable
private fun CenterGuides(vertical: Boolean, horizontal: Boolean, frameWidth: Dp, frameHeight: Dp) {
    val line = Modifier.background(PetruColors.Accent)
    if (vertical) Box(Modifier.offset(x = frameWidth / 2).width(1.dp).height(frameHeight).then(line))
    if (horizontal) Box(Modifier.offset(y = frameHeight / 2).width(frameWidth).height(1.dp).then(line))
}

/** The result of one drag or pinch step: the finger's own position, the clip as shown, and the center guides. */
internal class DragStep(
    val rawX: Float,
    val rawY: Float,
    val guideX: Boolean,
    val guideY: Boolean,
    val clip: TextClip,
)

/** Snaps to the frame's center within [snapPx] pixels and keeps the size inside its limits. */
internal fun dragStep(
    clip: TextClip,
    rawX: Float,
    rawY: Float,
    zoom: Float,
    frameW: Int,
    frameH: Int,
    snapPx: Float,
): DragStep {
    val half = TEXT_PERMILLE / 2f
    val guideX = abs(rawX - half) / TEXT_PERMILLE * frameW < snapPx
    val guideY = abs(rawY - half) / TEXT_PERMILLE * frameH < snapPx
    val x = if (guideX) half else rawX
    val y = if (guideY) half else rawY
    val size = (clip.sizePermille * zoom).toInt().coerceIn(TEXT_SIZE_MIN, TEXT_SIZE_MAX)
    val moved = clip.copy(
        xPermille = x.toInt().coerceIn(0, TEXT_PERMILLE),
        yPermille = y.toInt().coerceIn(0, TEXT_PERMILLE),
        sizePermille = size,
    )
    return DragStep(rawX, rawY, guideX, guideY, moved)
}

/** Drag pans and pinch zooms the selected video clip at the playhead; the frame is fitted inside this layer. */
@Composable
private fun FramingGestureLayer(pose: Pose, aspect: Float, onEvent: (EditorEvent) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val frameWidth: Dp = min(maxWidth.value, maxHeight.value * aspect).dp
        val frameHeight: Dp = frameWidth / aspect
        val density = LocalDensity.current
        val frameW = with(density) { frameWidth.toPx() }.coerceAtLeast(1f)
        val frameH = with(density) { frameHeight.toPx() }.coerceAtLeast(1f)
        var live by remember(pose) { mutableStateOf(pose) }
        Box(
            Modifier
                .align(androidx.compose.ui.Alignment.Center)
                .width(frameWidth)
                .height(frameHeight)
                .pointerInput(pose.sizePermille > 0) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var current = live
                        do {
                            val event = awaitPointerEvent()
                            val pan = event.calculatePan()
                            val zoom = event.calculateZoom()
                            if (pan.x != 0f || pan.y != 0f || zoom != 1f) {
                                current = framingStep(current, pan.x / frameW, pan.y / frameH, zoom)
                                live = current
                                onEvent(EditorEvent.FramingPreview(current))
                                event.changes.forEach { it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                        if (current != pose) onEvent(EditorEvent.FramingCommit(current))
                    }
                }
                .described(R.string.editor_video_picture_drag_to_move),
        )
    }
}

/**
 * [pose] after the finger moved by ([dx], [dy]) frame widths and heights and pinched by [zoom]. Dragging the picture
 * right shows more of its left side, so the pan runs the other way, and a zoomed picture moves less per finger move.
 */
internal fun framingStep(pose: Pose, dx: Float, dy: Float, zoom: Float): Pose {
    val size = (pose.sizePermille * zoom).roundToInt()
    val level = pose.sizePermille.toFloat() / FRAMING_ZOOM_MIN
    return Framing.clamp(
        pose.copy(
            xPermille = pose.xPermille - (dx * TEXT_PERMILLE / level).roundToInt(),
            yPermille = pose.yPermille - (dy * TEXT_PERMILLE / level).roundToInt(),
            sizePermille = size,
        ),
    )
}
