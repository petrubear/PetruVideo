package com.petruvideo.core.model.engine

import com.petruvideo.core.model.ClipFilter
import com.petruvideo.core.model.FilterLook
import com.petruvideo.core.model.Timeline

/** Changes the filter of one video or photo clip; audio and text clips are rejected as not found. */
abstract class FilterCommand internal constructor(
    private val clipId: String,
    private val label: String,
) : Command {
    internal abstract fun next(current: ClipFilter): ClipFilter

    override fun apply(timeline: Timeline): CommandResult {
        val index = timeline.videoLane.indexOfFirst { it.id == clipId }
        if (index < 0) return CommandResult.Rejected(RejectReason.NotFound)
        val clip = timeline.videoLane[index]
        val filter = next(clip.filter).normalized()
        if (filter == clip.filter) return CommandResult.Rejected(RejectReason.OutOfRange)
        val lane = timeline.videoLane.toMutableList().also { it[index] = clip.copy(filter = filter) }
        return CommandResult.Applied(timeline.copy(videoLane = lane), label)
    }
}

/** Chooses the look; the strength goes back to 100% and the adjustments stay. */
class SetFilterLook(clipId: String, private val look: FilterLook) : FilterCommand(clipId, "Filter") {
    override fun next(current: ClipFilter) = current.copy(look = look, intensity = 100)
}

class SetFilterIntensity(clipId: String, private val intensity: Int) : FilterCommand(clipId, "Filter strength") {
    override fun next(current: ClipFilter) = current.copy(intensity = intensity)
}

/** Sets the twelve adjustments (clamped); the look and its strength stay. */
class SetFilterAdjustments(clipId: String, private val values: ClipFilter) : FilterCommand(clipId, "Adjust") {
    override fun next(current: ClipFilter) = current.copy(
        brightness = values.brightness,
        contrast = values.contrast,
        saturation = values.saturation,
        warmth = values.warmth,
        vignette = values.vignette,
        exposure = values.exposure,
        gamma = values.gamma,
        highlights = values.highlights,
        shadows = values.shadows,
        tint = values.tint,
        hue = values.hue,
        sharpen = values.sharpen,
    )
}

/** Puts the twelve adjustments back to their defaults and keeps the look. */
class ResetFilterAdjustments(clipId: String) : FilterCommand(clipId, "Reset adjustments") {
    override fun next(current: ClipFilter) =
        current.copy(
            brightness = 0, contrast = 0, saturation = 0, warmth = 0, vignette = 0,
            exposure = 0, gamma = 0, highlights = 0, shadows = 0, tint = 0, hue = 0, sharpen = 0,
        )
}

/** Clears the look and the adjustments. */
class RemoveFilter(clipId: String) : FilterCommand(clipId, "Remove filter") {
    override fun next(current: ClipFilter) = ClipFilter()
}

/** Copies the filter of [fromClipId] to every video and photo clip, as one edit. */
class ApplyFilterToAll(private val fromClipId: String) : Command {
    override fun apply(timeline: Timeline): CommandResult {
        val source = timeline.videoLane.firstOrNull { it.id == fromClipId }
            ?: return CommandResult.Rejected(RejectReason.NotFound)
        if (timeline.videoLane.size < 2 || timeline.videoLane.all { it.filter == source.filter }) {
            return CommandResult.Rejected(RejectReason.OutOfRange)
        }
        val lane = timeline.videoLane.map { it.copy(filter = source.filter) }
        return CommandResult.Applied(timeline.copy(videoLane = lane), "Apply filter to all")
    }
}
