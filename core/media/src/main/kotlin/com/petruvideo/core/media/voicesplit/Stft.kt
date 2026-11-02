package com.petruvideo.core.media.voicesplit

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** The real and imaginary parts of a short-time Fourier transform: [frames] by [Stft.bins]. */
class Spectrum(val frames: Int, val re: Array<FloatArray>, val im: Array<FloatArray>)

/**
 * The short-time Fourier transform Open-Unmix expects: periodic Hann window, centred (reflect padded by half a frame),
 * so [forward] gives 1 + length / hop frames and [inverse] gives the signal back by overlap-add.
 */
class Stft(val nFft: Int = N_FFT, val hop: Int = HOP) {
    val bins = nFft / 2 + 1
    private val window = DoubleArray(nFft) { 0.5 - 0.5 * cos(2.0 * PI * it / nFft) }
    private val cosTable = DoubleArray(nFft / 2) { cos(2.0 * PI * it / nFft) }
    private val sinTable = DoubleArray(nFft / 2) { sin(2.0 * PI * it / nFft) }
    private val reversed = IntArray(nFft).also { table ->
        val bits = Integer.numberOfTrailingZeros(nFft)
        for (i in 0 until nFft) table[i] = Integer.reverse(i) ushr (32 - bits)
    }

    fun frameCount(length: Int) = 1 + length / hop

    fun forward(signal: FloatArray): Spectrum {
        val frames = frameCount(signal.size)
        val re = Array(frames) { FloatArray(bins) }
        val im = Array(frames) { FloatArray(bins) }
        val real = DoubleArray(nFft)
        val imag = DoubleArray(nFft)
        for (t in 0 until frames) {
            for (i in 0 until nFft) {
                real[i] = sample(signal, t * hop + i - nFft / 2) * window[i]
                imag[i] = 0.0
            }
            fft(real, imag, inverse = false)
            for (k in 0 until bins) {
                re[t][k] = real[k].toFloat()
                im[t][k] = imag[k].toFloat()
            }
        }
        return Spectrum(frames, re, im)
    }

    fun inverse(spectrum: Spectrum, length: Int): FloatArray {
        val padded = (spectrum.frames - 1) * hop + nFft
        val out = DoubleArray(padded)
        val norm = DoubleArray(padded)
        val real = DoubleArray(nFft)
        val imag = DoubleArray(nFft)
        for (t in 0 until spectrum.frames) {
            for (k in 0 until bins) {
                real[k] = spectrum.re[t][k].toDouble()
                imag[k] = spectrum.im[t][k].toDouble()
            }
            for (k in 1 until nFft / 2) {
                real[nFft - k] = real[k]
                imag[nFft - k] = -imag[k]
            }
            fft(real, imag, inverse = true)
            for (i in 0 until nFft) {
                out[t * hop + i] += real[i] * window[i]
                norm[t * hop + i] += window[i] * window[i]
            }
        }
        return FloatArray(length) { i ->
            val at = i + nFft / 2
            if (at < padded && norm[at] > MIN_NORM) (out[at] / norm[at]).toFloat() else 0f
        }
    }

    /** Reflect padding: the signal mirrored around its first and last sample. */
    private fun sample(signal: FloatArray, index: Int): Double {
        val n = signal.size
        if (n == 1) return signal[0].toDouble()
        val period = 2 * (n - 1)
        var i = index % period
        if (i < 0) i += period
        return signal[if (i < n) i else period - i].toDouble()
    }

    private fun fft(real: DoubleArray, imag: DoubleArray, inverse: Boolean) {
        for (i in 0 until nFft) {
            val j = reversed[i]
            if (j > i) {
                val r = real[i]; real[i] = real[j]; real[j] = r
                val m = imag[i]; imag[i] = imag[j]; imag[j] = m
            }
        }
        var size = 2
        while (size <= nFft) {
            butterflies(real, imag, size, inverse)
            size *= 2
        }
        if (inverse) for (i in 0 until nFft) {
            real[i] /= nFft
            imag[i] /= nFft
        }
    }

    private fun butterflies(real: DoubleArray, imag: DoubleArray, size: Int, inverse: Boolean) {
        val half = size / 2
        val step = nFft / size
        for (start in 0 until nFft step size) {
            for (k in 0 until half) {
                val wr = cosTable[k * step]
                val wi = if (inverse) sinTable[k * step] else -sinTable[k * step]
                val a = start + k
                val b = a + half
                val tr = real[b] * wr - imag[b] * wi
                val ti = real[b] * wi + imag[b] * wr
                real[b] = real[a] - tr
                imag[b] = imag[a] - ti
                real[a] += tr
                imag[a] += ti
            }
        }
    }

    companion object {
        const val N_FFT = 4096
        const val HOP = 1024
        private const val MIN_NORM = 1e-8
    }
}
