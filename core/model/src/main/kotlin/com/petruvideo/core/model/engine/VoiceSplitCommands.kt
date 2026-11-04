package com.petruvideo.core.model.engine

import com.petruvideo.core.model.AudioClip
import com.petruvideo.core.model.AudioOrigin
import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VideoClip
import java.util.UUID

/** The longest clip the voice split works on, so the work and the storage stay small (feature 027). */
const val VOICE_SPLIT_MAX_MS = 5 * 60 * 1_000L

private fun newId() = UUID.randomUUID().toString()

/** Why the voice of the video clip [clipId] cannot be split, or null when it can. */
object VoiceSplitRules {
    fun reasonNot(timeline: Timeline, clipId: String): String? {
        timeline.overlayLane.firstOrNull { it.id == clipId }?.let { overlay ->
            return when {
                !overlay.attachedAudio -> "No sound in this clip"
                overlay.durationMs > VOICE_SPLIT_MAX_MS -> "Works on clips up to 5 minutes"
                else -> null
            }
        }
        val clip = timeline.videoLane.firstOrNull { it.id == clipId } ?: return "Select a clip"
        return when {
            !clip.attachedAudio -> "No sound in this clip"
            clip.reversed -> "Not available for reversed clips"
            clip.speedCurve != null -> "Not available with a speed curve"
            clip.sourceLengthMs > VOICE_SPLIT_MAX_MS -> "Works on clips up to 5 minutes"
            else -> null
        }
    }
}

/**
 * Turns a video clip's attached sound into two audio clips at the clip's place, Voice ([voice]) and Backing
 * ([backing], everything else), and makes the video silent. Both play for the clip's length at its speed, with its
 * volume, reverb, and effects, so the sound starts out as before. Needs two free audio layers there.
 */
class SplitVoice(
    private val videoClipId: String,
    private val voice: MediaItem,
    private val backing: MediaItem,
    private val newClipId: () -> String = ::newId,
) : Command {
    override fun apply(timeline: Timeline): CommandResult {
        val index = timeline.videoLane.indexOfFirst { it.id == videoClipId }
        if (index < 0) return splitOverlay(timeline)
        val clip = timeline.videoLane.getOrNull(index) ?: return CommandResult.Rejected(RejectReason.NotFound)
        refusal(timeline, clip)?.let { return CommandResult.Rejected(it) }
        val start = timeline.videoStartOf(clip.id) ?: return CommandResult.Rejected(RejectReason.NotFound)
        fun stem(media: MediaItem) = AudioClip(
            id = newClipId(),
            mediaItemId = media.id,
            sourceStartMs = 0,
            sourceEndMs = minOf(clip.sourceLengthMs, media.durationMs ?: clip.sourceLengthMs),
            timelineStartMs = start,
            muted = clip.muted,
            origin = AudioOrigin.Detached,
            speedMilli = clip.speedMilli,
            volumePercent = clip.volumePercent,
            reverb = clip.reverb,
            fx = clip.fx,
        )
        val first = stem(voice)
        val second = stem(backing)
        var lane = timeline.audioLane
        for (added in listOf(first, second)) {
            if (!AudioPlacement.fits(lane, added.timelineStartMs, added.lengthMs)) {
                return CommandResult.Rejected(RejectReason.LimitReached)
            }
            lane = lane + added
        }
        val video = timeline.videoLane.toMutableList().also {
            it[index] = clip.copy(attachedAudio = false, muted = false)
        }
        return CommandResult.Applied(
            timeline = timeline.copy(
                videoLane = video,
                media = timeline.media + (voice.id to voice) + (backing.id to backing),
                audioLane = lane.sortedBy { it.timelineStartMs },
            ),
            label = "Split voice",
        )
    }

    /** From an overlay: the stems play for the overlay's length at its start, with its volume, reverb, and effects. */
    private fun splitOverlay(timeline: Timeline): CommandResult {
        val index = timeline.overlayLane.indexOfFirst { it.id == videoClipId }
        val clip = timeline.overlayLane.getOrNull(index) ?: return CommandResult.Rejected(RejectReason.NotFound)
        val refused = when {
            !clip.attachedAudio -> RejectReason.NoAttachedAudio
            timeline.media[clip.mediaItemId]?.availability == Availability.Missing -> RejectReason.MissingMedia
            VoiceSplitRules.reasonNot(timeline, clip.id) != null -> RejectReason.OutOfRange
            listOf(voice, backing).any { it.kind != MediaKind.Audio || it.availability == Availability.Missing } ->
                RejectReason.MissingMedia
            else -> null
        }
        if (refused != null) return CommandResult.Rejected(refused)
        fun stem(media: MediaItem) = AudioClip(
            id = newClipId(),
            mediaItemId = media.id,
            sourceStartMs = 0,
            sourceEndMs = minOf(clip.durationMs, media.durationMs ?: clip.durationMs),
            timelineStartMs = clip.startMs,
            muted = clip.muted,
            origin = AudioOrigin.Detached,
            volumePercent = clip.volumePercent,
            reverb = clip.reverb,
            fx = clip.fx,
        )
        var lane = timeline.audioLane
        for (added in listOf(stem(voice), stem(backing))) {
            if (!AudioPlacement.fits(lane, added.timelineStartMs, added.lengthMs)) {
                return CommandResult.Rejected(RejectReason.LimitReached)
            }
            lane = lane + added
        }
        val overlays = timeline.overlayLane.toMutableList().also {
            it[index] = clip.copy(attachedAudio = false, muted = false)
        }
        return CommandResult.Applied(
            timeline = timeline.copy(
                overlayLane = overlays,
                media = timeline.media + (voice.id to voice) + (backing.id to backing),
                audioLane = lane.sortedBy { it.timelineStartMs },
            ),
            label = "Split voice",
        )
    }

    private fun refusal(timeline: Timeline, clip: VideoClip): RejectReason? = when {
        !clip.attachedAudio -> RejectReason.NoAttachedAudio
        timeline.media[clip.mediaItemId]?.availability == Availability.Missing -> RejectReason.MissingMedia
        VoiceSplitRules.reasonNot(timeline, clip.id) != null -> RejectReason.OutOfRange
        listOf(voice, backing).any { it.kind != MediaKind.Audio || it.availability == Availability.Missing } ->
            RejectReason.MissingMedia
        else -> null
    }
}
