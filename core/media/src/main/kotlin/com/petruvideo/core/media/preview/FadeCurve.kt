package com.petruvideo.core.media.preview

import kotlin.math.PI
import kotlin.math.cos

/** The fade gain of a clip at a frame: a raised-cosine rise over the first frames and fall over the last ones. */
object FadeCurve {
    fun factor(frame: Long, fadeInFrames: Long, fadeOutFrames: Long, totalFrames: Long): Float {
        var gain = 1f
        if (fadeInFrames > 0 && frame < fadeInFrames) gain *= ramp(frame.toFloat() / fadeInFrames)
        val untilEnd = totalFrames - frame
        if (fadeOutFrames > 0 && untilEnd < fadeOutFrames) {
            gain *= ramp(untilEnd.coerceAtLeast(0).toFloat() / fadeOutFrames)
        }
        return gain
    }

    private fun ramp(t: Float) = (0.5 - 0.5 * cos(PI * t.coerceIn(0f, 1f))).toFloat()
}
