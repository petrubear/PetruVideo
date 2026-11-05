package com.petruvideo.feature.editor.ui

import android.graphics.Paint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.designsystem.components.PetruChip
import com.petruvideo.core.designsystem.components.PillButton
import com.petruvideo.core.designsystem.components.SheetSurface
import com.petruvideo.core.model.Sticker
import com.petruvideo.core.model.StickerCatalog
import com.petruvideo.core.model.StickerCategory
import com.petruvideo.feature.editor.EditorEvent
import com.petruvideo.core.designsystem.enumLabel
import androidx.compose.ui.res.stringResource
import com.petruvideo.feature.editor.R
import com.petruvideo.core.designsystem.described
import com.petruvideo.core.designsystem.keyedText

/** The stickers the phone can draw; a symbol its fonts lack is left out. */
private fun playableStickers(): List<Sticker> {
    val paint = Paint()
    return StickerCatalog.all.filter { runCatching { paint.hasGlyph(it.glyph) }.getOrDefault(true) }
}

private val GRID_HEIGHT = 168.dp
private val CELL = 56.dp

/** The Stickers gallery above the tool bar: categories and a grid; it stays open while stickers are added. */
@Composable
fun StickersPanel(open: Boolean, onEvent: (EditorEvent) -> Unit, modifier: Modifier = Modifier) {
    if (!open) return
    val available = remember { playableStickers() }
    var category by remember { mutableStateOf(StickerCategory.Faces) }
    SheetSurface(modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.editor_stickers),
                    style = PetruType.Title,
                    color = PetruColors.OnBackground,
                    modifier = Modifier.weight(1f),
                )
                PillButton(stringResource(R.string.editor_close), onClick = { onEvent(EditorEvent.StickersDismiss) })
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StickerCategory.entries.forEach {
                    PetruChip(enumLabel("stickercat", it), selected = it == category, onClick = { category = it })
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(CELL),
                modifier = Modifier.fillMaxWidth().height(GRID_HEIGHT),
            ) {
                items(available.filter { it.category == category }, key = { it.glyph }) { sticker ->
                    Box(
                        Modifier
                            .size(CELL)
                            .clickable(role = Role.Button) { onEvent(EditorEvent.AddStickerLayer(sticker)) }
                            .described(
                                R.string.editor_sticker_named,
                                keyedText("sticker_" + sticker.label.lowercase().replace(' ', '_')),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            sticker.glyph,
                            style = TextStyle(fontSize = 30.sp),
                            color = sticker.color?.let(::Color) ?: PetruColors.OnBackground,
                        )
                    }
                }
            }
        }
    }
}
