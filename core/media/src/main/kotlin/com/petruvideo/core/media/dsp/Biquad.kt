package com.petruvideo.core.media.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** One second-order filter section (RBJ cookbook shapes) with its own state for one channel. */
class Biquad {
    private var b0 = 1f
    private var b1 = 0f
    private var b2 = 0f
    private var a1 = 0f
    private var a2 = 0f
    private var z1 = 0f
    private var z2 = 0f

    fun reset() {
        z1 = 0f
        z2 = 0f
    }

    fun process(x: Float): Float {
        val y = b0 * x + z1
        z1 = b1 * x - a1 * y + z2
        z2 = b2 * x - a2 * y
        return y
    }

    fun lowShelf(sampleRate: Int, freq: Float, gainDb: Float) {
        val a = 10.0.pow(gainDb / 40.0)
        val w = 2 * PI * freq / sampleRate
        val c = cos(w)
        val beta = 2 * sqrt(a) * (sin(w) / 2 * sqrt(2.0))
        val a0 = (a + 1) + (a - 1) * c + beta
        set(
            b0 = a * ((a + 1) - (a - 1) * c + beta) / a0,
            b1 = 2 * a * ((a - 1) - (a + 1) * c) / a0,
            b2 = a * ((a + 1) - (a - 1) * c - beta) / a0,
            a1 = -2 * ((a - 1) + (a + 1) * c) / a0,
            a2 = ((a + 1) + (a - 1) * c - beta) / a0,
        )
    }

    fun highShelf(sampleRate: Int, freq: Float, gainDb: Float) {
        val a = 10.0.pow(gainDb / 40.0)
        val w = 2 * PI * freq / sampleRate
        val c = cos(w)
        val beta = 2 * sqrt(a) * (sin(w) / 2 * sqrt(2.0))
        val a0 = (a + 1) - (a - 1) * c + beta
        set(
            b0 = a * ((a + 1) + (a - 1) * c + beta) / a0,
            b1 = -2 * a * ((a - 1) + (a + 1) * c) / a0,
            b2 = a * ((a + 1) + (a - 1) * c - beta) / a0,
            a1 = 2 * ((a - 1) - (a + 1) * c) / a0,
            a2 = ((a + 1) - (a - 1) * c - beta) / a0,
        )
    }

    fun peaking(sampleRate: Int, freq: Float, gainDb: Float, q: Float) {
        val a = 10.0.pow(gainDb / 40.0)
        val w = 2 * PI * freq / sampleRate
        val alpha = sin(w) / (2 * q)
        val a0 = 1 + alpha / a
        set(
            b0 = (1 + alpha * a) / a0,
            b1 = -2 * cos(w) / a0,
            b2 = (1 - alpha * a) / a0,
            a1 = -2 * cos(w) / a0,
            a2 = (1 - alpha / a) / a0,
        )
    }

    private fun set(b0: Double, b1: Double, b2: Double, a1: Double, a2: Double) {
        this.b0 = b0.toFloat()
        this.b1 = b1.toFloat()
        this.b2 = b2.toFloat()
        this.a1 = a1.toFloat()
        this.a2 = a2.toFloat()
    }
}
