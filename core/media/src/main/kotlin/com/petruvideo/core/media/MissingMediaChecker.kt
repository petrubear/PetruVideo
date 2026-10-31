package com.petruvideo.core.media

import android.content.Context
import android.net.Uri
import com.petruvideo.core.media.preview.MediaAvailabilityChecker
import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.MediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Recomputes media availability when a project opens (FR-012). An item is missing when its source cannot be
 * opened or its size no longer matches the stored fingerprint. Availability is never stored.
 */
class MissingMediaChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) : MediaAvailabilityChecker {

    override suspend fun check(items: Collection<MediaItem>): Map<String, Availability> = withContext(Dispatchers.IO) {
        items.associate { it.id to availabilityOf(it) }
    }

    private fun availabilityOf(item: MediaItem): Availability {
        val size = runCatching {
            context.contentResolver.openAssetFileDescriptor(Uri.parse(item.uri), "r")?.use { it.length }
        }.getOrNull()
        return when {
            size == null -> Availability.Missing
            size != UNKNOWN_LENGTH && size != item.fingerprint.sizeBytes -> Availability.Missing
            else -> Availability.Available
        }
    }

    private companion object {
        /** `AssetFileDescriptor.UNKNOWN_LENGTH`: some providers cannot report a size; then only openability counts. */
        const val UNKNOWN_LENGTH = android.content.res.AssetFileDescriptor.UNKNOWN_LENGTH
    }
}
