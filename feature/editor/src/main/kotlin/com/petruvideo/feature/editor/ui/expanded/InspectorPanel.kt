package com.petruvideo.feature.editor.ui.expanded

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.designsystem.components.LabeledSlider
import com.petruvideo.core.designsystem.components.SegmentedControl
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.formatDuration

/** What the inspector shows about the selected clip. */
data class InspectorClip(val name: String, val label: String, val lengthMs: Long, val startMs: Long)

/**
 * The right pane of the unfolded editor: selected clip name and length, and the Video, Audio, and Speed tabs.
 * Their controls belong to later features, so they are dimmed and not interactive.
 */
@Composable
fun InspectorPanel(clip: InspectorClip?, modifier: Modifier = Modifier) {
    var tab by remember { mutableIntStateOf(0) }
    Column(
        modifier.fillMaxHeight().background(PetruColors.Surface).padding(16.dp),
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
        }
        SegmentedControl(listOf("Video", "Audio", "Speed"), tab, onSelect = { tab = it })
        Column(Modifier.alpha(DIMMED), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LabeledSlider("Scale", "100%", 0.5f, {}, enabled = false)
            LabeledSlider("Opacity", "100%", 1f, {}, enabled = false)
            LabeledSlider("Volume", "80%", 0.8f, {}, enabled = false)
            Text("Coming soon", style = PetruType.Tiny, color = PetruColors.OnSurfaceMuted)
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
private const val DIMMED = 0.4f
