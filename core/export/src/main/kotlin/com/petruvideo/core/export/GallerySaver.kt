package com.petruvideo.core.export

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

/**
 * Publishes a rendered file to the gallery under Movies/PetruVideo. On Android 10 and up it goes through MediaStore
 * as a pending item that becomes visible only when fully written, so no partial file shows; no storage permission
 * is needed. On Android 8 and 9 it writes to the public Movies folder (the legacy write permission is requested by
 * the screen) and scans it.
 */
class GallerySaver(private val context: Context) : ExportSaver {
    override suspend fun save(file: File, displayName: String): SavedVideo? = withContext(Dispatchers.IO) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) saveWithMediaStore(file, displayName)
            else saveLegacy(file, displayName)
        }.getOrNull()
    }

    private fun saveWithMediaStore(file: File, displayName: String): SavedVideo? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, MIME)
            put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/$ALBUM")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        return try {
            val out = resolver.openOutputStream(uri) ?: error("no stream")
            out.use { stream -> file.inputStream().use { it.copyTo(stream) } }
            resolver.update(uri, ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }, null, null)
            SavedVideo(uri.toString(), file.length())
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun saveLegacy(file: File, displayName: String): SavedVideo? {
        val movies = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val dir = File(movies, ALBUM).apply { mkdirs() }
        val target = File(dir, displayName)
        try {
            file.copyTo(target, overwrite = true)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            target.delete()
            throw e
        }
        val uri = suspendCancellableCoroutine { continuation ->
            MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf(MIME)) { _, scanned ->
                continuation.resume(scanned?.toString() ?: target.toURI().toString())
            }
        }
        return SavedVideo(uri, target.length())
    }

    private companion object {
        const val MIME = "video/mp4"
        const val ALBUM = "PetruVideo"
    }
}
