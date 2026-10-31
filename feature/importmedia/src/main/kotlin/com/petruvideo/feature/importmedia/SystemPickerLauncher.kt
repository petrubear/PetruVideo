package com.petruvideo.feature.importmedia

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.petruvideo.core.model.MediaKind

/**
 * The no-broad-access path: the system document picker, which needs no media permission. Documents (not the
 * photo picker) are used because their grants can be made persistable, so clips keep working after a restart
 * (research 6, Spike 2).
 */
@Composable
fun rememberSystemPicker(onPicked: (List<String>) -> Unit): (MediaKind) -> Unit {
    val context = LocalContext.current
    val contract = ActivityResultContracts.OpenMultipleDocuments()
    val launcher = rememberLauncherForActivityResult(contract) { uris: List<Uri> ->
        uris.forEach { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        if (uris.isNotEmpty()) onPicked(uris.map(Uri::toString))
    }
    return remember(launcher) {
        { kind ->
            val mime = when (kind) {
                MediaKind.Video -> "video/*"
                MediaKind.Photo -> "image/*"
                MediaKind.Audio -> "audio/*"
            }
            launcher.launch(arrayOf(mime))
        }
    }
}
