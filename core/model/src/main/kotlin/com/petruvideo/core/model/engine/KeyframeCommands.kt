package com.petruvideo.core.model.engine

import com.petruvideo.core.model.FRAMING_NEUTRAL
import com.petruvideo.core.model.KEYFRAME_MAX
import com.petruvideo.core.model.KEYFRAME_MIN_GAP_MS
import com.petruvideo.core.model.Keyframe
import com.petruvideo.core.model.Keyframes
import com.petruvideo.core.model.Pose
import com.petruvideo.core.model.Timeline

/** The new keyframes of a clip, and the plain values to use when there are none left. */
internal data class KeyframeEdit(val frames: List<Keyframe>, val plain: Pose? = null)

/** Edits the keyframes of the overlay or text clip [clipId]; rejects an unknown id or no change. */
abstract class KeyframeCommand internal constructor(
    private val clipId: String,
    private val label: String,
) : Command {
    internal abstract fun edit(clip: KeyframeTarget): KeyframeEdit?

    /** How a rejected edit is reported; commands that hit the keyframe limit say so. */
    internal open fun rejection(clip: KeyframeTarget) = RejectReason.OutOfRange

    override fun apply(timeline: Timeline): CommandResult {
        val overlayIndex = timeline.overlayLane.indexOfFirst { it.id == clipId }
        if (overlayIndex >= 0) return applyToOverlay(timeline, overlayIndex)
        val textIndex = timeline.textLane.indexOfFirst { it.id == clipId }
        if (textIndex >= 0) return applyToText(timeline, textIndex)
        val videoIndex = timeline.videoLane.indexOfFirst { it.id == clipId }
        if (videoIndex >= 0) return applyToVideo(timeline, videoIndex)
        return CommandResult.Rejected(RejectReason.NotFound)
    }

    private fun applyToVideo(timeline: Timeline, index: Int): CommandResult {
        val clip = timeline.videoLane[index]
        val start = timeline.videoStartOf(clip.id) ?: return CommandResult.Rejected(RejectReason.NotFound)
        val target = KeyframeTarget(FRAMING_NEUTRAL, clip.keyframes, start, clip.lengthMs, seedsStart = true)
        val edit = edit(target) ?: return CommandResult.Rejected(rejection(target))
        val changed = clip.withKeyframes(edit.frames)
        if (changed == clip) return CommandResult.Rejected(RejectReason.OutOfRange)
        val lane = timeline.videoLane.toMutableList().also { it[index] = changed }
        return CommandResult.Applied(timeline.copy(videoLane = lane), label)
    }

    private fun applyToOverlay(timeline: Timeline, index: Int): CommandResult {
        val clip = timeline.overlayLane[index]
        val target = KeyframeTarget(clip.basePose, clip.keyframes, clip.startMs, clip.durationMs)
        val edit = edit(target) ?: return CommandResult.Rejected(rejection(target))
        val changed = clip.withKeyframes(edit.frames, edit.plain).normalized()
        if (changed == clip) return CommandResult.Rejected(RejectReason.OutOfRange)
        val lane = timeline.overlayLane.toMutableList().also { it[index] = changed }
        return CommandResult.Applied(timeline.copy(overlayLane = lane), label)
    }

    private fun applyToText(timeline: Timeline, index: Int): CommandResult {
        val clip = timeline.textLane[index]
        val target = KeyframeTarget(clip.basePose, clip.keyframes, clip.startMs, clip.durationMs)
        val edit = edit(target) ?: return CommandResult.Rejected(rejection(target))
        val changed = clip.withKeyframes(edit.frames, edit.plain).normalized()
        if (changed == clip) return CommandResult.Rejected(RejectReason.OutOfRange)
        val lane = timeline.textLane.toMutableList().also { it[index] = changed }
        return CommandResult.Applied(timeline.copy(textLane = lane), label)
    }
}

/** What a keyframe edit needs to know about its clip, whichever lane it is in. */
class KeyframeTarget internal constructor(
    val base: Pose,
    val frames: List<Keyframe>,
    val startMs: Long,
    val durationMs: Long,
    /** A video clip's first keyframe made away from its start is preceded by one holding the plain look. */
    val seedsStart: Boolean = false,
) {
    fun rel(timelineMs: Long) = (timelineMs - startMs).coerceIn(0, durationMs)

    fun poseAtRel(relMs: Long) = Keyframes.poseAt(base, frames, relMs)
}

/** [frames] to build on for a change at [relMs]: a video clip with none gets a start keyframe holding the look. */
private fun KeyframeTarget.seeded(relMs: Long): List<Keyframe>? =
    if (seedsStart && frames.isEmpty() && relMs >= KEYFRAME_MIN_GAP_MS) listOf(Keyframe(0, base)) else frames

