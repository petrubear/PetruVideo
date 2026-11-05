package com.petruvideo.feature.editor.ui.expanded

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.designsystem.components.PetruChip
import com.petruvideo.core.model.AudioFx
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.OverlayClip
import com.petruvideo.core.model.TextClip
import com.petruvideo.core.model.Reverb
import com.petruvideo.core.model.formatDuration
import com.petruvideo.feature.editor.ClipTool
import com.petruvideo.feature.editor.EditorEvent
import com.petruvideo.feature.editor.FreezePanelState
import com.petruvideo.feature.editor.SpeedCurvePanelState
import com.petruvideo.feature.editor.ui.SpeedCurveControls
import com.petruvideo.feature.editor.SheetState
import com.petruvideo.feature.editor.ui.FreezeControls
import com.petruvideo.feature.editor.TransformPanelState
import com.petruvideo.feature.editor.ui.TransformControls
import com.petruvideo.feature.editor.FilterPanelState
import com.petruvideo.feature.editor.ui.AdjustControls
import com.petruvideo.feature.editor.ui.CompressorControls
import com.petruvideo.feature.editor.ui.EqualizerControls
import com.petruvideo.feature.editor.ui.FadeControls
import com.petruvideo.feature.editor.ui.LooksControls
import com.petruvideo.feature.editor.KeyframePanelState
import com.petruvideo.feature.editor.ui.KeyframeControls
import com.petruvideo.feature.editor.ui.OverlayControls
import com.petruvideo.feature.editor.ui.NoiseControls
import com.petruvideo.feature.editor.ui.ReverbControls
import com.petruvideo.feature.editor.ui.ReverbSwitch
import com.petruvideo.feature.editor.ui.SpeedVolumeControls
import com.petruvideo.feature.editor.ui.TextAnimationControls
import com.petruvideo.feature.editor.ui.TextEditControls
import com.petruvideo.feature.editor.ui.TextStyleControls
import com.petruvideo.feature.editor.ui.VoiceControls
import androidx.compose.ui.res.stringResource
import com.petruvideo.feature.editor.R
import com.petruvideo.core.designsystem.UiText

/** What the inspector shows about the selected clip. */
data class InspectorClip(val name: String, val label: String, val lengthMs: Long, val startMs: Long)

/**
 * The right pane of the unfolded editor: selected clip name and length, and its Speed and Volume controls (the same
 * controls as the bottom sheet). Controls that do not apply to the clip are listed in [unavailable] with the reason.
 */
