package com.petruvideo.core.model.engine

import com.petruvideo.core.model.CURVE_LENGTH_MAX_MS
import com.petruvideo.core.model.MIN_CLIP_MS
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.SPEED_UNIT
import com.petruvideo.core.model.SpeedCurve
import com.petruvideo.core.model.SpeedPreset
import com.petruvideo.core.model.Timeline

/** Sets the speed curve of the main-track clip [clipId] to [curve]; rejects a curve that does not fit the clip. */
open class SetSpeedCurve(private val clipId: String, private val curve: SpeedCurve) : Command {
    override fun apply(timeline: Timeline): CommandResult {
        val index = timeline.videoLane.indexOfFirst { it.id == clipId }
        if (index < 0) return CommandResult.Rejected(RejectReason.NotFound)
        val clip = timeline.videoLane[index]
        val media = timeline.media[clip.mediaItemId]
        val fits = curve.isValid && curve.sourceLengthMs == clip.sourceLengthMs && media?.kind == MediaKind.Video
        if (!fits) return CommandResult.Rejected(RejectReason.OutOfRange)
        if (curve.timelineMs < MIN_CLIP_MS) return CommandResult.Rejected(RejectReason.BelowMinimumLength)
        if (curve.timelineMs > CURVE_LENGTH_MAX_MS || clip.speedCurve == curve) {
            return CommandResult.Rejected(RejectReason.OutOfRange)
        }
        val changed = clip.copy(speedCurve = curve).let { it.withKeyframes(it.keyframes) }
        val lane = timeline.videoLane.toMutableList().also { it[index] = changed }
        return CommandResult.Applied(timeline.copy(videoLane = lane), "Speed curve")
    }
}

/** Sets one of the named curves, scaled to the clip. */
class ApplySpeedPreset(private val clipId: String, private val preset: SpeedPreset) : Command {
    override fun apply(timeline: Timeline): CommandResult {
        val clip = timeline.videoLane.firstOrNull { it.id == clipId }
            ?: return CommandResult.Rejected(RejectReason.NotFound)
        return SetSpeedCurve(clipId, SpeedCurve.preset(preset, clip.sourceLengthMs)).apply(timeline)
    }
}

/** Removes the speed curve and plays the clip at constant 1x. */
class ResetSpeedCurve(private val clipId: String) : Command {
    override fun apply(timeline: Timeline): CommandResult {
        val index = timeline.videoLane.indexOfFirst { it.id == clipId }
        if (index < 0) return CommandResult.Rejected(RejectReason.NotFound)
        val clip = timeline.videoLane[index]
        if (clip.speedCurve == null) return CommandResult.Rejected(RejectReason.OutOfRange)
        val changed = clip.copy(speedCurve = null, speedMilli = SPEED_UNIT).let { it.withKeyframes(it.keyframes) }
        val lane = timeline.videoLane.toMutableList().also { it[index] = changed }
        return CommandResult.Applied(timeline.copy(videoLane = lane), "Reset speed curve")
    }
}
