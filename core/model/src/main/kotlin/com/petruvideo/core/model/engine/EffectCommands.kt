package com.petruvideo.core.model.engine

import com.petruvideo.core.model.ClipEffect
import com.petruvideo.core.model.EffectKind
import com.petruvideo.core.model.TRANSITION_MAX_MS
import com.petruvideo.core.model.TRANSITION_MIN_MS
import com.petruvideo.core.model.TRANSITION_STEP_MS
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.Transition
import com.petruvideo.core.model.TransitionKind
import com.petruvideo.core.model.transitionLimitMs

/**
 * Sets or replaces the transition at the cut after [clipId]. The length is rounded to the step and limited to half of
 * the shorter neighbour (with [Notice.TransitionLimited]); neighbours too short for the minimum reject the command.
 */
class SetTransition(
    private val clipId: String,
    private val kind: TransitionKind,
    private val lengthMs: Long,
) : Command {
    override fun apply(timeline: Timeline): CommandResult {
        val index = timeline.videoLane.indexOfFirst { it.id == clipId }
        if (index < 0) return CommandResult.Rejected(RejectReason.NotFound)
        val next = timeline.videoLane.getOrNull(index + 1) ?: return CommandResult.Rejected(RejectReason.NotFound)
        val clip = timeline.videoLane[index]
        val limit = transitionLimitMs(clip.lengthMs, next.lengthMs)
        if (limit < TRANSITION_MIN_MS) return CommandResult.Rejected(RejectReason.BelowMinimumLength)
        val wanted = (lengthMs / TRANSITION_STEP_MS * TRANSITION_STEP_MS).coerceIn(TRANSITION_MIN_MS, TRANSITION_MAX_MS)
        val length = minOf(wanted, limit)
        val transition = Transition(kind, length)
        if (transition == clip.transitionOut) return CommandResult.Rejected(RejectReason.OutOfRange)
        val lane = timeline.videoLane.toMutableList().also { it[index] = clip.copy(transitionOut = transition) }
        val notices = if (length < wanted) listOf(Notice.TransitionLimited) else emptyList()
        return CommandResult.Applied(timeline.copy(videoLane = lane), "Transition", notices)
    }
}

class RemoveTransition(private val clipId: String) : Command {
    override fun apply(timeline: Timeline): CommandResult {
        val index = timeline.videoLane.indexOfFirst { it.id == clipId }
        if (index < 0) return CommandResult.Rejected(RejectReason.NotFound)
        val clip = timeline.videoLane[index]
        if (clip.transitionOut == null) return CommandResult.Rejected(RejectReason.OutOfRange)
        val lane = timeline.videoLane.toMutableList().also { it[index] = clip.copy(transitionOut = null) }
        return CommandResult.Applied(timeline.copy(videoLane = lane), "Remove transition")
    }
}

/** Sets the clip's effect (one per clip); the strength is clamped. */
class SetClipEffect(private val clipId: String, private val kind: EffectKind, private val strength: Int) : Command {
    override fun apply(timeline: Timeline): CommandResult {
        val index = timeline.videoLane.indexOfFirst { it.id == clipId }
        if (index < 0) return CommandResult.Rejected(RejectReason.NotFound)
        val clip = timeline.videoLane[index]
        val effect = ClipEffect(kind, strength).normalized()
        if (effect == clip.effect) return CommandResult.Rejected(RejectReason.OutOfRange)
        val lane = timeline.videoLane.toMutableList().also { it[index] = clip.copy(effect = effect) }
        return CommandResult.Applied(timeline.copy(videoLane = lane), "Effect")
    }
}

class ResetClipEffect(private val clipId: String) : Command {
    override fun apply(timeline: Timeline): CommandResult {
        val index = timeline.videoLane.indexOfFirst { it.id == clipId }
        if (index < 0) return CommandResult.Rejected(RejectReason.NotFound)
        val clip = timeline.videoLane[index]
        if (clip.effect == null) return CommandResult.Rejected(RejectReason.OutOfRange)
        val lane = timeline.videoLane.toMutableList().also { it[index] = clip.copy(effect = null) }
        return CommandResult.Applied(timeline.copy(videoLane = lane), "Remove effect")
    }
}
