package com.petruvideo.feature.importmedia

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.petruvideo.core.designsystem.adaptive.Arrangement
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
    AddMediaScreen(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier,
        columns = columnsFor(arrangement),
    )
}

private fun columnsFor(arrangement: Arrangement): Int = when (arrangement) {
    is Arrangement.Compact -> if (arrangement.wideContent) MEDIUM_COLUMNS else COMPACT_COLUMNS
    else -> EXPANDED_COLUMNS
}

