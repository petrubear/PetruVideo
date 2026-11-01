package com.petruvideo.core.model.engine

import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.OVERLAY_DEFAULT_MS
import com.petruvideo.core.model.OVERLAY_MIN_MS
import com.petruvideo.core.model.OverlayClip
import com.petruvideo.core.model.Timeline
import java.util.UUID

private fun newOverlayId() = UUID.randomUUID().toString()

/** Changes the overlay [clipId]; rejects an unknown id or no change. */
abstract class OverlayCommand internal constructor(
    private val clipId: String,
    private val label: String,
) : Command {
    internal abstract fun next(clip: OverlayClip, media: MediaItem?): OverlayClip?

    override fun apply(timeline: Timeline): CommandResult {
        val index = timeline.overlayLane.indexOfFirst { it.id == clipId }
        if (index < 0) return CommandResult.Rejected(RejectReason.NotFound)
        val clip = timeline.overlayLane[index]
        val changed = next(clip, timeline.media[clip.mediaItemId])?.normalized()
            ?: return CommandResult.Rejected(RejectReason.OutOfRange)
        if (changed == clip) return CommandResult.Rejected(RejectReason.OutOfRange)
        val lane = timeline.overlayLane.toMutableList().also { it[index] = changed }.sortedBy { it.startMs }
        return CommandResult.Applied(timeline.copy(overlayLane = lane), label)
    }
}

/** Puts a video or photo over the main video at [atMs]: a photo 5 s, a video up to 10 s from its start. */
class AddOverlay(
    private val media: MediaItem,
    private val atMs: Long,
    private val newId: () -> String = ::newOverlayId,
) : Command {
    override fun apply(timeline: Timeline): CommandResult {
        if (media.kind == MediaKind.Audio) return CommandResult.Rejected(RejectReason.OutOfRange)
        if (media.availability == Availability.Missing) return CommandResult.Rejected(RejectReason.MissingMedia)
        val length = when (media.kind) {
            MediaKind.Photo -> OVERLAY_DEFAULT_MS
            else -> minOf(media.durationMs ?: OVERLAY_DEFAULT_MS, OVERLAY_VIDEO_MAX_MS)
        }
        if (length < OVERLAY_MIN_MS) return CommandResult.Rejected(RejectReason.BelowMinimumLength)
        val clip = OverlayClip(newId(), media.id, 0, length, atMs.coerceAtLeast(0))
        val lane = (timeline.overlayLane + clip).sortedBy { it.startMs }
        return CommandResult.Applied(
            timeline.copy(media = timeline.media + (media.id to media), overlayLane = lane),
            "Add overlay",
        )
    }

    private companion object {
        const val OVERLAY_VIDEO_MAX_MS = 10_000L
    }
}

/** Sets the center (permille of the frame) and the width (permille of the frame width). */
class SetOverlayPlacement(clipId: String, private val x: Int, private val y: Int, private val width: Int) :
    OverlayCommand(clipId, "Move overlay") {
    override fun next(clip: OverlayClip, media: MediaItem?) =
        clip.copy(xPermille = x, yPermille = y, widthPermille = width)
}

class SetOverlayOpacity(clipId: String, private val percent: Int) : OverlayCommand(clipId, "Overlay opacity") {
    override fun next(clip: OverlayClip, media: MediaItem?) = clip.copy(opacityPercent = percent)
}

/** Moves the overlay in time (never below 0); the length is kept. */
class MoveOverlay(clipId: String, private val startMs: Long) : OverlayCommand(clipId, "Move overlay clip") {
    override fun next(clip: OverlayClip, media: MediaItem?) = clip.copy(startMs = startMs)
}

/**
 * Sets the overlay's start and end on the timeline. A video keeps to its source: trimming the start moves the
 * source start, and the end cannot go past the end of the file; a photo can be made longer or shorter freely.
 */
class TrimOverlay(clipId: String, private val startMs: Long, private val endMs: Long) :
    OverlayCommand(clipId, "Trim overlay") {
    override fun next(clip: OverlayClip, media: MediaItem?): OverlayClip? {
        val start = startMs.coerceAtLeast(0)
        if (endMs - start < OVERLAY_MIN_MS) return null
        val photo = media?.kind == MediaKind.Photo
        val sourceStart = if (photo) 0L else clip.sourceStartMs + (start - clip.startMs)
        val sourceEnd = sourceStart + (endMs - start)
        val limit = media?.durationMs
        if (!photo && sourceStart < 0) return null
        if (!photo && limit != null && sourceEnd > limit) return null
        return clip.copy(startMs = start, sourceStartMs = sourceStart, sourceEndMs = sourceEnd)
    }
}
