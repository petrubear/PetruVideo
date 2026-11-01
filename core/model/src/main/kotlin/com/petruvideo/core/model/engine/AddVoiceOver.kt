package com.petruvideo.core.model.engine

import com.petruvideo.core.model.AudioClip
import com.petruvideo.core.model.AudioOrigin
import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VOICE_OVER_MIN_MS
import java.util.UUID

/**
 * Puts a recorded voice-over on the audio lane at exactly [startMs] (unlike an added file it is never moved to a free
 * spot, because it has to line up with the picture). Rejects a take under 0.5 s and one that would overlap a clip.
 */
class AddVoiceOver(
    private val media: MediaItem,
    private val startMs: Long,
    private val newClipId: () -> String = { UUID.randomUUID().toString() },
) : Command {
    override fun apply(timeline: Timeline): CommandResult {
        val length = media.durationMs
        if (media.kind != MediaKind.Audio || length == null || startMs < 0) {
            return CommandResult.Rejected(RejectReason.OutOfRange)
        }
        if (media.availability == Availability.Missing) return CommandResult.Rejected(RejectReason.MissingMedia)
        if (length < VOICE_OVER_MIN_MS) return CommandResult.Rejected(RejectReason.BelowMinimumLength)
        if (AudioPlacement.nextFreeStart(timeline.audioLane, startMs, length) != startMs) {
            return CommandResult.Rejected(RejectReason.OutOfRange)
        }
        val clip = AudioClip(newClipId(), media.id, 0, length, startMs, false, AudioOrigin.Added)
        val lane = (timeline.audioLane + clip).sortedBy { it.timelineStartMs }
        return CommandResult.Applied(
            timeline.copy(media = timeline.media + (media.id to media), audioLane = lane),
            "Add voice-over",
        )
    }
}
