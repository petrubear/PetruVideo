package com.petruvideo.core.model.engine

import com.petruvideo.core.model.MIN_CLIP_MS
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.rescaled
import com.petruvideo.core.model.SPEED_MAX_MILLI
import com.petruvideo.core.model.SPEED_MIN_MILLI
import com.petruvideo.core.model.SPEED_UNIT
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VOLUME_MAX_PERCENT
import com.petruvideo.core.model.timelineLengthMs

/**
 * Sets the speed of a video or audio clip ([speedMilli]: 1000 = 1x). The value is limited to 0.25x..4x, to the
 * fastest speed that keeps the clip at [MIN_CLIP_MS], and on the audio lane to the slowest speed that does not
 * overlap the next clip; a limit that changed the value is reported with [Notice.SpeedLimited]. Later video clips
 * move on their own because the lane is gapless; an audio clip keeps its start.
 */
class SetSpeed(private val clipId: String, private val speedMilli: Int) : Command {
    override fun apply(timeline: Timeline): CommandResult {
        timeline.videoLane.indexOfFirst { it.id == clipId }.takeIf { it >= 0 }?.let { return setVideo(timeline, it) }
        timeline.audioLane.indexOfFirst { it.id == clipId }.takeIf { it >= 0 }?.let { return setAudio(timeline, it) }
        return CommandResult.Rejected(RejectReason.NotFound)
    }

    private fun setVideo(timeline: Timeline, index: Int): CommandResult {
        val clip = timeline.videoLane[index]
        if (timeline.media[clip.mediaItemId]?.kind == MediaKind.Photo) {
            return CommandResult.Rejected(RejectReason.OutOfRange)
        }
        val applied = limit(clip.sourceLengthMs, minSpeed = SPEED_MIN_MILLI)
        if (applied == null || applied == clip.speedMilli) return CommandResult.Rejected(RejectReason.OutOfRange)
        val faster = clip.copy(speedMilli = applied).rescaled(clip.keyframes, clip.lengthMs)
        val lane = timeline.videoLane.toMutableList().also { it[index] = faster }
        return applied(timeline.copy(videoLane = lane), applied)
    }

    private fun setAudio(timeline: Timeline, index: Int): CommandResult {
        val clip = timeline.audioLane[index]
        val next = timeline.audioLane.filter { it.id != clip.id && it.timelineStartMs >= clip.timelineStartMs }
            .minOfOrNull { it.timelineStartMs }
        val minSpeed = if (next == null) {
            SPEED_MIN_MILLI
        } else {
            slowestFit(clip.sourceLengthMs, next - clip.timelineStartMs)
        }
        val applied = limit(clip.sourceLengthMs, minSpeed)
        if (applied == null || applied == clip.speedMilli) return CommandResult.Rejected(RejectReason.OutOfRange)
        val lane = timeline.audioLane.toMutableList().also { it[index] = clip.copy(speedMilli = applied) }
        return applied(timeline.copy(audioLane = lane), applied)
    }

    private fun applied(timeline: Timeline, applied: Int) = CommandResult.Applied(
        timeline = timeline,
        label = "Change speed",
        notices = if (applied != speedMilli) listOf(Notice.SpeedLimited) else emptyList(),
    )

    /** The requested speed clamped to [minSpeed]..the fastest speed that keeps [MIN_CLIP_MS], or null if empty. */
    private fun limit(sourceLengthMs: Long, minSpeed: Int): Int? {
        var fastest = minOf(SPEED_MAX_MILLI.toLong(), sourceLengthMs * SPEED_UNIT / MIN_CLIP_MS).toInt()
        while (fastest > SPEED_MIN_MILLI && timelineLengthMs(sourceLengthMs, fastest) < MIN_CLIP_MS) fastest--
        val lowest = maxOf(SPEED_MIN_MILLI, minSpeed)
        if (lowest > fastest) return null
        return speedMilli.coerceIn(lowest, fastest)
    }

    /** The slowest speed at which [sourceLengthMs] of footage takes no more than [availableMs] of timeline. */
    private fun slowestFit(sourceLengthMs: Long, availableMs: Long): Int {
        if (availableMs <= 0) return Int.MAX_VALUE
        val needed = (sourceLengthMs * SPEED_UNIT + availableMs - 1) / availableMs
        var speed = needed.coerceIn(SPEED_MIN_MILLI.toLong(), Int.MAX_VALUE.toLong()).toInt()
        while (speed < SPEED_MAX_MILLI && timelineLengthMs(sourceLengthMs, speed) > availableMs) speed++
        return speed
    }
}

/**
 * Sets the volume of a clip that has sound: any audio clip, or a video clip whose sound is still attached.
 * Independent of mute.
 */
class SetVolume(private val clipId: String, private val percent: Int) : Command {
    override fun apply(timeline: Timeline): CommandResult {
        val value = percent.coerceIn(0, VOLUME_MAX_PERCENT)
        val videoIndex = timeline.videoLane.indexOfFirst { it.id == clipId }
        if (videoIndex >= 0) {
            val clip = timeline.videoLane[videoIndex]
            if (!clip.attachedAudio) return CommandResult.Rejected(RejectReason.NoAttachedAudio)
            if (clip.volumePercent == value) return CommandResult.Rejected(RejectReason.OutOfRange)
            val lane = timeline.videoLane.toMutableList().also { it[videoIndex] = clip.copy(volumePercent = value) }
            return CommandResult.Applied(timeline.copy(videoLane = lane), "Change volume")
        }
        val audioIndex = timeline.audioLane.indexOfFirst { it.id == clipId }
        if (audioIndex < 0) return CommandResult.Rejected(RejectReason.NotFound)
        val clip = timeline.audioLane[audioIndex]
        if (clip.volumePercent == value) return CommandResult.Rejected(RejectReason.OutOfRange)
        val lane = timeline.audioLane.toMutableList().also { it[audioIndex] = clip.copy(volumePercent = value) }
        return CommandResult.Applied(timeline.copy(audioLane = lane), "Change volume")
    }
}
