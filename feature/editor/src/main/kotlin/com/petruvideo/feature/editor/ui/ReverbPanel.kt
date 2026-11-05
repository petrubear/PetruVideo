package com.petruvideo.feature.editor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.adaptive.Arrangement as WindowArrangement
import com.petruvideo.core.designsystem.adaptive.LocalArrangement
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.designsystem.components.PetruChip
import com.petruvideo.core.designsystem.components.PillButton
import com.petruvideo.core.designsystem.components.SheetSurface
import com.petruvideo.core.model.REVERB_DECAY_MAX_MS
import com.petruvideo.core.model.REVERB_DECAY_MIN_MS
import com.petruvideo.core.model.REVERB_PERCENT_MAX
import com.petruvideo.core.model.REVERB_PREDELAY_MAX_MS
import com.petruvideo.core.model.Reverb
import com.petruvideo.core.model.ReverbPreset
import com.petruvideo.core.model.ReverbPresets
import com.petruvideo.feature.editor.EditorEvent
import com.petruvideo.feature.editor.ReverbPanelState
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import com.petruvideo.core.designsystem.enumLabel
import androidx.compose.ui.res.stringResource
import com.petruvideo.feature.editor.R

private const val DECAY_STEP_MS = 50
private val DECAY_RATIO = REVERB_DECAY_MAX_MS.toFloat() / REVERB_DECAY_MIN_MS
private val MAX_PANEL_BODY = 280.dp
private val MAX_PANEL_BODY_TABLETOP = 120.dp

internal fun decayToPosition(decayMs: Int): Float =
    (ln(decayMs.toFloat() / REVERB_DECAY_MIN_MS) / ln(DECAY_RATIO)).coerceIn(0f, 1f)

internal fun positionToDecay(position: Float): Int {
    val raw = REVERB_DECAY_MIN_MS * DECAY_RATIO.pow(position)
    return ((raw / DECAY_STEP_MS).roundToInt() * DECAY_STEP_MS).coerceIn(REVERB_DECAY_MIN_MS, REVERB_DECAY_MAX_MS)
}

internal fun formatDecay(decayMs: Int): String = "%.2f s".format(java.util.Locale.US, decayMs / 1000f)

/** The reverb panel above the clip tool bar: switch, presets, Mix and Decay, and More for the other three. */
@Composable
fun ReverbPanel(panel: ReverbPanelState?, onEvent: (EditorEvent) -> Unit, modifier: Modifier = Modifier) {
    if (panel == null) return
    var more by remember { mutableStateOf(false) }
    SheetSurface(modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.editor_reverb),
                        style = PetruType.Title,
                        color = PetruColors.OnBackground,
                    )
                    Text(panel.clipName, style = PetruType.Caption, color = PetruColors.OnSurfaceMuted, maxLines = 1)
                }
                ReverbSwitch(panel.reverb, onEvent, Modifier.padding(horizontal = 12.dp))
                PillButton(stringResource(R.string.editor_done), onClick = { onEvent(EditorEvent.ReverbDismiss) })
            }
            val tabletop = LocalArrangement.current is WindowArrangement.Tabletop
            val maxBody = if (tabletop) MAX_PANEL_BODY_TABLETOP else MAX_PANEL_BODY
            Column(Modifier.heightIn(max = maxBody).verticalScroll(rememberScrollState())) {
                ReverbControls(panel.reverb, onEvent, showAll = more, onMore = { more = !more })
            }
        }
    }
}

@Composable
fun ReverbSwitch(reverb: Reverb, onEvent: (EditorEvent) -> Unit, modifier: Modifier = Modifier) {
    val textEditoroff = stringResource(R.string.editor_off)
    val textEditoron = stringResource(R.string.editor_on)
    val textEditorreverb = stringResource(R.string.editor_reverb)
    Switch(
        checked = reverb.enabled,
        onCheckedChange = { onEvent(EditorEvent.ReverbToggle(it)) },
        colors = SwitchDefaults.colors(
            checkedThumbColor = PetruColors.OnAccent,
            checkedTrackColor = PetruColors.Accent,
            uncheckedThumbColor = PetruColors.OnSurfaceMuted,
            uncheckedTrackColor = PetruColors.SurfaceHighest,
        ),
        modifier = modifier.semantics {
            contentDescription = textEditorreverb
            stateDescription = if (reverb.enabled) textEditoron else textEditoroff
        },
    )
}

