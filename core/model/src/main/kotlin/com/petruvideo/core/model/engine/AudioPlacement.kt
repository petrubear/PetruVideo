package com.petruvideo.core.model.engine

import com.petruvideo.core.model.AudioClip

/** Placement rule shared by every command that puts an audio clip on the lane (FR-020d). */
object AudioPlacement {
    /**
     * The earliest start at or after [desiredStartMs] where a clip of [lengthMs] does not overlap any clip in
     * [lane] (ignoring [ignoreClipId], e.g. the clip being moved).
     */
    fun nextFreeStart(lane: List<AudioClip>, desiredStartMs: Long, lengthMs: Long, ignoreClipId: String? = null): Long {
        var start = desiredStartMs.coerceAtLeast(0)
        for (clip in lane.filter { it.id != ignoreClipId }.sortedBy { it.timelineStartMs }) {
            if (clip.timelineEndMs <= start) continue
            if (clip.timelineStartMs >= start + lengthMs) break
            start = clip.timelineEndMs
        }
        return start
    }
}
