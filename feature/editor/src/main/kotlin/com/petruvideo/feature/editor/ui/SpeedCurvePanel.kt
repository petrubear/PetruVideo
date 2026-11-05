package com.petruvideo.feature.editor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.designsystem.components.PetruChip
import com.petruvideo.core.designsystem.components.PillButton
import com.petruvideo.core.designsystem.components.SheetSurface
import com.petruvideo.core.model.CURVE_SPEED_MAX
import com.petruvideo.core.model.CURVE_SPEED_MIN
import com.petruvideo.core.model.SpeedCurve
import com.petruvideo.core.model.SpeedPreset
import com.petruvideo.feature.editor.EditorEvent
import com.petruvideo.feature.editor.SpeedCurvePanelState
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import com.petruvideo.core.designsystem.enumLabel
import androidx.compose.ui.res.stringResource
import com.petruvideo.feature.editor.R

private val GRAPH_HEIGHT = 110.dp
private val POINT_TARGET = 48.dp
private const val NUDGE_SPEED = 1.15
private const val NUDGE_SHARE = 0.03

/** The Speed curve panel above the clip tool bar. */
@Composable
fun SpeedCurvePanel(panel: SpeedCurvePanelState?, onEvent: (EditorEvent) -> Unit, modifier: Modifier = Modifier) {
    if (panel == null) return
    SheetSurface(modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.editor_speed_curve),
                    style = PetruType.Title,
                    color = PetruColors.OnBackground,
                    modifier = Modifier.weight(1f),
                )
                PillButton(stringResource(R.string.editor_done), onClick = { onEvent(EditorEvent.SpeedCurveDismiss) })
            }
            SpeedCurveControls(panel, onEvent)
        }
    }
}

/** Presets, the graph with its point buttons, the length readout, and Reset; also hosted by the inspector. */
@Composable
fun SpeedCurveControls(panel: SpeedCurvePanelState, onEvent: (EditorEvent) -> Unit, modifier: Modifier = Modifier) {
    var selected by remember(panel.clipId) { mutableIntStateOf(0) }
    val index = selected.coerceIn(0, panel.curve.points.lastIndex)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SpeedPreset.entries.forEach { preset ->
                PetruChip(
                    enumLabel("speedpreset", preset),
                    selected = false,
                    onClick = { onEvent(EditorEvent.SpeedCurvePreset(preset)) },
                )
            }
        }
        SpeedGraph(panel.curve, index, onSelect = { selected = it }, onEvent = onEvent)
        Readout(panel)
        PointButtons(panel.curve, index, onEvent)
        if (panel.hasCurve) TextAction(
            stringResource(R.string.editor_reset_to_constant_speed),
        ) { onEvent(EditorEvent.SpeedCurveReset) }
    }
}

