package com.petruvideo.core.model.export

import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** Video bitrates per resolution, frame rate, and quality, and the size estimate built from them. */
object BitrateTable {
    const val AUDIO_BITRATE = 128_000
    private const val REFERENCE_FPS = 30.0
    private const val FPS_EXPONENT = 0.75
    private const val MBIT = 1_000_000

    private fun recommendedAt30(resolution: Resolution): Int = when (resolution) {
        Resolution.R720 -> 6 * MBIT
        Resolution.R1080 -> 12 * MBIT
        Resolution.R2K -> 20 * MBIT
        Resolution.R4K -> 45 * MBIT
    }

    fun videoBitrate(resolution: Resolution, frameRate: FrameRate, quality: Quality): Int =
        (recommendedAt30(resolution) * (frameRate.fps / REFERENCE_FPS).pow(FPS_EXPONENT) * quality.bitrateFactor)
            .roundToInt()

    /** Estimated file size for [durationMs] at [videoBitrate] (use the clamped value the encoder will really use). */
    fun estimatedBytes(videoBitrate: Int, durationMs: Long): Long =
        ((videoBitrate + AUDIO_BITRATE) * (durationMs / 1000.0) / 8.0).roundToLong()
}
