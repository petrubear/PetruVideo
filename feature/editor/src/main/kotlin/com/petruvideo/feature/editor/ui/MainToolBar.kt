package com.petruvideo.feature.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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

private fun iconFor(tab: ToolbarTab): ImageVector = when (tab) {
    ToolbarTab.Edit -> PetruIcons.ToolEdit
    ToolbarTab.Audio -> PetruIcons.ToolAudio
    ToolbarTab.Text -> PetruIcons.ToolText
    ToolbarTab.Overlay -> PetruIcons.ToolOverlay
    ToolbarTab.Effects -> PetruIcons.ToolEffects
    ToolbarTab.Filters -> PetruIcons.ToolFilters
}

/** The main tool bar: Edit is active, every other entry is dimmed and shows "coming soon" when tapped. */
@Composable
fun MainToolBar(
    onInactiveTap: (String) -> Unit,
    modifier: Modifier = Modifier,
    onTab: (ToolbarTab) -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(PetruColors.Background)
            .border(1.dp, PetruColors.SurfaceHigh)
            .navigationBarsPadding()
            .height(76.dp)
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToolbarTab.entries.forEach { tab ->
            ToolButton(
                label = tab.label,
                icon = iconFor(tab),
                active = tab.active,
                onClick = { if (tab.active) onTab(tab) else onInactiveTap(tab.label) },
            )
        }
    }
}

@Composable
internal fun ToolButton(
    label: String,
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color? = null,
    unavailableNote: String = "coming soon",
) {
    val color = tint ?: if (active) PetruColors.Accent else PetruColors.OnBackground
    Column(
        Modifier
            .height(60.dp)
            .defaultMinSize(minWidth = 48.dp)
            .clip(PetruShapes.Button)
            .alpha(if (active) 1f else 0.4f)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp)
            .semantics { contentDescription = if (active) label else "$label, $unavailableNote" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Text(label, style = PetruType.Tiny, color = color)
    }
}

/** The bar of the Filters tab with no picture clip selected. */
@Composable
fun FiltersTabBar(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .background(PetruColors.Background)
            .border(1.dp, PetruColors.SurfaceHigh)
            .navigationBarsPadding()
            .height(76.dp)
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToolButton(label = "Edit", icon = PetruIcons.ToolEdit, active = true, onClick = onBack)
        Text("Select a clip to add a filter", style = PetruType.Body, color = PetruColors.OnSurfaceMuted)
    }
}

/** The bar of the Text tab with nothing selected: back to the Edit tools, and Add text at the playhead. */
@Composable
fun TextTabBar(onBack: () -> Unit, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .background(PetruColors.Background)
            .border(1.dp, PetruColors.SurfaceHigh)
            .navigationBarsPadding()
            .height(76.dp)
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToolButton(label = "Edit", icon = PetruIcons.ToolEdit, active = true, onClick = onBack)
        ToolButton(label = "Add text", icon = PetruIcons.Add, active = true, onClick = onAdd)
    }
}
