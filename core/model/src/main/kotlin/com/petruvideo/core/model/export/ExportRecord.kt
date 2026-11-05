package com.petruvideo.core.model.export

/** One finished export kept in the history: where the video is and what it was made from. */
data class ExportRecord(
    val id: String,
    val projectName: String,
    val uri: String,
    val durationMs: Long,
    val bytes: Long,
    val settings: ExportSettings,
    val finishedAt: Long,
)
