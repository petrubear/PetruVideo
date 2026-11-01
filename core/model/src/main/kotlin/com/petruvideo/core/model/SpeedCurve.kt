package com.petruvideo.core.model

import kotlin.math.roundToLong

const val CURVE_SPEED_MIN = 100
const val CURVE_SPEED_MAX = 10_000
const val CURVE_POINTS_MAX = 12
const val CURVE_POINT_GAP_MS = 100L
const val CURVE_LENGTH_MAX_MS = 600_000L

/** A point of a speed curve: [positionMs] into the clip as it plays, and the [speedMilli] there (1000 = 1x). */
data class SpeedPoint(val positionMs: Long, val speedMilli: Int)

/** A piece of a curve played at one constant [speed] (1.0 = 1x) from [startMs] to [endMs] of played source time. */
class SpeedStep(val startMs: Long, val endMs: Long, val speed: Double) {
    val timelineMs: Double get() = (endMs - startMs) / speed
}

/** The named shapes the user can pick; each is a list of (share of the clip, speed) pairs. */
enum class SpeedPreset(val label: String, val shape: List<Pair<Double, Int>>) {
    Montage("Montage", listOf(0.0 to 1_000, 0.2 to 4_000, 0.45 to 500, 0.75 to 4_000, 1.0 to 1_000)),
    Hero("Hero time", listOf(0.0 to 1_000, 0.25 to 2_000, 0.4 to 300, 0.6 to 300, 0.75 to 2_000, 1.0 to 1_000)),
    Bullet("Bullet time", listOf(0.0 to 1_000, 0.35 to 500, 0.5 to 100, 0.65 to 500, 1.0 to 1_000)),
    Jump("Jump", listOf(0.0 to 1_000, 0.5 to 1_000, 0.55 to 300, 1.0 to 300)),
    FlashIn("Flash in", listOf(0.0 to 200, 0.35 to 200, 0.5 to 1_000, 1.0 to 1_000)),
    FlashOut("Flash out", listOf(0.0 to 1_000, 0.5 to 1_000, 0.65 to 200, 1.0 to 200)),
}

/**
 * The speed of a clip along its length (feature 018): points on the clip's played source time, with the speed changing
 * linearly between them. The model, the player, and the export all use the same short constant-speed [steps], so the
 * clip's [timelineMs], [sourceAt], and [timelineAt] agree with what is played.
 */
class SpeedCurve(val points: List<SpeedPoint>) {
    val sourceLengthMs: Long get() = points.last().positionMs

    val isValid: Boolean
        get() = points.size in 2..CURVE_POINTS_MAX && points.first().positionMs == 0L &&
            points.all { it.speedMilli in CURVE_SPEED_MIN..CURVE_SPEED_MAX } &&
            points.zipWithNext().all { (a, b) -> b.positionMs - a.positionMs >= CURVE_POINT_GAP_MS }

    /** The speed (1.0 = 1x) at played source time [positionMs]; the end points hold outside the curve. */
    fun speedAt(positionMs: Double): Double {
        val first = points.first()
        val last = points.last()
        if (positionMs <= first.positionMs) return first.speedMilli / SPEED_DIVISOR
        if (positionMs >= last.positionMs) return last.speedMilli / SPEED_DIVISOR
        val after = points.indexOfFirst { it.positionMs > positionMs }
        val a = points[after - 1]
        val b = points[after]
        val t = (positionMs - a.positionMs) / (b.positionMs - a.positionMs)
        return (a.speedMilli + (b.speedMilli - a.speedMilli) * t) / SPEED_DIVISOR
    }

    /** The constant-speed pieces the curve is played in, in order. */
    val steps: List<SpeedStep> by lazy {
        val out = ArrayList<SpeedStep>()
        var start = 0L
        while (start < sourceLengthMs) {
            val length = (STEP_TIMELINE_MS * speedAt(start.toDouble())).roundToLong().coerceIn(STEP_MIN_MS, STEP_MAX_MS)
            val end = minOf(start + length, sourceLengthMs)
            out += SpeedStep(start, end, speedAt((start + end) / 2.0))
            start = end
        }
        out
    }

    /** The clip's length on the timeline: the time its pieces take at their speeds. */
    val timelineMs: Long by lazy { steps.sumOf { it.timelineMs }.roundToLong() }

    /** The played source time shown [timelineOffsetMs] after the clip's start on the timeline. */
    fun sourceAt(timelineOffsetMs: Long): Long {
        var elapsed = 0.0
        for (step in steps) {
            val next = elapsed + step.timelineMs
            if (timelineOffsetMs < next) return step.startMs + ((timelineOffsetMs - elapsed) * step.speed).roundToLong()
            elapsed = next
        }
        return sourceLengthMs
    }

