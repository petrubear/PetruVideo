package com.petruvideo.feature.projects

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.petruvideo.core.designsystem.adaptive.Arrangement
import com.petruvideo.core.designsystem.adaptive.LocalArrangement
import com.petruvideo.feature.projects.ui.ProjectsScreen

@Composable
fun ProjectsRoute(
    onOpenEditor: (projectId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProjectsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val arrangement = LocalArrangement.current
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ProjectsEffect.OpenEditor -> onOpenEditor(effect.projectId)
            }
        }
    }
    ProjectsScreen(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier,
        wideContent = (arrangement as? Arrangement.Compact)?.wideContent == true,
    )
}
