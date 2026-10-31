package com.petruvideo.core.data.autosave

import com.petruvideo.core.model.Timeline
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed interface SaveStatus {
    data object Saved : SaveStatus
    data object Saving : SaveStatus
    data class Failed(val cause: Throwable) : SaveStatus
}

/**
 * Debounced single-writer autosave. The newest submitted timeline replaces older pending ones; the write
 * runs [debounceMs] after the last submission, or immediately on [flush]. A failed write keeps the pending
 * timeline (the database transaction already rolled back, so the last good rows stay) until [retry] or the
 * next submission.
 */
class AutosaveWriter(
    private val scope: CoroutineScope,
    private val save: suspend (projectId: String, timeline: Timeline) -> Unit,
    private val debounceMs: Long = DEFAULT_DEBOUNCE_MS,
) {
    private class Pending(val projectId: String, val timeline: Timeline)

    private val writeLock = Mutex()
    private var pending: Pending? = null
    private var timer: Job? = null
    private val _status = MutableStateFlow<SaveStatus>(SaveStatus.Saved)
    val status: StateFlow<SaveStatus> = _status

    fun submit(projectId: String, timeline: Timeline) {
        pending = Pending(projectId, timeline)
        _status.value = SaveStatus.Saving
        timer?.cancel()
        timer = scope.launch {
            delay(debounceMs)
            writePending()
        }
    }

    /** Writes any pending timeline now, e.g. when the app stops. */
    suspend fun flush() {
        timer?.cancel()
        writePending()
    }

    /** Tries again after a failure, e.g. once the user has freed space. */
    suspend fun retry() = flush()

    private suspend fun writePending() = withContext(NonCancellable) {
        writeLock.withLock {
            val job = pending ?: return@withLock
            pending = null
            try {
                save(job.projectId, job.timeline)
                if (pending == null) _status.value = SaveStatus.Saved
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                if (pending == null) pending = job
                _status.value = SaveStatus.Failed(e)
            }
        }
    }

    private companion object {
        const val DEFAULT_DEBOUNCE_MS = 1_500L
    }
}
