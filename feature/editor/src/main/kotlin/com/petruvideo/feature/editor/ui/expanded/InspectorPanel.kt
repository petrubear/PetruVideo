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
import com.petruvideo.feature.editor.SheetState
import com.petruvideo.feature.editor.FilterPanelState
import com.petruvideo.feature.editor.ui.AdjustControls
import com.petruvideo.feature.editor.ui.EqualizerControls
import com.petruvideo.feature.editor.ui.FadeControls
import com.petruvideo.feature.editor.ui.LooksControls
import com.petruvideo.feature.editor.ui.OverlayControls
import com.petruvideo.feature.editor.ui.NoiseControls
import com.petruvideo.feature.editor.ui.ReverbControls
import com.petruvideo.feature.editor.ui.ReverbSwitch
import com.petruvideo.feature.editor.ui.SpeedVolumeControls
import com.petruvideo.feature.editor.ui.TextAnimationControls
import com.petruvideo.feature.editor.ui.TextEditControls
import com.petruvideo.feature.editor.ui.TextStyleControls
import com.petruvideo.feature.editor.ui.VoiceControls

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
    unavailable: Map<ClipTool, String>,
    reverb: Reverb?,
    muted: Boolean,
    fx: AudioFx?,
    clipLengthMs: Long,
    text: TextClip?,
    filter: FilterPanelState?,
    overlay: OverlayClip?,
    onEvent: (EditorEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxHeight().background(PetruColors.Surface).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Inspector", style = PetruType.Label, color = PetruColors.OnBackground)
            if (clip != null) {
                Text(
                    "${clip.label} · ${"%.1f".format(clip.lengthMs / MS_PER_SECOND)}s",
                    style = PetruType.Mono,
                    color = PetruColors.Accent,
                )
            }
        }
        if (clip == null) {
            Text("Select a clip to see its details.", style = PetruType.Body, color = PetruColors.OnSurfaceMuted)
        } else {
            Text(clip.name, style = PetruType.Body, color = PetruColors.OnBackground)
            Text(
                "Starts at ${formatDuration(clip.startMs)}",
                style = PetruType.Mono,
                color = PetruColors.OnSurfaceMuted,
            )
            settings.forEach { sheet ->
                Text(sheet.tool.label, style = PetruType.Overline, color = PetruColors.OnSurfaceMuted)
                SpeedVolumeControls(sheet, onEvent)
            }
            if (ClipTool.Mute !in unavailable) {
                Text("Sound", style = PetruType.Overline, color = PetruColors.OnSurfaceMuted)
                PetruChip(
                    if (muted) "Unmute" else "Mute",
                    selected = muted,
                    onClick = { onEvent(EditorEvent.UseTool(ClipTool.Mute)) },
                )
            }
            if (reverb != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Reverb", style = PetruType.Overline, color = PetruColors.OnSurfaceMuted)
                    ReverbSwitch(reverb, onEvent)
                }
                ReverbControls(reverb, onEvent, showAll = true)
            }
            if (overlay != null) {
                Text("Overlay", style = PetruType.Overline, color = PetruColors.OnSurfaceMuted)
                OverlayControls(overlay, onEvent)
            }
            if (filter != null) {
                Text("Filters", style = PetruType.Overline, color = PetruColors.OnSurfaceMuted)
                LooksControls(filter, onEvent)
                Text("Adjust", style = PetruType.Overline, color = PetruColors.OnSurfaceMuted)
                AdjustControls(filter.filter, onEvent)
            }
            if (text != null) {
                Text("Text", style = PetruType.Overline, color = PetruColors.OnSurfaceMuted)
                TextEditControls(text, onEvent)
                Text("Style", style = PetruType.Overline, color = PetruColors.OnSurfaceMuted)
                TextStyleControls(text, onEvent)
                Text("Animation", style = PetruType.Overline, color = PetruColors.OnSurfaceMuted)
                TextAnimationControls(text, onEvent)
            }
            if (fx != null) {
                Text("Fade", style = PetruType.Overline, color = PetruColors.OnSurfaceMuted)
                FadeControls(fx, clipLengthMs, onEvent)
                Text("Noise reduction", style = PetruType.Overline, color = PetruColors.OnSurfaceMuted)
                NoiseControls(fx, onEvent)
                Text("Equalizer", style = PetruType.Overline, color = PetruColors.OnSurfaceMuted)
                EqualizerControls(fx, onEvent)
                Text("Voice effects", style = PetruType.Overline, color = PetruColors.OnSurfaceMuted)
                VoiceControls(fx, onEvent)
            }
            unavailable.forEach { (tool, reason) ->
                Text("${tool.label}: $reason", style = PetruType.Caption, color = PetruColors.OnSurfaceMuted)
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
