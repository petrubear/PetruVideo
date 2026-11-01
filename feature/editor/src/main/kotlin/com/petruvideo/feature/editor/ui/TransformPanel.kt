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
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.designsystem.components.PetruChip
import com.petruvideo.core.designsystem.components.PillButton
import com.petruvideo.core.designsystem.components.SheetSurface
import com.petruvideo.feature.editor.EditorEvent
import com.petruvideo.feature.editor.TransformPanelState

/** The Transform panel above the clip tool bar. */
@Composable
fun TransformPanel(panel: TransformPanelState?, onEvent: (EditorEvent) -> Unit, modifier: Modifier = Modifier) {
    if (panel == null) return
    SheetSurface(modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Transform",
                    style = PetruType.Title,
                    color = PetruColors.OnBackground,
                    modifier = Modifier.weight(1f),
                )
                PillButton("Done", onClick = { onEvent(EditorEvent.TransformDismiss) })
            }
            TransformControls(panel, onEvent)
        }
    }
}

/** Rotate, the two flips, Crop, and Reset; also hosted by the inspector on large screens. */
@Composable
fun TransformControls(panel: TransformPanelState, onEvent: (EditorEvent) -> Unit, modifier: Modifier = Modifier) {
    val transform = panel.transform
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PetruChip(
                "Rotate", selected = transform.quarterTurns != 0,
                onClick = { onEvent(EditorEvent.TransformRotate) },
            )
            PetruChip(
                "Flip horizontal", selected = transform.flipH,
                onClick = { onEvent(EditorEvent.TransformFlip(horizontal = true)) },
            )
            PetruChip(
                "Flip vertical", selected = transform.flipV,
                onClick = { onEvent(EditorEvent.TransformFlip(horizontal = false)) },
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            PetruChip("Crop", selected = !transform.crop.isFull, onClick = { onEvent(EditorEvent.CropOpen) })
            if (!transform.isNeutral) TextAction("Reset") { onEvent(EditorEvent.TransformReset) }
        }
        Text(
            if (transform.isNeutral) "The picture is as filmed." else summary(panel),
            style = PetruType.Caption,
            color = PetruColors.OnSurfaceMuted,
        )
    }
}

private fun summary(panel: TransformPanelState): String {
    val t = panel.transform
    return listOfNotNull(
        "Turned ${t.quarterTurns * QUARTER}°".takeIf { t.quarterTurns != 0 },
        "Flipped horizontally".takeIf { t.flipH },
        "Flipped vertically".takeIf { t.flipV },
        "Cropped".takeIf { !t.crop.isFull },
    ).joinToString(", ")
}

private const val QUARTER = 90
