package com.petruvideo.feature.export

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.petruvideo.feature.export.ui.ExportScreen

/** Opens the project, shows the export screen, asks for the permissions an export needs, and opens results. */
@Composable
fun ExportRoute(
    projectId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val request = ActivityResultContracts.RequestMultiplePermissions()
    val permissions = rememberLauncherForActivityResult(request) { granted ->
        // Notifications are optional; only the legacy storage write on Android 8 and 9 is required.
        val storageOk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
            granted[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
        if (storageOk) viewModel.onEvent(ExportEvent.Start)
    }

    LaunchedEffect(projectId) { viewModel.open(projectId) }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ExportEffect.OpenVideo -> context.startActivity(
                    Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(effect.uri), "video/mp4")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                )
                is ExportEffect.ShareVideo -> context.startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).setType("video/mp4")
                            .putExtra(Intent.EXTRA_STREAM, Uri.parse(effect.uri))
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                        "Share video",
                    ),
                )
            }
        }
    }
    BackHandler(onBack = onBack)

    ExportScreen(
        state = state,
        onEvent = { event ->
            if (event == ExportEvent.Start) {
                val missing = requiredPermissions().filter {
                    ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                }
                if (missing.isEmpty()) viewModel.onEvent(event) else permissions.launch(missing.toTypedArray())
            } else {
                viewModel.onEvent(event)
            }
        },
        onBack = onBack,
        modifier = modifier,
    )
}

private fun requiredPermissions(): List<String> = buildList {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
}
