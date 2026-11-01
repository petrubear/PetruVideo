package com.petruvideo.core.model.engine

import com.petruvideo.core.model.AudioFx
import com.petruvideo.core.model.MIN_CLIP_MS
import com.petruvideo.core.model.SPEED_MAX_MILLI
import com.petruvideo.core.model.SPEED_MIN_MILLI
import com.petruvideo.core.model.SPEED_UNIT
import com.petruvideo.core.model.VOLUME_MAX_PERCENT
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.Reverb
import com.petruvideo.core.model.Timeline

/**
 * Checks invariants 1 to 3 and 6 from contracts/editing-engine.md. Invariant 1 (gapless, ordered video lane)
 * holds by construction because positions are derived from order, so only lengths are verified here.
 */
object TimelineInvariants {
    fun violations(timeline: Timeline): List<String> = buildList {
        for (clip in timeline.videoLane) {
            val media = timeline.media[clip.mediaItemId]
            addAll(
                clipViolations(
                    "video", clip.id, clip.lengthMs, clip.speedMilli, clip.volumePercent, media,
                ),
            )
            addAll(soundViolations("video", clip.id, clip.reverb, clip.fx))
            if (!clip.filter.inRange) add("video clip ${clip.id} filter is out of range")
            addAll(effectViolations(clip))
            if (media?.kind == MediaKind.Photo && clip.speedMilli != SPEED_UNIT) {
                add("photo clip ${clip.id} has a speed")
            }
            if (media != null && outsideSource(media, clip.sourceStartMs, clip.sourceEndMs)) {
                add("video clip ${clip.id} trim is outside its source")
            }
        }
        if (timeline.videoLane.lastOrNull()?.transitionOut != null) add("the last video clip has a transition")
        for (clip in timeline.audioLane) {
            val media = timeline.media[clip.mediaItemId]
            addAll(
                clipViolations(
                    "audio", clip.id, clip.lengthMs, clip.speedMilli, clip.volumePercent, media,
                ),
            )
            addAll(soundViolations("audio", clip.id, clip.reverb, clip.fx))
            if (media != null && outsideSource(media, clip.sourceStartMs, clip.sourceEndMs)) {
                add("audio clip ${clip.id} trim is outside its source")
            }
        }
        addAll(extraLaneViolations(timeline))
        val sorted = timeline.audioLane.sortedBy { it.timelineStartMs }
        sorted.zipWithNext().forEach { (a, b) ->
            if (a.timelineEndMs > b.timelineStartMs) add("audio clips ${a.id} and ${b.id} overlap")
        }
    }

    private fun effectViolations(clip: com.petruvideo.core.model.VideoClip) = buildList {
        if (clip.effect?.inRange == false) add("video clip ${clip.id} effect is out of range")
        if (clip.transitionOut?.inRange == false) add("video clip ${clip.id} transition is out of range")
    }

    private fun extraLaneViolations(timeline: Timeline) = buildList {
        for (clip in timeline.overlayLane) {
            if (!clip.inRange) add("overlay clip ${clip.id} is out of range")
            if (clip.mediaItemId !in timeline.media) add("overlay clip ${clip.id} refers to unknown media")
        }
        for (clip in timeline.textLane) {
            if (!clip.inRange) add("text clip ${clip.id} is out of range")
        }
    }

    private fun clipViolations(
        kind: String,
        id: String,
        lengthMs: Long,
        speedMilli: Int,
        volumePercent: Int,
        media: MediaItem?,
    ) = buildList {
        if (lengthMs < MIN_CLIP_MS) add("$kind clip $id is shorter than $MIN_CLIP_MS ms")
        if (media == null) add("$kind clip $id refers to unknown media")
        if (speedMilli !in SPEED_MIN_MILLI..SPEED_MAX_MILLI) add("$kind clip $id speed is out of range")
        if (volumePercent !in 0..VOLUME_MAX_PERCENT) add("$kind clip $id volume is out of range")
    }

    private fun soundViolations(kind: String, id: String, reverb: Reverb, fx: AudioFx) = buildList {
        if (!reverb.inRange) add("$kind clip $id reverb is out of range")
        if (!fx.inRange) add("$kind clip $id audio effects are out of range")
    }

    private fun outsideSource(media: MediaItem, startMs: Long, endMs: Long) =
        startMs < 0 || exceedsSource(media.kind, media.durationMs, endMs)

    private fun exceedsSource(kind: MediaKind, durationMs: Long?, endMs: Long) =
        kind != MediaKind.Photo && durationMs != null && endMs > durationMs
}
