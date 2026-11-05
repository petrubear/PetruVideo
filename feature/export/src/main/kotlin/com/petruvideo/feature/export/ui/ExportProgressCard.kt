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
import androidx.compose.ui.res.stringResource
import com.petruvideo.feature.export.R

/** Progress with Cancel while rendering, the saved result with Open and Share, or the failure; then the save button. */
@Composable
fun ExportProgressCard(state: ExportUiState, onEvent: (ExportEvent) -> Unit) {
    val job = state.job
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (val s = job?.state) {
            ExportState.Waiting -> Progress(0, onEvent)
            is ExportState.Rendering -> Progress(s.progress, onEvent)
            is ExportState.Finished -> Card {
                Text(
                    stringResource(R.string.export_saved_to_gallery),
                    style = PetruType.Label,
                    color = PetruColors.Accent,
                )
                val where = "${formatBytes(s.bytes)} · Movies/PetruVideo"
                Text(where, style = PetruType.Caption, color = PetruColors.OnSurfaceMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PetruChip(
                        stringResource(R.string.export_open),
                        selected = false,
                        onClick = { onEvent(ExportEvent.Open) },
                    )
                    PetruChip(
                        stringResource(R.string.export_share),
                        selected = false,
                        onClick = { onEvent(ExportEvent.Share) },
                    )
                }
            }
            is ExportState.Failed -> Card {
                Text(stringResource(R.string.export_export_failed), style = PetruType.Label, color = PetruColors.Danger)
                Text(s.reason.message(), style = PetruType.Body, color = PetruColors.OnBackground)
            }
            ExportState.Cancelled -> Card {
                Text(
                    stringResource(R.string.export_export_cancelled),
                    style = PetruType.Label,
                    color = PetruColors.OnBackground,
                )
            }
            null -> Unit
        }
        state.blocker?.takeIf { !state.rendering }?.let {
            Text(it.message(), style = PetruType.Body, color = PetruColors.OnSurfaceMuted)
        }
        PillButton(
            text = if (job?.state is ExportState.Failed || job?.state == ExportState.Cancelled) {
                stringResource(R.string.export_try_again)
            } else {
                stringResource(R.string.export_save_to_gallery)
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
                    Text(
                        stringResource(R.string.export_rendering_in_background),
                        style = PetruType.Label,
                        color = PetruColors.OnBackground,
                    )
                    Text("$percent%", style = PetruType.Mono, color = PetruColors.Accent)
                }
                val progressDescription = stringResource(R.string.export_export_progress)
                val progressState = stringResource(R.string.export_percent_state, percent)
                LinearProgressIndicator(
                    progress = { percent / PERCENT },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .semantics {
                            contentDescription = progressDescription
                            stateDescription = progressState
                        },
                    color = PetruColors.Accent,
                    trackColor = PetruColors.SurfaceHighest,
                )
            }
            PetruChip(
                stringResource(R.string.export_cancel),
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
