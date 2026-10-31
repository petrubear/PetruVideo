package com.petruvideo.core.model.engine

import com.petruvideo.core.model.AudioClip
import com.petruvideo.core.model.AudioOrigin
import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.MIN_CLIP_MS
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.PHOTO_DEFAULT_MS
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VideoClip
import java.util.UUID

private fun newId() = UUID.randomUUID().toString()

/** A video or photo to add, with an optional trim (video only). */
data class AddVideoItem(val media: MediaItem, val trimStartMs: Long? = null, val trimEndMs: Long? = null)

/**
 * Inserts videos and photos on the video lane in selection order, after the clip under the playhead (at the
 * playhead when it sits on a boundary, at the start when the lane is empty or the playhead is at zero, at the
 * end when the playhead is past it). Sets the project format from the first item if it is still unset.
 */
class AddVideoClips(
    private val items: List<AddVideoItem>,
    private val playheadMs: Long,
    private val newClipId: () -> String = ::newId,
) : Command {
    override fun apply(timeline: Timeline): CommandResult {
        if (items.isEmpty()) return CommandResult.Rejected(RejectReason.OutOfRange)
        val clips = mutableListOf<VideoClip>()
        for (item in items) {
            val media = item.media
            if (media.availability == Availability.Missing) return CommandResult.Rejected(RejectReason.MissingMedia)
            if (media.kind == MediaKind.Audio) return CommandResult.Rejected(RejectReason.OutOfRange)
            clips += toClip(item) ?: return rejectedTrim(item)
        }
        val index = insertIndex(timeline, playheadMs)
        val lane = timeline.videoLane.toMutableList().apply { addAll(index, clips) }
        val format = timeline.format ?: deriveFormat(items.first().media)
        return CommandResult.Applied(
            timeline = timeline.copy(
                format = format,
                media = timeline.media + items.associate { it.media.id to it.media },
                videoLane = lane,
            ),
            label = "Add clips",
        )
    }

    private fun toClip(item: AddVideoItem): VideoClip? {
        val media = item.media
        if (media.kind == MediaKind.Photo) {
            return VideoClip(newClipId(), media.id, 0, PHOTO_DEFAULT_MS, attachedAudio = false)
        }
        val duration = media.durationMs ?: return null
        val start = item.trimStartMs ?: 0
        val end = item.trimEndMs ?: duration
        if (start < 0 || end > duration || end <= start) return null
        if (end - start < MIN_CLIP_MS) return null
        return VideoClip(newClipId(), media.id, start, end, attachedAudio = media.hasAudio)
    }

    private fun rejectedTrim(item: AddVideoItem): CommandResult {
        val duration = item.media.durationMs ?: 0
        val start = item.trimStartMs ?: 0
        val end = item.trimEndMs ?: duration
        val outOfRange = start < 0 || end > duration || end <= start
        return CommandResult.Rejected(if (outOfRange) RejectReason.OutOfRange else RejectReason.BelowMinimumLength)
    }

    private fun insertIndex(timeline: Timeline, playheadMs: Long): Int {
        var start = 0L
        timeline.videoLane.forEachIndexed { index, clip ->
            val end = start + clip.lengthMs
            if (playheadMs <= start) return index
            if (playheadMs < end) return index + 1
            start = end
        }
        return timeline.videoLane.size
    }
}

/**
 * Places audio files on the audio lane starting at the playhead, each following the previous one. A clip that
 * would overlap another audio clip snaps to the next free position and the result carries a [Notice.Snapped].
 * Overlapping a video's attached sound is allowed.
 */
class AddAudioClips(
    private val items: List<MediaItem>,
    private val playheadMs: Long,
    private val newClipId: () -> String = ::newId,
) : Command {
    override fun apply(timeline: Timeline): CommandResult {
        if (items.isEmpty()) return CommandResult.Rejected(RejectReason.OutOfRange)
        var lane = timeline.audioLane
        var snapped = false
        var cursor = playheadMs.coerceAtLeast(0)
        for (media in items) {
            if (media.availability == Availability.Missing) return CommandResult.Rejected(RejectReason.MissingMedia)
            val duration = media.durationMs
            if (media.kind != MediaKind.Audio || duration == null || duration < MIN_CLIP_MS) {
                return CommandResult.Rejected(RejectReason.OutOfRange)
            }
            val start = AudioPlacement.nextFreeStart(lane, cursor, duration)
            snapped = snapped || start != cursor
            lane = (lane + AudioClip(newClipId(), media.id, 0, duration, start, false, AudioOrigin.Added))
                .sortedBy { it.timelineStartMs }
            cursor = start + duration
        }
        return CommandResult.Applied(
            timeline = timeline.copy(media = timeline.media + items.associateBy { it.id }, audioLane = lane),
            label = "Add audio",
            notices = if (snapped) listOf(Notice.Snapped) else emptyList(),
        )
    }
}
