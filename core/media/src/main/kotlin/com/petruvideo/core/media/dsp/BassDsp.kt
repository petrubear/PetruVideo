package com.petruvideo.core.media.dsp

import com.petruvideo.core.media.preview.ReverbDsp

/** Bass boost: a low shelf at 120 Hz up to +12 dB, then a soft limit. Amount is 0 to 100 and can change live. */
class BassDsp(private val sampleRate: Int, private val channels: Int) {
    private val shelf = Array(channels) { Biquad() }
    private var applied = -1
    @Volatile private var amount = 0

    fun setAmount(percent: Int) {
        amount = percent
    }

    fun reset() = shelf.forEach { it.reset() }

    fun process(buffer: FloatArray, frames: Int) {
        val now = amount
        if (now != applied) {
            shelf.forEach { it.lowShelf(sampleRate, SHELF_HZ, now * MAX_DB / 100f) }
            applied = now
        }
        if (now == 0) return
        for (frame in 0 until frames) {
            for (ch in 0 until channels) {
                val index = frame * channels + ch
                buffer[index] = ReverbDsp.limit(shelf[ch].process(buffer[index]))
            }
        }
    }

    private companion object {
        const val SHELF_HZ = 120f
        const val MAX_DB = 12f
    }
}
