package com.petruvideo.core.model.engine

import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.MIN_CLIP_MS
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.Timeline

/**
 * Moves a video clip to [newIndex] (its position in the lane after the move). Attached sound is part of the clip
 * so it moves with it; detached audio stays where it is. Moving to the current index is rejected (no change).
 */
class MoveVideoClip(private val clipId: String, private val newIndex: Int) : Command {
    override fun apply(timeline: Timeline): CommandResult {
        val from = timeline.videoLane.indexOfFirst { it.id == clipId }
        return when {
            from < 0 -> CommandResult.Rejected(RejectReason.NotFound)
            newIndex !in timeline.videoLane.indices || newIndex == from ->
                CommandResult.Rejected(RejectReason.OutOfRange)
            else -> {
                val lane = timeline.videoLane.toMutableList()
                lane.add(newIndex, lane.removeAt(from))
                CommandResult.Applied(timeline.copy(videoLane = lane), "Move clip")
            }
        }
    }
}

/**
 * Rebinds every clip that uses [mediaItemId] to [replacement] (a file chosen by the user after the original went
 * missing). The media id stays the same so clips keep pointing at it; trims are kept and clamped to the new
 * length, with a [Notice.LengthChanged] when a clip had to shrink. Attached sound survives only if the new file
 * has sound.
 */
class RelinkMedia(private val mediaItemId: String, private val replacement: MediaItem) : Command {
    override fun apply(timeline: Timeline): CommandResult {
        val old = timeline.media[mediaItemId] ?: return CommandResult.Rejected(RejectReason.NotFound)
        if (replacement.kind != old.kind) return CommandResult.Rejected(RejectReason.OutOfRange)
        val duration = replacement.durationMs
        if (replacement.kind != MediaKind.Photo && (duration == null || duration < MIN_CLIP_MS)) {
            return CommandResult.Rejected(RejectReason.BelowMinimumLength)
        }
        var shrunk = false
        val limit = if (replacement.kind == MediaKind.Photo) null else duration

        fun clamp(start: Long, end: Long): Pair<Long, Long> {
            if (limit == null || end <= limit) return start to end
            shrunk = true
            val newStart = minOf(start, limit - MIN_CLIP_MS)
            return newStart to maxOf(minOf(end, limit), newStart + MIN_CLIP_MS)
        }

        val video = timeline.videoLane.map { clip ->
            if (clip.mediaItemId != mediaItemId) return@map clip
            val (start, end) = clamp(clip.sourceStartMs, clip.sourceEndMs)
            val attached = clip.attachedAudio && replacement.hasAudio
            clip.copy(sourceStartMs = start, sourceEndMs = end, attachedAudio = attached)
        }
        val audio = timeline.audioLane.map { clip ->
            if (clip.mediaItemId != mediaItemId) return@map clip
            val (start, end) = clamp(clip.sourceStartMs, clip.sourceEndMs)
            clip.copy(sourceStartMs = start, sourceEndMs = end)
        }
        val overlay = timeline.overlayLane.map { clip ->
            if (clip.mediaItemId != mediaItemId) return@map clip
            val (start, end) = clamp(clip.sourceStartMs, clip.sourceEndMs)
            clip.copy(sourceStartMs = start, sourceEndMs = end)
        }
        val relinked = replacement.copy(id = mediaItemId, availability = Availability.Available)
        return CommandResult.Applied(
            timeline = timeline.copy(
                media = timeline.media + (mediaItemId to relinked),
                videoLane = video,
                audioLane = audio,
                overlayLane = overlay,
            ),
            label = "Relink media",
            notices = if (shrunk) listOf(Notice.LengthChanged) else emptyList(),
        )
    }
}


/** Removes a missing media item and every clip that uses it (the "Remove" choice for a missing clip). */
class RemoveMissingMedia(private val mediaItemId: String) : Command {
    override fun apply(timeline: Timeline): CommandResult {
        if (mediaItemId !in timeline.media) return CommandResult.Rejected(RejectReason.NotFound)
        return CommandResult.Applied(
            timeline = timeline.copy(
                media = timeline.media - mediaItemId,
                videoLane = timeline.videoLane.filterNot { it.mediaItemId == mediaItemId },
                audioLane = timeline.audioLane.filterNot { it.mediaItemId == mediaItemId },
                overlayLane = timeline.overlayLane.filterNot { it.mediaItemId == mediaItemId },
            ),
            label = "Remove missing media",
        )
    }
}
