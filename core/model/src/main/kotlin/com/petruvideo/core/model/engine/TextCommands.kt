package com.petruvideo.core.model.engine

import com.petruvideo.core.model.Keyframes
import com.petruvideo.core.model.TEXT_ADD_MIN_MS
import com.petruvideo.core.model.TEXT_DEFAULT_MS
import com.petruvideo.core.model.TEXT_MAX_CHARS
import com.petruvideo.core.model.TEXT_MAX_MS
import com.petruvideo.core.model.TEXT_MIN_MS
import com.petruvideo.core.model.TEXT_PLACEHOLDER
import com.petruvideo.core.model.TextAnimation
import com.petruvideo.core.model.TextClip
import com.petruvideo.core.model.TextStyle
import com.petruvideo.core.model.Timeline
import java.util.UUID

private fun newTextId() = UUID.randomUUID().toString()

/** Replaces the text clip [clipId] with [change] applied to it; rejects an unknown id or no change. */
abstract class TextClipCommand internal constructor(
    private val clipId: String,
    private val label: String,
) : Command {
    internal abstract fun next(clip: TextClip): TextClip?

    override fun apply(timeline: Timeline): CommandResult {
        val index = timeline.textLane.indexOfFirst { it.id == clipId }
        if (index < 0) return CommandResult.Rejected(RejectReason.NotFound)
        val clip = timeline.textLane[index]
        val changed = next(clip)?.normalized() ?: return CommandResult.Rejected(RejectReason.OutOfRange)
        if (changed == clip) return CommandResult.Rejected(RejectReason.OutOfRange)
        val lane = timeline.textLane.toMutableList().also { it[index] = changed }.sortedBy { it.startMs }
        return CommandResult.Applied(timeline.copy(textLane = lane), label)
    }
}

/**
 * Adds a text clip at [atMs] (normally the playhead), 5 s long, shorter when the project ends sooner but never under
 * 0.5 s. A project with no length yet gets the full 5 s.
 */
class AddText(
    private val atMs: Long,
    private val newId: () -> String = ::newTextId,
    private val text: String = TEXT_PLACEHOLDER,
) : Command {
    override fun apply(timeline: Timeline): CommandResult {
        val start = atMs.coerceAtLeast(0)
        val projectMs = timeline.totalDurationMs
        val room = if (projectMs <= 0) TEXT_DEFAULT_MS else projectMs - start
        val duration = room.coerceIn(TEXT_ADD_MIN_MS, TEXT_DEFAULT_MS)
        val clip = TextClip(newId(), text, start, duration)
        val lane = (timeline.textLane + clip).sortedBy { it.startMs }
        return CommandResult.Applied(timeline.copy(textLane = lane), "Add text")
    }
}

class SetTextContent(clipId: String, private val text: String) : TextClipCommand(clipId, "Edit text") {
    override fun next(clip: TextClip) = clip.copy(text = text.take(TEXT_MAX_CHARS))
}

class SetTextStyle(clipId: String, private val style: TextStyle) : TextClipCommand(clipId, "Text style") {
    override fun next(clip: TextClip) = clip.copy(style = style)
}

/** Sets the center (permille of the frame) and the size (permille of the frame height). */
class SetTextPlacement(
    clipId: String,
    private val xPermille: Int,
    private val yPermille: Int,
    private val sizePermille: Int,
    private val atMs: Long? = null,
) : TextClipCommand(clipId, "Move text") {
    override fun next(clip: TextClip): TextClip? {
        if (clip.keyframes.isEmpty()) {
            return clip.copy(xPermille = xPermille, yPermille = yPermille, sizePermille = sizePermille)
        }
        // With keyframes the change is a keyframe at the time given; without a time it cannot be placed.
        val time = atMs ?: return null
        val rel = (time - clip.startMs).coerceIn(0, clip.durationMs)
        val pose = clip.poseAt(rel).copy(xPermille = xPermille, yPermille = yPermille, sizePermille = sizePermille)
        return Keyframes.withPoseAt(clip.keyframes, rel, pose)?.let { clip.withKeyframes(it) }
    }
}

class SetTextAnimation(clipId: String, private val animIn: TextAnimation, private val animOut: TextAnimation) :
    TextClipCommand(clipId, "Text animation") {
    override fun next(clip: TextClip) = clip.copy(animIn = animIn, animOut = animOut)
}

/** Moves the clip so it starts at [startMs] (never below 0); the length is kept. */
class MoveText(clipId: String, private val startMs: Long) : TextClipCommand(clipId, "Move text clip") {
    override fun next(clip: TextClip) = clip.copy(startMs = startMs.coerceAtLeast(0))
}

/** Sets the clip's start and end; the length must stay between 0.2 s and 10 minutes. */
class TrimText(clipId: String, private val startMs: Long, private val endMs: Long) :
    TextClipCommand(clipId, "Trim text clip") {
    override fun next(clip: TextClip): TextClip? {
        val start = startMs.coerceAtLeast(0)
        val length = endMs - start
        if (length < TEXT_MIN_MS || length > TEXT_MAX_MS) return null
        val shift = start - clip.startMs
        val frames = Keyframes.window(clip.basePose, clip.keyframes, shift, shift + length)
        return clip.copy(startMs = start, durationMs = length).withKeyframes(frames)
    }
}
