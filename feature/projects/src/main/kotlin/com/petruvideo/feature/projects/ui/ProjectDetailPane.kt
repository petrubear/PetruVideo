package com.petruvideo.feature.projects.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.petruvideo.core.data.repo.ProjectSummary
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruShapes
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.designsystem.components.PillButton
import com.petruvideo.core.media.thumbs.GridThumbnail
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.formatDuration
import kotlin.math.min

/**
 * The right pane of the two-pane Projects view: thumbnail, clip count, length, format, the project actions, and the
 * project's media. Export belongs to a later feature, so it is dimmed and says "coming soon".
 */
@Composable
fun ProjectDetailPane(
    summary: ProjectSummary?,
    media: List<MediaItem>,
    onOpen: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (summary == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select a project", style = PetruType.Body, color = PetruColors.OnSurfaceMuted)
        }
        return
    }
    val project = summary.project
    BoxWithConstraints(modifier.fillMaxSize()) {
        // A short pane (the lower half in tabletop posture) keeps the thumbnail beside the facts so the media fits.
        val short = maxHeight < SHORT_PANE
        val stacked = !short && maxWidth < SIDE_BY_SIDE_MIN
        val thumbWidth = if (short) SHORT_THUMB_WIDTH else THUMB_WIDTH
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (stacked) {
                PlaceholderThumbnail(
                    seed = project.id,
                    modifier = Modifier.fillMaxWidth().aspectRatio(THUMB_RATIO).clip(PetruShapes.Sheet),
                )
                ProjectFacts(summary, onOpen)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    PlaceholderThumbnail(
                        seed = project.id,
                        modifier = Modifier.width(thumbWidth).aspectRatio(THUMB_RATIO).clip(PetruShapes.Sheet),
                    )
                    ProjectFacts(summary, onOpen, Modifier.weight(1f))
                }
            }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionChip("Duplicate", onDuplicate)
            ActionChip("Export", onExport, dimmed = true)
            ActionChip("Delete", onDelete, color = PetruColors.Danger)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Media in this project", style = PetruType.Label, color = PetruColors.OnBackground)
            Text("${media.size} items", style = PetruType.Mono, color = PetruColors.OnSurfaceMuted)
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(MEDIA_COLUMNS),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(media, key = { it.id }) { MediaTile(it) }
        }
        }
    }
}

@Composable
private fun ProjectFacts(summary: ProjectSummary, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val project = summary.project
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(project.name, style = PetruType.Display, color = PetruColors.OnBackground, maxLines = 2)
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Stat("CLIPS", summary.clipCount.toString())
            Stat("LENGTH", formatDuration(summary.durationMs))
            Stat("FORMAT", project.format?.let { "${min(it.width, it.height)}p, ${it.fps}" } ?: "-")
        }
        PillButton("Open editor", onOpen, Modifier.fillMaxWidth())
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(label, style = PetruType.Tiny, color = PetruColors.OnSurfaceMuted)
        Text(value, style = PetruType.Label, color = PetruColors.OnBackground)
    }
}

@Composable
private fun ActionChip(
    label: String,
    onClick: () -> Unit,
    dimmed: Boolean = false,
    color: androidx.compose.ui.graphics.Color = PetruColors.OnBackground,
) {
    Text(
        label,
        style = PetruType.Label,
        color = color,
        modifier = Modifier
            .alpha(if (dimmed) DIMMED else 1f)
            .clip(PetruShapes.Button)
            .background(PetruColors.Surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    )
}

@Composable
private fun MediaTile(item: MediaItem) {
    Box(
        Modifier
            .aspectRatio(TILE_RATIO)
            .clip(PetruShapes.Chip)
            .background(PetruColors.SurfaceHighest),
    ) {
        if (item.kind != MediaKind.Audio) GridThumbnail(item.uri, Modifier.fillMaxSize())
        item.durationMs?.let {
            Text(
                if (item.kind == MediaKind.Audio) "Audio · ${formatDuration(it)}" else formatDuration(it),
                style = PetruType.MonoSmall,
                color = PetruColors.OnBackground,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .background(PetruColors.Background.copy(alpha = 0.7f), PetruShapes.Chip)
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            )
        }
    }
}

private const val THUMB_RATIO = 1.5f
private val THUMB_WIDTH = 260.dp
private val SIDE_BY_SIDE_MIN = 560.dp
private val SHORT_PANE = 560.dp
private val SHORT_THUMB_WIDTH = 150.dp
private const val TILE_RATIO = 1.6f
private const val MEDIA_COLUMNS = 4
private const val DIMMED = 0.4f
