package com.petruvideo.core.media.dsp

import com.petruvideo.core.media.preview.ReverbDsp
import kotlin.math.abs

/**
 * Three-band equalizer: low shelf 200 Hz, peak 1 kHz, high shelf 4 kHz. Band gains move toward their targets in
 * small steps per block, so a drag never clicks. Flat settings pass the audio through untouched.
 */
class EqualizerDsp(private val sampleRate: Int, private val channels: Int) {
    private val low = Array(channels) { Biquad() }
    private val mid = Array(channels) { Biquad() }
    private val high = Array(channels) { Biquad() }
    private val current = FloatArray(BANDS)
    private val target = FloatArray(BANDS)

    @Volatile private var pendingLow = 0
    @Volatile private var pendingMid = 0
    @Volatile private var pendingHigh = 0

    fun setGains(lowDb: Int, midDb: Int, highDb: Int) {
        pendingLow = lowDb
        pendingMid = midDb
        pendingHigh = highDb
    }

    fun reset() {
        (low + mid + high).forEach { it.reset() }
    }

    fun process(buffer: FloatArray, frames: Int) {
        target[0] = pendingLow.toFloat()
        target[1] = pendingMid.toFloat()
        target[2] = pendingHigh.toFloat()
        var moving = false
        for (i in 0 until BANDS) {
            val step = (target[i] - current[i]).coerceIn(-STEP_DB, STEP_DB)
            if (step != 0f) moving = true
            current[i] += step
        }
        val flat = current.all { abs(it) < FLAT_DB } && !moving
        if (flat) return
        if (moving) updateCoefficients()
        for (frame in 0 until frames) {
            for (ch in 0 until channels) {
                val index = frame * channels + ch
                val x = mid[ch].process(low[ch].process(high[ch].process(buffer[index])))
                buffer[index] = ReverbDsp.limit(x)
            }
        }
    }

    private fun updateCoefficients() {
        for (ch in 0 until channels) {
            low[ch].lowShelf(sampleRate, LOW_HZ, current[0])
            mid[ch].peaking(sampleRate, MID_HZ, current[1], MID_Q)
            high[ch].highShelf(sampleRate, HIGH_HZ, current[2])
        }
    }

    private companion object {
        const val BANDS = 3
        const val STEP_DB = 1.5f
        const val FLAT_DB = 0.01f
        const val LOW_HZ = 200f
        const val MID_HZ = 1000f
        const val MID_Q = 0.7f
        const val HIGH_HZ = 4000f
    }
}
