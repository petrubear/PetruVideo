package com.petruvideo.core.model

const val FRAMING_ZOOM_MIN = 1000
const val FRAMING_ZOOM_MAX = 4000

/** The look of a video clip without keyframes: centered, filling the frame, fully opaque. */
val FRAMING_NEUTRAL = Pose(TEXT_PERMILLE / 2, TEXT_PERMILLE / 2, FRAMING_ZOOM_MIN, 100)

/**
 * How a [Pose] frames a video clip: [Pose.sizePermille] is the zoom (1000 fills the frame), x and y the point of the
 * clip at the frame center, and opacity darkens the picture toward black.
 */
object Framing {
    /** [pose] limited to the zoom range, with the shown window kept inside the clip so no edge shows. */
    fun clamp(pose: Pose): Pose {
        val zoom = pose.sizePermille.coerceIn(FRAMING_ZOOM_MIN, FRAMING_ZOOM_MAX)
        val half = HALF_WINDOW / zoom
        return Pose(
            pose.xPermille.coerceIn(half, TEXT_PERMILLE - half),
            pose.yPermille.coerceIn(half, TEXT_PERMILLE - half),
            zoom,
            pose.opacityPercent.coerceIn(0, 100),
        )
    }

    private const val HALF_WINDOW = 500_000
}

/** This clip, already cut to its new edges, with the keyframes of the part [fromRel] to [toRel] of its old time. */
fun VideoClip.reframed(old: List<Keyframe>, fromRel: Long, toRel: Long): VideoClip =
    if (old.isEmpty()) this else withKeyframes(Keyframes.window(FRAMING_NEUTRAL, old, fromRel, toRel))

/** This clip at its new speed, with the keyframes of the clip that was [oldLengthMs] long scaled to the new length. */
fun VideoClip.rescaled(old: List<Keyframe>, oldLengthMs: Long): VideoClip {
    if (old.isEmpty() || oldLengthMs <= 0) return this
    val scaled = old.map { it.copy(timeMs = it.timeMs * lengthMs / oldLengthMs) }
    val spaced = scaled.fold(emptyList<Keyframe>()) { kept, k ->
        if (kept.isNotEmpty() && k.timeMs - kept.last().timeMs < KEYFRAME_MIN_GAP_MS) kept else kept + k
    }
    return withKeyframes(spaced)
}
