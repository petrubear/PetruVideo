package com.petruvideo.feature.editor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.designsystem.adaptive.Arrangement as WindowArrangement
import com.petruvideo.core.designsystem.adaptive.LocalArrangement
import com.petruvideo.core.designsystem.components.PetruChip
import com.petruvideo.core.designsystem.components.PillButton
import com.petruvideo.core.designsystem.components.SheetSurface
import com.petruvideo.core.model.AudioFx
import com.petruvideo.core.model.ECHO_DELAY_MAX_MS
import com.petruvideo.core.model.ECHO_DELAY_MIN_MS
import com.petruvideo.core.model.ECHO_REPEATS_MAX
import com.petruvideo.core.model.EQ_BAND_MAX_DB
import com.petruvideo.core.model.EqPreset
import com.petruvideo.core.model.EqPresets
import com.petruvideo.core.model.Equalizer
import com.petruvideo.core.model.FADE_MAX_MS
import com.petruvideo.core.model.Fade
import com.petruvideo.core.model.FX_PERCENT_MAX
import com.petruvideo.core.model.NoiseReduction
import com.petruvideo.core.model.NoiseStrength
import com.petruvideo.core.model.VoiceFx
import com.petruvideo.feature.editor.ClipTool
import com.petruvideo.core.model.PITCH_MAX_SEMITONES
import com.petruvideo.feature.editor.AudioPanelState
import com.petruvideo.feature.editor.AudioToolKind
import com.petruvideo.feature.editor.EditorEvent
import kotlin.math.roundToInt
import com.petruvideo.core.designsystem.enumLabel
import androidx.compose.ui.res.stringResource
import com.petruvideo.feature.editor.R

private val MAX_PANEL_BODY = 300.dp
private val MAX_PANEL_BODY_TABLETOP = 120.dp
private const val MIN_FADE_RANGE_MS = 100L

internal fun formatSeconds(ms: Long): String = "%.1f s".format(java.util.Locale.US, ms / 1000f)

private fun formatSigned(value: Int, unit: String) = (if (value > 0) "+$value" else "$value") + " $unit"

/** The Audio list, or one audio tool's panel, above the clip tool bar. */
@Composable
fun AudioPanel(panel: AudioPanelState?, onEvent: (EditorEvent) -> Unit, modifier: Modifier = Modifier) {
    if (panel == null) return
    SheetSurface(modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(panel.kind.labelRes), style = PetruType.Title, color = PetruColors.OnBackground)
                    Text(panel.clipName, style = PetruType.Caption, color = PetruColors.OnSurfaceMuted, maxLines = 1)
                }
                if (panel.kind != AudioToolKind.List) {
                    PetruChip(
                        stringResource(R.string.editor_tools),
                        selected = false,
                        onClick = { onEvent(EditorEvent.OpenAudioTool(AudioToolKind.List)) },
                    )
                }
                PillButton(
                    stringResource(R.string.editor_done),
                    onClick = { onEvent(EditorEvent.AudioDismiss) },
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            val tabletop = LocalArrangement.current is WindowArrangement.Tabletop
            Column(
                Modifier.heightIn(max = if (tabletop) MAX_PANEL_BODY_TABLETOP else MAX_PANEL_BODY)
                    .verticalScroll(rememberScrollState()),
            ) {
                AudioPanelBody(panel, onEvent)
            }
        }
    }
}

/** The body of one tool; the inspector uses these directly. */
@Composable
fun AudioPanelBody(panel: AudioPanelState, onEvent: (EditorEvent) -> Unit) {
    when (panel.kind) {
        AudioToolKind.List -> AudioToolList(panel, onEvent)
        AudioToolKind.Fade -> FadeControls(panel.fx, panel.clipLengthMs, onEvent)
        AudioToolKind.Noise -> NoiseControls(panel.fx, onEvent)
        AudioToolKind.Eq -> EqualizerControls(panel.fx, onEvent)
        AudioToolKind.Voice -> VoiceControls(panel.fx, onEvent)
        AudioToolKind.Ducking -> panel.ducking?.let { DuckingControls(it, panel.duckingIdle, onEvent) }
    }
}

private class ToolRow(val name: String, val hint: String, val open: () -> Unit)

