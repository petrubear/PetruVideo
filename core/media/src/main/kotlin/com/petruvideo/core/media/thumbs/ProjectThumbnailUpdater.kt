package com.petruvideo.core.media.thumbs

import android.content.Context
import android.graphics.Bitmap
import com.petruvideo.core.media.preview.ThumbnailRefresher
import com.petruvideo.core.model.Timeline
import java.io.File

/**
 * Keeps a project's thumbnail equal to the first frame of the first clip on its timeline. [save] stores the
 * resulting reference (a file path) on the project, e.g. `repository::setThumbnail`. The file name follows the first
 * clip (see [ProjectThumbFiles]), and the project's earlier pictures are deleted.
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
        val dir = File(context.filesDir, "project-thumbs").apply { mkdirs() }
        if (bitmap == null || key == null) {
            save(projectId, null)
            ProjectThumbFiles.deleteOthers(dir, projectId, keep = null)
        } else {
            val file = ProjectThumbFiles.fileFor(dir, projectId, key)
            write(file, bitmap)
            save(projectId, file.path)
            ProjectThumbFiles.deleteOthers(dir, projectId, keep = file)
        }
    }

    private fun write(file: File, bitmap: Bitmap) {
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, it) }
    }

    private companion object {
        const val PROJECT_THUMB_HEIGHT_PX = 256
        const val PNG_QUALITY = 100
    }
}
