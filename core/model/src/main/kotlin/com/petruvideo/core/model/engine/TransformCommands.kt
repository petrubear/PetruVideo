package com.petruvideo.core.model.engine

import com.petruvideo.core.model.ClipTransform
import com.petruvideo.core.model.CropBox
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VideoClip

/** Changes the transform of the main-track clip [clipId]; rejects an unknown id or a change that changes nothing. */
abstract class TransformCommand internal constructor(
    private val clipId: String,
    private val label: String,
) : Command {
    internal abstract fun next(current: ClipTransform): ClipTransform?

    override fun apply(timeline: Timeline): CommandResult {
        val index = timeline.videoLane.indexOfFirst { it.id == clipId }
        if (index < 0) return CommandResult.Rejected(RejectReason.NotFound)
        val clip: VideoClip = timeline.videoLane[index]
        val transform = next(clip.transform) ?: return CommandResult.Rejected(RejectReason.OutOfRange)
        if (transform == clip.transform) return CommandResult.Rejected(RejectReason.OutOfRange)
        val lane = timeline.videoLane.toMutableList().also { it[index] = clip.copy(transform = transform) }
        return CommandResult.Applied(timeline.copy(videoLane = lane), label)
    }
}

/** A quarter turn clockwise; the crop turns with the picture. */
class RotateClip(clipId: String) : TransformCommand(clipId, "Rotate") {
    override fun next(current: ClipTransform) = current.rotated()
}

/** Mirrors the picture left to right ([horizontal]) or top to bottom; the crop mirrors with it. */
class FlipClip(clipId: String, private val horizontal: Boolean) : TransformCommand(clipId, "Flip") {
    override fun next(current: ClipTransform) =
        if (horizontal) current.flippedHorizontally() else current.flippedVertically()
}

/** Sets the crop box (the whole picture removes the crop); a box that is not inside the picture is pulled in. */
class SetCrop(clipId: String, private val box: CropBox) : TransformCommand(clipId, "Crop") {
    override fun next(current: ClipTransform) = current.copy(crop = box.normalized())
}

/** Removes the turn, the mirrors, and the crop. */
class ResetTransform(clipId: String) : TransformCommand(clipId, "Reset transform") {
    override fun next(current: ClipTransform) = ClipTransform()
}