@Composable
private fun AudioToolList(panel: AudioPanelState, onEvent: (EditorEvent) -> Unit) {
    val fx = panel.fx
    val voiceNames = listOfNotNull(
        stringResource(R.string.editor_echo).takeIf { fx.voice.echo.enabled },
        stringResource(R.string.editor_robot).takeIf { fx.voice.robot.enabled },
        stringResource(R.string.editor_pitch).takeIf { fx.voice.pitch.enabled },
        stringResource(R.string.editor_bass).takeIf { fx.voice.bass.enabled },
    )
    val fadeHint = if (fx.fade.isNeutral) {
        stringResource(R.string.editor_off)
    } else {
        stringResource(R.string.editor_fade_hint, formatSeconds(fx.fade.inMs), formatSeconds(fx.fade.outMs))
    }
    val tool = { kind: AudioToolKind -> { onEvent(EditorEvent.OpenAudioTool(kind)) } }
    val use = { clipTool: ClipTool -> { onEvent(EditorEvent.UseTool(clipTool)) } }
    val rows = listOf(
        ToolRow(stringResource(R.string.editor_volume), "${panel.volumePercent}%", use(ClipTool.Volume)),
        ToolRow(
            stringResource(R.string.editor_mute),
            if (panel.muted) stringResource(R.string.editor_muted) else stringResource(R.string.editor_off),
            use(ClipTool.Mute),
        ),
        ToolRow(stringResource(R.string.editor_fade), fadeHint, tool(AudioToolKind.Fade)),
        ToolRow(
            stringResource(R.string.editor_noise_reduction),
            if (fx.noise.enabled) enumLabel("noise", fx.noise.strength) else stringResource(R.string.editor_off),
            tool(AudioToolKind.Noise),
        ),
        ToolRow(
            stringResource(R.string.editor_equalizer),
            if (fx.eq.isFlat) stringResource(R.string.editor_off) else enumLabel("eqpreset", fx.eq.preset),
            tool(AudioToolKind.Eq),
        ),
        ToolRow(
            stringResource(R.string.editor_voice_effects),
            voiceNames.joinToString(", ").ifEmpty { stringResource(R.string.editor_off) },
            tool(AudioToolKind.Voice),
        ),
        ToolRow(
            stringResource(R.string.editor_reverb),
            if (panel.reverb.enabled) enumLabel("reverb", panel.reverb.preset) else stringResource(R.string.editor_off),
            use(ClipTool.Reverb),
        ),
    ) + listOfNotNull(
        panel.ducking?.let {
            ToolRow(
                stringResource(R.string.editor_ducking),
                if (it.enabled) "${it.amountPercent}%" else stringResource(R.string.editor_off),
                tool(AudioToolKind.Ducking),
            )
        },
    )
    Column {
        rows.forEach { row ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .clickable(role = Role.Button, onClick = row.open)
                    .padding(vertical = 8.dp)
                    .semantics { contentDescription = "${row.name}, ${row.hint}" },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(row.name, style = PetruType.Body, color = PetruColors.OnBackground)
                Text(row.hint, style = PetruType.Mono, color = PetruColors.OnSurfaceMuted)
            }
        }
    }
}

@Composable
fun FadeControls(fx: AudioFx, clipLengthMs: Long, onEvent: (EditorEvent) -> Unit, modifier: Modifier = Modifier) {
    var shown by remember(fx) { mutableStateOf(fx) }
    val maxMs = minOf(FADE_MAX_MS, clipLengthMs / 2)
    val finish = { onEvent(EditorEvent.AudioFxCommit(shown)) }
    val change = { fade: Fade ->
        shown = shown.copy(fade = fade)
        onEvent(EditorEvent.AudioFxPreview(shown))
    }
    Column(modifier.fillMaxWidth()) {
        if (maxMs < MIN_FADE_RANGE_MS) {
            Text(
                stringResource(R.string.editor_this_clip_is_too_short),
                style = PetruType.Body,
                color = PetruColors.OnSurfaceMuted,
            )
            return
        }
        val range = 0f..maxMs.toFloat()
        val fade = shown.fade
        SettingSlider(
            stringResource(R.string.editor_fade_in), formatSeconds(fade.inMs), fade.inMs.toFloat().coerceAtMost(
                range.endInclusive,
            ), range,
            { change(fade.copy(inMs = it.roundToInt().toLong())) }, finish,
        )
        SettingSlider(
            stringResource(R.string.editor_fade_out), formatSeconds(fade.outMs), fade.outMs.toFloat().coerceAtMost(
                range.endInclusive,
            ), range,
            { change(fade.copy(outMs = it.roundToInt().toLong())) }, finish,
        )
        TextAction(stringResource(R.string.editor_reset)) { onEvent(EditorEvent.AudioFxReset(AudioToolKind.Fade)) }
    }
}

