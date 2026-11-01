package com.petruvideo.feature.projects.templates

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** The Templates screen: pick a template, choose files, and land in the editor with the filled project. */
@Composable
fun TemplatesRoute(
    onBack: () -> Unit,
    onOpenEditor: (projectId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TemplatesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach {
            runCatching {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        viewModel.picked(uris.map { it.toString() })
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is TemplatesEffect.PickMedia -> picker.launch(arrayOf("video/*", "image/*"))
                is TemplatesEffect.OpenEditor -> onOpenEditor(effect.projectId)
            }
        }
    }
    BackHandler(onBack = onBack)
    TemplatesScreen(state, viewModel::use, viewModel::dismissMessage, onBack, modifier)
}
