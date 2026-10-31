package com.petruvideo.feature.editor.ui.expanded

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruShapes
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.designsystem.components.PillButton
import com.petruvideo.core.media.thumbs.GridThumbnail
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.formatDuration

/**
 * The project's media in a two-column grid with an Import button. Tapping an item adds it at the playhead;
 * dragging it onto the timeline drops it at the drop position.
 */
@Composable
fun MediaPanel(
    media: List<MediaItem>,
    onImport: () -> Unit,
    onAdd: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxSize().background(PetruColors.Surface).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Media", style = PetruType.Label, color = PetruColors.OnBackground, modifier = Modifier.weight(1f))
            PillButton("+ Import", onImport)
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(media, key = { it.id }) { item -> MediaTile(item, onAdd) }
        }
        Text(
            "Drag a clip onto any timeline track, or tap it to add at the playhead.",
            style = PetruType.Body,
            color = PetruColors.OnSurfaceMuted,
        )
    }
}

@Composable
private fun MediaTile(item: MediaItem, onAdd: (MediaItem) -> Unit) {
    Box(
        Modifier
            .aspectRatio(1f)
            .clip(PetruShapes.Chip)
            .background(PetruColors.SurfaceHighest)
            .mediaDragSource(item.id) { onAdd(item) }
            .semantics { contentDescription = "Add ${item.displayName}" },
    ) {
        if (item.kind != MediaKind.Audio) GridThumbnail(item.uri, Modifier.fillMaxSize())
        item.durationMs?.let {
            Text(
                formatDuration(it),
                style = PetruType.MonoSmall,
                color = PetruColors.OnBackground,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(PetruColors.Background.copy(alpha = 0.7f), PetruShapes.Chip)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
    }
}
