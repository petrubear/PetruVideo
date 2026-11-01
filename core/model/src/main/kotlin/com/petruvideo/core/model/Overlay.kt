package com.petruvideo.core.model

const val OVERLAY_WIDTH_MIN = 50
const val OVERLAY_WIDTH_MAX = 1_000
const val OVERLAY_WIDTH_DEFAULT = 350
const val OVERLAY_DEFAULT_MS = 5_000L
const val OVERLAY_MIN_MS = 500L

/**
 * A picture-in-picture clip: a video or photo shown over the main video. Its place is given as shares of the frame
 * (center, and width as a share of the frame width), so it looks the same at any export size. It has no sound of its
 * own and plays at normal speed.
 */
data class OverlayClip(
    val id: String,
    val mediaItemId: String,
    val sourceStartMs: Long,
    val sourceEndMs: Long,
    val startMs: Long,
    val xPermille: Int = 750,
    val yPermille: Int = 250,
    val widthPermille: Int = OVERLAY_WIDTH_DEFAULT,
    val opacityPercent: Int = 100,
    val keyframes: List<Keyframe> = emptyList(),
    /** Removes a backdrop colour from the picture (feature 022). */
    val chroma: ChromaKey = ChromaKey(),
) {
    val durationMs: Long get() = sourceEndMs - sourceStartMs
    val endMs: Long get() = startMs + durationMs

    /** The plain place, width, and opacity as a pose; used when there are no keyframes. */
    val basePose: Pose get() = Pose(xPermille, yPermille, widthPermille, opacityPercent)

    /** The shown values [relMs] after the clip's start. */
    fun poseAt(relMs: Long): Pose = Keyframes.poseAt(basePose, keyframes, relMs)

    /** The clip as the preview box and sliders show it at [timeMs]: plain values set to the pose there. */
    fun shownAt(timeMs: Long): OverlayClip {
        if (keyframes.isEmpty()) return this
        val pose = poseAt((timeMs - startMs).coerceIn(0, durationMs))
        return copy(
            xPermille = pose.xPermille, yPermille = pose.yPermille,
            widthPermille = pose.sizePermille, opacityPercent = pose.opacityPercent,
        )
    }

    /** The clip with [frames]; the plain values follow the first keyframe, or [plain] when the list is empty. */
    fun withKeyframes(frames: List<Keyframe>, plain: Pose? = null): OverlayClip {
        val pose = frames.firstOrNull()?.pose ?: plain ?: basePose
        return copy(
            keyframes = frames,
            xPermille = pose.xPermille,
            yPermille = pose.yPermille,
            widthPermille = pose.sizePermille,
            opacityPercent = pose.opacityPercent,
        )
    }

    fun normalized() = copy(
        startMs = startMs.coerceAtLeast(0),
        xPermille = xPermille.coerceIn(0, TEXT_PERMILLE),
        yPermille = yPermille.coerceIn(0, TEXT_PERMILLE),
        widthPermille = widthPermille.coerceIn(OVERLAY_WIDTH_MIN, OVERLAY_WIDTH_MAX),
        opacityPercent = opacityPercent.coerceIn(0, 100),
        keyframes = Keyframes.normalized(keyframes, durationMs, OVERLAY_WIDTH_MIN..OVERLAY_WIDTH_MAX),
        chroma = chroma.normalized(),
    )

    val inRange: Boolean get() = normalized() == this && durationMs >= OVERLAY_MIN_MS && sourceStartMs >= 0
}
