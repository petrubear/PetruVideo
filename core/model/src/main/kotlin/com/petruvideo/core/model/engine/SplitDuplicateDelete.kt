package com.petruvideo.core.model.engine

import com.petruvideo.core.model.MIN_CLIP_MS
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.AnimKind
import com.petruvideo.core.model.PHOTO_MIN_MS
import com.petruvideo.core.model.TEXT_MIN_MS
import com.petruvideo.core.model.SPEED_UNIT
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.withoutFadeIn
import com.petruvideo.core.model.withoutFadeOut
import com.petruvideo.core.model.VideoClip
import java.util.UUID

private fun newId() = UUID.randomUUID().toString()

/** Timeline offset into a clip converted to source time. */
private fun sourceOffset(timelineOffsetMs: Long, speedMilli: Int) =
    (timelineOffsetMs * speedMilli + SPEED_UNIT / 2) / SPEED_UNIT

/**
 * Splits a video or audio clip at [atMs] on the timeline (normally the playhead). Both halves keep the clip's
 * settings, including attached sound, and together cover the original source range exactly.
 */
class Split(
    private val clipId: String,
    private val atMs: Long,
    private val newClipId: () -> String = ::newId,
) : Command {
    override fun apply(timeline: Timeline): CommandResult {
        val videoIndex = timeline.videoLane.indexOfFirst { it.id == clipId }
        if (videoIndex >= 0) return splitVideo(timeline, videoIndex)
        val audioIndex = timeline.audioLane.indexOfFirst { it.id == clipId }
        if (audioIndex >= 0) return splitAudio(timeline, audioIndex)
        val textIndex = timeline.textLane.indexOfFirst { it.id == clipId }
        if (textIndex >= 0) return splitText(timeline, textIndex)
        return CommandResult.Rejected(RejectReason.NotFound)
    }

    private fun splitText(timeline: Timeline, index: Int): CommandResult {
        val clip = timeline.textLane[index]
        val offset = atMs - clip.startMs
        if (offset <= 0 || offset >= clip.durationMs) return CommandResult.Rejected(RejectReason.OutOfRange)
        if (offset < TEXT_MIN_MS || clip.durationMs - offset < TEXT_MIN_MS) {
            return CommandResult.Rejected(RejectReason.BelowMinimumLength)
        }
        val first = clip.copy(
            id = newClipId(), durationMs = offset, animOut = clip.animOut.copy(kind = AnimKind.None),
        )
        val second = clip.copy(
            id = newClipId(), startMs = atMs, durationMs = clip.durationMs - offset,
            animIn = clip.animIn.copy(kind = AnimKind.None),
        )
        val lane = timeline.textLane.toMutableList().apply { removeAt(index); addAll(index, listOf(first, second)) }
        return CommandResult.Applied(timeline.copy(textLane = lane), "Split text")
    }

    private fun splitVideo(timeline: Timeline, index: Int): CommandResult {
        val clip = timeline.videoLane[index]
        val clipStart = timeline.videoStartOf(clip.id) ?: return CommandResult.Rejected(RejectReason.NotFound)
        val offset = atMs - clipStart
        val photo = timeline.media[clip.mediaItemId]?.kind == MediaKind.Photo
        val minimum = if (photo) PHOTO_MIN_MS else MIN_CLIP_MS
        if (offset <= 0 || offset >= clip.lengthMs) return CommandResult.Rejected(RejectReason.OutOfRange)
        if (offset < minimum || clip.lengthMs - offset < minimum) {
            return CommandResult.Rejected(RejectReason.BelowMinimumLength)
        }
        val first: VideoClip
        val second: VideoClip
        if (photo) {
            first = clip.copy(id = newClipId(), sourceStartMs = 0, sourceEndMs = offset)
            second = clip.copy(id = newClipId(), sourceStartMs = 0, sourceEndMs = clip.lengthMs - offset)
        } else {
            val cut = clip.sourceStartMs + sourceOffset(offset, clip.speedMilli)
            first = clip.copy(id = newClipId(), sourceEndMs = cut, fx = clip.fx.withoutFadeOut())
            second = clip.copy(id = newClipId(), sourceStartMs = cut, fx = clip.fx.withoutFadeIn())
        }
        val lane = timeline.videoLane.toMutableList().apply { removeAt(index); addAll(index, listOf(first, second)) }
        return CommandResult.Applied(timeline.copy(videoLane = lane), "Split clip")
    }

    private fun splitAudio(timeline: Timeline, index: Int): CommandResult {
        val clip = timeline.audioLane[index]
        val offset = atMs - clip.timelineStartMs
        if (offset <= 0 || offset >= clip.lengthMs) return CommandResult.Rejected(RejectReason.OutOfRange)
        if (offset < MIN_CLIP_MS || clip.lengthMs - offset < MIN_CLIP_MS) {
            return CommandResult.Rejected(RejectReason.BelowMinimumLength)
        }
        val cut = clip.sourceStartMs + sourceOffset(offset, clip.speedMilli)
        val first = clip.copy(id = newClipId(), sourceEndMs = cut, fx = clip.fx.withoutFadeOut())
        val second = clip.copy(
            id = newClipId(), sourceStartMs = cut, timelineStartMs = atMs, fx = clip.fx.withoutFadeIn(),
        )
        val lane = timeline.audioLane.toMutableList().apply { removeAt(index); addAll(index, listOf(first, second)) }
        return CommandResult.Applied(timeline.copy(audioLane = lane), "Split audio")
    }
}

