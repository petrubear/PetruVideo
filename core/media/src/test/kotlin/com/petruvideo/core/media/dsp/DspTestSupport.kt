package com.petruvideo.core.media.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

const val TEST_RATE = 44_100

fun sine(freq: Double, seconds: Double, amplitude: Float = 0.3f, rate: Int = TEST_RATE) =
    FloatArray((seconds * rate).toInt()) { (amplitude * sin(2 * PI * freq * it / rate)).toFloat() }

fun rms(a: FloatArray, from: Int = 0, to: Int = a.size): Float {
    var s = 0.0
    for (i in from until to) s += a[i] * a[i]
    return sqrt(s / (to - from)).toFloat()
}

/** Amplitude of the [freq] component of [a] between [from] and [to] (single-bin correlation). */
fun toneLevel(a: FloatArray, freq: Double, from: Int = 0, to: Int = a.size, rate: Int = TEST_RATE): Float {
    var re = 0.0
    var im = 0.0
    for (i in from until to) {
        re += a[i] * cos(2 * PI * freq * i / rate)
        im += a[i] * sin(2 * PI * freq * i / rate)
    }
    return (2 * sqrt(re * re + im * im) / (to - from)).toFloat()
}

fun db(ratio: Float) = 20 * log10(ratio.toDouble()).toFloat()

/** Runs [block] over [input] in 480-frame chunks, as a processor would. */
fun chunked(input: FloatArray, channels: Int = 1, block: (FloatArray, Int) -> Unit): FloatArray {
    val out = input.copyOf()
    val step = 480 * channels
    var at = 0
    while (at < out.size) {
        val n = minOf(step, out.size - at)
        val part = out.copyOfRange(at, at + n)
        block(part, n / channels)
        part.copyInto(out, at)
        at += n
    }
    return out
}