    /** How long after the clip's start on the timeline the played source time [positionMs] is reached. */
    fun timelineAt(positionMs: Long): Long {
        var elapsed = 0.0
        for (step in steps) {
            if (positionMs < step.endMs) return (elapsed + (positionMs - step.startMs) / step.speed).roundToLong()
            elapsed += step.timelineMs
        }
        return elapsed.roundToLong()
    }

    /**
     * The part of the curve from [fromMs] to [toMs] of played source time, now starting at 0: the points inside, and a
     * point at each edge holding the speed there. Edges beyond the curve hold the end speeds.
     */
    fun windowed(fromMs: Long, toMs: Long): SpeedCurve {
        val inside = points.filter { it.positionMs > fromMs && it.positionMs < toMs }
            .map { it.copy(positionMs = it.positionMs - fromMs) }
        val head = SpeedPoint(0, milliAt(fromMs))
        val tail = SpeedPoint(toMs - fromMs, milliAt(toMs))
        return SpeedCurve(spaced(listOf(head) + inside + tail))
    }

    private fun milliAt(positionMs: Long) = (speedAt(positionMs.toDouble()) * SPEED_DIVISOR).roundToLong().toInt()

    /** The curve with point [index] moved to ([positionMs], [speedMilli]); the ends keep their positions. */
    fun withPointMoved(index: Int, positionMs: Long, speedMilli: Int): SpeedCurve? {
        if (index !in points.indices) return null
        val low = if (index == 0) 0L else points[index - 1].positionMs + CURVE_POINT_GAP_MS
        val high = if (index == points.lastIndex) sourceLengthMs else points[index + 1].positionMs - CURVE_POINT_GAP_MS
        val fixedEnd = index == 0 || index == points.lastIndex
        val position = if (fixedEnd) points[index].positionMs else positionMs.coerceIn(low, maxOf(low, high))
        val moved = SpeedPoint(position, speedMilli.coerceIn(CURVE_SPEED_MIN, CURVE_SPEED_MAX))
        return SpeedCurve(points.toMutableList().also { it[index] = moved })
    }

    /** The curve with a point added at [positionMs] holding the curve's speed there, or null when it does not fit. */
    fun withPointAdded(positionMs: Long): SpeedCurve? {
        if (points.size >= CURVE_POINTS_MAX) return null
        if (points.any { kotlin.math.abs(it.positionMs - positionMs) < CURVE_POINT_GAP_MS }) return null
        if (positionMs <= 0 || positionMs >= sourceLengthMs) return null
        val added = SpeedPoint(positionMs, milliAt(positionMs))
        return SpeedCurve((points + added).sortedBy { it.positionMs })
    }

    /** The curve without point [index]; the two ends cannot be removed. */
    fun withPointRemoved(index: Int): SpeedCurve? =
        if (points.size > 2 && index in 1 until points.lastIndex) {
            SpeedCurve(points.filterIndexed { i, _ -> i != index })
        } else {
            null
        }

    /** Drops inside points that sit closer than the minimum gap to a neighbour, keeping both ends. */
    private fun spaced(list: List<SpeedPoint>): List<SpeedPoint> {
        val kept = ArrayList<SpeedPoint>()
        for (p in list.dropLast(1)) {
            if (kept.isEmpty() || p.positionMs - kept.last().positionMs >= CURVE_POINT_GAP_MS) kept += p
        }
        val end = list.last()
        if (kept.size > 1 && end.positionMs - kept.last().positionMs < CURVE_POINT_GAP_MS) kept.removeAt(kept.lastIndex)
        return kept + end
    }

    override fun equals(other: Any?) = other is SpeedCurve && other.points == points

    override fun hashCode() = points.hashCode()

    override fun toString() = "SpeedCurve($points)"

    companion object {
        private const val SPEED_DIVISOR = 1000.0
        private const val STEP_TIMELINE_MS = 100.0
        private const val STEP_MIN_MS = 20L
        private const val STEP_MAX_MS = 500L

        /** [preset] scaled to a clip whose trimmed source is [sourceLengthMs] long. */
        fun preset(preset: SpeedPreset, sourceLengthMs: Long): SpeedCurve = SpeedCurve(
            preset.shape.map { (share, speed) -> SpeedPoint((share * sourceLengthMs).roundToLong(), speed) },
        )

        /** Text form for storage: `position:speed` pairs joined by `;`. */
        fun encode(curve: SpeedCurve): String = curve.points.joinToString(";") { "${it.positionMs}:${it.speedMilli}" }

        /** The curve stored as [text], or null when it is empty or unreadable. */
        fun decode(text: String?): SpeedCurve? {
            if (text.isNullOrBlank()) return null
            val points = text.split(';').map { pair ->
                val parts = pair.split(':')
                val position = parts.getOrNull(0)?.toLongOrNull() ?: return null
                val speed = parts.getOrNull(1)?.toIntOrNull() ?: return null
                SpeedPoint(position, speed)
            }
            return SpeedCurve(points).takeIf { it.points.size >= 2 }
        }
    }
}
