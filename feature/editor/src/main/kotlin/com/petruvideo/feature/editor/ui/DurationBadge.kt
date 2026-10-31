package com.petruvideo.feature.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruShapes
import com.petruvideo.core.designsystem.PetruType

/** The selected clip's length, updated live while a trim handle is dragged. */
@Composable
fun DurationBadge(lengthMs: Long, modifier: Modifier = Modifier) {
    Text(
        formatLength(lengthMs),
        style = PetruType.MonoSmall,
        color = PetruColors.Background,
        modifier = modifier
            .clip(PetruShapes.Chip)
            .background(PetruColors.OnBackground)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** `m:ss.t`: tenths matter because clips can be trimmed down to 0.1 s. */
private fun formatLength(ms: Long): String {
    val clamped = ms.coerceAtLeast(0)
    val minutes = clamped / MS_PER_MINUTE
    val seconds = clamped / MS_PER_SECOND % SECONDS_PER_MINUTE
    val tenths = clamped / MS_PER_TENTH % TENTHS
    return "%d:%02d.%d".format(minutes, seconds, tenths)
}

private const val MS_PER_MINUTE = 60_000L
private const val MS_PER_SECOND = 1_000L
private const val MS_PER_TENTH = 100L
private const val SECONDS_PER_MINUTE = 60L
private const val TENTHS = 10L
