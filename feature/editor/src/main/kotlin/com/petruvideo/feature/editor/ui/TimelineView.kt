package com.petruvideo.feature.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import com.petruvideo.feature.editor.ui.expanded.mediaDropTarget
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.designsystem.components.IconAction
import com.petruvideo.core.designsystem.icons.PetruIcons
import com.petruvideo.core.model.Timeline
import com.petruvideo.feature.editor.ClipSelection
import com.petruvideo.feature.editor.TimelineGeometry

/**
 * The timeline: ruler, video lane, the attached-sound waveforms, the audio clips lane, a fixed center playhead,
 * and the add clip button. The content scrolls under the playhead; scrolling by hand seeks, and playback moves
 * the content, so the preview always shows the frame under the playhead.
 */
@Composable
fun TimelineView(
    timeline: Timeline,
    positionMs: Long,
    selection: ClipSelection?,
    onSeek: (Long) -> Unit,
    onSelect: (ClipSelection?) -> Unit,
    onMoveClip: (clipId: String, newIndex: Int) -> Unit,
    onTrim: (clipId: String, start: Boolean, newSourceMs: Long) -> Unit,
    onAddClip: () -> Unit,
    onDropMedia: (mediaItemId: String, atMs: Long) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    geometry: TimelineGeometry = remember { TimelineGeometry() },
) {
    val density = LocalDensity.current.density
    val scroll = rememberScrollState()
    val total = timeline.totalDurationMs

    LaunchedEffect(positionMs, density) {
        if (!scroll.isScrollInProgress) scroll.scrollTo((geometry.scrollForPlayhead(positionMs) * density).toInt())
    }
    LaunchedEffect(scroll, total, density) {
        snapshotFlow { scroll.value }.collect {
            if (scroll.isScrollInProgress) onSeek(geometry.playheadForScroll(it / density, total))
        }
    }

    var viewportLeftPx by remember { mutableFloatStateOf(0f) }
    var viewportWidthPx by remember { mutableFloatStateOf(0f) }
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(PetruColors.SurfaceLow)
            .onGloballyPositioned {
                viewportLeftPx = it.positionInWindow().x
                viewportWidthPx = it.size.width.toFloat()
            }
            .mediaDropTarget { id, windowX ->
                val contentPx = scroll.value + (windowX - viewportLeftPx)
                val halfPx = viewportWidthPx / 2f
                onDropMedia(id, geometry.pxToTime((contentPx - halfPx) / density).coerceIn(0L, total))
            },
    ) {
        val half = maxWidth / 2
        val contentWidth = geometry.contentWidthPx(total, maxWidth.value).dp
        Box(Modifier.horizontalScroll(scroll)) {
            Column(
                Modifier
                    .width(contentWidth)
                    .fillMaxSize()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        onSelect(null)
                    },
            ) {
                Box(Modifier.padding(start = half)) {
                    TimelineRuler(geometry, total, geometry.currentSecond(positionMs))
                }
                Spacer(Modifier.height(8.dp))
                Column(Modifier.padding(start = half), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    VideoLaneView(
                        lane = timeline.videoLane,
                        media = timeline.media,
                        geometry = geometry,
                        selectedClipId = (selection as? ClipSelection.Video)?.clipId,
                        onSelect = { onSelect(ClipSelection.Video(it)) },
                        onMoveClip = onMoveClip,
                        onTrim = onTrim,
                    )
                    AttachedAudioRow(timeline.videoLane, timeline.media, geometry)
                    AudioClipsRow(
                        clips = timeline.audioLane,
                        media = timeline.media,
                        geometry = geometry,
                        selectedClipId = (selection as? ClipSelection.Audio)?.clipId,
                        onSelect = { onSelect(ClipSelection.Audio(it)) },
                        onTrim = onTrim,
                    )
                }
            }
        }
        Playhead(Modifier.align(Alignment.TopCenter).padding(top = 8.dp))
        if (timeline.videoLane.isEmpty() && timeline.audioLane.isEmpty()) {
            Text(
                "Add your first clip",
                style = PetruType.Body,
                color = PetruColors.OnSurfaceMuted,
                modifier = Modifier.align(Alignment.Center).padding(top = 24.dp),
            )
        }
        IconAction(
            icon = PetruIcons.Add,
            contentDescription = "Add clip",
            onClick = onAddClip,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 30.dp, end = 6.dp)
                .background(PetruColors.OnBackground, CircleShape),
            tint = PetruColors.Background,
        )
    }
}