@Composable
internal fun FxSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val textEditoroff = stringResource(R.string.editor_off)
    val textEditoron = stringResource(R.string.editor_on)
    Row(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = PetruType.Body, color = PetruColors.OnBackground)
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PetruColors.OnAccent,
                checkedTrackColor = PetruColors.Accent,
                uncheckedThumbColor = PetruColors.OnSurfaceMuted,
                uncheckedTrackColor = PetruColors.SurfaceHighest,
            ),
            modifier = Modifier.semantics {
                contentDescription = label
                stateDescription = if (checked) textEditoron else textEditoroff
            },
        )
    }
}

@Composable
fun NoiseControls(fx: AudioFx, onEvent: (EditorEvent) -> Unit, modifier: Modifier = Modifier) {
    val noise = fx.noise
    val commit = { next: NoiseReduction -> onEvent(EditorEvent.AudioFxCommit(fx.copy(noise = next))) }
    Column(modifier.fillMaxWidth()) {
        FxSwitch(stringResource(R.string.editor_noise_reduction), noise.enabled) { commit(noise.copy(enabled = it)) }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NoiseStrength.entries.forEach { strength ->
                PetruChip(
                    enumLabel("noise", strength),
                    selected = noise.strength == strength,
                    onClick = { commit(noise.copy(enabled = true, strength = strength)) },
                )
            }
        }
        Text(
            stringResource(R.string.editor_best_for_steady_noise_in),
            style = PetruType.Caption,
            color = PetruColors.OnSurfaceMuted,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
fun EqualizerControls(fx: AudioFx, onEvent: (EditorEvent) -> Unit, modifier: Modifier = Modifier) {
    var shown by remember(fx) { mutableStateOf(fx) }
    val finish = { onEvent(EditorEvent.AudioFxCommit(shown)) }
    val range = -EQ_BAND_MAX_DB.toFloat()..EQ_BAND_MAX_DB.toFloat()
    val change = { eq: Equalizer ->
        shown = shown.copy(eq = eq.normalized())
        onEvent(EditorEvent.AudioFxPreview(shown))
    }
    val eq = shown.eq
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (eq.preset == EqPreset.Custom) PetruChip(
                enumLabel("eqpreset", EqPreset.Custom),
                selected = true,
                onClick = {},
            )
            EqPresets.all.forEach { preset ->
                PetruChip(
                    enumLabel("eqpreset", preset),
                    selected = eq.preset == preset,
                    onClick = { onEvent(EditorEvent.AudioFxCommit(fx.copy(eq = EqPresets.settings(preset)))) },
                )
            }
        }
        SettingSlider(stringResource(R.string.editor_low), formatSigned(eq.lowDb, "dB"), eq.lowDb.toFloat(), range,
            { change(eq.copy(lowDb = it.roundToInt())) }, finish)
        SettingSlider(stringResource(R.string.editor_mid), formatSigned(eq.midDb, "dB"), eq.midDb.toFloat(), range,
            { change(eq.copy(midDb = it.roundToInt())) }, finish)
        SettingSlider(stringResource(R.string.editor_high), formatSigned(eq.highDb, "dB"), eq.highDb.toFloat(), range,
            { change(eq.copy(highDb = it.roundToInt())) }, finish)
        TextAction(stringResource(R.string.editor_reset)) { onEvent(EditorEvent.AudioFxReset(AudioToolKind.Eq)) }
    }
}

