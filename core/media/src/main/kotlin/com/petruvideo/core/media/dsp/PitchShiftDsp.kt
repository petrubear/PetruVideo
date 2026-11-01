package com.petruvideo.core.media.dsp

import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

/**
 * Pitch shift without changing duration: two read heads move through a delay line at a different speed from the
 * writer and are cross-faded with sine-squared windows, so one head fades out as the other fades in. The shift
 * can change while running. Zero semitones passes the audio through untouched. Adds about half a window of delay.
 */
class PitchShiftDsp(sampleRate: Int, private val channels: Int) {
    private val window = (sampleRate * WINDOW_MS / 1000).coerceAtLeast(2)
    private val size = window * 2 + 2
    private val line = Array(channels) { FloatArray(size) }
    private var write = 0
    private var phase = 0.0
    @Volatile private var semitones = 0

    fun setSemitones(value: Int) {
        semitones = value
    }

    fun reset() {
        line.forEach { it.fill(0f) }
        write = 0
        phase = 0.0
    }

    fun process(buffer: FloatArray, frames: Int) {
        val shift = semitones
        if (shift == 0) {
            // Keep the line warm so switching on does not play stale audio.
            for (frame in 0 until frames) {
                for (ch in 0 until channels) line[ch][write] = buffer[frame * channels + ch]
                write = (write + 1) % size
            }
            return
        }
        val ratio = 2.0.pow(shift / 12.0)
        val increment = (1.0 - ratio) / window
        for (frame in 0 until frames) {
            val phaseB = (phase + 0.5) % 1.0
            val gainA = sin(PI * phase).let { (it * it).toFloat() }
            val gainB = 1f - gainA
            val delayA = phase * window + 1
            val delayB = phaseB * window + 1
            for (ch in 0 until channels) {
                val index = frame * channels + ch
                line[ch][write] = buffer[index]
                buffer[index] = read(ch, delayA) * gainA + read(ch, delayB) * gainB
            }
            write = (write + 1) % size
            phase += increment
            phase -= kotlin.math.floor(phase)
        }
    }

    private fun read(channel: Int, delay: Double): Float {
        val position = write - delay
        val base = kotlin.math.floor(position).toInt()
        val frac = (position - base).toFloat()
        val a = line[channel][base.mod(size)]
        val b = line[channel][(base + 1).mod(size)]
        return a + (b - a) * frac
    }

    private companion object {
        const val WINDOW_MS = 30
    }
}
