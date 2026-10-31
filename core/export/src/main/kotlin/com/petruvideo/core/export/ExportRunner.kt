package com.petruvideo.core.export

import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.export.ExportSettings
import java.io.File

/** One render request: the snapshot, the settings, the real video bitrate, and the temporary output file. */
data class ExportRequest(
    val timeline: Timeline,
    val settings: ExportSettings,
    val videoBitrate: Int,
    val output: File,
)

sealed interface RunResult {
    data class Done(val bytes: Long, val durationMs: Long) : RunResult
    data class Error(val detail: String) : RunResult
}

/** Renders a request to its output file. Cancelling the calling coroutine cancels the render. */
interface ExportRunner {
    suspend fun run(request: ExportRequest, onProgress: (Int) -> Unit): RunResult
}

/** Publishes a rendered file (to the gallery) and returns where it went. */
interface ExportSaver {
    /** Returns the stored location, or null when saving failed. Nothing is left behind on failure. */
    suspend fun save(file: File, displayName: String): SavedVideo?
}

data class SavedVideo(val uri: String, val bytes: Long)

/** Free space on the volume the render and the saved file use. */
fun interface StorageInfo {
    fun freeBytes(): Long
}
