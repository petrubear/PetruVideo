package com.petruvideo.feature.editor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.designsystem.components.PillButton
import com.petruvideo.core.designsystem.components.SegmentedControl
import com.petruvideo.core.designsystem.components.SheetSurface
import com.petruvideo.feature.editor.EditorEvent
import com.petruvideo.feature.editor.KeyframePanelState

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
                    "Keyframes",
                    style = PetruType.Title,
                    color = PetruColors.OnBackground,
                    modifier = Modifier.weight(1f),
                )
                PillButton("Done", onClick = { onEvent(EditorEvent.KeyframeDismiss) })
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
        panel.keyframes.isEmpty() -> "No keyframes"
        selected >= 0 -> "Keyframe ${selected + 1} of ${panel.keyframes.size}"
        else -> "${panel.keyframes.size} keyframes"
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            readout,
            style = PetruType.Caption,
            color = PetruColors.OnSurfaceMuted,
            modifier = Modifier.semantics { contentDescription = readout },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextAction("Previous") { onEvent(EditorEvent.KeyframeJump(forward = false)) }
            TextAction("Add keyframe") { onEvent(EditorEvent.KeyframeAdd) }
            TextAction("Next") { onEvent(EditorEvent.KeyframeJump(forward = true)) }
        }
        if (selected >= 0) {
            SegmentedControl(
                options = listOf("Linear", "Ease"),
                selectedIndex = if (panel.keyframes[selected].ease) 1 else 0,
                onSelect = { onEvent(EditorEvent.KeyframeMotion(ease = it == 1)) },
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextAction("Copy here") { onEvent(EditorEvent.KeyframeCopyHere) }
                TextAction("Delete keyframe") { onEvent(EditorEvent.KeyframeDelete) }
            }
        }
    }
}
