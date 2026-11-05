package com.petruvideo.feature.export.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.enumLabel
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.model.export.ExportSettings
import com.petruvideo.core.model.export.Option
import com.petruvideo.core.model.export.Quality
import com.petruvideo.feature.export.ExportEvent
import com.petruvideo.feature.export.ExportUiState
import androidx.compose.ui.res.stringResource
import com.petruvideo.feature.export.R

/** Resolution, frame rate, and quality, with the file facts. Unavailable options are dimmed and explain themselves. */
@Composable
fun ExportSettingsPanel(state: ExportUiState, settings: ExportSettings, onEvent: (ExportEvent) -> Unit) {
    var hint by remember { mutableStateOf<String?>(null) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(PetruColors.Surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Group(stringResource(R.string.export_group_resolution)) {
            Segments(
                state.resolutions.map { Option(it.value.label, it.unavailable) },
                selected = settings.resolution.label,
                onSelect = { label ->
                    state.resolutions.first { it.value.label == label }
                        .let { onEvent(ExportEvent.SelectResolution(it.value)) }
                },
                onBlocked = { hint = it },
            )
        }
        Group(stringResource(R.string.export_group_frame_rate)) {
            Segments(
                state.frameRates.map { Option(it.value.fps.toString(), it.unavailable) },
                selected = settings.frameRate.fps.toString(),
                onSelect = { label ->
                    state.frameRates.first { it.value.fps.toString() == label }
                        .let { onEvent(ExportEvent.SelectFrameRate(it.value)) }
                },
                onBlocked = { hint = it },
            )
        }
        val qualityLabels = Quality.entries.associateWith { enumLabel("quality", it) }
        Group(stringResource(R.string.export_group_quality)) {
            Segments(
                state.qualities.map { Option(qualityLabels.getValue(it)) },
                selected = qualityLabels.getValue(settings.quality),
                onSelect = { label ->
                    onEvent(ExportEvent.SelectQuality(qualityLabels.entries.first { it.value == label }.key))
                },
                onBlocked = { hint = it },
            )
        }
        hint?.let { Text(it, style = PetruType.Caption, color = PetruColors.OnSurfaceMuted) }
        Row(stringResource(R.string.export_format), "MP4 · H.264", PetruColors.OnBackground)
        Row(stringResource(R.string.export_bitrate), formatBitrate(state.videoBitrate), PetruColors.OnBackground)
        Row(stringResource(R.string.export_estimated_size), "~ ${formatBytes(state.estimatedBytes)}", PetruColors.Accent)
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = PetruType.Overline, color = PetruColors.OnSurfaceMuted)
        content()
    }
}

@Composable
private fun Row(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = PetruType.Label, color = PetruColors.OnBackground)
        Text(value, style = PetruType.Mono, color = valueColor)
    }
}

@Composable
private fun Segments(
    options: List<Option<String>>,
    selected: String,
    onSelect: (String) -> Unit,
    onBlocked: (String?) -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PetruColors.Background)
            .padding(4.dp),
    ) {
        options.forEach { option ->
            val isSelected = option.value == selected
            val reason = option.unavailable?.let { enumLabel("export_unavailable", it) }
            Box(
                Modifier
                    .weight(if (option.value.length > LONG_LABEL) LONG_WEIGHT else 1f)
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) PetruColors.Accent else PetruColors.Background)
                    .alpha(if (option.allowed) 1f else DIMMED)
                    .clickable {
                        if (option.allowed) {
                            onBlocked(null)
                            onSelect(option.value)
                        } else {
                            onBlocked("${option.value}: $reason")
                        }
                    }
                    .semantics {
                        role = Role.RadioButton
                        this.selected = isSelected
                        contentDescription = option.value
                        if (reason != null) stateDescription = reason
                    }
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    option.value,
                    style = PetruType.Label,
                    color = if (isSelected) PetruColors.OnAccent else PetruColors.OnBackground,
                )
            }
        }
    }
}

private const val DIMMED = 0.4f
private const val LONG_LABEL = 8
private const val LONG_WEIGHT = 1.6f
