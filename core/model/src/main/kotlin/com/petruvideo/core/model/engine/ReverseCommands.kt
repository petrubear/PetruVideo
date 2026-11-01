package com.petruvideo.core.model.engine

import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.REVERSE_MAX_MS
import com.petruvideo.core.model.Timeline

/**
 * Turns reverse on or off for the main-track clip [clipId]. Photos cannot be reversed and a clip can be reversed only
 * when its trimmed part is at most [REVERSE_MAX_MS]; turning it off is always possible.
 */
class SetReversed(private val clipId: String, private val reversed: Boolean) : Command {
    override fun apply(timeline: Timeline): CommandResult {
        val index = timeline.videoLane.indexOfFirst { it.id == clipId }
        if (index < 0) return CommandResult.Rejected(RejectReason.NotFound)
        val clip = timeline.videoLane[index]
        if (clip.reversed == reversed) return CommandResult.Rejected(RejectReason.OutOfRange)
        if (reversed) {
            val media = timeline.media[clip.mediaItemId]
            if (media == null || media.kind != MediaKind.Video) return CommandResult.Rejected(RejectReason.OutOfRange)
            if (clip.sourceLengthMs > REVERSE_MAX_MS) return CommandResult.Rejected(RejectReason.OutOfRange)
        }
        val lane = timeline.videoLane.toMutableList().also { it[index] = clip.copy(reversed = reversed) }
        return CommandResult.Applied(timeline.copy(videoLane = lane), if (reversed) "Reverse" else "Play forwards")
    }
}
