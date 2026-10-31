package com.petruvideo.core.data.importing

import android.net.Uri
import com.petruvideo.core.media.probe.MediaProbeException
import com.petruvideo.core.media.probe.MediaProber
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.error.AppError
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Imports selected files; implemented by [ImportMediaUseCase] and replaced by a fake in view model tests. */
fun interface MediaImporter {
    suspend fun import(sources: List<String>, onProgress: (ImportProgress) -> Unit): ImportResult
}

data class ImportProgress(val completed: Int, val total: Int)

/** [items] are the files that could be read, in selection order; [errors] name the files that could not. */
data class ImportResult(val items: List<MediaItem>, val errors: List<AppError.UnsupportedFile>)

/**
 * Reads the selected files one by one (metadata and fingerprint only, originals are never written), reports
 * progress after each file, and stops between files when the calling coroutine is cancelled. A file that cannot
 * be read becomes an error entry and does not stop the batch. Importing the same file twice yields two items.
 */
class ImportMediaUseCase(
    private val prober: MediaProber,
    private val newId: () -> String,
) : MediaImporter {
    @Inject
    constructor(prober: MediaProber) : this(prober, { UUID.randomUUID().toString() })

    override suspend fun import(sources: List<String>, onProgress: (ImportProgress) -> Unit): ImportResult =
        invoke(sources.map(Uri::parse), onProgress)

    suspend operator fun invoke(
        sources: List<Uri>,
        onProgress: (ImportProgress) -> Unit = {},
    ): ImportResult {
        val items = mutableListOf<MediaItem>()
        val errors = mutableListOf<AppError.UnsupportedFile>()
        sources.forEachIndexed { index, uri ->
            currentCoroutineContext().ensureActive()
            val result = prober.probe(uri, newId())
            result.onSuccess { items += it }
            result.onFailure { error ->
                if (error is CancellationException) throw error
                errors += ((error as? MediaProbeException)?.error as? AppError.UnsupportedFile)
                    ?: AppError.UnsupportedFile(uri.lastPathSegment.orEmpty())
            }
            onProgress(ImportProgress(index + 1, sources.size))
        }
        return ImportResult(items, errors)
    }
}
