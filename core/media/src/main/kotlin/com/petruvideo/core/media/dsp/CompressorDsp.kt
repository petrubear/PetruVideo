package com.petruvideo.core.media.dsp

import com.petruvideo.core.media.preview.ReverbDsp
import com.petruvideo.core.model.Compressor
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

/**
 * Feed-forward compressor (feature 034). The loudest channel drives one shared gain, so the stereo picture does not
 * shift. Above the threshold the level rises 1 dB per ratio dB, with a soft knee; the gain follows with the attack
 * and release times. The manual level is smoothed so a drag never clicks, and the result goes through the shared
 * soft limiter so loud sound is never clipped hard. A compressor that does nothing passes the audio untouched.
 */
class CompressorDsp(private val sampleRate: Int, private val channels: Int) {
    @Volatile private var settings = Compressor()
    private var reductionDb = 0f
    private var levelLinear = 1f
    private var wasOn = false

    fun setSettings(value: Compressor) {
        settings = value
    }

    /** True when the compressor changes the sound. */
    val active: Boolean get() = settings.audible

    fun reset() {
        reductionDb = 0f
        levelLinear = dbToLinear(settings.levelDb.toFloat())
        wasOn = false
    }

    fun process(buffer: FloatArray, frames: Int) {
        val now = settings
        if (!now.audible) {
            wasOn = false
            return
        }
        if (!wasOn) {
            reset()
            wasOn = true
        }
        val threshold = now.thresholdDb.toFloat()
        val ratio = now.ratioTenths / RATIO_UNIT
        val attack = coefficient(now.attackMs)
        val release = coefficient(now.releaseMs)
        val levelTarget = dbToLinear(now.levelDb.toFloat())
        for (frame in 0 until frames) {
            val base = frame * channels
            var peak = 0f
            for (ch in 0 until channels) peak = max(peak, abs(buffer[base + ch]))
            val inputDb = 20f * log10(max(peak, SILENCE))
            val target = gainDb(inputDb, threshold, ratio)
            val smoothing = if (target < reductionDb) attack else release
            reductionDb = target + smoothing * (reductionDb - target)
            levelLinear += (levelTarget - levelLinear) * LEVEL_STEP
            val gain = dbToLinear(reductionDb) * levelLinear
            for (ch in 0 until channels) buffer[base + ch] = ReverbDsp.limit(buffer[base + ch] * gain)
        }
    }

    /** The gain in dB (0 or below) for an input level, with a soft knee around the threshold. */
    private fun gainDb(inputDb: Float, threshold: Float, ratio: Float): Float {
        val over = inputDb - threshold
        val slope = 1f / ratio - 1f
        return when {
            2f * over < -KNEE_DB -> 0f
            2f * abs(over) <= KNEE_DB -> slope * (over + KNEE_DB / 2f).let { it * it } / (2f * KNEE_DB)
            else -> slope * over
        }
    }

    private fun coefficient(ms: Int): Float = exp(-1f / (ms / MS_PER_SECOND * sampleRate))

    private fun dbToLinear(db: Float): Float = 10f.pow(db / 20f)

    private companion object {
        const val RATIO_UNIT = 10f
        const val KNEE_DB = 6f
        const val SILENCE = 1e-6f
        const val MS_PER_SECOND = 1_000f
        const val LEVEL_STEP = 0.002f
    }
}
