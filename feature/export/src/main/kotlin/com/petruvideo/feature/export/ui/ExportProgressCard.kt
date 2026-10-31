package com.petruvideo.feature.export.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.designsystem.components.PetruChip
import com.petruvideo.core.designsystem.components.PillButton
import com.petruvideo.core.model.export.ExportState
import com.petruvideo.feature.export.ExportEvent
import com.petruvideo.feature.export.ExportUiState

/** Progress with Cancel while rendering, the saved result with Open and Share, or the failure; then the save button. */
@Composable
fun ExportProgressCard(state: ExportUiState, onEvent: (ExportEvent) -> Unit) {
    val job = state.job
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (val s = job?.state) {
            ExportState.Waiting -> Progress(0, onEvent)
            is ExportState.Rendering -> Progress(s.progress, onEvent)
            is ExportState.Finished -> Card {
                Text("Saved to gallery", style = PetruType.Label, color = PetruColors.Accent)
                val where = "${formatBytes(s.bytes)} · Movies/PetruVideo"
                Text(where, style = PetruType.Caption, color = PetruColors.OnSurfaceMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PetruChip("Open", selected = false, onClick = { onEvent(ExportEvent.Open) })
                    PetruChip("Share", selected = false, onClick = { onEvent(ExportEvent.Share) })
                }
            }
            is ExportState.Failed -> Card {
                Text("Export failed", style = PetruType.Label, color = PetruColors.Danger)
                Text(s.reason.message(), style = PetruType.Body, color = PetruColors.OnBackground)
            }
            ExportState.Cancelled -> Card {
                Text("Export cancelled", style = PetruType.Label, color = PetruColors.OnBackground)
            }
            null -> Unit
        }
        state.blocker?.takeIf { !state.rendering }?.let {
            Text(it.message(), style = PetruType.Body, color = PetruColors.OnSurfaceMuted)
        }
        PillButton(
            text = if (job?.state is ExportState.Failed || job?.state == ExportState.Cancelled) {
                "Try again"
            } else {
                "Save to gallery"
            },
            onClick = { onEvent(ExportEvent.Start) },
            enabled = state.blocker == null && !state.rendering,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Progress(percent: Int, onEvent: (ExportEvent) -> Unit) {
    Card {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Rendering in background", style = PetruType.Label, color = PetruColors.OnBackground)
                    Text("$percent%", style = PetruType.Mono, color = PetruColors.Accent)
                }
                LinearProgressIndicator(
                    progress = { percent / PERCENT },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .semantics {
                            contentDescription = "Export progress"
                            stateDescription = "$percent percent"
                        },
                    color = PetruColors.Accent,
                    trackColor = PetruColors.SurfaceHighest,
                )
            }
            PetruChip(
                "Cancel",
                selected = false,
                onClick = { onEvent(ExportEvent.Cancel) },
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PetruColors.Surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

private const val PERCENT = 100f
