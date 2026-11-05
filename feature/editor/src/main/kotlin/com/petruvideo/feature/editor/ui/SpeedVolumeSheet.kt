package com.petruvideo.feature.editor.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.designsystem.components.PetruChip
import com.petruvideo.core.designsystem.components.PillButton
import com.petruvideo.core.designsystem.components.SheetSurface
import com.petruvideo.core.model.SPEED_MAX_MILLI
import com.petruvideo.core.model.SPEED_MIN_MILLI
import com.petruvideo.core.model.SPEED_UNIT
import com.petruvideo.core.model.VOLUME_DEFAULT_PERCENT
import com.petruvideo.core.model.VOLUME_MAX_PERCENT
import com.petruvideo.core.model.formatDuration
import com.petruvideo.core.model.timelineLengthMs
import com.petruvideo.feature.editor.ClipTool
import com.petruvideo.feature.editor.EditorEvent
import com.petruvideo.feature.editor.SheetState
import com.petruvideo.feature.editor.formatSpeed
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import androidx.compose.ui.res.stringResource
import com.petruvideo.feature.editor.R

private val QUICK_SPEEDS = listOf(250, 500, 1000, 2000, 4000)
private const val VOLUME_STEP = 5
private const val SPEED_STEP = 10
private const val SNAP_WINDOW = 0.015f
private val LOG2 = ln(2f)

/** Slider position (0..1) for a speed: equal steps per doubling between 0.25x and 4x. */
internal fun speedToPosition(speedMilli: Int): Float =
    (ln(speedMilli.toFloat() / SPEED_MIN_MILLI) / LOG2 / SPEED_OCTAVES).coerceIn(0f, 1f)

/** Speed for a slider position, snapped to the quick choices when close and otherwise to 0.01x. */
internal fun positionToSpeed(position: Float): Int {
    QUICK_SPEEDS.firstOrNull { kotlin.math.abs(speedToPosition(it) - position) < SNAP_WINDOW }?.let { return it }
    val raw = SPEED_MIN_MILLI * 2f.pow(position * SPEED_OCTAVES)
    return ((raw / SPEED_STEP).roundToInt() * SPEED_STEP).coerceIn(SPEED_MIN_MILLI, SPEED_MAX_MILLI)
}

private const val SPEED_OCTAVES = 4f

private fun quickLabel(milli: Int) = when (milli) {
    SPEED_MIN_MILLI -> "0.25x"
    500 -> "0.5x"
    SPEED_UNIT -> "1x"
    2000 -> "2x"
    else -> "4x"
}

/**
 * The Speed or Volume control above the clip tool bar, so the timeline and preview stay visible. Used on phone and
 * tabletop layouts and below 840 dp wide; the inspector hosts the same [SpeedVolumeControls] on wider windows.
 */
@Composable
fun SpeedVolumeSheet(sheet: SheetState?, onEvent: (EditorEvent) -> Unit, modifier: Modifier = Modifier) {
    if (sheet == null) return
    val speed = sheet.tool == ClipTool.Speed
    var shown by remember(sheet.clipId, sheet.tool, sheet.value) { mutableIntStateOf(sheet.value) }
    val length = formatDuration(timelineLengthMs(sheet.sourceLengthMs, shown))
    SheetSurface(modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(sheet.tool.labelRes), style = PetruType.Title, color = PetruColors.OnBackground)
                    Text(
                        if (speed) "${sheet.clipName} · $length" else sheet.clipName,
                        style = PetruType.Caption,
                        color = PetruColors.OnSurfaceMuted,
                        maxLines = 1,
                    )
                }
                Readout(speed, shown, Modifier.padding(horizontal = 12.dp))
                PillButton(stringResource(R.string.editor_done), onClick = { onEvent(EditorEvent.SheetDismiss) })
            }
            SpeedVolumeControls(sheet, onEvent, showReadout = false, onShown = { shown = it })
        }
    }
}

