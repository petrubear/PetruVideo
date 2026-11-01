package com.petruvideo.feature.editor

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

/**
 * The microphone question, asked only when the user taps Record (constitution Principle VII). Returns the function to
 * call on that tap; the answer comes back as a [MicAccess].
 */
@Composable
fun rememberMicrophone(onResult: (MicAccess) -> Unit): () -> Unit {
    val context = LocalContext.current
    val latest = rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val canAskAgain = context.activity()?.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)
            ?: false
        latest.value(MicPermission.fromAnswer(granted, canAskAgain))
    }
    return {
        val allowed = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (allowed) latest.value(MicAccess.Granted) else launcher.launch(Manifest.permission.RECORD_AUDIO)
    }
}

/** Opens this app's page in the phone's settings, where the microphone can be turned on. */
fun openAppSettings(context: Context) {
    val page = Uri.fromParts("package", context.packageName, null)
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, page).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