@Composable
fun VoiceControls(fx: AudioFx, onEvent: (EditorEvent) -> Unit, modifier: Modifier = Modifier) {
    var shown by remember(fx) { mutableStateOf(fx) }
    val finish = { onEvent(EditorEvent.AudioFxCommit(shown)) }
    val change = { voice: VoiceFx ->
        shown = shown.copy(voice = voice)
        onEvent(EditorEvent.AudioFxPreview(shown))
    }
    val commit = { voice: VoiceFx -> onEvent(EditorEvent.AudioFxCommit(fx.copy(voice = voice))) }
    val saved = fx.voice
    val voice = shown.voice
    val percent = 0f..FX_PERCENT_MAX.toFloat()
    Column(modifier.fillMaxWidth()) {
        FxSwitch(
            stringResource(R.string.editor_echo),
            voice.echo.enabled,
        ) { commit(saved.copy(echo = saved.echo.copy(enabled = it))) }
        if (voice.echo.enabled) {
            val echo = voice.echo
            SettingSlider(stringResource(R.string.editor_delay), "${echo.delayMs} ms", echo.delayMs.toFloat(),
                ECHO_DELAY_MIN_MS.toFloat()..ECHO_DELAY_MAX_MS.toFloat(),
                { change(voice.copy(echo = echo.copy(delayMs = it.roundToInt()))) }, finish)
            SettingSlider(
                stringResource(R.string.editor_repeats),
                "${echo.repeats}",
                echo.repeats.toFloat(),
                1f..ECHO_REPEATS_MAX.toFloat(),
                { change(voice.copy(echo = echo.copy(repeats = it.roundToInt()))) }, finish)
        }
        FxSwitch(
            stringResource(R.string.editor_robot),
            voice.robot.enabled,
        ) { commit(saved.copy(robot = saved.robot.copy(enabled = it))) }
        if (voice.robot.enabled) {
            val robot = voice.robot
            SettingSlider(
                stringResource(R.string.editor_intensity), "${robot.intensity}%", robot.intensity.toFloat(), percent,
                { change(voice.copy(robot = robot.copy(intensity = it.roundToInt()))) }, finish)
        }
        FxSwitch(
            stringResource(R.string.editor_pitch),
            voice.pitch.enabled,
        ) { commit(saved.copy(pitch = saved.pitch.copy(enabled = it))) }
        if (voice.pitch.enabled) {
            val pitch = voice.pitch
            SettingSlider(
                stringResource(R.string.editor_shift), formatSigned(pitch.semitones, "st"), pitch.semitones.toFloat(),
                -PITCH_MAX_SEMITONES.toFloat()..PITCH_MAX_SEMITONES.toFloat(),
                { change(voice.copy(pitch = pitch.copy(semitones = it.roundToInt()))) }, finish)
        }
        FxSwitch(
            stringResource(R.string.editor_bass),
            voice.bass.enabled,
        ) { commit(saved.copy(bass = saved.bass.copy(enabled = it))) }
        if (voice.bass.enabled) {
            val bass = voice.bass
            SettingSlider(stringResource(R.string.editor_amount), "${bass.amount}%", bass.amount.toFloat(), percent,
                { change(voice.copy(bass = bass.copy(amount = it.roundToInt()))) }, finish)
        }
        TextAction(stringResource(R.string.editor_reset)) { onEvent(EditorEvent.AudioFxReset(AudioToolKind.Voice)) }
    }
}

/** Ducking of an audio-lane clip: a switch, how much it is lowered, and how smoothly it moves. */
@Composable
fun DuckingControls(
    ducking: com.petruvideo.core.model.Ducking,
    idle: Boolean,
    onEvent: (EditorEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var shown by remember(ducking) { mutableStateOf(ducking) }
    Column(modifier.fillMaxWidth()) {
        FxSwitch(stringResource(R.string.editor_lower_this_clip_while_other), shown.enabled) {
            shown = shown.copy(enabled = it)
            onEvent(EditorEvent.DuckingChange(shown))
        }
        if (shown.enabled) {
            SettingSlider(
                stringResource(R.string.editor_amount),
                "${shown.amountPercent}%",
                shown.amountPercent.toFloat(),
                0f..DUCK_RANGE_MAX,
                { shown = shown.copy(amountPercent = it.roundToInt()) },
                { onEvent(EditorEvent.DuckingChange(shown)) },
            )
            val options = com.petruvideo.core.model.DuckSmoothness.entries
            com.petruvideo.core.designsystem.components.SegmentedControl(
                options = options.map { enumLabel("ducksmooth", it) },
                selectedIndex = options.indexOf(shown.smoothness),
                onSelect = {
                    shown = shown.copy(smoothness = options[it])
                    onEvent(EditorEvent.DuckingChange(shown))
                },
            )
            if (idle) {
                Text(
                    stringResource(R.string.editor_nothing_is_playing_over_this),
                    style = PetruType.Caption,
                    color = PetruColors.OnSurfaceMuted,
                )
            }
        }
    }
}

private const val DUCK_RANGE_MAX = 100f
