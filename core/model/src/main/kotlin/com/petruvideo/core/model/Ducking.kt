package com.petruvideo.core.model

import kotlin.math.exp

const val DUCK_AMOUNT_DEFAULT = 60
const val DUCK_STEP_MS = 20
const val DUCK_PEAKS_PER_SECOND = 20

/** How quickly a ducked clip goes down ([attackMs]) and comes back ([releaseMs]); both are time constants. */
enum class DuckSmoothness(val label: String, val attackMs: Int, val releaseMs: Int) {
    Quick("Quick", 60, 250),
    Medium("Medium", 150, 600),
    Slow("Slow", 300, 1200),
}

/**
 * Ducking of one audio clip: lowered by [amountPercent] (100 = a quiet background, never silence) while other
 * sound plays.
 */
data class Ducking(
    val enabled: Boolean = false,
    val amountPercent: Int = DUCK_AMOUNT_DEFAULT,
    val smoothness: DuckSmoothness = DuckSmoothness.Medium,
) {
    fun normalized() = copy(amountPercent = amountPercent.coerceIn(0, 100))

    val inRange: Boolean get() = amountPercent in 0..100
}

/** The gain (0.1 to 1) of a ducked clip every [stepMs] from its start. */
class DuckEnvelope(val stepMs: Int, val gains: FloatArray) {
    /** The gain [relMs] after the clip's start, interpolated between steps; the ends hold. */
    fun gainAt(relMs: Long): Float {
        if (gains.isEmpty()) return 1f
        val position = relMs.coerceAtLeast(0).toDouble() / stepMs
        val index = position.toInt()
        if (index >= gains.lastIndex) return gains.last()
        val fraction = (position - index).toFloat()
        return gains[index] + (gains[index + 1] - gains[index]) * fraction
    }
}

/**
 * Works out when a ducked clip is lowered: whenever another sound is audible (a video clip's sound, or an audio-lane
 * clip that is not ducking itself), looking ahead by the attack time and smoothing the gain.
 */
object DuckingPlanner {
    private const val THRESHOLD = 0.04f
    private const val MAX_REDUCTION = 0.9f

    /** A sound that can trigger ducking: where it plays on the timeline and where in its source each moment is. */
    private class Trigger(
        val startMs: Long,
        val lengthMs: Long,
        val volumePercent: Int,
        val peaks: FloatArray,
        val sourceAt: (offsetMs: Long) -> Long,
    ) {
        fun levelAt(timeMs: Long): Float {
            if (timeMs < startMs || timeMs >= startMs + lengthMs) return 0f
            val source = sourceAt(timeMs - startMs)
            val peak = peaks.getOrNull((source * DUCK_PEAKS_PER_SECOND / MS_PER_SECOND).toInt()) ?: return 0f
            return peak * volumePercent / PERCENT
        }
    }

    /** The envelope for the ducked clip [clipId], or null when it is not ducking; [peaks] gives a media's peaks. */
    fun envelopeFor(timeline: Timeline, clipId: String, peaks: (String) -> FloatArray?): DuckEnvelope? {
        val clip = timeline.audioLane.firstOrNull { it.id == clipId }?.takeIf { it.ducking.enabled } ?: return null
        val triggers = triggersFor(timeline, clip, peaks)
        val steps = (clip.lengthMs / DUCK_STEP_MS).toInt() + 2
        val active = BooleanArray(steps) { i ->
            val time = clip.timelineStartMs + i.toLong() * DUCK_STEP_MS
            triggers.any { it.levelAt(time) >= THRESHOLD }
        }
        val lookAhead = clip.ducking.smoothness.attackMs / DUCK_STEP_MS
        val lowered = clip.ducking.amountPercent.coerceIn(0, 100) / PERCENT
        val low = 1f - MAX_REDUCTION * lowered
        val target = FloatArray(steps)
        var nextActive = Int.MAX_VALUE
        for (i in steps - 1 downTo 0) {
            if (active[i]) nextActive = i
            target[i] = if (nextActive != Int.MAX_VALUE && nextActive - i <= lookAhead) low else 1f
        }
        return DuckEnvelope(DUCK_STEP_MS, smooth(target, clip.ducking.smoothness))
    }

    /** Whether any sound that could trigger ducking overlaps [clip] in time (muted and silent clips do not count). */
    fun hasTriggerOver(timeline: Timeline, clip: AudioClip): Boolean {
        val overlapsVideo = timeline.videoLane.any { video ->
            val start = timeline.videoStartOf(video.id) ?: return@any false
            video.attachedAudio && video.audible && start < clip.timelineEndMs &&
                start + video.lengthMs > clip.timelineStartMs
        }
        return overlapsVideo || timeline.audioLane.any {
            it.id != clip.id && !it.ducking.enabled && it.audible &&
                it.timelineStartMs < clip.timelineEndMs && it.timelineEndMs > clip.timelineStartMs
        }
    }

    private fun smooth(target: FloatArray, smoothness: DuckSmoothness): FloatArray {
        val attack = 1f - exp(-DUCK_STEP_MS.toFloat() / smoothness.attackMs)
        val release = 1f - exp(-DUCK_STEP_MS.toFloat() / smoothness.releaseMs)
        var gain = target.firstOrNull() ?: 1f
        return FloatArray(target.size) { i ->
            gain += (target[i] - gain) * if (target[i] < gain) attack else release
            gain
        }
    }

    private fun triggersFor(timeline: Timeline, target: AudioClip, peaks: (String) -> FloatArray?): List<Trigger> {
        val fromVideo = timeline.videoLane.mapNotNull { clip ->
            if (!clip.attachedAudio || !clip.audible) return@mapNotNull null
            val start = timeline.videoStartOf(clip.id) ?: return@mapNotNull null
            peaks(clip.mediaItemId)?.let {
                Trigger(start, clip.lengthMs, clip.volumePercent, it, clip::sourceTimeAt)
            }
        }
        val fromAudio = timeline.audioLane.mapNotNull { clip ->
            if (clip.id == target.id || clip.ducking.enabled || !clip.audible) return@mapNotNull null
            peaks(clip.mediaItemId)?.let {
                Trigger(clip.timelineStartMs, clip.lengthMs, clip.volumePercent, it) { offset ->
                    clip.sourceStartMs + offset * clip.speedMilli / SPEED_UNIT
                }
            }
        }
        return fromVideo + fromAudio
    }

    private const val PERCENT = 100f
    private const val MS_PER_SECOND = 1000L
}

private val VideoClip.audible: Boolean get() = !muted && volumePercent > 0
private val AudioClip.audible: Boolean get() = !muted && volumePercent > 0
