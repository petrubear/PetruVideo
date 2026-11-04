package com.petruvideo.core.model.engine

import com.petruvideo.core.model.AudioClip
import com.petruvideo.core.model.AudioOrigin
import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.MIN_CLIP_MS
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.Timeline
import java.util.UUID

private fun newId() = UUID.randomUUID().toString()

/**
 * Turns a video clip's attached sound into a [AudioOrigin.Detached] audio clip with the same source range at the
 * clip's position, and makes the video silent. If another audio clip already occupies that spot the new clip snaps
 * to the next free position and the result carries a [Notice.Snapped].
 */
class DetachAudio(private val videoClipId: String, private val newClipId: () -> String = ::newId) : Command {
    override fun apply(timeline: Timeline): CommandResult {
        val index = timeline.videoLane.indexOfFirst { it.id == videoClipId }
        if (index < 0) return detachOverlay(timeline)
        val clip = timeline.videoLane[index]
        if (!clip.attachedAudio) return CommandResult.Rejected(RejectReason.NoAttachedAudio)
        val desired = timeline.videoStartOf(clip.id) ?: return CommandResult.Rejected(RejectReason.NotFound)
        val start = AudioPlacement.nextFreeStart(timeline.audioLane, desired, clip.lengthMs)
        val audio = AudioClip(
            id = newClipId(),
            mediaItemId = clip.mediaItemId,
            sourceStartMs = clip.sourceStartMs,
            sourceEndMs = clip.sourceEndMs,
            timelineStartMs = start,
            origin = AudioOrigin.Detached,
            speedMilli = clip.speedMilli,
            volumePercent = clip.volumePercent,
            muted = clip.muted,
            reverb = clip.reverb,
            fx = clip.fx,
        )
        val video = timeline.videoLane.toMutableList().also {
            it[index] = clip.copy(attachedAudio = false, muted = false)
        }
        return CommandResult.Applied(
            timeline = timeline.copy(
                videoLane = video,
                audioLane = (timeline.audioLane + audio).sortedBy { it.timelineStartMs },
            ),
            label = "Detach audio",
            notices = if (start != desired) listOf(Notice.Snapped) else emptyList(),
        )
    }

    /** From an overlay: its sound must stay in sync with the picture, so there is no snapping, only a refusal. */
    private fun detachOverlay(timeline: Timeline): CommandResult {
        val index = timeline.overlayLane.indexOfFirst { it.id == videoClipId }
        if (index < 0) return CommandResult.Rejected(RejectReason.NotFound)
        val clip = timeline.overlayLane[index]
        if (!clip.attachedAudio) return CommandResult.Rejected(RejectReason.NoAttachedAudio)
        if (!AudioPlacement.fits(timeline.audioLane, clip.startMs, clip.durationMs)) {
            return CommandResult.Rejected(RejectReason.LimitReached)
        }
        val audio = AudioClip(
            id = newClipId(),
            mediaItemId = clip.mediaItemId,
            sourceStartMs = clip.sourceStartMs,
            sourceEndMs = clip.sourceEndMs,
            timelineStartMs = clip.startMs,
            origin = AudioOrigin.Detached,
            volumePercent = clip.volumePercent,
            muted = clip.muted,
            reverb = clip.reverb,
            fx = clip.fx,
        )
        val overlays = timeline.overlayLane.toMutableList().also {
            it[index] = clip.copy(attachedAudio = false, muted = false)
        }
        return CommandResult.Applied(
            timeline = timeline.copy(
                overlayLane = overlays,
                audioLane = (timeline.audioLane + audio).sortedBy { it.timelineStartMs },
            ),
            label = "Detach audio",
        )
    }
}

/** Repositions an audio clip. An overlap with another audio clip snaps to the next free position. */
class MoveAudioClip(private val clipId: String, private val newStartMs: Long) : Command {
    override fun apply(timeline: Timeline): CommandResult {
        val clip = timeline.audioLane.firstOrNull { it.id == clipId }
            ?: return CommandResult.Rejected(RejectReason.NotFound)
        if (newStartMs < 0 || newStartMs == clip.timelineStartMs) return CommandResult.Rejected(RejectReason.OutOfRange)
        val start = AudioPlacement.nextFreeStart(timeline.audioLane, newStartMs, clip.lengthMs, ignoreClipId = clipId)
        if (start == clip.timelineStartMs) return CommandResult.Rejected(RejectReason.OutOfRange)
        val lane = timeline.audioLane.map { if (it.id == clipId) it.copy(timelineStartMs = start) else it }
            .sortedBy { it.timelineStartMs }
        return CommandResult.Applied(
            timeline = timeline.copy(audioLane = lane),
            label = "Move audio",
            notices = if (start != newStartMs) listOf(Notice.Snapped) else emptyList(),
        )
    }
}

class SetMuted(private val clipId: String, private val muted: Boolean) : Command {
    override fun apply(timeline: Timeline): CommandResult {
        val overlay = timeline.overlayLane.firstOrNull { it.id == clipId }
        if (overlay != null) {
            if (!overlay.attachedAudio) return CommandResult.Rejected(RejectReason.NoAttachedAudio)
            if (overlay.muted == muted) return CommandResult.Rejected(RejectReason.OutOfRange)
            val lane = timeline.overlayLane.map { if (it.id == clipId) it.copy(muted = muted) else it }
            return CommandResult.Applied(timeline.copy(overlayLane = lane), if (muted) "Mute audio" else "Unmute audio")
        }
        val clip = timeline.audioLane.firstOrNull { it.id == clipId }
            ?: return CommandResult.Rejected(RejectReason.NotFound)
        if (clip.muted == muted) return CommandResult.Rejected(RejectReason.OutOfRange)
        val lane = timeline.audioLane.map { if (it.id == clipId) it.copy(muted = muted) else it }
        return CommandResult.Applied(timeline.copy(audioLane = lane), if (muted) "Mute audio" else "Unmute audio")
    }
}

/**
 * Swaps the sound of an audio clip for [replacement]. The clip keeps its position and length; when the new file is
 * shorter the clip shortens to it and the result carries a [Notice.LengthChanged].
 */
class ReplaceAudio(private val clipId: String, private val replacement: MediaItem) : Command {
    override fun apply(timeline: Timeline): CommandResult {
        val clip = timeline.audioLane.firstOrNull { it.id == clipId }
            ?: return CommandResult.Rejected(RejectReason.NotFound)
        if (replacement.availability == Availability.Missing) return CommandResult.Rejected(RejectReason.MissingMedia)
        val duration = replacement.durationMs
        if (replacement.kind != MediaKind.Audio || duration == null || duration < MIN_CLIP_MS) {
            return CommandResult.Rejected(RejectReason.OutOfRange)
        }
        val length = minOf(clip.sourceLengthMs, duration)
        val replaced = clip.copy(mediaItemId = replacement.id, sourceStartMs = 0, sourceEndMs = length)
        val lane = timeline.audioLane.map { if (it.id == clipId) replaced else it }
        return CommandResult.Applied(
            timeline = timeline.copy(media = timeline.media + (replacement.id to replacement), audioLane = lane),
            label = "Replace audio",
            notices = if (length < clip.sourceLengthMs) listOf(Notice.LengthChanged) else emptyList(),
        )
    }
}
