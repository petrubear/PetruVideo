package com.petruvideo.core.model

const val KEYFRAME_MAX = 50
const val KEYFRAME_MIN_GAP_MS = 33L

/** The values a keyframe animates: place (center, permille of the frame), size, and opacity. */
data class Pose(val xPermille: Int, val yPermille: Int, val sizePermille: Int, val opacityPercent: Int)

/**
 * A moment inside an overlay or text clip, [timeMs] after its start, holding the clip's [pose]. [ease] picks the
 * motion for the stretch to the next keyframe: smooth start and end when true, constant speed when false.
 */
data class Keyframe(val timeMs: Long, val pose: Pose, val ease: Boolean = false)

/** Pure keyframe math shared by the preview, export, and the editing commands. */
object Keyframes {
    /**
     * The shown values [relMs] after the clip's start: [base] with no keyframes, the nearest keyframe's values
     * before the first and after the last, and a blend of the two around [relMs] in between.
     */
    fun poseAt(base: Pose, frames: List<Keyframe>, relMs: Long): Pose {
        if (frames.isEmpty()) return base
        val first = frames.first()
        if (relMs <= first.timeMs) return first.pose
        val last = frames.last()
        if (relMs >= last.timeMs) return last.pose
        val after = frames.indexOfFirst { it.timeMs > relMs }
        val from = frames[after - 1]
        val to = frames[after]
        val linear = (relMs - from.timeMs).toFloat() / (to.timeMs - from.timeMs)
        val t = if (from.ease) linear * linear * (3f - 2f * linear) else linear
        return Pose(
            lerp(from.pose.xPermille, to.pose.xPermille, t),
            lerp(from.pose.yPermille, to.pose.yPermille, t),
            lerp(from.pose.sizePermille, to.pose.sizePermille, t),
            lerp(from.pose.opacityPercent, to.pose.opacityPercent, t),
        )
    }

    private fun lerp(a: Int, b: Int, t: Float) = Math.round(a + (b - a) * t)

    /**
     * The keyframes for the part of a clip from [fromRel] to [toRel] (times relative to the old start), now relative
     * to [fromRel]. Keyframes outside are dropped and the value at each new edge is kept by a keyframe there; a clip
     * without keyframes stays without.
     */
    fun window(base: Pose, frames: List<Keyframe>, fromRel: Long, toRel: Long): List<Keyframe> {
        if (frames.isEmpty()) return frames
        val length = toRel - fromRel
        val inside = frames.filter { it.timeMs in fromRel..toRel }.map { it.copy(timeMs = it.timeMs - fromRel) }
        val before = frames.lastOrNull { it.timeMs <= fromRel }
        val head = if (inside.none { it.timeMs < KEYFRAME_MIN_GAP_MS }) {
            listOf(Keyframe(0, poseAt(base, frames, fromRel), before?.ease ?: false))
        } else {
            emptyList()
        }
        val tail = if (inside.none { it.timeMs > length - KEYFRAME_MIN_GAP_MS }) {
            listOf(Keyframe(length, poseAt(base, frames, toRel)))
        } else {
            emptyList()
        }
        return head + inside + tail
    }

    /**
     * [frames] with a keyframe holding [pose] at [relMs]: changes the keyframe within a frame of that time, or adds
     * one. Null when a new keyframe would pass [KEYFRAME_MAX].
     */
    fun withPoseAt(frames: List<Keyframe>, relMs: Long, pose: Pose): List<Keyframe>? {
        val near = frames.indexOfFirst { kotlin.math.abs(it.timeMs - relMs) < KEYFRAME_MIN_GAP_MS }
        if (near >= 0) return frames.toMutableList().also { it[near] = it[near].copy(pose = pose) }
        if (frames.size >= KEYFRAME_MAX) return null
        return (frames + Keyframe(relMs, pose)).sortedBy { it.timeMs }
    }

    /** Sorts, drops a second keyframe at the same time, and clamps times to the clip and values to their ranges. */
    fun normalized(frames: List<Keyframe>, durationMs: Long, sizeRange: IntRange): List<Keyframe> =
        frames.map {
            Keyframe(
                it.timeMs.coerceIn(0, durationMs),
                Pose(
                    it.pose.xPermille.coerceIn(0, TEXT_PERMILLE),
                    it.pose.yPermille.coerceIn(0, TEXT_PERMILLE),
                    it.pose.sizePermille.coerceIn(sizeRange),
                    it.pose.opacityPercent.coerceIn(0, 100),
                ),
                it.ease,
            )
        }.sortedBy { it.timeMs }.distinctBy { it.timeMs }.take(KEYFRAME_MAX)
}
