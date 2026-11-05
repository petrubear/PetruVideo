package com.petruvideo.feature.export.ui

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.enumLabel
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.designsystem.adaptive.LocalArrangement
import com.petruvideo.core.designsystem.components.IconAction
import com.petruvideo.core.designsystem.icons.PetruIcons
import com.petruvideo.core.model.formatDuration
import com.petruvideo.feature.export.ExportRow
import com.petruvideo.feature.export.ExportsEvent
import com.petruvideo.feature.export.ExportsUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.compose.ui.res.stringResource
import com.petruvideo.feature.export.R

private const val UNDO_MS = 5_000L
private const val TWO_COLUMNS_DP = 840

/** The history of finished exports, newest first; two columns when unfolded. */
@Composable
fun ExportsScreen(
    state: ExportsUiState,
    onEvent: (ExportsEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(PetruColors.Background)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconAction(PetruIcons.Back, stringResource(R.string.export_back), onBack)
                Text(stringResource(R.string.export_exports), style = PetruType.Title, color = PetruColors.OnBackground)
            }
            when {
                !state.loaded -> Unit
                state.rows.isEmpty() -> EmptyMessage()
                else -> ExportList(state.rows, onEvent, Modifier.weight(1f))
            }
        }
        state.undo?.let { removed ->
            UndoBar(removed.projectName, onEvent, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun EmptyMessage() {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            stringResource(R.string.export_videos_you_export_appear_here),
            style = PetruType.Body,
            color = PetruColors.OnSurfaceMuted,
        )
    }
}

@Composable
private fun ExportList(rows: List<ExportRow>, onEvent: (ExportsEvent) -> Unit, modifier: Modifier) {
    val twoColumns = LocalConfiguration.current.screenWidthDp >= TWO_COLUMNS_DP && LocalArrangement.current.let {
        it !is com.petruvideo.core.designsystem.adaptive.Arrangement.Tabletop
    }
    if (twoColumns) {
        LazyVerticalGrid(
            GridCells.Fixed(2), modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) { items(rows, key = { it.record.id }) { ExportRowItem(it, onEvent) } }
    } else {
        LazyColumn(
            modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { items(rows, key = { it.record.id }) { ExportRowItem(it, onEvent) } }
    }
}

@Composable
private fun ExportRowItem(row: ExportRow, onEvent: (ExportsEvent) -> Unit) {
    val record = row.record
    val whenText = DateUtils.formatDateTime(
        LocalContext.current, record.finishedAt,
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH,
    )
    val summary = "${record.projectName}, $whenText, ${formatDuration(record.durationMs)}, ${formatBytes(record.bytes)}"
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PetruColors.Surface)
            .clickable(enabled = !row.missing) { onEvent(ExportsEvent.Open(record.id)) }
            .padding(12.dp)
            .semantics { contentDescription = if (row.missing) "$summary, file not found" else summary },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Thumbnail(record.uri, row.missing)
        Column(Modifier.weight(1f)) {
            Text(record.projectName, style = PetruType.Label, color = PetruColors.OnBackground, maxLines = 1)
            Text(whenText, style = PetruType.Caption, color = PetruColors.OnSurfaceMuted)
            Text(
                "${formatDuration(record.durationMs)} · ${formatBytes(record.bytes)}",
                style = PetruType.Caption,
                color = PetruColors.OnSurfaceMuted,
            )
            Text(
                if (row.missing) stringResource(R.string.export_file_not_found) else stringResource(
                    R.string.export_format_label,
                    record.settings.resolution.label,
                    record.settings.frameRate.fps,
                    enumLabel("quality", record.settings.quality),
                ),
                style = PetruType.Caption,
                color = if (row.missing) PetruColors.Danger else PetruColors.OnSurfaceMuted,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            if (!row.missing) {
                IconAction(PetruIcons.Play, stringResource(R.string.export_play_named, record.projectName), { onEvent(ExportsEvent.Open(record.id)) })
                IconAction(PetruIcons.Export, stringResource(R.string.export_share_named, record.projectName), { onEvent(ExportsEvent.Share(record.id)) })
            }
            IconAction(
                PetruIcons.Delete, stringResource(R.string.export_remove_named, record.projectName),
                { onEvent(ExportsEvent.Remove(record.id)) },
            )
        }
    }
}

@Composable
private fun Thumbnail(uri: String, missing: Boolean) {
    val context = LocalContext.current
    val frame by produceState<ImageBitmap?>(null, uri) {
        value = withContext(Dispatchers.IO) { loadFrame(context, uri) }
    }
    Box(
        Modifier.size(width = 96.dp, height = 64.dp).clip(RoundedCornerShape(10.dp)).background(PetruColors.SurfaceHigh)
            .alpha(if (missing) 0.4f else 1f),
    ) {
        frame?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
    }
}

private fun loadFrame(context: android.content.Context, uri: String): ImageBitmap? {
    val retriever = MediaMetadataRetriever()
    return try {
        runCatching {
            retriever.setDataSource(context, Uri.parse(uri))
            retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let { frame ->
                val scale = THUMB_WIDTH.toFloat() / frame.width
                Bitmap.createScaledBitmap(frame, THUMB_WIDTH, (frame.height * scale).toInt().coerceAtLeast(1), true)
                    .asImageBitmap()
            }
        }.getOrNull()
    } finally {
        retriever.release()
    }
}

private const val THUMB_WIDTH = 192

@Composable
private fun UndoBar(name: String, onEvent: (ExportsEvent) -> Unit, modifier: Modifier) {
    LaunchedEffect(name) {
        delay(UNDO_MS)
        onEvent(ExportsEvent.DismissUndo)
    }
    Row(
        modifier.padding(16.dp).clip(RoundedCornerShape(14.dp)).background(PetruColors.SurfaceHighest)
            .padding(start = 16.dp).height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.export_removed_named, name), style = PetruType.Body, color = PetruColors.OnBackground)
        Text(
            stringResource(R.string.export_undo),
            style = PetruType.Label,
            color = PetruColors.Accent,
            modifier = Modifier.clickable { onEvent(ExportsEvent.Undo) }.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}
