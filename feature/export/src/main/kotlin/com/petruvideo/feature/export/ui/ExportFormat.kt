package com.petruvideo.feature.export.ui

import com.petruvideo.core.export.Blocker
import com.petruvideo.core.model.export.ExportFailure
import java.util.Locale

internal fun formatBitrate(bitsPerSecond: Int): String =
    if (bitsPerSecond >= MBIT) String.format(Locale.US, "%.1f Mbps", bitsPerSecond / MBIT.toDouble())
    else "${bitsPerSecond / KBIT} kbps"

internal fun formatBytes(bytes: Long): String = when {
    bytes >= GB -> String.format(Locale.US, "%.1f GB", bytes / GB.toDouble())
    bytes >= MB -> String.format(Locale.US, "%d MB", bytes / MB)
    else -> String.format(Locale.US, "%d KB", bytes / KB)
}

internal fun Blocker.message(): String = when (this) {
    Blocker.NoClips -> "Add a clip to export."
    is Blocker.MissingMedia -> "$name is missing. Relink or remove it in the editor first."
    is Blocker.NotEnoughStorage ->
        "Not enough free space (about ${formatBytes(neededBytes)} needed). Free some space or choose Smaller."
    Blocker.Busy -> "Another export is running."
}

internal fun ExportFailure.message(): String = when (this) {
    is ExportFailure.MissingMedia -> "$name is missing."
    is ExportFailure.NotEnoughStorage -> "Not enough free space (about ${formatBytes(neededBytes)} needed)."
    is ExportFailure.EncoderError -> "The phone could not encode this video ($detail). Try 1080p or Smaller."
    ExportFailure.Interrupted -> "The export did not finish because the app was closed."
    ExportFailure.Pressure -> "The phone is too hot or low on memory. Try again in a moment."
    ExportFailure.SaveFailed -> "The video could not be saved to the gallery."
}

private const val KBIT = 1_000
private const val MBIT = 1_000_000
private const val KB = 1_000L
private const val MB = KB * 1_000
private const val GB = MB * 1_000
