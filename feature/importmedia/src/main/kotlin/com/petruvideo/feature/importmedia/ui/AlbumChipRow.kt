package com.petruvideo.feature.importmedia.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.Spacing
import com.petruvideo.core.designsystem.components.PetruChip
import com.petruvideo.core.media.library.LibraryAlbum

@Composable
fun AlbumChipRow(
    albums: List<LibraryAlbum>,
    selected: LibraryAlbum,
    onSelect: (LibraryAlbum) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = Spacing.ScreenGutter),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(albums) { album ->
            PetruChip(album.label, selected = album == selected, onClick = { onSelect(album) })
        }
    }
}
