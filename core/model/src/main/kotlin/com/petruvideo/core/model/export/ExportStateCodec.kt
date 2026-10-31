package com.petruvideo.core.model.export

/** Flat form of an [ExportState] for storage: a state name, a progress, a reason key, and the result fields. */
data class StoredState(
    val state: String,
    val progress: Int = 0,
    val reason: String? = null,
    val resultUri: String? = null,
    val bytes: Long = 0,
    val durationMs: Long = 0,
)

object ExportStateCodec {
    fun encode(state: ExportState): StoredState = when (state) {
        ExportState.Waiting -> StoredState("Waiting")
        is ExportState.Rendering -> StoredState("Rendering", progress = state.progress)
        is ExportState.Finished ->
            StoredState("Finished", resultUri = state.uri, bytes = state.bytes, durationMs = state.durationMs)
        ExportState.Cancelled -> StoredState("Cancelled")
        is ExportState.Failed -> StoredState("Failed", reason = encodeReason(state.reason))
    }

    fun decode(stored: StoredState): ExportState = when (stored.state) {
        "Waiting" -> ExportState.Waiting
        "Rendering" -> ExportState.Rendering(stored.progress)
        "Finished" -> ExportState.Finished(stored.resultUri.orEmpty(), stored.bytes, stored.durationMs)
        "Cancelled" -> ExportState.Cancelled
        else -> ExportState.Failed(decodeReason(stored.reason))
    }

    private fun encodeReason(reason: ExportFailure): String = when (reason) {
        is ExportFailure.MissingMedia -> "MissingMedia:${reason.name}"
        is ExportFailure.NotEnoughStorage -> "NotEnoughStorage:${reason.neededBytes}"
        is ExportFailure.EncoderError -> "EncoderError:${reason.detail}"
        ExportFailure.Interrupted -> "Interrupted"
        ExportFailure.Pressure -> "Pressure"
        ExportFailure.SaveFailed -> "SaveFailed"
    }

    private fun decodeReason(text: String?): ExportFailure {
        val key = text?.substringBefore(':')
        val arg = text?.substringAfter(':', "").orEmpty()
        return when (key) {
            "MissingMedia" -> ExportFailure.MissingMedia(arg)
            "NotEnoughStorage" -> ExportFailure.NotEnoughStorage(arg.toLongOrNull() ?: 0)
            "EncoderError" -> ExportFailure.EncoderError(arg)
            "Pressure" -> ExportFailure.Pressure
            "SaveFailed" -> ExportFailure.SaveFailed
            else -> ExportFailure.Interrupted
        }
    }
}
