package com.petruvideo.core.media.dsp

import com.petruvideo.core.model.NoiseStrength
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Steady-noise reduction by spectral gating: 512-sample frames with 50% overlap, a per-frequency noise floor kept
 * as a slowly rising running minimum, and a per-frequency gain that removes the floor (over-subtracted) down to a
 * maximum reduction set by the strength (6, 12, or 20 dB). Adds one frame (512 samples) of delay.
 */
class NoiseReductionDsp(private val channels: Int) {
    private val fft = Fft(FRAME)
    private val window = FloatArray(FRAME) { sqrt(0.5 - 0.5 * cos(2 * PI * it / FRAME)).toFloat() }
    private val states = Array(channels) { Channel() }
    private val re = FloatArray(FRAME)
    private val im = FloatArray(FRAME)
    @Volatile private var strength = NoiseStrength.Medium

    private inner class Channel {
        val history = FloatArray(FRAME)
        var write = 0
        var count = 0
        val output = FloatArray(FRAME)
        var out = 0
        val noise = FloatArray(BINS) { 0f }
        val average = FloatArray(BINS) { 0f }
        val gain = FloatArray(BINS) { 1f }
        var primed = false
    }

    fun setStrength(value: NoiseStrength) {
        strength = value
    }

    fun reset() {
        for (s in states) {
            s.history.fill(0f)
            s.output.fill(0f)
            s.noise.fill(0f)
            s.average.fill(0f)
            s.gain.fill(1f)
            s.write = 0
            s.count = 0
            s.out = 0
            s.primed = false
        }
    }

    fun process(buffer: FloatArray, frames: Int) {
        val floor = 10.0.pow(-maxReductionDb(strength) / 20.0).toFloat()
        val overSubtract = when (strength) {
            NoiseStrength.Low -> 1.2f
            NoiseStrength.Medium -> 1.6f
            NoiseStrength.High -> 2.0f
        }
        for (frame in 0 until frames) {
            for (ch in 0 until channels) {
                val s = states[ch]
                val index = frame * channels + ch
                val x = buffer[index]
                buffer[index] = s.output[s.out]
                s.output[s.out] = 0f
                s.out = (s.out + 1) % FRAME
                s.history[s.write] = x
                s.write = (s.write + 1) % FRAME
                if (++s.count == HOP) {
                    s.count = 0
                    processFrame(s, floor, overSubtract)
                }
            }
        }
    }

    private fun processFrame(s: Channel, floor: Float, overSubtract: Float) {
        for (i in 0 until FRAME) {
            re[i] = s.history[(s.write + i) % FRAME] * window[i]
            im[i] = 0f
        }
        fft.transform(re, im, inverse = false)
        for (k in 0 until BINS) {
            val mag = sqrt(re[k] * re[k] + im[k] * im[k])
            s.average[k] = if (s.primed) s.average[k] * AVERAGE_KEEP + mag * (1f - AVERAGE_KEEP) else mag
            if (!s.primed || s.average[k] < s.noise[k]) s.noise[k] = s.average[k] else s.noise[k] *= NOISE_RISE
            val wanted = if (mag > 1e-9f) max(floor, 1f - overSubtract * s.noise[k] / mag) else floor
            s.gain[k] = s.gain[k] * GAIN_SMOOTH + wanted * (1f - GAIN_SMOOTH)
        }
        s.primed = true
        for (k in 0 until BINS) {
            re[k] *= s.gain[k]
            im[k] *= s.gain[k]
            if (k in 1 until FRAME / 2) {
                re[FRAME - k] = re[k]
                im[FRAME - k] = -im[k]
            }
        }
        fft.transform(re, im, inverse = true)
        for (i in 0 until FRAME) {
            val at = (s.out + i) % FRAME
            s.output[at] += re[i] * window[i]
        }
    }

    private fun maxReductionDb(value: NoiseStrength) = when (value) {
        NoiseStrength.Low -> 6.0
        NoiseStrength.Medium -> 12.0
        NoiseStrength.High -> 20.0
    }

    private companion object {
        const val FRAME = 512
        const val HOP = FRAME / 2
        const val BINS = FRAME / 2 + 1
        const val NOISE_RISE = 1.002f
        const val AVERAGE_KEEP = 0.85f
        const val GAIN_SMOOTH = 0.5f
    }
}

/** In-place radix-2 FFT of a fixed power-of-two size; the inverse is scaled by 1/n. */
class Fft(private val n: Int) {
    private val cosTable = FloatArray(n / 2) { cos(2 * PI * it / n).toFloat() }
    private val sinTable = FloatArray(n / 2) { sin(2 * PI * it / n).toFloat() }
    private val reversed = IntArray(n).also { r ->
        val bits = Integer.numberOfTrailingZeros(n)
        for (i in 0 until n) r[i] = Integer.reverse(i) ushr (32 - bits)
    }

    fun transform(re: FloatArray, im: FloatArray, inverse: Boolean) {
        for (i in 0 until n) {
            val j = reversed[i]
            if (j > i) {
                val tr = re[i]; re[i] = re[j]; re[j] = tr
                val ti = im[i]; im[i] = im[j]; im[j] = ti
            }
        }
        val sign = if (inverse) 1f else -1f
        var size = 2
        while (size <= n) {
            val half = size / 2
            val step = n / size
            var start = 0
            while (start < n) {
                for (k in 0 until half) {
                    val wr = cosTable[k * step]
                    val wi = sign * sinTable[k * step]
                    val a = start + k
                    val b = a + half
                    val xr = re[b] * wr - im[b] * wi
                    val xi = re[b] * wi + im[b] * wr
                    re[b] = re[a] - xr
                    im[b] = im[a] - xi
                    re[a] += xr
                    im[a] += xi
                }
                start += size
            }
            size *= 2
        }
        if (inverse) {
            for (i in 0 until n) {
                re[i] /= n
                im[i] /= n
            }
        }
    }
}
