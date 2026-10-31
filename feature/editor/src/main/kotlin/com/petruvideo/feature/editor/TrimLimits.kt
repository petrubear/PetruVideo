package com.petruvideo.feature.editor

import com.petruvideo.core.model.AudioClip
import com.petruvideo.core.model.MIN_CLIP_MS
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.PHOTO_MAX_MS
import com.petruvideo.core.model.PHOTO_MIN_MS
import com.petruvideo.core.model.SPEED_UNIT
import com.petruvideo.core.model.timelineLengthMs
import com.petruvideo.core.model.VideoClip

/** What a trim handle drag would produce: the source position to commit and the timeline length it gives. */
data class TrimTarget(val newSourceMs: Long, val lengthMs: Long)

/**
 * Pure clamping for the trim handles so a drag stops at the minimum length and at the source bounds, exactly where
 * the engine would reject it. [deltaMs] is the drag distance in timeline time (positive is to the right).
 */
object TrimLimits {
    /** A drag distance in timeline time converted to source time at [speedMilli]. */
    private fun toSource(deltaMs: Long, speedMilli: Int) =
        Math.floorDiv(deltaMs * speedMilli + SPEED_UNIT / 2, SPEED_UNIT)

    /** The shortest source length that still gives [MIN_CLIP_MS] on the timeline at [speedMilli]. */
    private fun minSource(speedMilli: Int) = (MIN_CLIP_MS * speedMilli + SPEED_UNIT - 1) / SPEED_UNIT

    fun video(clip: VideoClip, media: MediaItem?, start: Boolean, deltaMs: Long): TrimTarget {
        val photo = media?.kind == MediaKind.Photo
        val duration = media?.durationMs
        val minSource = minSource(clip.speedMilli)
        val delta = toSource(deltaMs, clip.speedMilli)
        return if (start) {
            val range = if (photo) {
                (clip.sourceEndMs - PHOTO_MAX_MS)..(clip.sourceEndMs - PHOTO_MIN_MS)
            } else {
                0L..(clip.sourceEndMs - minSource)
            }
            val target = (clip.sourceStartMs + delta).coerceIn(range.first, range.last)
            TrimTarget(target, timelineLengthMs(clip.sourceEndMs - target, clip.speedMilli))
        } else {
            val range = if (photo) {
                (clip.sourceStartMs + PHOTO_MIN_MS)..(clip.sourceStartMs + PHOTO_MAX_MS)
            } else {
                (clip.sourceStartMs + minSource)..(duration ?: clip.sourceEndMs)
            }
            val target = (clip.sourceEndMs + delta).coerceIn(range.first, range.last)
            TrimTarget(target, timelineLengthMs(target - clip.sourceStartMs, clip.speedMilli))
        }
    }

    fun audio(clip: AudioClip, media: MediaItem?, start: Boolean, deltaMs: Long): TrimTarget {
        val duration = media?.durationMs ?: clip.sourceEndMs
        val minSource = minSource(clip.speedMilli)
        val delta = toSource(deltaMs, clip.speedMilli)
        return if (start) {
            val target = (clip.sourceStartMs + delta).coerceIn(0L, clip.sourceEndMs - minSource)
            TrimTarget(target, timelineLengthMs(clip.sourceEndMs - target, clip.speedMilli))
        } else {
            val target = (clip.sourceEndMs + delta).coerceIn(clip.sourceStartMs + minSource, duration)
            TrimTarget(target, timelineLengthMs(target - clip.sourceStartMs, clip.speedMilli))
        }
    }
}
