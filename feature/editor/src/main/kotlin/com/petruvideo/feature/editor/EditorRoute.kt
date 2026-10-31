package com.petruvideo.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.designsystem.components.IconAction
import com.petruvideo.core.designsystem.icons.PetruIcons

/** Stub that loads the project through `ProjectSession`; replaced by the real editor in US3 (T086). */
@Composable
fun EditorRoute(
    projectId: String,
    onBack: () -> Unit,
    onAddMedia: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditorViewModel = hiltViewModel(),
) {
    val project by viewModel.project.collectAsStateWithLifecycle()
    LaunchedEffect(projectId) { viewModel.open(projectId) }

    Column(modifier.fillMaxSize().background(PetruColors.Background).statusBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconAction(PetruIcons.Back, "Back", onBack)
            Text(project?.name.orEmpty(), style = PetruType.Title, color = PetruColors.OnBackground)
            IconAction(PetruIcons.Add, "Add media", onAddMedia)
        }
    }
}