@Composable
private fun Readout(speed: Boolean, shown: Int, modifier: Modifier = Modifier) {
    val boost = !speed && shown > VOLUME_DEFAULT_PERCENT
    Column(modifier, horizontalAlignment = Alignment.End) {
        Text(
            if (speed) formatSpeed(shown) else "$shown%",
            style = PetruType.Display,
            color = if (boost) PetruColors.Boost else PetruColors.Accent,
        )
        if (boost) Text(stringResource(R.string.editor_boost), style = PetruType.Overline, color = PetruColors.Boost)
    }
}

/**
 * Readout, slider, quick choices, and Reset for one tool. The value shown follows the finger while dragging and the
 * clip's committed value otherwise.
 */
@Composable
fun SpeedVolumeControls(
    sheet: SheetState,
    onEvent: (EditorEvent) -> Unit,
    modifier: Modifier = Modifier,
    showReadout: Boolean = true,
    onShown: (Int) -> Unit = {},
) {
    val tool = sheet.tool
    val speed = tool == ClipTool.Speed
    var shown by remember(sheet.clipId, tool, sheet.value) { mutableIntStateOf(sheet.value) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (showReadout) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (speed) {
                    Text(
                        stringResource(
                            R.string.editor_length_value,
                            formatDuration(timelineLengthMs(sheet.sourceLengthMs, shown)),
                        ),
                        style = PetruType.Mono,
                        color = PetruColors.OnSurfaceMuted,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                Readout(speed, shown)
            }
        }
        val preview = { value: Int ->
            shown = value
            onShown(value)
            onEvent(EditorEvent.SheetPreview(tool, value))
        }
        val commit = { value: Int -> onEvent(EditorEvent.SheetCommit(tool, value)) }
        if (speed) SpeedSlider(shown, preview, commit) else VolumeSlider(shown, preview, commit)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (speed) {
                QUICK_SPEEDS.forEach { milli ->
                    PetruChip(
                        quickLabel(milli),
                        selected = shown == milli,
                        onClick = { shown = milli; onShown(milli); commit(milli) },
                    )
                }
            }
            PetruChip(
                stringResource(R.string.editor_reset),
                selected = false,
                onClick = { onEvent(EditorEvent.SheetReset(tool)) },
            )
        }
    }
}

@Composable
private fun SpeedSlider(value: Int, onPreview: (Int) -> Unit, onCommit: (Int) -> Unit) {
    val textEditorspeed = stringResource(R.string.editor_speed)
    var dragging by remember { mutableStateOf(false) }
    Slider(
        value = speedToPosition(value),
        onValueChange = {
            dragging = true
            onPreview(positionToSpeed(it))
        },
        onValueChangeFinished = {
            if (dragging) onCommit(value)
            dragging = false
        },
        colors = sliderColors(PetruColors.Accent),
        modifier = Modifier.fillMaxWidth().semantics {
            contentDescription = textEditorspeed
            stateDescription = formatSpeed(value)
        },
    )
}

@Composable
private fun VolumeSlider(value: Int, onPreview: (Int) -> Unit, onCommit: (Int) -> Unit) {
    val textEditorvolume = stringResource(R.string.editor_volume)
    val percentText = stringResource(R.string.editor_percent_value, value)
    var dragging by remember { mutableStateOf(false) }
    Slider(
        value = value.toFloat(),
        onValueChange = {
            dragging = true
            onPreview((it / VOLUME_STEP).roundToInt() * VOLUME_STEP)
        },
        onValueChangeFinished = {
            if (dragging) onCommit(value)
            dragging = false
        },
        valueRange = 0f..VOLUME_MAX_PERCENT.toFloat(),
        steps = VOLUME_MAX_PERCENT / VOLUME_STEP - 1,
        colors = sliderColors(if (value > VOLUME_DEFAULT_PERCENT) PetruColors.Boost else PetruColors.Accent),
        modifier = Modifier.fillMaxWidth().semantics {
            contentDescription = textEditorvolume
            stateDescription = percentText
        },
    )
}

@Composable
private fun sliderColors(active: androidx.compose.ui.graphics.Color) = SliderDefaults.colors(
    thumbColor = active,
    activeTrackColor = active,
    inactiveTrackColor = PetruColors.SurfaceHighest,
    activeTickColor = active,
    inactiveTickColor = PetruColors.SurfaceHighest,
)
