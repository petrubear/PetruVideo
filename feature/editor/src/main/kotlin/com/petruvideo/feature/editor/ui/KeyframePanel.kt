package com.petruvideo.feature.editor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.designsystem.components.PillButton
import com.petruvideo.core.designsystem.components.SegmentedControl
import com.petruvideo.core.designsystem.components.SheetSurface
import com.petruvideo.core.model.FRAMING_ZOOM_MAX
import com.petruvideo.core.model.FRAMING_ZOOM_MIN
import com.petruvideo.core.model.Framing
import com.petruvideo.core.model.Pose
import com.petruvideo.feature.editor.EditorEvent
import com.petruvideo.feature.editor.KeyframePanelState
import androidx.compose.ui.res.stringResource
import com.petruvideo.feature.editor.R
import androidx.compose.ui.res.pluralStringResource

/** The Keyframes panel above the clip tool bar. */
@Composable
fun KeyframePanel(panel: KeyframePanelState?, onEvent: (EditorEvent) -> Unit, modifier: Modifier = Modifier) {
    if (panel == null) return
    SheetSurface(modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.editor_keyframes),
                    style = PetruType.Title,
                    color = PetruColors.OnBackground,
                    modifier = Modifier.weight(1f),
                )
                PillButton(stringResource(R.string.editor_done), onClick = { onEvent(EditorEvent.KeyframeDismiss) })
            }
            KeyframeControls(panel, onEvent)
        }
    }
}

/** The keyframe buttons and motion choice; also hosted by the inspector on large screens. */
@Composable
fun KeyframeControls(panel: KeyframePanelState, onEvent: (EditorEvent) -> Unit, modifier: Modifier = Modifier) {
    val selected = panel.keyframes.indexOfFirst { it.timeMs == panel.selectedRelMs }
    val readout = when {
        panel.keyframes.isEmpty() -> stringResource(R.string.editor_no_keyframes)
        selected >= 0 -> stringResource(R.string.editor_keyframe_n_of, selected + 1, panel.keyframes.size)
        else -> pluralStringResource(R.plurals.editor_keyframe_count, panel.keyframes.size, panel.keyframes.size)
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            readout,
            style = PetruType.Caption,
            color = PetruColors.OnSurfaceMuted,
            modifier = Modifier.semantics { contentDescription = readout },
        )
        panel.framing?.let { FramingSliders(it, onEvent) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextAction(stringResource(R.string.editor_previous)) { onEvent(EditorEvent.KeyframeJump(forward = false)) }
            TextAction(stringResource(R.string.editor_add_keyframe)) { onEvent(EditorEvent.KeyframeAdd) }
            TextAction(stringResource(R.string.editor_next)) { onEvent(EditorEvent.KeyframeJump(forward = true)) }
        }
        if (selected >= 0) {
            SegmentedControl(
                options = listOf(stringResource(R.string.editor_linear), stringResource(R.string.editor_ease)),
                selectedIndex = if (panel.keyframes[selected].ease) 1 else 0,
                onSelect = { onEvent(EditorEvent.KeyframeMotion(ease = it == 1)) },
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextAction(stringResource(R.string.editor_copy_here)) { onEvent(EditorEvent.KeyframeCopyHere) }
                TextAction(stringResource(R.string.editor_delete_keyframe)) { onEvent(EditorEvent.KeyframeDelete) }
            }
        }
    }
}

/** Zoom and opacity of a video clip at the playhead; the preview drag pans (a keyframe is made at the playhead). */
@Composable
private fun FramingSliders(pose: Pose, onEvent: (EditorEvent) -> Unit) {
    var shown by remember(pose) { mutableStateOf(pose) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.weight(1f)) {
            SettingSlider(
                stringResource(R.string.editor_zoom),
                "${shown.sizePermille / PERMILLE_PER_PERCENT}%",
                shown.sizePermille.toFloat(),
                FRAMING_ZOOM_MIN.toFloat()..FRAMING_ZOOM_MAX.toFloat(),
                {
                    shown = Framing.clamp(shown.copy(sizePermille = it.roundToInt()))
                    onEvent(EditorEvent.FramingPreview(shown))
                },
                { onEvent(EditorEvent.FramingCommit(shown)) },
            )
        }
        Box(Modifier.weight(1f)) {
            SettingSlider(
                stringResource(R.string.editor_opacity),
                "${shown.opacityPercent}%",
                shown.opacityPercent.toFloat(),
                0f..PERCENT_MAX,
                {
                    shown = shown.copy(opacityPercent = it.roundToInt())
                    onEvent(EditorEvent.FramingPreview(shown))
                },
                { onEvent(EditorEvent.FramingCommit(shown)) },
            )
        }
    }
    Text(
        stringResource(R.string.editor_drag_the_preview_to_move),
        style = PetruType.Caption,
        color = PetruColors.OnSurfaceMuted,
    )
}

private const val PERMILLE_PER_PERCENT = 10
private const val PERCENT_MAX = 100f