private fun KeyframeTarget.indexAt(relMs: Long) = frames.indexOfFirst { it.timeMs == relMs }

/** Adds a keyframe at timeline time [atMs] holding the values the clip shows there; one already there is kept. */
class AddKeyframe(clipId: String, private val atMs: Long) : KeyframeCommand(clipId, "Add keyframe") {
    override fun edit(clip: KeyframeTarget): KeyframeEdit? {
        if (atMs < clip.startMs || atMs > clip.startMs + clip.durationMs) return null
        val rel = clip.rel(atMs)
        if (clip.frames.any { kotlin.math.abs(it.timeMs - rel) < KEYFRAME_MIN_GAP_MS }) return null
        return Keyframes.withPoseAt(clip.frames, rel, clip.poseAtRel(rel))?.let { KeyframeEdit(it) }
    }

    override fun rejection(clip: KeyframeTarget) =
        if (clip.frames.size >= KEYFRAME_MAX) RejectReason.LimitReached else RejectReason.OutOfRange
}

/** Sets the clip's pose at timeline time [atMs] as a keyframe there (changing or adding one). */
class SetPoseAt(clipId: String, private val atMs: Long, private val pose: Pose) :
    KeyframeCommand(clipId, "Animate") {
    override fun edit(clip: KeyframeTarget): KeyframeEdit? =
        clip.seeded(clip.rel(atMs))?.let { frames ->
            Keyframes.withPoseAt(frames, clip.rel(atMs), pose)?.let { KeyframeEdit(it) }
        }

    override fun rejection(clip: KeyframeTarget) =
        if (clip.frames.size >= KEYFRAME_MAX) RejectReason.LimitReached else RejectReason.OutOfRange
}

/** Moves the keyframe at clip time [fromRelMs] to [toRelMs]; it cannot pass or touch a neighbour. */
class MoveKeyframe(clipId: String, private val fromRelMs: Long, private val toRelMs: Long) :
    KeyframeCommand(clipId, "Move keyframe") {
    override fun edit(clip: KeyframeTarget): KeyframeEdit? {
        val index = clip.indexAt(fromRelMs)
        if (index < 0 || toRelMs !in 0..clip.durationMs) return null
        val before = clip.frames.getOrNull(index - 1)
        val after = clip.frames.getOrNull(index + 1)
        if (before != null && toRelMs - before.timeMs < KEYFRAME_MIN_GAP_MS) return null
        if (after != null && after.timeMs - toRelMs < KEYFRAME_MIN_GAP_MS) return null
        val frames = clip.frames.toMutableList().also { it[index] = it[index].copy(timeMs = toRelMs) }
        return KeyframeEdit(frames)
    }
}

/** Copies the keyframe at clip time [fromRelMs] to timeline time [toAtMs]; one already there is replaced. */
class CopyKeyframe(clipId: String, private val fromRelMs: Long, private val toAtMs: Long) :
    KeyframeCommand(clipId, "Copy keyframe") {
    override fun edit(clip: KeyframeTarget): KeyframeEdit? {
        val source = clip.frames.getOrNull(clip.indexAt(fromRelMs)) ?: return null
        val rel = clip.rel(toAtMs)
        val kept = clip.frames.filter { kotlin.math.abs(it.timeMs - rel) >= KEYFRAME_MIN_GAP_MS }
        if (kept.size >= KEYFRAME_MAX) return null
        return KeyframeEdit((kept + source.copy(timeMs = rel)).sortedBy { it.timeMs })
    }

    override fun rejection(clip: KeyframeTarget) =
        if (clip.frames.size >= KEYFRAME_MAX) RejectReason.LimitReached else RejectReason.OutOfRange
}

/** Removes the keyframe at clip time [relMs]; removing the last one keeps the values it held as the plain values. */
class DeleteKeyframe(clipId: String, private val relMs: Long) : KeyframeCommand(clipId, "Delete keyframe") {
    override fun edit(clip: KeyframeTarget): KeyframeEdit? {
        val index = clip.indexAt(relMs)
        if (index < 0) return null
        val frames = clip.frames.filterIndexed { i, _ -> i != index }
        return KeyframeEdit(frames, plain = clip.frames[index].pose)
    }
}

/** Picks the motion (ease or linear) of the stretch after the keyframe at clip time [relMs]. */
class SetKeyframeMotion(clipId: String, private val relMs: Long, private val ease: Boolean) :
    KeyframeCommand(clipId, "Keyframe motion") {
    override fun edit(clip: KeyframeTarget): KeyframeEdit? {
        val index = clip.indexAt(relMs)
        if (index < 0) return null
        return KeyframeEdit(clip.frames.toMutableList().also { it[index] = it[index].copy(ease = ease) })
    }
}
