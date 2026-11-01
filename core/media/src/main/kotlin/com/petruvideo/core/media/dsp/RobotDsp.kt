package com.petruvideo.core.media.dsp

import kotlin.math.PI
import kotlin.math.sin

/** Robot voice: ring modulation at 70 Hz mixed with the dry voice, plus a short comb. Intensity is 0 to 100. */
class RobotDsp(private val sampleRate: Int, private val channels: Int) {
    private val comb = Array(channels) { FloatArray(sampleRate * COMB_MS / 1000 + 1) }
    private var combIndex = 0
    private var phase = 0.0
    @Volatile private var intensity = 0

    fun setIntensity(percent: Int) {
        intensity = percent
    }

    fun reset() {
        comb.forEach { it.fill(0f) }
        combIndex = 0
        phase = 0.0
    }

    fun process(buffer: FloatArray, frames: Int) {
        val mix = intensity / 100f
        if (mix == 0f) return
        val step = 2 * PI * CARRIER_HZ / sampleRate
        val size = comb[0].size
        for (frame in 0 until frames) {
            val carrier = sin(phase).toFloat()
            phase += step
            if (phase > 2 * PI) phase -= 2 * PI
            for (ch in 0 until channels) {
                val index = frame * channels + ch
                val x = buffer[index]
                val ring = x * carrier
                val mixed = x * (1 - mix) + ring * mix
                val delayed = comb[ch][combIndex]
                val out = mixed + delayed * COMB_FEEDBACK * mix
                comb[ch][combIndex] = out
                buffer[index] = out.coerceIn(-1f, 1f)
            }
            combIndex = (combIndex + 1) % size
        }
    }

    private companion object {
        const val CARRIER_HZ = 70.0
        const val COMB_MS = 3
        const val COMB_FEEDBACK = 0.4f
    }
}
