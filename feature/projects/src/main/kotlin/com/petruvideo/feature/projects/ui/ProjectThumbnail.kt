package com.petruvideo.feature.projects.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.petruvideo.core.model.Project
import java.io.File

/**
 * The picture of the project's first clip, filling its box (cropped, not stretched). The colored box is drawn under
 * it, so it shows while the picture loads and stays when there is no picture or the file is gone.
 */
@Composable
fun ProjectThumbnail(project: Project, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit = {}) {
    PlaceholderThumbnail(project.id, modifier) {
        project.thumbnailRef?.let { path ->
            val context = LocalContext.current
            AsyncImage(
                model = ImageRequest.Builder(context).data(File(path)).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
        content()
    }
}