/**
 * Presets, Mix and Decay time, and (when [showAll]) Room size, Damping, and Pre-delay. [onMore] is null where all
 * settings always show (the inspector).
 */
@Composable
fun ReverbControls(
    reverb: Reverb,
    onEvent: (EditorEvent) -> Unit,
    showAll: Boolean,
    modifier: Modifier = Modifier,
    onMore: (() -> Unit)? = null,
) {
    var shown by remember(reverb) { mutableStateOf(reverb) }
    val change = { next: Reverb ->
        shown = next
        onEvent(EditorEvent.ReverbPreview(next))
    }
    val finish = { onEvent(EditorEvent.ReverbCommit(shown)) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (shown.preset == ReverbPreset.Custom) PetruChip(
                enumLabel("reverb", ReverbPreset.Custom),
                selected = true,
                onClick = {},
            )
            ReverbPresets.all.forEach { preset ->
                PetruChip(
                    enumLabel("reverb", preset),
                    selected = shown.preset == preset,
                    onClick = { onEvent(EditorEvent.ReverbPick(preset)) },
                )
            }
        }
        SettingSlider(
            stringResource(R.string.editor_mix), "${shown.mix}%", shown.mix.toFloat(), 0f..REVERB_PERCENT_MAX.toFloat(),
            { change(shown.copy(mix = it.roundToInt())) }, finish)
        SettingSlider(stringResource(R.string.editor_decay_time), formatDecay(shown.decayMs), decayToPosition(
            shown.decayMs,
        ), 0f..1f,
            { change(shown.copy(decayMs = positionToDecay(it))) }, finish)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            if (onMore != null) {
                TextAction(
                    if (showAll) stringResource(R.string.editor_less) else stringResource(R.string.editor_more),
                    onMore,
                )
            } else {
                Spacer(Modifier)
            }
            TextAction(stringResource(R.string.editor_reset)) { onEvent(EditorEvent.ReverbReset) }
        }
        if (showAll || onMore == null) {
            SettingSlider(
                stringResource(R.string.editor_room_size),
                "${shown.roomSize}%",
                shown.roomSize.toFloat(),
                0f..REVERB_PERCENT_MAX.toFloat(),
                { change(shown.copy(roomSize = it.roundToInt())) }, finish)
            SettingSlider(
                stringResource(R.string.editor_damping),
                "${shown.damping}%",
                shown.damping.toFloat(),
                0f..REVERB_PERCENT_MAX.toFloat(),
                { change(shown.copy(damping = it.roundToInt())) }, finish)
            SettingSlider(
                stringResource(R.string.editor_predelay), "${shown.preDelayMs} ms", shown.preDelayMs.toFloat(),
                0f..REVERB_PREDELAY_MAX_MS.toFloat(), { change(shown.copy(preDelayMs = it.roundToInt())) }, finish)
        }
    }
}

@Composable
internal fun TextAction(text: String, onClick: () -> Unit) {
    Text(
        text,
        style = PetruType.Label,
        color = PetruColors.Accent,
        modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 12.dp),
    )
}

@Composable
internal fun SettingSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    onFinished: () -> Unit,
) {
    var dragging by remember { mutableStateOf(false) }
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = PetruType.Body, color = PetruColors.OnBackground)
            Text(valueText, style = PetruType.Mono, color = PetruColors.OnSurfaceMuted)
        }
        Slider(
            value = value,
            onValueChange = {
                dragging = true
                onChange(it)
            },
            onValueChangeFinished = {
                if (dragging) onFinished()
                dragging = false
            },
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = PetruColors.Accent,
                activeTrackColor = PetruColors.Accent,
                inactiveTrackColor = PetruColors.SurfaceHighest,
            ),
            modifier = Modifier.semantics {
                contentDescription = label
                stateDescription = valueText
            },
        )
    }
}
