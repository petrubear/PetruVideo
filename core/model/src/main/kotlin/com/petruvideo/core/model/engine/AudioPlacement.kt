package com.petruvideo.core.model.engine

import com.petruvideo.core.model.AudioClip

/** How many audio clips may play at the same moment (feature 027). */
const val MAX_AUDIO_LAYERS = 3

/** Placement rule shared by every command that puts an audio clip on the lane (FR-020d). */
object AudioPlacement {
    /** Whether a clip at [startMs] for [lengthMs] keeps every moment at [MAX_AUDIO_LAYERS] clips or fewer. */
    fun fits(lane: List<AudioClip>, startMs: Long, lengthMs: Long, ignoreClipId: String? = null): Boolean {
        val others = lane.filter { it.id != ignoreClipId }
        val end = startMs + lengthMs
        // The number playing only changes where a clip starts, so those moments are all that need checking.
        val moments = listOf(startMs) + others.map { it.timelineStartMs }.filter { it > startMs && it < end }
        return moments.all { t -> others.count { it.timelineStartMs <= t && t < it.timelineEndMs } < MAX_AUDIO_LAYERS }
    }

    /**
     * The earliest start at or after [desiredStartMs] where a clip of [lengthMs] keeps every moment at
     * [MAX_AUDIO_LAYERS] clips or fewer (ignoring [ignoreClipId], e.g. the clip being moved).
     */
    fun nextFreeStart(lane: List<AudioClip>, desiredStartMs: Long, lengthMs: Long, ignoreClipId: String? = null): Long {
        val desired = desiredStartMs.coerceAtLeast(0)
        val others = lane.filter { it.id != ignoreClipId }
        val candidates = (listOf(desired) + others.map { it.timelineEndMs }.filter { it > desired }).distinct().sorted()
        return candidates.firstOrNull { fits(lane, it, lengthMs, ignoreClipId) } ?: candidates.last()
    }

    /** The first moment at or after [startMs] where [MAX_AUDIO_LAYERS] other clips already play, or null. */
    fun firstFullMoment(lane: List<AudioClip>, startMs: Long, ignoreClipId: String?): Long? {
        val others = lane.filter { it.id != ignoreClipId }
        val moments = (listOf(startMs) + others.map { it.timelineStartMs }.filter { it > startMs }).sorted()
        return moments.firstOrNull { t ->
            others.count { it.timelineStartMs <= t && t < it.timelineEndMs } >= MAX_AUDIO_LAYERS
        }
    }

    /** The row (0 up) each clip is drawn and played on: the first row whose last clip has ended. */
    fun rows(lane: List<AudioClip>): Map<String, Int> {
        val rowEnds = mutableListOf<Long>()
        val rows = LinkedHashMap<String, Int>()
        for (clip in lane.sortedWith(compareBy({ it.timelineStartMs }, { it.id }))) {
            val free = rowEnds.indexOfFirst { it <= clip.timelineStartMs }
            val row = if (free >= 0) free else rowEnds.size.also { rowEnds += 0L }
            rowEnds[row] = clip.timelineEndMs
            rows[clip.id] = row
        }
        return rows
    }
}