@Composable
fun InspectorPanel(
    clip: InspectorClip?,
    settings: List<SheetState>,
    unavailable: Map<ClipTool, UiText>,
    reverb: Reverb?,
    muted: Boolean,
    fx: AudioFx?,
    clipLengthMs: Long,
    text: TextClip?,
    filter: FilterPanelState?,
    overlay: OverlayClip?,
    keyframes: KeyframePanelState?,
    onEvent: (EditorEvent) -> Unit,
    modifier: Modifier = Modifier,
    transform: TransformPanelState? = null,
    freeze: FreezePanelState? = null,
    curve: SpeedCurvePanelState? = null,
    chromaChecker: Boolean = false,
    chromaPicking: Boolean = false,
) {
    Column(
        modifier.fillMaxHeight().background(PetruColors.Surface).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.editor_inspector), style = PetruType.Label, color = PetruColors.OnBackground)
            if (clip != null) {
                Text(
                    "${clip.label} · ${"%.1f".format(clip.lengthMs / MS_PER_SECOND)}s",
                    style = PetruType.Mono,
                    color = PetruColors.Accent,
                )
            }
        }
        if (clip == null) {
            Text(
                stringResource(R.string.editor_select_a_clip_to_see),
                style = PetruType.Body,
                color = PetruColors.OnSurfaceMuted,
            )
        } else {
            Text(clip.name, style = PetruType.Body, color = PetruColors.OnBackground)
            Text(
                stringResource(R.string.editor_starts_at, formatDuration(clip.startMs)),
                style = PetruType.Mono,
                color = PetruColors.OnSurfaceMuted,
            )
            settings.forEach { sheet ->
                Text(
                    stringResource(sheet.tool.labelRes),
                    style = PetruType.Overline,
                    color = PetruColors.OnSurfaceMuted,
                )
                SpeedVolumeControls(sheet, onEvent)
            }
            if (ClipTool.Mute !in unavailable) {
                Text(
                    stringResource(R.string.editor_sound),
                    style = PetruType.Overline,
                    color = PetruColors.OnSurfaceMuted,
                )
                PetruChip(
                    if (muted) stringResource(R.string.editor_unmute) else stringResource(R.string.editor_mute),
                    selected = muted,
                    onClick = { onEvent(EditorEvent.UseTool(ClipTool.Mute)) },
                )
            }
            if (reverb != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        stringResource(R.string.editor_reverb),
                        style = PetruType.Overline,
                        color = PetruColors.OnSurfaceMuted,
                    )
                    ReverbSwitch(reverb, onEvent)
                }
                ReverbControls(reverb, onEvent, showAll = true)
            }
            if (overlay != null) {
                Text(
                    stringResource(R.string.editor_overlay),
                    style = PetruType.Overline,
                    color = PetruColors.OnSurfaceMuted,
                )
                OverlayControls(overlay, onEvent, checker = chromaChecker, picking = chromaPicking)
            }
            VideoToolSections(freeze, transform, keyframes, curve, onEvent)
            if (filter != null) {
                Text(
                    stringResource(R.string.editor_filters),
                    style = PetruType.Overline,
                    color = PetruColors.OnSurfaceMuted,
                )
                LooksControls(filter, onEvent)
                Text(
                    stringResource(R.string.editor_adjust),
                    style = PetruType.Overline,
                    color = PetruColors.OnSurfaceMuted,
                )
                AdjustControls(filter.filter, onEvent)
            }
            if (text != null) {
                Text(
                    stringResource(R.string.editor_text),
                    style = PetruType.Overline,
                    color = PetruColors.OnSurfaceMuted,
                )
                TextEditControls(text, onEvent)
                Text(
                    stringResource(R.string.editor_style),
                    style = PetruType.Overline,
                    color = PetruColors.OnSurfaceMuted,
                )
                TextStyleControls(text, onEvent)
                Text(
                    stringResource(R.string.editor_animation),
                    style = PetruType.Overline,
                    color = PetruColors.OnSurfaceMuted,
                )
                TextAnimationControls(text, onEvent)
            }
            if (fx != null) {
                Text(
                    stringResource(R.string.editor_fade),
                    style = PetruType.Overline,
                    color = PetruColors.OnSurfaceMuted,
                )
                FadeControls(fx, clipLengthMs, onEvent)
                Text(
                    stringResource(R.string.editor_noise_reduction),
                    style = PetruType.Overline,
                    color = PetruColors.OnSurfaceMuted,
                )
                NoiseControls(fx, onEvent)
                Text(
                    stringResource(R.string.editor_equalizer),
                    style = PetruType.Overline,
                    color = PetruColors.OnSurfaceMuted,
                )
                EqualizerControls(fx, onEvent)
                Text(
                    stringResource(R.string.editor_compressor),
                    style = PetruType.Overline,
                    color = PetruColors.OnSurfaceMuted,
                )
                CompressorControls(fx, onEvent)
                Text(
                    stringResource(R.string.editor_voice_effects),
                    style = PetruType.Overline,
                    color = PetruColors.OnSurfaceMuted,
                )
                VoiceControls(fx, onEvent)
            }
            unavailable.forEach { (tool, reason) ->
                Text(
                    "${stringResource(tool.labelRes)}: $reason",
                    style = PetruType.Caption,
                    color = PetruColors.OnSurfaceMuted,
                )
            }
        }
    }
}

internal fun inspectorClip(
    name: String,
    label: String,
    lengthMs: Long,
    startMs: Long,
    media: MediaItem?,
): InspectorClip = InspectorClip(media?.displayName ?: name, label, lengthMs, startMs)

private const val MS_PER_SECOND = 1_000f

/** The Freeze frame, Transform, and Keyframes sections of the inspector, for the tools that are open. */
@Composable
private fun VideoToolSections(
    freeze: FreezePanelState?,
    transform: TransformPanelState?,
    keyframes: KeyframePanelState?,
    curve: SpeedCurvePanelState?,
    onEvent: (EditorEvent) -> Unit,
) {
    if (curve != null) {
        Text(
            stringResource(R.string.editor_speed_curve),
            style = PetruType.Overline,
            color = PetruColors.OnSurfaceMuted,
        )
        SpeedCurveControls(curve, onEvent)
    }
    if (freeze != null) {
        Text(
            stringResource(R.string.editor_freeze_frame),
            style = PetruType.Overline,
            color = PetruColors.OnSurfaceMuted,
        )
        FreezeControls(freeze, onEvent)
    }
    if (transform != null) {
        Text(stringResource(R.string.editor_transform), style = PetruType.Overline, color = PetruColors.OnSurfaceMuted)
        TransformControls(transform, onEvent)
    }
    if (keyframes != null) {
        Text(stringResource(R.string.editor_keyframes), style = PetruType.Overline, color = PetruColors.OnSurfaceMuted)
        KeyframeControls(keyframes, onEvent)
    }
}