/**
 * Copies a clip. A video copy goes right after the original; an audio copy goes to the first free position at or
 * after the original's end.
 */
class Duplicate(private val clipId: String, private val newClipId: () -> String = ::newId) : Command {
    override fun apply(timeline: Timeline): CommandResult {
        val videoIndex = timeline.videoLane.indexOfFirst { it.id == clipId }
        if (videoIndex >= 0) {
            val lane = timeline.videoLane.toMutableList()
            lane.add(videoIndex + 1, lane[videoIndex].copy(id = newClipId()))
            return CommandResult.Applied(timeline.copy(videoLane = lane), "Duplicate clip")
        }
        timeline.textLane.firstOrNull { it.id == clipId }?.let { text ->
            val lane = (timeline.textLane + text.copy(id = newClipId(), startMs = text.endMs)).sortedBy { it.startMs }
            return CommandResult.Applied(timeline.copy(textLane = lane), "Duplicate text")
        }
        val clip = timeline.audioLane.firstOrNull { it.id == clipId }
            ?: return CommandResult.Rejected(RejectReason.NotFound)
        val start = AudioPlacement.nextFreeStart(timeline.audioLane, clip.timelineEndMs, clip.lengthMs)
        val lane = (timeline.audioLane + clip.copy(id = newClipId(), timelineStartMs = start))
            .sortedBy { it.timelineStartMs }
        return CommandResult.Applied(timeline.copy(audioLane = lane), "Duplicate audio")
    }
}

/** Removes a clip. A video clip's attached sound goes with it; detached audio stays where it is. */
class Delete(private val clipId: String) : Command {
    override fun apply(timeline: Timeline): CommandResult = when {
        timeline.videoLane.any { it.id == clipId } -> CommandResult.Applied(
            timeline.copy(videoLane = timeline.videoLane.filterNot { it.id == clipId }),
            "Delete clip",
        )
        timeline.audioLane.any { it.id == clipId } -> CommandResult.Applied(
            timeline.copy(audioLane = timeline.audioLane.filterNot { it.id == clipId }),
            "Delete audio",
        )
        timeline.textLane.any { it.id == clipId } -> CommandResult.Applied(
            timeline.copy(textLane = timeline.textLane.filterNot { it.id == clipId }),
            "Delete text",
        )
        else -> CommandResult.Rejected(RejectReason.NotFound)
    }
}
