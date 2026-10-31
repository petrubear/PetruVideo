package com.petruvideo.feature.editor

import com.petruvideo.core.model.AudioClip
import com.petruvideo.core.model.MIN_CLIP_MS
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.PHOTO_MAX_MS
import com.petruvideo.core.model.PHOTO_MIN_MS
import com.petruvideo.core.model.VideoClip

/** What a trim handle drag would produce: the source position to commit and the clip length it gives. */
data class TrimTarget(val newSourceMs: Long, val lengthMs: Long)

/**
 * Pure clamping for the trim handles so a drag stops at the minimum length and at the source bounds, exactly where
 * the engine would reject it. [deltaMs] is the drag distance in timeline time (positive is to the right).
 */
object TrimLimits {
    fun video(clip: VideoClip, media: MediaItem?, start: Boolean, deltaMs: Long): TrimTarget {
        val photo = media?.kind == MediaKind.Photo
        val duration = media?.durationMs
        return if (start) {
            val range = if (photo) {
                (clip.sourceEndMs - PHOTO_MAX_MS)..(clip.sourceEndMs - PHOTO_MIN_MS)
            } else {
                0L..(clip.sourceEndMs - MIN_CLIP_MS)
            }
            val target = (clip.sourceStartMs + deltaMs).coerceIn(range.first, range.last)
            TrimTarget(target, clip.sourceEndMs - target)
        } else {
            val range = if (photo) {
                (clip.sourceStartMs + PHOTO_MIN_MS)..(clip.sourceStartMs + PHOTO_MAX_MS)
            } else {
                (clip.sourceStartMs + MIN_CLIP_MS)..(duration ?: clip.sourceEndMs)
            }
            val target = (clip.sourceEndMs + deltaMs).coerceIn(range.first, range.last)
            TrimTarget(target, target - clip.sourceStartMs)
        }
    }

    fun audio(clip: AudioClip, media: MediaItem?, start: Boolean, deltaMs: Long): TrimTarget {
        val duration = media?.durationMs ?: clip.sourceEndMs
        return if (start) {
            val target = (clip.sourceStartMs + deltaMs).coerceIn(0L, clip.sourceEndMs - MIN_CLIP_MS)
            TrimTarget(target, clip.sourceEndMs - target)
        } else {
            val target = (clip.sourceEndMs + deltaMs).coerceIn(clip.sourceStartMs + MIN_CLIP_MS, duration)
            TrimTarget(target, target - clip.sourceStartMs)
        }
    }
}
