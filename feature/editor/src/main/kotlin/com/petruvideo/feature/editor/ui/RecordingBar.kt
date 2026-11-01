package com.petruvideo.feature.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.feature.editor.EditorEvent
import com.petruvideo.feature.editor.VoiceOverUi

private val STOP_SIZE = 56.dp
private val METER_HEIGHT = 10.dp
private const val MS_PER_SECOND = 1000L
private const val SECONDS_PER_MINUTE = 60

/** "0:07" for a take of 7 seconds. */
internal fun timerText(elapsedMs: Long): String {
    val seconds = elapsedMs / MS_PER_SECOND
    return "%d:%02d".format(seconds / SECONDS_PER_MINUTE, seconds % SECONDS_PER_MINUTE)
}

/** What replaces the bottom bars during a voice-over: the count-in with Cancel, or the meter, timer, and Stop. */
@Composable
fun RecordingBar(take: VoiceOverUi, onEvent: (EditorEvent) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .background(PetruColors.Background)
            .border(1.dp, PetruColors.SurfaceHigh)
            .navigationBarsPadding()
            .height(88.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (take) {
            is VoiceOverUi.CountIn -> CountIn(take, onEvent)
            is VoiceOverUi.Recording -> Running(take, onEvent)
        }
    }
}

@Composable
private fun RowScope.CountIn(take: VoiceOverUi.CountIn, onEvent: (EditorEvent) -> Unit) {
    val spoken = "Recording starts in ${take.secondsLeft}"
    Text(
        "${take.secondsLeft}",
        style = PetruType.Display,
        color = PetruColors.Accent,
        modifier = Modifier.width(48.dp).semantics {
            liveRegion = LiveRegionMode.Polite
            contentDescription = spoken
        },
    )
    Column(Modifier.weight(1f)) {
        Text("Get ready", style = PetruType.Label, color = PetruColors.OnBackground)
        Text("Use headphones for a clean voice", style = PetruType.Caption, color = PetruColors.OnSurfaceMuted)
    }
    TextAction("Cancel") { onEvent(EditorEvent.CancelVoiceOver) }
}

@Composable
private fun RowScope.Running(
    take: VoiceOverUi.Recording,
    onEvent: (EditorEvent) -> Unit,
) {
    val time = timerText(take.elapsedMs)
    Column(
        Modifier.weight(1f).semantics {
            liveRegion = LiveRegionMode.Polite
            contentDescription = "Recording, $time"
        },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(time, style = PetruType.Mono, color = PetruColors.OnBackground)
        LevelMeter(take.level)
    }
    Box(
        Modifier
            .size(STOP_SIZE)
            .clip(CircleShape)
            .background(PetruColors.Danger)
            .clickable(role = Role.Button) { onEvent(EditorEvent.StopVoiceOver) }
            .semantics { contentDescription = "Stop recording" },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(20.dp).background(PetruColors.OnBackground, RoundedCornerShape(4.dp)))
    }
}

@Composable
private fun LevelMeter(level: Float) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(METER_HEIGHT)
            .clip(RoundedCornerShape(5.dp))
            .background(PetruColors.SurfaceHigh)
            .semantics { contentDescription = "Voice level" },
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(level.coerceIn(0f, 1f))
                .background(PetruColors.Accent),
        )
    }
}
