package com.petruvideo.feature.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.petruvideo.feature.export.ui.ExportsScreen
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.res.stringResource

/** Shows the Exports list and opens or shares a video through the system. */
@Composable
fun ExportsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExportsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    LaunchedEffect(viewModel) { viewModel.refresh() }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ExportsEffect.OpenVideo -> context.startActivity(
                    Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(effect.uri), "video/mp4")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                )
                is ExportsEffect.ShareVideo -> context.startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).setType("video/mp4")
                            .putExtra(Intent.EXTRA_STREAM, Uri.parse(effect.uri))
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                        resources.getString(R.string.export_share_video),
                    ),
                )
            }
        }
    }
    BackHandler(onBack = onBack)
    ExportsScreen(state, viewModel::onEvent, onBack, modifier)
}

/** Opens the video's own location to see if it is still there. */
@Module
@InstallIn(SingletonComponent::class)
object ExportsModule {
    @Provides
    fun fileChecker(@ApplicationContext context: Context): ExportFileChecker = ExportFileChecker { uri ->
        withContext(Dispatchers.IO) {
            runCatching { context.contentResolver.openFileDescriptor(Uri.parse(uri), "r")?.use { true } ?: false }
                .getOrDefault(false)
        }
    }
}
