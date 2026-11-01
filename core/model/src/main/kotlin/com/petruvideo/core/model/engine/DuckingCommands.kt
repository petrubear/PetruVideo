package com.petruvideo.core.model.engine

import com.petruvideo.core.model.Ducking
import com.petruvideo.core.model.Timeline

/** Sets the ducking of the audio-lane clip [clipId]; the amount is clamped to 0..100. */
class SetDucking(private val clipId: String, private val ducking: Ducking) : Command {
    override fun apply(timeline: Timeline): CommandResult {
        val index = timeline.audioLane.indexOfFirst { it.id == clipId }
        if (index < 0) return CommandResult.Rejected(RejectReason.NotFound)
        val clip = timeline.audioLane[index]
        val next = ducking.normalized()
        if (next == clip.ducking) return CommandResult.Rejected(RejectReason.OutOfRange)
        val lane = timeline.audioLane.toMutableList().also { it[index] = clip.copy(ducking = next) }
        return CommandResult.Applied(timeline.copy(audioLane = lane), "Ducking")
    }
}
