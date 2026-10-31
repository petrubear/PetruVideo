package com.petruvideo.core.model.engine

import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.ProjectFormat
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

private const val MAX_SHORT_SIDE = 1080
private const val DEFAULT_FPS = 30
private val SUPPORTED_FPS = listOf(24, 30, 60)

/**
 * Format of a new project, taken from its first video or photo (research 4): the display aspect ratio, scaled
 * down so the short side is at most 1080 (never upscaled), even dimensions, and a frame rate rounded to the
 * nearest of 24, 30 and 60 (30 when unknown). [MediaItem.width] and [MediaItem.height] are display sizes, so
 * rotation is already applied.
 */
fun deriveFormat(first: MediaItem): ProjectFormat {
    val width = requireNotNull(first.width) { "format needs a video or photo" }
    val height = requireNotNull(first.height) { "format needs a video or photo" }
    val scale = min(1.0, MAX_SHORT_SIDE.toDouble() / min(width, height))
    return ProjectFormat(
        width = evenDown((width * scale).roundToInt()),
        height = evenDown((height * scale).roundToInt()),
        fps = nearestSupportedFps(first.frameRate),
    )
}

private fun evenDown(value: Int) = value - value % 2

private fun nearestSupportedFps(fps: Float?): Int =
    if (fps == null) DEFAULT_FPS else SUPPORTED_FPS.minBy { abs(it - fps) }
