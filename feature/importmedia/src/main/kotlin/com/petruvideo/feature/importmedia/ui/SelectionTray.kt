package com.petruvideo.feature.importmedia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.petruvideo.core.data.importing.ImportProgress
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruShapes
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.designsystem.Spacing
import com.petruvideo.core.designsystem.components.PillButton
import com.petruvideo.core.media.thumbs.GridThumbnail
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.formatDuration
import com.petruvideo.feature.importmedia.SelectedItem
import androidx.compose.ui.res.stringResource
import com.petruvideo.feature.importmedia.R

/**
 * The selection tray: thumbnails in selection order, count, total duration, and the single action
 * "Add to timeline in order". While importing it shows progress and a Cancel button instead.
 */
@Composable
fun SelectionTray(
    selection: List<SelectedItem>,
    totalDurationMs: Long,
    progress: ImportProgress?,
    onAdd: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(PetruColors.Surface, PetruShapes.Sheet)
            .border(1.dp, PetruColors.Outline, PetruShapes.Sheet)
            .navigationBarsPadding()
            .padding(start = Spacing.ScreenGutter, end = Spacing.ScreenGutter, top = 14.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text("${selection.size} selected", style = PetruType.Label, color = PetruColors.OnBackground)
            Text(
                "${formatDuration(totalDurationMs)} total",
                style = PetruType.Mono,
                color = PetruColors.OnSurfaceMuted,
            )
        }
        if (selection.isNotEmpty()) TrayThumbnails(selection)
        if (progress != null) {
            ImportingRow(progress, onCancel)
        } else {
            PillButton(
                stringResource(R.string.import_add_to_timeline_in_order),
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth(),
                enabled = selection.isNotEmpty(),
            )
        }
    }
}

@Composable
private fun TrayThumbnails(selection: List<SelectedItem>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(selection, key = { it.uri }) { item ->
            val shape = RoundedCornerShape(10.dp)
            val first = item == selection.first()
            Box(
                Modifier
                    .size(56.dp)
                    .clip(shape)
                    .background(PetruColors.SurfaceHighest)
                    .then(if (first) Modifier.border(2.dp, PetruColors.Accent, shape) else Modifier),
            ) {
                if (item.kind != MediaKind.Audio) GridThumbnail(item.uri, Modifier.size(56.dp))
            }
        }
    }
}

@Composable
private fun ImportingRow(progress: ImportProgress, onCancel: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.import_adding_progress, progress.completed, progress.total),
                style = PetruType.Body,
                color = PetruColors.OnBackground,
            )
            Text(
                stringResource(R.string.import_cancel),
                style = PetruType.Label,
                color = PetruColors.Accent,
                modifier = Modifier
                    .clip(PetruShapes.Button)
                    .clickable(onClick = onCancel)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            )
        }
        LinearProgressIndicator(
            progress = { if (progress.total == 0) 0f else progress.completed.toFloat() / progress.total },
            modifier = Modifier.fillMaxWidth(),
            color = PetruColors.Accent,
            trackColor = PetruColors.SurfaceHighest,
        )
    }
}
