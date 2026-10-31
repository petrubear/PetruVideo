package com.petruvideo.core.media.thumbs

import android.content.Context
import android.graphics.Bitmap
import com.petruvideo.core.media.preview.ThumbnailRefresher
import com.petruvideo.core.model.Timeline
import java.io.File

/**
 * Keeps a project's thumbnail equal to the first frame of the first clip on its timeline. [save] stores the
 * resulting reference (a file path) on the project, e.g. `repository::setThumbnail`.
 */
class ProjectThumbnailUpdater(
    private val context: Context,
    private val generator: ThumbnailStripGenerator,
    private val save: suspend (projectId: String, ref: String?) -> Unit,
) : ThumbnailRefresher {
    private var lastKey: Pair<String, String?>? = null

    override suspend fun refresh(projectId: String, timeline: Timeline) {
        val first = timeline.videoLane.firstOrNull()
        val key = first?.let { "${it.mediaItemId}@${it.sourceStartMs}" }
        if (lastKey == projectId to key) return
        lastKey = projectId to key
        val media = first?.let { timeline.media[it.mediaItemId] }
        val bitmap = media?.let { generator.frame(it, first.sourceStartMs, PROJECT_THUMB_HEIGHT_PX) }
        if (bitmap == null) {
            save(projectId, null)
        } else {
            save(projectId, write(projectId, bitmap).path)
        }
    }

    private fun write(projectId: String, bitmap: Bitmap): File {
        val file = File(File(context.filesDir, "project-thumbs").apply { mkdirs() }, "$projectId.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, it) }
        return file
    }

    private companion object {
        const val PROJECT_THUMB_HEIGHT_PX = 256
        const val PNG_QUALITY = 100
    }
}
