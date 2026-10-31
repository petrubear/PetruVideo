package com.petruvideo.core.model.engine

import com.petruvideo.core.model.MIN_CLIP_MS
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.Timeline

/**
 * Checks invariants 1 to 3 and 6 from contracts/editing-engine.md. Invariant 1 (gapless, ordered video lane)
 * holds by construction because positions are derived from order, so only lengths are verified here.
 */
object TimelineInvariants {
    fun violations(timeline: Timeline): List<String> = buildList {
        for (clip in timeline.videoLane) {
            if (clip.lengthMs < MIN_CLIP_MS) add("video clip ${clip.id} is shorter than $MIN_CLIP_MS ms")
            val media = timeline.media[clip.mediaItemId]
            if (media == null) {
                add("video clip ${clip.id} refers to unknown media ${clip.mediaItemId}")
            } else if (clip.sourceStartMs < 0 || exceedsSource(media.kind, media.durationMs, clip.sourceEndMs)) {
                add("video clip ${clip.id} trim is outside its source")
            }
        }
        for (clip in timeline.audioLane) {
            if (clip.lengthMs < MIN_CLIP_MS) add("audio clip ${clip.id} is shorter than $MIN_CLIP_MS ms")
            val media = timeline.media[clip.mediaItemId]
            if (media == null) {
                add("audio clip ${clip.id} refers to unknown media ${clip.mediaItemId}")
            } else if (clip.sourceStartMs < 0 || exceedsSource(media.kind, media.durationMs, clip.sourceEndMs)) {
                add("audio clip ${clip.id} trim is outside its source")
            }
        }
        val sorted = timeline.audioLane.sortedBy { it.timelineStartMs }
        sorted.zipWithNext().forEach { (a, b) ->
            if (a.timelineEndMs > b.timelineStartMs) add("audio clips ${a.id} and ${b.id} overlap")
        }
    }

    private fun exceedsSource(kind: MediaKind, durationMs: Long?, endMs: Long) =
        kind != MediaKind.Photo && durationMs != null && endMs > durationMs
}
