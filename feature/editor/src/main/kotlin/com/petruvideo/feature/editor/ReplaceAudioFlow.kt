package com.petruvideo.feature.editor

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.petruvideo.core.model.MediaKind
import kotlinx.coroutines.launch

/**
 * Lets the user pick an audio file for the Replace tool. Returns a function that opens the system document picker
 * for [clipId]; the chosen file is kept readable across restarts, probed, and sent as [EditorEvent.ReplaceAudio].
 * A file that is not audio is ignored.
 */
@Composable
fun rememberReplaceAudioPicker(
    media: EditorMediaViewModel,
    onEvent: (EditorEvent) -> Unit,
): (clipId: String) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var target by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val clipId = target
        if (uri != null && clipId != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            scope.launch {
                media.probe(uri.toString())?.takeIf { it.kind == MediaKind.Audio }
                    ?.let { onEvent(EditorEvent.ReplaceAudio(clipId, it)) }
            }
        }
    }
    return { clipId ->
        target = clipId
        picker.launch(arrayOf("audio/*"))
    }
}
