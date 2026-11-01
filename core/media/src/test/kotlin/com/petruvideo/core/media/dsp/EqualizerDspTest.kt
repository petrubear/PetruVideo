package com.petruvideo.core.media.dsp

import io.kotest.matchers.shouldBe
import org.junit.Test
import kotlin.math.abs

class EqualizerDspTest {
    private fun gainAt(freq: Double, low: Int, mid: Int, high: Int): Float {
        val dsp = EqualizerDsp(TEST_RATE, 1).also { it.setGains(low, mid, high) }
        val input = sine(freq, 2.0, 0.1f)
        val out = chunked(input) { b, n -> dsp.process(b, n) }
        val from = TEST_RATE
        return db(toneLevel(out, freq, from) / toneLevel(input, freq, from))
    }

    @Test
    fun `flat leaves the sound unchanged`() {
        val dsp = EqualizerDsp(TEST_RATE, 1)
        val input = sine(1_000.0, 0.5)
        chunked(input) { b, n -> dsp.process(b, n) } shouldBe input
    }

    @Test
    fun `bands move their own region`() {
        (gainAt(60.0, 9, 0, 0) > 6f) shouldBe true
        (abs(gainAt(1_000.0, 9, 0, 0)) < 2f) shouldBe true
        (gainAt(1_000.0, 0, 9, 0) > 6f) shouldBe true
        (gainAt(10_000.0, 0, 0, 9) > 6f) shouldBe true
        (gainAt(10_000.0, 0, 0, -9) < -6f) shouldBe true
        (abs(gainAt(100.0, 0, 0, 9)) < 2f) shouldBe true
    }

    @Test
    fun `extreme settings stay stable and inside full scale`() {
        val dsp = EqualizerDsp(TEST_RATE, 2).also { it.setGains(12, 12, 12) }
        val input = FloatArray(TEST_RATE * 2) { if (it % 2 == 0) 0.9f else -0.9f }
        val out = chunked(input, 2) { b, n -> dsp.process(b, n) }
        out.all { it.isFinite() && kotlin.math.abs(it) <= 1f } shouldBe true
    }

    @Test
    fun `changing a band mid stream does not jump`() {
        val dsp = EqualizerDsp(TEST_RATE, 1)
        val input = sine(500.0, 2.0, 0.2f)
        val out = input.copyOf()
        dsp.process(out.copyOfRange(0, 4800).also { }, 4800)
        dsp.setGains(12, 0, 0)
        val part = out.copyOfRange(4800, 9600)
        dsp.process(part, 4800)
        var maxStep = 0f
        for (i in 1 until part.size) maxStep = maxOf(maxStep, abs(part[i] - part[i - 1]))
        (maxStep < 0.2f) shouldBe true
    }
}
