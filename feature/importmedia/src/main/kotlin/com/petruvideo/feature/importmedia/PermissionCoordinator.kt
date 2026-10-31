package com.petruvideo.feature.importmedia

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.petruvideo.core.model.MediaKind

enum class MediaAccess { NotAsked, Granted, Partial, Denied }

/**
 * Just-in-time media access (constitution Principle VII). Pure logic is separate from the Compose glue so it
 * can be tested on the JVM. Partial access ("selected photos and videos") exists from Android 14.
 */
object PermissionCoordinator {
    private const val SDK_PARTIAL_ACCESS = 34
    private const val SDK_GRANULAR_MEDIA = 33

    fun permissionsFor(kind: MediaKind, sdk: Int): List<String> = when {
        sdk >= SDK_PARTIAL_ACCESS && kind == MediaKind.Audio -> listOf(Manifest.permission.READ_MEDIA_AUDIO)
        sdk >= SDK_PARTIAL_ACCESS -> listOf(kindPermission(kind), Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        sdk >= SDK_GRANULAR_MEDIA -> listOf(kindPermission(kind))
        else -> listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun accessFrom(kind: MediaKind, sdk: Int, granted: Set<String>, requestedBefore: Boolean): MediaAccess {
        val required = permissionsFor(kind, sdk)
        return when {
            required.first() in granted -> MediaAccess.Granted
            required.size > 1 && required[1] in granted -> MediaAccess.Partial
            requestedBefore -> MediaAccess.Denied
            else -> MediaAccess.NotAsked
        }
    }

    private fun kindPermission(kind: MediaKind) = when (kind) {
        MediaKind.Video -> Manifest.permission.READ_MEDIA_VIDEO
        MediaKind.Photo -> Manifest.permission.READ_MEDIA_IMAGES
        MediaKind.Audio -> Manifest.permission.READ_MEDIA_AUDIO
    }
}

/** Handle returned by [rememberMediaAccess]. */
class MediaAccessRequester(val access: MediaAccess, val request: () -> Unit)

@Composable
fun rememberMediaAccess(kind: MediaKind, onChanged: (MediaAccess) -> Unit): MediaAccessRequester {
    val context = LocalContext.current
    var requested by rememberSaveable { mutableStateOf(false) }
    val sdk = Build.VERSION.SDK_INT

    fun current(): MediaAccess {
        val granted = PermissionCoordinator.permissionsFor(kind, sdk)
            .filter { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
            .toSet()
        return PermissionCoordinator.accessFrom(kind, sdk, granted, requested)
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        requested = true
        onChanged(current())
    }
    val access = current()
    return remember(kind, access) {
        MediaAccessRequester(access) {
            launcher.launch(PermissionCoordinator.permissionsFor(kind, sdk).toTypedArray())
        }
    }
}
