package com.petruvideo.feature.export

import com.petruvideo.core.model.export.ExportRecord

/** One row of the Exports list: the record and whether its video can still be opened. */
data class ExportRow(val record: ExportRecord, val missing: Boolean = false)

data class ExportsUiState(
    val rows: List<ExportRow> = emptyList(),
    val loaded: Boolean = false,
    /** The entry just removed, offered for Undo for a few seconds. */
    val undo: ExportRecord? = null,
)

sealed interface ExportsEvent {
    data class Open(val id: String) : ExportsEvent
    data class Share(val id: String) : ExportsEvent
    data class Remove(val id: String) : ExportsEvent
    data object Undo : ExportsEvent
    data object DismissUndo : ExportsEvent
}

sealed interface ExportsEffect {
    data class OpenVideo(val uri: String) : ExportsEffect
    data class ShareVideo(val uri: String) : ExportsEffect
}

/** Tells whether an exported video can still be opened (it may have been deleted from the gallery). */
fun interface ExportFileChecker {
    suspend fun exists(uri: String): Boolean
}