@Composable
private fun Readout(panel: SpeedCurvePanelState) {
    val text = stringResource(
        R.string.editor_clip_length_readout,
        formatSeconds(panel.lengthMs),
        formatSeconds(panel.constantLengthMs),
    )
    Text(
        text,
        style = PetruType.Caption,
        color = PetruColors.OnSurfaceMuted,
        modifier = Modifier.semantics { contentDescription = text; liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun PointButtons(curve: SpeedCurve, index: Int, onEvent: (EditorEvent) -> Unit) {
    val point = curve.points[index]
    val change = { next: SpeedCurve? -> next?.let { onEvent(EditorEvent.SpeedCurveCommit(it)) } }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        val slower = (point.speedMilli / NUDGE_SPEED).roundToInt()
        val faster = (point.speedMilli * NUDGE_SPEED).roundToInt()
        TextAction(
            stringResource(R.string.editor_slower),
        ) { change(curve.withPointMoved(index, point.positionMs, slower)) }
        TextAction(
            stringResource(R.string.editor_faster),
        ) { change(curve.withPointMoved(index, point.positionMs, faster)) }
        val step = (curve.sourceLengthMs * NUDGE_SHARE).roundToLong()
        TextAction(
            stringResource(R.string.editor_earlier),
        ) { change(curve.withPointMoved(index, point.positionMs - step, point.speedMilli)) }
        TextAction(
            stringResource(R.string.editor_later),
        ) { change(curve.withPointMoved(index, point.positionMs + step, point.speedMilli)) }
        TextAction(stringResource(R.string.editor_delete_point)) { change(curve.withPointRemoved(index)) }
    }
}

private fun speedToFraction(speedMilli: Int): Float {
    val span = ln(CURVE_SPEED_MAX.toDouble() / CURVE_SPEED_MIN)
    return (ln(speedMilli.toDouble() / CURVE_SPEED_MIN) / span).toFloat()
}

private fun fractionToSpeed(fraction: Float): Int {
    val span = ln(CURVE_SPEED_MAX.toDouble() / CURVE_SPEED_MIN)
    return (CURVE_SPEED_MIN * exp(fraction.coerceIn(0f, 1f) * span)).roundToInt()
}

/** The curve as a graph: time along x, speed on a log scale from 0.1x (bottom) to 10x (top), points to drag. */
@Composable
private fun SpeedGraph(
    curve: SpeedCurve,
    selected: Int,
    onSelect: (Int) -> Unit,
    onEvent: (EditorEvent) -> Unit,
) {
    val textEditordeletepoint = stringResource(R.string.editor_delete_point)
    BoxWithConstraints(
        Modifier.fillMaxWidth().height(GRAPH_HEIGHT + POINT_TARGET).padding(horizontal = POINT_TARGET / 2),
    ) {
        val density = LocalDensity.current
        val width: Dp = maxWidth
        val widthPx = with(density) { width.toPx() }
        val heightPx = with(density) { GRAPH_HEIGHT.toPx() }
        val length = curve.sourceLengthMs.coerceAtLeast(1)
        var draft by remember(curve) { mutableStateOf(curve) }
        val shown = if (draft.points.size == curve.points.size) draft else curve
        Box(Modifier.offset(y = POINT_TARGET / 2).fillMaxWidth().height(GRAPH_HEIGHT)) {
            GraphLines(shown, widthPx, heightPx, length) { positionMs ->
                curve.withPointAdded(positionMs)?.let { onEvent(EditorEvent.SpeedCurveCommit(it)) }
            }
            shown.points.forEachIndexed { i, point ->
                val x = width * (point.positionMs.toFloat() / length)
                val y = GRAPH_HEIGHT * (1f - speedToFraction(point.speedMilli))
                val pointText =
                    if (i == selected) R.string.editor_point_description_selected else R.string.editor_point_description
                val pointDescription = stringResource(
                    pointText,
                    i + 1,
                    formatSeconds(point.positionMs),
                    "%.1f".format(point.speedMilli / SPEED_LABEL_DIVISOR),
                )
                Box(
                    Modifier
                        .offset(x - POINT_TARGET / 2, y - POINT_TARGET / 2)
                        .size(POINT_TARGET)
                        .pointerInput(i, curve, widthPx, heightPx) {
                            detectDragGestures(
                                onDragStart = { onSelect(i) },
                                onDrag = { change, drag ->
                                    change.consume()
                                    val p = draft.points[i]
                                    val position = p.positionMs + (drag.x / widthPx * length).roundToLong()
                                    val fraction = speedToFraction(p.speedMilli) - drag.y / heightPx
                                    draft.withPointMoved(i, position, fractionToSpeed(fraction))?.let {
                                        draft = it
                                        onEvent(EditorEvent.SpeedCurveDraft(it))
                                    }
                                },
                                onDragEnd = { onEvent(EditorEvent.SpeedCurveCommit(draft)) },
                                onDragCancel = { draft = curve },
                            )
                        }
                        .semantics {
                            contentDescription = pointDescription
                            customActions = listOf(
                                CustomAccessibilityAction(textEditordeletepoint) {
                                    curve.withPointRemoved(i)?.let { onEvent(EditorEvent.SpeedCurveCommit(it)) } != null
                                },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(if (i == selected) SELECTED_DOT else DOT)
                            .background(
                                if (i == selected) PetruColors.Accent else PetruColors.OnBackground, CircleShape,
                            ),
                    )
                }
            }
        }
    }
}

private val DOT = 12.dp
private val SELECTED_DOT = 18.dp
private const val SPEED_LABEL_DIVISOR = 1000f

@Composable
private fun GraphLines(
    curve: SpeedCurve,
    widthPx: Float,
    heightPx: Float,
    length: Long,
    onTap: (Long) -> Unit,
) {
    val grid = PetruColors.Outline
    val line = PetruColors.Accent
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(GRAPH_HEIGHT)
            .background(PetruColors.Background)
            .pointerInput(widthPx, length) {
                detectTapGestures { offset -> onTap((offset.x / widthPx * length).roundToLong()) }
            },
    ) {
        for (speed in listOf(CURVE_SPEED_MIN, 1_000, CURVE_SPEED_MAX)) {
            val y = heightPx * (1f - speedToFraction(speed))
            drawLine(grid, Offset(0f, y), Offset(widthPx, y), strokeWidth = 1f)
        }
        val path = Path()
        val steps = GRAPH_STEPS
        for (s in 0..steps) {
            val position = length * s / steps.toDouble()
            val y = heightPx * (1f - speedToFraction((curve.speedAt(position) * SPEED_LABEL_DIVISOR).roundToInt()))
            val x = widthPx * s / steps
            if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, line, style = Stroke(width = LINE_WIDTH))
    }
}

private const val GRAPH_STEPS = 60
private const val LINE_WIDTH = 4f
