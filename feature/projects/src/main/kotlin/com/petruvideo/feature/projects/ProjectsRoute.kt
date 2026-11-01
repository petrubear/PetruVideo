package com.petruvideo.feature.projects

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.designsystem.adaptive.AdaptiveScaffold
import com.petruvideo.core.designsystem.adaptive.ArrangementResolver
import com.petruvideo.core.designsystem.adaptive.LocalArrangement
import com.petruvideo.feature.projects.ui.ProjectsScreen

@Composable
fun ProjectsRoute(
    onOpenEditor: (projectId: String) -> Unit,
    onOpenExport: (projectId: String) -> Unit,
    onOpenExports: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProjectsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val arrangement = LocalArrangement.current
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ProjectsEffect.OpenEditor -> onOpenEditor(effect.projectId)
                is ProjectsEffect.OpenExport -> onOpenExport(effect.projectId)
                ProjectsEffect.OpenExports -> onOpenExports()
                ProjectsEffect.OpenSettings -> onOpenSettings()
            }
        }
    }
    AdaptiveScaffold(
        arrangement = arrangement,
        modifier = modifier,
        compact = { wide ->
            ProjectsScreen(state, viewModel::onEvent, wideContent = wide)
        },
        expanded = { hinge ->
            ProjectsScreen(state, viewModel::onEvent, twoPane = true, hinge = hinge)
        },
        tabletopTop = { TabletopTitle() },
        tabletopBottom = {
            // The controls half is wide when the window is, but fall back to the phone layout if it is not.
            BoxWithConstraints {
                if (maxWidth.value >= ArrangementResolver.EXPANDED_MIN_DP) {
                    ProjectsScreen(state, viewModel::onEvent, twoPane = true)
                } else {
                    ProjectsScreen(state, viewModel::onEvent)
                }
            }
        },
    )
}

@Composable
private fun TabletopTitle() {
    Box(Modifier.fillMaxSize().background(PetruColors.Background), contentAlignment = Alignment.Center) {
        Text("Projects", style = PetruType.Display, color = PetruColors.OnBackground)
    }
}
