package com.petruvideo.core.media.chroma

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads the colour of an overlay's own picture at a point, for the Pick tool of the chroma key (feature 022). */
fun interface ChromaSampler {
    /** The opaque ARGB colour of [media] at [sourceTimeMs], at ([u], [v]) from 0 to 1 across the picture, or null. */
    suspend fun sample(media: MediaItem, sourceTimeMs: Long, u: Float, v: Float): Int?
}

class DeviceChromaSampler @Inject constructor(
    @ApplicationContext private val context: Context,
) : ChromaSampler {
    override suspend fun sample(media: MediaItem, sourceTimeMs: Long, u: Float, v: Float): Int? =
        withContext(Dispatchers.IO) {
            if (u !in 0f..1f || v !in 0f..1f) return@withContext null
            val frame = runCatching { frameOf(media, sourceTimeMs) }.getOrNull() ?: return@withContext null
            val x = (u * frame.width).toInt().coerceIn(0, frame.width - 1)
            val y = (v * frame.height).toInt().coerceIn(0, frame.height - 1)
            val color = frame.getPixel(x, y) or OPAQUE
            frame.recycle()
            color
        }

    private fun frameOf(media: MediaItem, sourceTimeMs: Long): Bitmap? {
        val uri = Uri.parse(media.uri)
        if (media.kind == MediaKind.Photo) {
            return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.getFrameAtTime(sourceTimeMs * MICROS_PER_MS, MediaMetadataRetriever.OPTION_CLOSEST)
        } finally {
            retriever.release()
        }
    }

    private companion object {
        const val OPAQUE = 0xFF000000.toInt()
        const val MICROS_PER_MS = 1_000L
    }
}
