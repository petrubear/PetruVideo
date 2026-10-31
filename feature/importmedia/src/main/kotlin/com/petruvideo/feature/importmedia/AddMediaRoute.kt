package com.petruvideo.feature.importmedia

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.adaptive.AdaptiveScaffold
import com.petruvideo.core.designsystem.adaptive.Arrangement
import com.petruvideo.feature.importmedia.ui.AddMediaPanel
import com.petruvideo.core.designsystem.adaptive.LocalArrangement
import com.petruvideo.core.model.MediaItem
import com.petruvideo.feature.importmedia.ui.AddMediaScreen

private const val COMPACT_COLUMNS = 3
private const val MEDIUM_COLUMNS = 5
private const val EXPANDED_COLUMNS = 6

@Composable
fun AddMediaRoute(
    onItemsReady: (List<MediaItem>) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddMediaViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val arrangement = LocalArrangement.current
    val access = rememberMediaAccess(state.tab.kind) { viewModel.onEvent(AddMediaEvent.AccessChanged(it)) }
    val currentAccess by rememberUpdatedState(access)
    val picker = rememberSystemPicker { viewModel.onEvent(AddMediaEvent.PickedFromSystem(it)) }
    val currentPicker by rememberUpdatedState(picker)
    val currentItemsReady by rememberUpdatedState(onItemsReady)
    val currentClose by rememberUpdatedState(onClose)

    LaunchedEffect(access.access) { viewModel.onEvent(AddMediaEvent.AccessChanged(access.access)) }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is AddMediaEffect.Done -> {
                    currentItemsReady(effect.items)
                    if (effect.closeScreen) currentClose()
                }
                is AddMediaEffect.RequestAccess -> currentAccess.request()
                is AddMediaEffect.LaunchSystemPicker -> currentPicker(effect.kind)
                AddMediaEffect.Close -> currentClose()
            }
        }
    }
    val screen: @Composable (Int) -> Unit = { columns ->
        AddMediaScreen(state = state, onEvent = viewModel::onEvent, columns = columns)
    }
    AdaptiveScaffold(
        arrangement = arrangement,
        modifier = modifier,
        compact = { screen(columnsFor(arrangement)) },
        expanded = { AddMediaPanel { screen(EXPANDED_COLUMNS) } },
        // Tabletop: the picker fills the half below the hinge and the area above stays free.
        tabletopTop = { Box(Modifier.fillMaxSize().background(PetruColors.Background)) },
        tabletopBottom = { screen(EXPANDED_COLUMNS) },
    )
}

internal fun columnsFor(arrangement: Arrangement): Int = when (arrangement) {
    is Arrangement.Compact -> if (arrangement.wideContent) MEDIUM_COLUMNS else COMPACT_COLUMNS
    else -> EXPANDED_COLUMNS
}

