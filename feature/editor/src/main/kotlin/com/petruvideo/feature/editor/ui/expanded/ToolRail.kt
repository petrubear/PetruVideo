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
import androidx.compose.ui.draw.alpha
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
    val active: Boolean,
    val tab: ToolbarTab? = null,
)

private val ENTRIES = listOf(
    RailEntry(R.string.editor_media, PetruIcons.ToolEdit, true, ToolbarTab.Edit),
    RailEntry(R.string.editor_audio, PetruIcons.ToolAudio, true, ToolbarTab.Audio),
    RailEntry(R.string.editor_text, PetruIcons.ToolText, true, ToolbarTab.Text),
    RailEntry(R.string.editor_overlay, PetruIcons.ToolOverlay, true, ToolbarTab.Overlay),
    RailEntry(R.string.editor_effects, PetruIcons.ToolEffects, true, ToolbarTab.Effects),
    RailEntry(R.string.editor_filters, PetruIcons.ToolFilters, true, ToolbarTab.Filters),
)

/** The left rail of the unfolded editor. Media is the only active entry; the rest say "coming soon". */
@Composable
fun ToolRail(
    onInactiveTap: (String) -> Unit,
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
            val color = if (entry.active) PetruColors.Accent else PetruColors.OnBackground
            val entryLabel = stringResource(entry.labelRes)
            val comingSoon = stringResource(R.string.editor_label_coming_soon, entryLabel)
            Column(
                Modifier
                    .size(width = 56.dp, height = 56.dp)
                    .clip(PetruShapes.Button)
                    .alpha(if (entry.active) 1f else 0.4f)
                    .clickable(role = Role.Button) {
                        val tab = entry.tab
                        if (tab != null) onTab(tab) else onInactiveTap(entryLabel)
                    }
                    .semantics {
                        contentDescription = if (entry.active) entryLabel else comingSoon
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            ) {
                Icon(entry.icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                Text(entryLabel, style = PetruType.Tiny, color = color)
            }
        }
    }
}
