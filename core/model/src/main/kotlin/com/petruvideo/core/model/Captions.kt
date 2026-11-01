package com.petruvideo.core.model

import kotlin.math.ceil
import kotlin.math.roundToLong

const val CAPTION_MAX = 200
const val CAPTION_X = 500
const val CAPTION_Y = 880
const val CAPTION_SIZE = 50
const val CAPTION_BOX_OPACITY = 60

/** A spoken phrase: its place in a clip's source and on the timeline (both in milliseconds). */
data class CaptionPhrase(
    val clipId: String,
    val sourceStartMs: Long,
    val sourceEndMs: Long,
    val startMs: Long,
    val endMs: Long,
)

/** What a run of Auto captions adds: the words of a phrase and the time it is spoken on the timeline. */
data class CaptionLine(val startMs: Long, val endMs: Long, val text: String)

/** The phrases found in a project, how many clips could not be used, and whether the 200 limit cut the list. */
data class CaptionPlan(val phrases: List<CaptionPhrase>, val skippedClips: Int, val truncated: Boolean)

/**
 * Finds the spoken phrases of a project from the loudness of its sound (waveform peaks, 20 per second): speech is a
 * stretch above a quiet level, short pauses are bridged, very short sounds are dropped, and long stretches are cut at
 * their quietest point. Times follow the clip's trim, position, and speed.
 */
object CaptionPlanner {
    private const val BIN_MS = 1_000L / DUCK_PEAKS_PER_SECOND
    private const val SPEECH_LEVEL = 0.03f
    private const val BRIDGE_MS = 400L
    private const val MIN_MS = 300L
    private const val MAX_MS = 8_000L
    private const val CUT_WINDOW_MS = 3_000L

    /** What one clip gave: its phrases, or that it had speech that could not be used (reversed, curved, no data). */
    private class ClipOutcome(val phrases: List<CaptionPhrase>, val skipped: Boolean)

    fun plan(timeline: Timeline, peaks: (String) -> FloatArray?): CaptionPlan {
        val outcomes = timeline.videoLane.mapNotNull { outcomeOf(timeline, it, peaks) }
        val sorted = outcomes.flatMap { it.phrases }.sortedBy { it.startMs }
        return CaptionPlan(sorted.take(CAPTION_MAX), outcomes.count { it.skipped }, sorted.size > CAPTION_MAX)
    }

    private fun outcomeOf(timeline: Timeline, clip: VideoClip, peaks: (String) -> FloatArray?): ClipOutcome? {
        val media = timeline.media[clip.mediaItemId]
        val usable = clip.attachedAudio && media != null && media.kind == MediaKind.Video && media.hasAudio &&
            !clip.muted && clip.volumePercent > 0
        if (!usable) return null
        val levels = peaks(clip.mediaItemId)
        if (clip.reversed || clip.speedCurve != null || levels == null) return ClipOutcome(emptyList(), true)
        val start = timeline.videoStartOf(clip.id) ?: return null
        return ClipOutcome(phrasesOf(clip, start, levels), false)
    }

    private fun phrasesOf(clip: VideoClip, clipStart: Long, levels: FloatArray): List<CaptionPhrase> {
        val first = clip.sourceStartMs / BIN_MS
        val last = ceil(clip.sourceEndMs / BIN_MS.toDouble()).toLong()
        val loud = BooleanArray((last - first).toInt().coerceAtLeast(0)) { i ->
            val level = levels.getOrNull((first + i).toInt()) ?: 0f
            level * clip.volumePercent / PERCENT >= SPEECH_LEVEL
        }
        return runsOf(loud, levels, first).map { (from, to) ->
            val startSource = (from * BIN_MS).coerceAtLeast(clip.sourceStartMs)
            val endSource = (to * BIN_MS).coerceAtMost(clip.sourceEndMs)
            fun onTimeline(source: Long): Long {
                val played = (source - clip.sourceStartMs) * SPEED_UNIT / clip.speedMilli.toDouble()
                return clipStart + played.roundToLong()
            }
            CaptionPhrase(clip.id, startSource, endSource, onTimeline(startSource), onTimeline(endSource))
        }.filter { it.endMs - it.startMs >= MIN_MS }
    }

    /** Bridged runs of speech as (first bin, bin after the last), long ones cut at their quietest point. */
    private fun runsOf(loud: BooleanArray, levels: FloatArray, offset: Long): List<Pair<Long, Long>> {
        val runs = ArrayList<Pair<Long, Long>>()
        var i = 0
        while (i < loud.size) {
            if (!loud[i]) { i++; continue }
            var end = i
            var gap = 0
            var j = i
            while (j < loud.size && gap * BIN_MS < BRIDGE_MS) {
                if (loud[j]) { end = j; gap = 0 } else gap++
                j++
            }
            runs += cutLong(offset + i, offset + end + 1, levels)
            i = end + 1
        }
        return runs
    }

    private fun cutLong(from: Long, to: Long, levels: FloatArray): List<Pair<Long, Long>> {
        if ((to - from) * BIN_MS <= MAX_MS) return listOf(from to to)
        val window = CUT_WINDOW_MS / BIN_MS
        val limit = from + MAX_MS / BIN_MS
        val cut = (limit - window..limit).minByOrNull { levels.getOrNull(it.toInt()) ?: 0f } ?: limit
        return listOf(from to cut) + cutLong(cut, to, levels)
    }

    private const val PERCENT = 100f
}
