package com.petruvideo.feature.export.ui

import com.petruvideo.core.export.Blocker
import com.petruvideo.core.model.export.ExportFailure
import java.util.Locale
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.petruvideo.feature.export.R

internal fun formatBitrate(bitsPerSecond: Int): String =
    if (bitsPerSecond >= MBIT) String.format(Locale.US, "%.1f Mbps", bitsPerSecond / MBIT.toDouble())
    else "${bitsPerSecond / KBIT} kbps"

internal fun formatBytes(bytes: Long): String = when {
    bytes >= GB -> String.format(Locale.US, "%.1f GB", bytes / GB.toDouble())
    bytes >= MB -> String.format(Locale.US, "%d MB", bytes / MB)
    else -> String.format(Locale.US, "%d KB", bytes / KB)
}

@Composable
internal fun Blocker.message(): String = when (this) {
    Blocker.NoClips -> stringResource(R.string.export_add_a_clip_to_export)
    is Blocker.MissingMedia -> stringResource(R.string.export_blocker_missing_media, name)
    is Blocker.NotEnoughStorage ->
        stringResource(R.string.export_blocker_not_enough_storage, formatBytes(neededBytes))
    Blocker.Busy -> stringResource(R.string.export_another_export_is_running)
}

@Composable
internal fun ExportFailure.message(): String = when (this) {
    is ExportFailure.MissingMedia -> stringResource(R.string.export_failure_missing_media, name)
    is ExportFailure.NotEnoughStorage ->
        stringResource(R.string.export_failure_not_enough_storage, formatBytes(neededBytes))
    is ExportFailure.EncoderError -> stringResource(R.string.export_failure_encoder_error, detail)
    ExportFailure.Interrupted -> stringResource(R.string.export_the_export_did_not_finish)
    ExportFailure.Pressure -> stringResource(R.string.export_the_phone_is_too_hot)
    ExportFailure.SaveFailed -> stringResource(R.string.export_the_video_could_not_be)
}

private const val KBIT = 1_000
private const val MBIT = 1_000_000
private const val KB = 1_000L
private const val MB = KB * 1_000
private const val GB = MB * 1_000
