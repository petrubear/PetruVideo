package com.petruvideo.core.media.freeze

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.petruvideo.core.media.probe.MediaProber
import com.petruvideo.core.model.MediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Makes the still of a freeze frame (feature 017). */
fun interface FreezeStillMaker {
    /** A photo made from the frame of [media] at [sourceTimeMs], or null when it cannot be read. */
    suspend fun make(media: MediaItem, sourceTimeMs: Long): MediaItem?
}

/**
 * Saves the frame closest to a time as a full-size JPEG in the app's storage (never in the gallery) and probes it as a
 * photo, so a freeze is an ordinary photo clip.
 */
class FreezeStills @Inject constructor(
    @ApplicationContext private val context: Context,
    private val probe: MediaProber,
) : FreezeStillMaker {
    override suspend fun make(media: MediaItem, sourceTimeMs: Long): MediaItem? = withContext(Dispatchers.IO) {
        val frame = frameAt(media, sourceTimeMs) ?: return@withContext null
        val file = File(File(context.filesDir, "freezes").apply { mkdirs() }, "freeze-${UUID.randomUUID()}.jpg")
        val saved = runCatching { file.outputStream().use { frame.compress(Bitmap.CompressFormat.JPEG, QUALITY, it) } }
        frame.recycle()
        if (saved.isFailure) {
            file.delete()
            return@withContext null
        }
        probe.probe(Uri.fromFile(file), UUID.randomUUID().toString()).getOrNull() ?: run {
            file.delete()
            null
        }
    }

    private fun frameAt(media: MediaItem, timeMs: Long): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            runCatching {
                retriever.setDataSource(context, Uri.parse(media.uri))
                retriever.getFrameAtTime(timeMs * MICROS_PER_MS, MediaMetadataRetriever.OPTION_CLOSEST)
            }.getOrNull()
        } finally {
            retriever.release()
        }
    }

    private companion object {
        const val QUALITY = 95
        const val MICROS_PER_MS = 1_000L
    }
}
