package com.petruvideo.core.media.dsp

import io.kotest.matchers.shouldBe
import org.junit.Test

class EffectsDspTest {
    @Test
    fun `echo repeats at multiples of the delay with falling level and stops`() {
        val dsp = EchoDsp(TEST_RATE, 1).also { it.setSettings(100, 3) }
        val input = FloatArray(TEST_RATE * 2).also { it[0] = 0.5f }
        val out = chunked(input) { b, n -> dsp.process(b, n) }
        val d = TEST_RATE / 10
        out[0] shouldBe 0.5f
        (out[d] in 0.25f..0.30f) shouldBe true
        (out[2 * d] in 0.14f..0.16f) shouldBe true
        (out[3 * d] in 0.08f..0.09f) shouldBe true
        out[4 * d] shouldBe 0f
        out[d / 2] shouldBe 0f
    }

    @Test
    fun `robot with zero intensity is the input and with intensity adds the carrier sidebands`() {
        val input = sine(440.0, 1.0)
        val off = RobotDsp(TEST_RATE, 1)
        chunked(input) { b, n -> off.process(b, n) } shouldBe input
        val on = RobotDsp(TEST_RATE, 1).also { it.setIntensity(100) }
        val out = chunked(input) { b, n -> on.process(b, n) }
        (toneLevel(out, 510.0) > 0.05f) shouldBe true
        (toneLevel(out, 370.0) > 0.05f) shouldBe true
    }

    @Test
    fun `bass boosts lows by amount and stays bounded`() {
        fun low(amount: Int): Float {
            val dsp = BassDsp(TEST_RATE, 1).also { it.setAmount(amount) }
            return toneLevel(chunked(sine(60.0, 1.5, 0.1f)) { b, n -> dsp.process(b, n) }, 60.0, TEST_RATE / 2)
        }
        (low(100) > low(50)) shouldBe true
        (low(50) > low(0)) shouldBe true
        val loud = BassDsp(TEST_RATE, 1).also { it.setAmount(100) }
        chunked(sine(60.0, 1.0, 0.95f)) { b, n -> loud.process(b, n) }.all { kotlin.math.abs(it) <= 1f } shouldBe true
    }
}
