package com.petruvideo.core.media.thumbs

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlin.math.max
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Timeline thumbnails at fixed time steps (research 7). Frames come from the platform frame extraction API on a
 * bounded dispatcher, are cancellable, and are cached on disk keyed by the media fingerprint, step, and size.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ThumbnailStripGenerator(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher,
) {
    @Inject
    constructor(@ApplicationContext context: Context) :
        this(context, Dispatchers.IO.limitedParallelism(EXTRACT_PARALLELISM))

    private val extractions = AtomicInteger()

    /** How many frames were actually extracted (cache hits do not count); exposed for tests. */
    val extractionCount: Int get() = extractions.get()

    /** The thumbnail of [item] at the step containing [timeMs], scaled so its height is [heightPx]. */
    suspend fun frame(item: MediaItem, timeMs: Long, heightPx: Int = DEFAULT_HEIGHT_PX): Bitmap? {
        if (item.kind == MediaKind.Audio) return null
        val bucket = if (item.kind == MediaKind.Photo) 0L else timeMs / STEP_MS * STEP_MS
        val cache = cacheFile(item, bucket, heightPx)
        return withContext(dispatcher) {
            currentCoroutineContext().ensureActive()
            readCache(cache) ?: runCatching { extract(item, bucket, heightPx) }.getOrNull()?.also {
                extractions.incrementAndGet()
                writeCache(cache, it)
            }
        }
    }

    private fun cacheFile(item: MediaItem, bucket: Long, heightPx: Int): File {
        val f = item.fingerprint
        val dir = File(context.cacheDir, "thumbs/${f.sizeBytes}-${f.lastModified}-${f.durationMs}/$heightPx")
        return File(dir.apply { mkdirs() }, "$bucket.webp")
    }

    private fun readCache(file: File): Bitmap? = if (file.exists()) BitmapFactory.decodeFile(file.path) else null

    @Suppress("DEPRECATION")
    private fun writeCache(file: File, bitmap: Bitmap) {
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            Bitmap.CompressFormat.WEBP
        }
        file.outputStream().use { bitmap.compress(format, WEBP_QUALITY, it) }
    }

    private fun extract(item: MediaItem, bucketMs: Long, heightPx: Int): Bitmap? {
        val uri = Uri.parse(item.uri)
        return if (item.kind == MediaKind.Photo) decodePhoto(uri, heightPx) else decodeFrame(uri, bucketMs, heightPx)
    }

    private fun decodeFrame(uri: Uri, timeMs: Long, heightPx: Int): Bitmap? {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val timeUs = timeMs * MICROS_PER_MS
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: heightPx
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt()
                ?: heightPx
            val scaledWidth = max(1, width * heightPx / max(1, height))
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                val option = MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                retriever.getScaledFrameAtTime(timeUs, option, scaledWidth, heightPx)
            } else {
                retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?.let { Bitmap.createScaledBitmap(it, scaledWidth, heightPx, true) }
            }
        } finally {
            retriever.release()
        }
    }

    private fun decodePhoto(uri: Uri, heightPx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outHeight / (sample * 2) >= heightPx) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    companion object {
        /** Fixed time step between cached frames. */
        const val STEP_MS = 1_000L
        const val DEFAULT_HEIGHT_PX = 128
        private const val EXTRACT_PARALLELISM = 2
        private const val MICROS_PER_MS = 1_000L
        private const val WEBP_QUALITY = 80
    }
}
