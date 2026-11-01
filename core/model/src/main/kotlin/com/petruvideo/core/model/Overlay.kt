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
) {
    val durationMs: Long get() = sourceEndMs - sourceStartMs
    val endMs: Long get() = startMs + durationMs

    fun normalized() = copy(
        startMs = startMs.coerceAtLeast(0),
        xPermille = xPermille.coerceIn(0, TEXT_PERMILLE),
        yPermille = yPermille.coerceIn(0, TEXT_PERMILLE),
        widthPermille = widthPermille.coerceIn(OVERLAY_WIDTH_MIN, OVERLAY_WIDTH_MAX),
        opacityPercent = opacityPercent.coerceIn(0, 100),
    )

    val inRange: Boolean get() = normalized() == this && durationMs >= OVERLAY_MIN_MS && sourceStartMs >= 0
}
