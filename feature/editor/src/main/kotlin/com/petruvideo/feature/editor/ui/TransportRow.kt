package com.petruvideo.feature.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.designsystem.components.IconAction
import com.petruvideo.core.designsystem.icons.PetruIcons
import com.petruvideo.feature.editor.PlaybackState

/** Time readout, undo, play or pause, redo, and the full-screen button. */
@Composable
fun TransportRow(
    playback: PlaybackState,
    canUndo: Boolean,
    canRedo: Boolean,
    enabled: Boolean,
    onUndo: () -> Unit,
    onPlayPause: () -> Unit,
    onRedo: () -> Unit,
    onFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = PetruColors.OnBackground)) { append(clock(playback.positionMs)) }
                withStyle(SpanStyle(color = PetruColors.OnSurfaceMuted)) { append(" / ${clock(playback.durationMs)}") }
            },
            style = PetruType.Mono,
            modifier = Modifier.weight(1f).padding(start = 8.dp, end = 4.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconAction(PetruIcons.Undo, "Undo", onUndo, dimmed = !canUndo)
            IconAction(
                icon = if (playback.isPlaying) PetruIcons.Pause else PetruIcons.Play,
                contentDescription = if (playback.isPlaying) "Pause" else "Play",
                onClick = onPlayPause,
                modifier = Modifier.background(PetruColors.OnBackground, CircleShape),
                tint = PetruColors.Background,
                dimmed = !enabled,
            )
            IconAction(PetruIcons.Redo, "Redo", onRedo, dimmed = !canRedo)
        }
        IconAction(PetruIcons.Fullscreen, "Fullscreen preview", onFullscreen)
    }
}

/** `mm:ss.t`, as in the design (00:06.2 / 00:21.0). */
internal fun clock(ms: Long): String {
    val clamped = ms.coerceAtLeast(0)
    val tenths = clamped / 100 % 10
    val seconds = clamped / 1_000 % 60
    val minutes = clamped / 60_000
    return "%02d:%02d.%d".format(minutes, seconds, tenths)
}
