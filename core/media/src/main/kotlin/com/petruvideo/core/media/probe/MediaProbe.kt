package com.petruvideo.core.media.probe

import android.content.Context
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.Fingerprint
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.error.AppError
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads a media file's metadata; implemented by [MediaProbe] and replaced by a fake in tests. */
fun interface MediaProber {
    suspend fun probe(uri: Uri, id: String): Result<MediaItem>
}

class MediaProbeException(val error: AppError, cause: Throwable? = null) : Exception(error.toString(), cause)

/** Reads metadata and a fingerprint from a content or file URI without modifying the file. */
class MediaProbe @Inject constructor(@ApplicationContext private val context: Context) : MediaProber {

    override suspend fun probe(uri: Uri, id: String): Result<MediaItem> =
        withContext(Dispatchers.IO) {
            val name = displayName(uri)
            try {
                Result.success(read(uri, id, name))
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Result.failure(MediaProbeException(AppError.UnsupportedFile(name), e))
            }
        }

    private fun read(uri: Uri, id: String, name: String): MediaItem {
        val mime = mimeType(uri, name) ?: error("unknown type")
        val kind = when {
            mime.startsWith("video/") -> MediaKind.Video
            mime.startsWith("image/") -> MediaKind.Photo
            mime.startsWith("audio/") -> MediaKind.Audio
            else -> error("unsupported type $mime")
        }
        val details = when (kind) {
            MediaKind.Video -> readVideo(uri)
            MediaKind.Audio -> readAudio(uri)
            MediaKind.Photo -> readPhoto(uri)
        }
        return MediaItem(
            id = id,
            kind = kind,
            uri = uri.toString(),
            displayName = name,
            durationMs = details.durationMs,
            width = details.width,
            height = details.height,
            frameRate = details.frameRate,
            hasAudio = details.hasAudio,
            fingerprint = Fingerprint(sizeBytes(uri), lastModified(uri), details.durationMs, mime),
            availability = Availability.Available,
        )
    }

    private class Details(
        val durationMs: Long?,
        val width: Int?,
        val height: Int?,
        val frameRate: Float?,
        val hasAudio: Boolean,
    )

    private fun readVideo(uri: Uri): Details {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
            var width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt()
            var height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt()
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toInt() ?: 0
            check(duration != null && duration > 0 && width != null && height != null && width > 0 && height > 0)
            if (rotation == ROTATION_90 || rotation == ROTATION_270) width = height.also { height = width }
            val hasAudio = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes"
            return Details(duration, width, height, videoFrameRate(uri), hasAudio)
        } finally {
            retriever.release()
        }
    }

    private fun videoFrameRate(uri: Uri): Float? {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                if (format.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true &&
                    format.containsKey(MediaFormat.KEY_FRAME_RATE)
                ) {
                    return format.getInteger(MediaFormat.KEY_FRAME_RATE).toFloat()
                }
            }
            return null
        } finally {
            extractor.release()
        }
    }

    private fun readAudio(uri: Uri): Details {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
            check(duration != null && duration > 0)
            return Details(duration, null, null, null, hasAudio = true)
        } finally {
            retriever.release()
        }
    }

    private fun readPhoto(uri: Uri): Details {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
        check(bounds.outWidth > 0 && bounds.outHeight > 0)
        val orientation = context.contentResolver.openInputStream(uri)!!.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }
        val swap = orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
            orientation == ExifInterface.ORIENTATION_ROTATE_270
        val width = if (swap) bounds.outHeight else bounds.outWidth
        val height = if (swap) bounds.outWidth else bounds.outHeight
        return Details(null, width, height, null, hasAudio = false)
    }

    private fun displayName(uri: Uri): String {
        val queried = if (uri.scheme == "content") queryColumn<String>(uri, OpenableColumns.DISPLAY_NAME) else null
        return queried ?: uri.lastPathSegment?.substringAfterLast('/') ?: uri.toString()
    }

    private fun mimeType(uri: Uri, name: String): String? =
        context.contentResolver.getType(uri)
            ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substringAfterLast('.', "").lowercase())

    private fun sizeBytes(uri: Uri): Long = when {
        uri.scheme == "file" -> File(uri.path!!).length()
        else -> queryColumn<Long>(uri, OpenableColumns.SIZE)
            ?: context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
            ?: 0L
    }

    private fun lastModified(uri: Uri): Long = when {
        uri.scheme == "file" -> File(uri.path!!).lastModified()
        else -> queryColumn<Long>(uri, DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            ?: queryColumn<Long>(uri, MediaStore.MediaColumns.DATE_MODIFIED)?.times(MILLIS)
            ?: 0L
    }

    /** First row, first column of a single-column query, or null when the provider does not know the column. */
    private inline fun <reified T> queryColumn(uri: Uri, column: String): T? = runCatching {
        context.contentResolver.query(uri, arrayOf(column), null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst() || cursor.isNull(0)) {
                null
            } else if (T::class == String::class) {
                cursor.getString(0) as T
            } else {
                cursor.getLong(0) as T
            }
        }
    }.getOrNull()

    private companion object {
        const val ROTATION_90 = 90
        const val ROTATION_270 = 270
        const val MILLIS = 1000L
    }
}
