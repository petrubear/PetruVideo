package com.petruvideo.feature.editor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.designsystem.TrackColors
import com.petruvideo.core.model.AudioClip
import com.petruvideo.core.model.AudioOrigin
import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.VideoClip
import com.petruvideo.core.media.waveform.WaveformPeaksGenerator
import com.petruvideo.feature.editor.TimelineGeometry
import com.petruvideo.feature.editor.TrimLimits

private val WAVE_ROW_HEIGHT = 32.dp
private val WAVE_SHAPE = RoundedCornerShape(7.dp)
private const val MUTED_ALPHA = 0.6f
private val LABEL_INSET = 20.dp

/**
 * The sound attached to each video clip, as a waveform right under the clip it belongs to. A video with no
 * sound, a photo, or a missing file leaves its stretch empty.
 */
@Composable
fun AttachedAudioRow(
    lane: List<VideoClip>,
    media: Map<String, MediaItem>,
    geometry: TimelineGeometry,
    modifier: Modifier = Modifier,
) {
    Row(modifier.height(WAVE_ROW_HEIGHT)) {
        lane.forEach { clip ->
            val item = media[clip.mediaItemId]
            val width = geometry.timeToPx(clip.lengthMs).dp
            if (clip.attachedAudio && item != null && item.availability == Availability.Available) {
                val range = clip.sourceStartMs..clip.sourceEndMs
                Waveform(item, range, TrackColors.Audio, TrackColors.AudioFill, Modifier.width(width))
            } else {
                Box(Modifier.width(width).fillMaxHeight())
            }
        }
    }
}

/** Detached and added audio clips at their own positions; muted clips are dimmed. */
@Composable
fun AudioClipsRow(
    clips: List<AudioClip>,
    media: Map<String, MediaItem>,
    geometry: TimelineGeometry,
    selectedClipId: String?,
    onSelect: (String) -> Unit,
    onTrim: (clipId: String, start: Boolean, newSourceMs: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var previewLength by remember { mutableStateOf<Pair<String, Long>?>(null) }
    Box(modifier.height(WAVE_ROW_HEIGHT)) {
        clips.forEach { clip ->
            val item = media[clip.mediaItemId] ?: return@forEach
            val rect = geometry.audioRect(clip)
            val detached = clip.origin == AudioOrigin.Detached
            val border = if (detached) TrackColors.Audio else TrackColors.Music
            val fill = if (detached) TrackColors.AudioFill else TrackColors.MusicFill
            Box(
                Modifier
                    .offset(x = rect.startPx.dp)
                    .width(rect.widthPx.dp)
                    .fillMaxHeight()
                    .alpha(if (clip.muted) MUTED_ALPHA else 1f)
                    .then(selectionOutline(clip.id == selectedClipId))
                    .clickable { onSelect(clip.id) }
                    .semantics {
                        contentDescription = "${if (detached) "Detached audio" else "Audio"}, ${item.displayName}"
                    },
            ) {
                Waveform(item, clip.sourceStartMs..clip.sourceEndMs, border, fill, Modifier.fillMaxSize())
                if (detached) {
                    Text(
                        "Detached",
                        style = PetruType.MonoSmall,
                        color = PetruColors.OnBackground,
                        modifier = Modifier.align(Alignment.CenterStart).offset(x = LABEL_INSET),
                    )
                }
                if (clip.muted && clip.id != selectedClipId) MutedBadge(Modifier.align(Alignment.CenterEnd))
                if (clip.id == selectedClipId) {
                    TrimHandles(
                        geometry = geometry,
                        currentLengthMs = clip.lengthMs,
                        target = { start, delta -> TrimLimits.audio(clip, item, start, delta) },
                        onPreview = { _, target -> previewLength = clip.id to target.lengthMs },
                        onCommit = { start, target ->
                            previewLength = null
                            if (target.lengthMs != clip.lengthMs) onTrim(clip.id, start, target.newSourceMs)
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                    DurationBadge(
                        previewLength?.takeIf { it.first == clip.id }?.second ?: clip.lengthMs,
                        Modifier.align(Alignment.CenterEnd).offset(x = -LABEL_INSET),
                    )
                }
            }
        }
    }
}

@Composable
private fun Waveform(
    item: MediaItem,
    sourceRange: LongRange,
    color: Color,
    fill: Color,
    modifier: Modifier = Modifier,
) {
    val loaders = LocalMediaLoaders.current
    val peaks by produceState<FloatArray?>(null, item.id) { value = loaders.peaks(item) }
    Box(modifier.fillMaxHeight().clip(WAVE_SHAPE).background(fill).border(1.5.dp, color, WAVE_SHAPE)) {
        val data = peaks ?: return@Box
        Canvas(Modifier.fillMaxSize()) {
            val perSecond = WaveformPeaksGenerator.PEAKS_PER_SECOND
            val first = (sourceRange.first * perSecond / MS_PER_SECOND).toInt().coerceIn(0, data.size)
            val last = (sourceRange.last * perSecond / MS_PER_SECOND).toInt().coerceIn(first, data.size)
            val slice = last - first
            if (slice == 0) return@Canvas
            val step = 3.dp.toPx()
            val bars = (size.width / step).toInt().coerceAtLeast(1)
            for (i in 0 until bars) {
                val from = first + i * slice / bars
                val to = (first + (i + 1) * slice / bars).coerceAtLeast(from + 1).coerceAtMost(last)
                var peak = 0f
                for (j in from until to) peak = maxOf(peak, data[j])
                val half = (size.height * 0.42f * peak).coerceAtLeast(1f)
                val x = i * step + step / 2
                val mid = size.height / 2
                drawLine(color, Offset(x, mid - half), Offset(x, mid + half), 2.dp.toPx(), StrokeCap.Round)
            }
        }
    }
}

private fun selectionOutline(selected: Boolean): Modifier =
    if (selected) Modifier.border(2.5.dp, PetruColors.OnBackground, WAVE_SHAPE) else Modifier

private const val MS_PER_SECOND = 1_000L
