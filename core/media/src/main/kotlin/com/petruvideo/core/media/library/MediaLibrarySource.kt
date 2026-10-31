package com.petruvideo.core.media.library

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import com.petruvideo.core.model.MediaKind
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The album chips of the Add media screen. */
enum class LibraryAlbum(val label: String, internal val pathHints: List<String>) {
    Recent("Recent", emptyList()),
    Camera("Camera", listOf("DCIM/Camera")),
    ScreenRecordings("Screen recordings", listOf("Screen recordings", "Screenrecorder", "Screenshots")),
    Downloads("Downloads", listOf("Download")),
}

data class LibraryItem(
    val uri: String,
    val kind: MediaKind,
    val displayName: String,
    val durationMs: Long?,
    val dateAddedSec: Long,
)

/** Queries the device media library (MediaStore) for the Add media grid. */
interface MediaLibrary {
    suspend fun items(kind: MediaKind, album: LibraryAlbum, limit: Int, offset: Int): List<LibraryItem>
}

class MediaLibrarySource @Inject constructor(
    @ApplicationContext private val context: Context,
) : MediaLibrary {

    override suspend fun items(kind: MediaKind, album: LibraryAlbum, limit: Int, offset: Int): List<LibraryItem> =
        withContext(Dispatchers.IO) {
            val collection = collection(kind)
            val hasDuration = kind != MediaKind.Photo
            val projection = buildList {
                add(MediaStore.MediaColumns._ID)
                add(MediaStore.MediaColumns.DISPLAY_NAME)
                add(MediaStore.MediaColumns.DATE_ADDED)
                if (hasDuration) add(MediaStore.MediaColumns.DURATION)
            }.toTypedArray()
            val (selection, args) = selection(album)
            val query = Bundle().apply {
                putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, args)
                putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, "${MediaStore.MediaColumns.DATE_ADDED} DESC")
                putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
                putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
            }
            val result = mutableListOf<LibraryItem>()
            context.contentResolver.query(collection, projection, query, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                val durCol = if (hasDuration) cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DURATION) else -1
                while (cursor.moveToNext()) {
                    result += LibraryItem(
                        uri = ContentUris.withAppendedId(collection, cursor.getLong(idCol)).toString(),
                        kind = kind,
                        displayName = cursor.getString(nameCol).orEmpty(),
                        durationMs = if (durCol >= 0 && !cursor.isNull(durCol)) cursor.getLong(durCol) else null,
                        dateAddedSec = cursor.getLong(dateCol),
                    )
                }
            }
            result
        }

    private fun collection(kind: MediaKind): Uri = when (kind) {
        MediaKind.Video -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        MediaKind.Photo -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        MediaKind.Audio -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }

    private fun selection(album: LibraryAlbum): Pair<String?, Array<String>?> {
        if (album.pathHints.isEmpty()) return null to null
        val column = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.MediaColumns.RELATIVE_PATH
        } else {
            @Suppress("DEPRECATION")
            MediaStore.MediaColumns.DATA
        }
        val clause = album.pathHints.joinToString(" OR ") { "$column LIKE ?" }
        return "($clause)" to album.pathHints.map { "%$it%" }.toTypedArray()
    }
}
