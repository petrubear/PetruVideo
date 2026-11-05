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
import androidx.compose.ui.res.stringResource
import com.petruvideo.feature.editor.R

private fun iconFor(tab: ToolbarTab): ImageVector = when (tab) {
    ToolbarTab.Edit -> PetruIcons.ToolEdit
    ToolbarTab.Audio -> PetruIcons.ToolAudio
    ToolbarTab.Text -> PetruIcons.ToolText
    ToolbarTab.Overlay -> PetruIcons.ToolOverlay
    ToolbarTab.Effects -> PetruIcons.ToolEffects
    ToolbarTab.Filters -> PetruIcons.ToolFilters
}

/** The main tool bar with one entry per tab. */
@Composable
fun MainToolBar(
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
                label = stringResource(tab.labelRes),
                icon = iconFor(tab),
                active = true,
                onClick = { onTab(tab) },
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
    unavailableNote: String = stringResource(R.string.editor_unavailable),
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

/** The bar of the Overlay tab with no overlay selected: back to Edit, and Add overlay from a file. */
@Composable
fun OverlayTabBar(onBack: () -> Unit, onAdd: () -> Unit, modifier: Modifier = Modifier) {
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
        ToolButton(
            label = stringResource(R.string.editor_edit),
            icon = PetruIcons.ToolEdit,
            active = true,
            onClick = onBack,
        )
        ToolButton(
            label = stringResource(R.string.editor_add_overlay),
            icon = PetruIcons.Add,
            active = true,
            onClick = onAdd,
        )
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
        ToolButton(
            label = stringResource(R.string.editor_edit),
            icon = PetruIcons.ToolEdit,
            active = true,
            onClick = onBack,
        )
        Text(
            stringResource(R.string.editor_select_a_clip_to_add),
            style = PetruType.Body,
            color = PetruColors.OnSurfaceMuted,
        )
    }
}

/** The bar of the Text tab with nothing selected: back to the Edit tools, and Add text at the playhead. */
@Composable
fun TextTabBar(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    onCaptions: () -> Unit = {},
    onStickers: () -> Unit = {},
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
        ToolButton(
            label = stringResource(R.string.editor_edit),
            icon = PetruIcons.ToolEdit,
            active = true,
            onClick = onBack,
        )
        ToolButton(
            label = stringResource(R.string.editor_add_text),
            icon = PetruIcons.Add,
            active = true,
            onClick = onAdd,
        )
        ToolButton(
            label = stringResource(R.string.editor_stickers),
            icon = PetruIcons.ToolOverlay,
            active = true,
            onClick = onStickers,
        )
        ToolButton(
            label = stringResource(R.string.editor_auto_captions),
            icon = PetruIcons.ToolText,
            active = true,
            onClick = onCaptions,
        )
    }
}

/** The bar of the Effects tab with no picture clip selected. */
@Composable
fun EffectsTabBar(onBack: () -> Unit, modifier: Modifier = Modifier, onBackground: () -> Unit = {}) {
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
        ToolButton(
            label = stringResource(R.string.editor_edit),
            icon = PetruIcons.ToolEdit,
            active = true,
            onClick = onBack,
        )
        ToolButton(
            label = stringResource(R.string.editor_background),
            icon = PetruIcons.ToolFilters,
            active = true,
            onClick = onBackground,
        )
        Text(
            stringResource(R.string.editor_select_a_clip_to_add_2),
            style = PetruType.Body,
            color = PetruColors.OnSurfaceMuted,
        )
    }
}

/** The bar of the Audio tab with no clip selected: back to Edit, Add music from a file, and Record voice-over. */
@Composable
fun AudioTabBar(onBack: () -> Unit, onAdd: () -> Unit, onRecord: () -> Unit, modifier: Modifier = Modifier) {
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
        ToolButton(
            label = stringResource(R.string.editor_edit),
            icon = PetruIcons.ToolEdit,
            active = true,
            onClick = onBack,
        )
        ToolButton(
            label = stringResource(R.string.editor_add_music),
            icon = PetruIcons.Add,
            active = true,
            onClick = onAdd,
        )
        ToolButton(
            label = stringResource(R.string.editor_record_voiceover),
            icon = PetruIcons.Mic,
            active = true,
            onClick = onRecord,
        )
    }
}
