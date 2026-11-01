package com.petruvideo.feature.editor

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.petruvideo.core.model.MediaKind
import kotlinx.coroutines.launch

/** Opens the system file picker for an audio file and sends the chosen file back as music. Cancelling adds nothing. */
@Composable
fun rememberMusicPicker(media: EditorMediaViewModel, onEvent: (EditorEvent) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            scope.launch {
                media.probe(uri.toString())?.takeIf { it.kind == MediaKind.Audio }
                    ?.let { onEvent(EditorEvent.AddMusicMedia(it)) }
            }
        }
    }
    return { picker.launch(arrayOf("audio/*")) }
}
