package com.petruvideo.feature.importmedia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.designsystem.Spacing
import com.petruvideo.core.designsystem.icons.PetruIcons
import com.petruvideo.core.media.thumbs.GridThumbnail
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.formatDuration
import com.petruvideo.feature.importmedia.GridItem

private const val LOAD_MORE_THRESHOLD = 12

/** Thumbnail grid with duration labels and numbered selection badges. [columns] is 3, 5 or 6 by width. */
@Composable
fun SelectionGrid(
    items: List<GridItem>,
    columns: Int,
    onToggle: (String) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = Spacing.ScreenGutter, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        itemsIndexed(items, key = { _, item -> item.uri }) { index, item ->
            if (index >= items.size - LOAD_MORE_THRESHOLD) LaunchedEffect(items.size) { onLoadMore() }
            GridCell(item, onClick = { onToggle(item.uri) })
        }
    }
}

@Composable
private fun GridCell(item: GridItem, onClick: () -> Unit) {
    val selected = item.selectionIndex != null
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(PetruColors.Surface)
            .then(if (selected) Modifier.border(2.5.dp, PetruColors.Accent, shape) else Modifier)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Checkbox
                this.selected = selected
                contentDescription = item.displayName
            },
    ) {
        if (item.kind == MediaKind.Audio) {
            Icon(
                PetruIcons.ToolAudio,
                contentDescription = null,
                tint = PetruColors.OnSurfaceMuted,
                modifier = Modifier.align(Alignment.Center).size(28.dp),
            )
        } else {
            GridThumbnail(item.uri, Modifier.fillMaxSize())
        }
        item.durationMs?.let {
            Text(
                formatDuration(it),
                style = PetruType.MonoSmall,
                color = PetruColors.OnBackground,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(PetruColors.Background.copy(alpha = 0.62f))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            )
        }
        SelectionBadge(item.selectionIndex, Modifier.align(Alignment.TopEnd).padding(6.dp))
    }
}

@Composable
private fun SelectionBadge(index: Int?, modifier: Modifier = Modifier) {
    val base = modifier.size(22.dp).clip(CircleShape)
    if (index != null) {
        Box(base.background(PetruColors.Accent), contentAlignment = Alignment.Center) {
            Text(index.toString(), style = PetruType.Caption, color = PetruColors.OnAccent)
        }
    } else {
        Box(base.border(2.dp, PetruColors.OnBackground.copy(alpha = 0.75f), CircleShape))
    }
}
