package com.petruvideo.feature.importmedia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.designsystem.Spacing
import com.petruvideo.core.designsystem.components.IconAction
import com.petruvideo.core.designsystem.components.PillButton
import com.petruvideo.core.designsystem.components.SegmentedControl
import com.petruvideo.core.designsystem.icons.PetruIcons
import com.petruvideo.feature.importmedia.AddMediaEvent
import com.petruvideo.feature.importmedia.AddMediaState
import com.petruvideo.feature.importmedia.AddMediaTab
import com.petruvideo.feature.importmedia.MediaAccess
import androidx.compose.ui.res.stringResource
import com.petruvideo.feature.importmedia.R

/**
 * The Add media screen as designed: close button and title, Videos / Photos / Audio selector, album chips, the
 * thumbnail grid, and the selection tray with the single "Add to timeline in order" action. [columns] is 3 on
 * compact, 5 on medium and 6 on expanded width.
 */
@Composable
fun AddMediaScreen(
    state: AddMediaState,
    onEvent: (AddMediaEvent) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 3,
) {
    Column(modifier.fillMaxSize().background(PetruColors.Background).statusBarsPadding()) {
        Header(onClose = { onEvent(AddMediaEvent.Close) })
        SegmentedControl(
            options = AddMediaTab.entries.map { stringResource(it.labelRes) },
            selectedIndex = state.tab.ordinal,
            onSelect = { onEvent(AddMediaEvent.SwitchTab(AddMediaTab.entries[it])) },
            modifier = Modifier.padding(horizontal = Spacing.ScreenGutter).padding(top = 4.dp),
        )
        AlbumChipRow(
            albums = state.albums,
            selected = state.selectedAlbum,
            onSelect = { onEvent(AddMediaEvent.SwitchAlbum(it)) },
            modifier = Modifier.padding(top = 10.dp),
        )
        ErrorBanner(state, onDismiss = { onEvent(AddMediaEvent.DismissErrors) })
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (state.permission) {
                MediaAccess.Granted, MediaAccess.Partial -> SelectionGrid(
                    items = state.items,
                    columns = columns,
                    onToggle = { onEvent(AddMediaEvent.ToggleItem(it)) },
                    onLoadMore = { onEvent(AddMediaEvent.LoadMore) },
                    modifier = Modifier.fillMaxSize(),
                )
                MediaAccess.NotAsked, MediaAccess.Denied -> AccessPrompt(state.permission, onEvent)
            }
        }
        SelectionTray(
            selection = state.selection,
            totalDurationMs = state.totalDurationMs,
            progress = state.importProgress,
            onAdd = { onEvent(AddMediaEvent.AddToTimelineInOrder) },
            onCancel = { onEvent(AddMediaEvent.CancelImport) },
        )
    }
}

@Composable
private fun Header(onClose: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
        IconAction(PetruIcons.Close, stringResource(R.string.import_close), onClose, Modifier.align(Alignment.CenterStart))
        Text(stringResource(R.string.import_add_media), style = PetruType.Title, color = PetruColors.OnBackground)
    }
}

@Composable
private fun ErrorBanner(state: AddMediaState, onDismiss: () -> Unit) {
    if (state.errors.isEmpty()) return
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.ScreenGutter, vertical = 8.dp)
            .background(PetruColors.SurfaceHigh, RoundedCornerShape(12.dp))
            .padding(start = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
            state.errors.forEach {
                Text(
                    "${it.name} can't be used. The file is unsupported or damaged.",
                    style = PetruType.BodySmall,
                    color = PetruColors.Danger,
                )
            }
        }
        IconAction(PetruIcons.Close, stringResource(R.string.import_dismiss_message), onDismiss)
    }
}

/** Explains why access is needed and offers the system picker, which needs no broad access (FR-010). */
@Composable
private fun AccessPrompt(access: MediaAccess, onEvent: (AddMediaEvent) -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(Spacing.ScreenGutter),
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val message = if (access == MediaAccess.Denied) {
            stringResource(R.string.import_media_access_is_off_allow)
        } else {
            stringResource(R.string.import_access_rationale)
        }
        Text(message, style = PetruType.Body, color = PetruColors.OnSurfaceMuted, textAlign = TextAlign.Center)
        PillButton(stringResource(R.string.import_allow_access), onClick = { onEvent(AddMediaEvent.RequestAccess) })
        Text(
            stringResource(R.string.import_choose_files_instead),
            style = PetruType.Label,
            color = PetruColors.Accent,
            modifier = Modifier
                .padding(8.dp)
                .clickable { onEvent(AddMediaEvent.OpenSystemPicker) },
        )
    }
}
