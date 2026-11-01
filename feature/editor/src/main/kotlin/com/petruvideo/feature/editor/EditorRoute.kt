package com.petruvideo.feature.editor

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.petruvideo.feature.editor.ui.EditorScreen
import com.petruvideo.feature.editor.ui.LocalMediaLoaders
import kotlinx.coroutines.launch

/** Opens the project through `ProjectSession`, shows the editor, and connects its effects to the screen. */
@Composable
fun EditorRoute(
    projectId: String,
    onBack: () -> Unit,
    onAddMedia: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditorViewModel = hiltViewModel(),
    mediaViewModel: EditorMediaViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var relinkTarget by remember { mutableStateOf<String?>(null) }
    val currentAddMedia by rememberUpdatedState(onAddMedia)
    val currentExport by rememberUpdatedState(onExport)

    val relinkPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val target = relinkTarget
        if (uri != null && target != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            scope.launch {
                mediaViewModel.probe(uri.toString())?.let { viewModel.onEvent(EditorEvent.Relink(target, it)) }
            }
        }
    }

    val pickReplacement = rememberReplaceAudioPicker(mediaViewModel, viewModel::onEvent)
    val pickOverlay = rememberOverlayPicker(mediaViewModel, viewModel::onEvent)
    val pickMusic = rememberMusicPicker(mediaViewModel, viewModel::onEvent)

    LaunchedEffect(projectId) { viewModel.open(projectId) }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                EditorEffect.OpenAddMedia -> currentAddMedia()
                EditorEffect.OpenExport -> currentExport()
                is EditorEffect.PickReplacementAudio -> pickReplacement(effect.clipId)
                EditorEffect.PickOverlayMedia -> pickOverlay()
                EditorEffect.PickMusic -> pickMusic()
                is EditorEffect.PickRelinkFile -> {
                    relinkTarget = effect.mediaItemId
                    relinkPicker.launch(arrayOf("video/*", "image/*", "audio/*"))
                }
            }
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.flush() }
    // Coming back from freeing space retries a save that failed for lack of it.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.flush() }
    BackHandler { viewModel.flush(); onBack() }

    CompositionLocalProvider(LocalMediaLoaders provides mediaViewModel.loaders) {
        EditorScreen(
            state = state,
            player = viewModel.previewPlayer,
            onEvent = viewModel::onEvent,
            onBack = { viewModel.flush(); onBack() },
            modifier = modifier,
        )
    }
}

