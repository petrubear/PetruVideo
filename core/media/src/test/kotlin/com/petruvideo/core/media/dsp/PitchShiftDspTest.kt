package com.petruvideo.core.media.dsp

import io.kotest.matchers.shouldBe
import org.junit.Test
import kotlin.math.abs
import kotlin.math.pow

class PitchShiftDspTest {
    private fun shifted(freq: Double, semitones: Int): FloatArray {
        val dsp = PitchShiftDsp(TEST_RATE, 1).also { it.setSemitones(semitones) }
        return chunked(sine(freq, 2.0)) { b, n -> dsp.process(b, n) }
    }

    @Test
    fun `the tone moves by the requested interval and the length is kept`() {
        for (semitones in listOf(12, 5, -12)) {
            val out = shifted(400.0, semitones)
            out.size shouldBe (2.0 * TEST_RATE).toInt()
            val target = 400.0 * 2.0.pow(semitones / 12.0)
            val at = toneLevel(out, target, TEST_RATE, TEST_RATE + 8_000)
            val away = toneLevel(out, 400.0, TEST_RATE, TEST_RATE + 8_000)
            (at > 0.12f) shouldBe true
            (at > away * 3) shouldBe true
        }
    }

    @Test
    fun `zero semitones leaves the sound unchanged`() {
        val input = sine(700.0, 0.5)
        val dsp = PitchShiftDsp(TEST_RATE, 1)
        chunked(input) { b, n -> dsp.process(b, n) } shouldBe input
    }

    @Test
    fun `changing the shift live does not click`() {
        val dsp = PitchShiftDsp(TEST_RATE, 1).also { it.setSemitones(3) }
        val a = sine(300.0, 1.0)
        val first = chunked(a) { b, n -> dsp.process(b, n) }
        dsp.setSemitones(7)
        val second = chunked(a) { b, n -> dsp.process(b, n) }
        var maxStep = 0f
        for (i in 1 until second.size) maxStep = maxOf(maxStep, abs(second[i] - second[i - 1]))
        (maxStep < 0.1f) shouldBe true
        first.size shouldBe second.size
    }
}
