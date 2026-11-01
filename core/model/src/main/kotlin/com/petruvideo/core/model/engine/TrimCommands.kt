package com.petruvideo.core.model.engine

import com.petruvideo.core.model.AudioClip
import com.petruvideo.core.model.MIN_CLIP_MS
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.PHOTO_MAX_MS
import com.petruvideo.core.model.PHOTO_MIN_MS
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VideoClip
import com.petruvideo.core.model.reframed

/**
 * Moves the start handle of a video or audio clip to [newSourceMs] (a position in the source). Footage trimmed
 * earlier is restored by moving the handle back. Later video clips shift on their own; an audio clip keeps its
 * content in place, so its timeline start moves with the handle. For a photo clip the source start stays 0 and the
 * handle changes the length instead: a negative [newSourceMs] lengthens it.
 */
class TrimStart(private val clipId: String, private val newSourceMs: Long) : Command {
    override fun apply(timeline: Timeline): CommandResult = trim(timeline, clipId, newSourceMs, start = true)
}

/** Moves the end handle of a video or audio clip to [newSourceMs]; photo clips can be lengthened this way. */
class TrimEnd(private val clipId: String, private val newSourceMs: Long) : Command {
    override fun apply(timeline: Timeline): CommandResult = trim(timeline, clipId, newSourceMs, start = false)
}

private fun trim(timeline: Timeline, clipId: String, newSourceMs: Long, start: Boolean): CommandResult {
    val videoIndex = timeline.videoLane.indexOfFirst { it.id == clipId }
    if (videoIndex >= 0) return trimVideo(timeline, videoIndex, newSourceMs, start)
    val audioIndex = timeline.audioLane.indexOfFirst { it.id == clipId }
    if (audioIndex >= 0) return trimAudio(timeline, audioIndex, newSourceMs, start)
    return CommandResult.Rejected(RejectReason.NotFound)
}

private fun trimVideo(timeline: Timeline, index: Int, newSourceMs: Long, start: Boolean): CommandResult {
    val clip = timeline.videoLane[index]
    val media = timeline.media[clip.mediaItemId] ?: return CommandResult.Rejected(RejectReason.NotFound)
    val trimmed = if (media.kind == MediaKind.Photo) {
        trimPhoto(clip, newSourceMs, start) ?: return CommandResult.Rejected(RejectReason.OutOfRange)
    } else {
        trimFootage(clip, media.durationMs, newSourceMs, start).let {
            it.clip ?: return CommandResult.Rejected(it.reason)
        }
    }
    // The part of the old clip that is kept starts at the start trim and is as long as the new clip.
    val from = if (start) clip.lengthMs - trimmed.lengthMs else 0L
    val kept = trimmed.reframed(clip.keyframes, from, from + trimmed.lengthMs)
    val lane = timeline.videoLane.toMutableList().also { it[index] = kept }
    return CommandResult.Applied(timeline.copy(videoLane = lane), "Trim clip")
}

/** The trimmed clip, or why it cannot be. */
private class Trimmed(val clip: VideoClip?, val reason: RejectReason = RejectReason.OutOfRange)

private fun trimFootage(clip: VideoClip, duration: Long?, newSourceMs: Long, start: Boolean): Trimmed {
    if (duration == null || newSourceMs < 0 || newSourceMs > duration) return Trimmed(null)
    // A reversed clip runs backwards, so its start handle trims the end of the source and the end handle the start.
    val value = if (clip.reversed) mirrored(clip, newSourceMs) else newSourceMs
    val trimsSourceStart = start != clip.reversed
    val candidate = if (trimsSourceStart) clip.copy(sourceStartMs = value) else clip.copy(sourceEndMs = value)
    return when {
        candidate.sourceStartMs < 0 || candidate.sourceEndMs > duration -> Trimmed(null)
        candidate.lengthMs < MIN_CLIP_MS -> Trimmed(null, RejectReason.BelowMinimumLength)
        else -> Trimmed(candidate)
    }
}

private fun trimPhoto(clip: VideoClip, newSourceMs: Long, start: Boolean): VideoClip? {
    val length = if (start) clip.sourceEndMs - newSourceMs else newSourceMs - clip.sourceStartMs
    if (length !in PHOTO_MIN_MS..PHOTO_MAX_MS) return null
    return clip.copy(sourceStartMs = 0, sourceEndMs = length)
}

private fun trimAudio(timeline: Timeline, index: Int, newSourceMs: Long, start: Boolean): CommandResult {
    val clip = timeline.audioLane[index]
    val duration = timeline.media[clip.mediaItemId]?.durationMs
        ?: return CommandResult.Rejected(RejectReason.NotFound)
    if (newSourceMs < 0 || newSourceMs > duration) return CommandResult.Rejected(RejectReason.OutOfRange)
    val candidate = if (start) {
        val shift = newSourceMs - clip.sourceStartMs
        clip.copy(sourceStartMs = newSourceMs, timelineStartMs = clip.timelineStartMs + shift)
    } else {
        clip.copy(sourceEndMs = newSourceMs)
    }
    if (candidate.lengthMs < MIN_CLIP_MS) return CommandResult.Rejected(RejectReason.BelowMinimumLength)
    if (candidate.timelineStartMs < 0 || overlapsOthers(timeline.audioLane, candidate)) {
        return CommandResult.Rejected(RejectReason.OutOfRange)
    }
    val lane = timeline.audioLane.toMutableList().also { it[index] = candidate }
    return CommandResult.Applied(timeline.copy(audioLane = lane), "Trim audio")
}

private fun overlapsOthers(lane: List<AudioClip>, clip: AudioClip) = lane.any {
    it.id != clip.id && it.timelineStartMs < clip.timelineEndMs && clip.timelineStartMs < it.timelineEndMs
}

/** A source time given as if the clip played forwards, as the source time for the opposite edge of a reversed clip. */
private fun mirrored(clip: VideoClip, forwardMs: Long): Long = clip.sourceStartMs + clip.sourceEndMs - forwardMs
