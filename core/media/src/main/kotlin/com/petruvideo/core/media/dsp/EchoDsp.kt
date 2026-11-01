package com.petruvideo.core.media.dsp

import com.petruvideo.core.media.preview.ReverbDsp

/**
 * Multi-tap echo: the dry sound plus [repeats] copies at multiples of the delay, each 0.55 times the one before.
 * Only the input is repeated, so nothing sounds after the clip ends.
 */
class EchoDsp(sampleRate: Int, private val channels: Int) {
    private val capacity = sampleRate * MAX_MS / 1000 * MAX_REPEATS + 1
    private val history = Array(channels) { FloatArray(capacity) }
    private val rate = sampleRate
    private var write = 0
    @Volatile private var delayMs = 250
    @Volatile private var repeats = 3

    fun setSettings(delayMs: Int, repeats: Int) {
        this.delayMs = delayMs
        this.repeats = repeats
    }

    fun reset() {
        history.forEach { it.fill(0f) }
        write = 0
    }

    fun process(buffer: FloatArray, frames: Int) {
        val delay = (delayMs * rate / 1000).coerceAtLeast(1)
        val taps = repeats
        for (frame in 0 until frames) {
            for (ch in 0 until channels) {
                val index = frame * channels + ch
                val dry = buffer[index]
                var out = dry
                var gain = TAP_GAIN
                for (k in 1..taps) {
                    val read = (write - k * delay).mod(capacity)
                    out += history[ch][read] * gain
                    gain *= TAP_GAIN
                }
                history[ch][write] = dry
                buffer[index] = ReverbDsp.limit(out)
            }
            write = (write + 1) % capacity
        }
    }

    private companion object {
        const val MAX_MS = 1000
        const val MAX_REPEATS = 5
        const val TAP_GAIN = 0.55f
    }
}
