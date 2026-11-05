package com.petruvideo.feature.editor.ui.expanded

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruShapes
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.designsystem.icons.PetruIcons
import com.petruvideo.feature.editor.ToolbarTab
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import com.petruvideo.feature.editor.R

private data class RailEntry(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val tab: ToolbarTab,
)

private val ENTRIES = listOf(
    RailEntry(R.string.editor_media, PetruIcons.ToolEdit, ToolbarTab.Edit),
    RailEntry(R.string.editor_audio, PetruIcons.ToolAudio, ToolbarTab.Audio),
    RailEntry(R.string.editor_text, PetruIcons.ToolText, ToolbarTab.Text),
    RailEntry(R.string.editor_overlay, PetruIcons.ToolOverlay, ToolbarTab.Overlay),
    RailEntry(R.string.editor_effects, PetruIcons.ToolEffects, ToolbarTab.Effects),
    RailEntry(R.string.editor_filters, PetruIcons.ToolFilters, ToolbarTab.Filters),
)

/** The left rail of the unfolded editor, one entry per tab. */
@Composable
fun ToolRail(
    modifier: Modifier = Modifier,
    onTab: (ToolbarTab) -> Unit = {},
) {
    Column(
        modifier
            .width(64.dp)
            .fillMaxHeight()
            .background(PetruColors.Background)
            .border(1.dp, PetruColors.SurfaceHigh)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ENTRIES.forEach { entry ->
            val color = PetruColors.Accent
            val entryLabel = stringResource(entry.labelRes)
            Column(
                Modifier
                    .size(width = 56.dp, height = 56.dp)
                    .clip(PetruShapes.Button)
                    .clickable(role = Role.Button) { onTab(entry.tab) }
                    .semantics { contentDescription = entryLabel },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            ) {
                Icon(entry.icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                Text(entryLabel, style = PetruType.Tiny, color = color)
            }
        }
    }
}
