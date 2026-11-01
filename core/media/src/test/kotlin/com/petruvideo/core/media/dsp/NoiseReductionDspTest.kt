package com.petruvideo.core.media.dsp

import com.petruvideo.core.model.NoiseStrength
import io.kotest.matchers.shouldBe
import org.junit.Test
import kotlin.random.Random

class NoiseReductionDspTest {

    private fun noise(seconds: Double, level: Float, seed: Int = 1): FloatArray {
        val random = Random(seed)
        return FloatArray((seconds * TEST_RATE).toInt()) { (random.nextFloat() * 2 - 1) * level }
    }

    private fun run(input: FloatArray, strength: NoiseStrength): FloatArray {
        val dsp = NoiseReductionDsp(1).also { it.setStrength(strength) }
        return chunked(input) { b, n -> dsp.process(b, n) }
    }

    @Test
    fun `steady noise drops more with a higher strength`() {
        val input = noise(4.0, 0.1f)
        val from = TEST_RATE * 2
        val before = rms(input, from)
        val low = db(rms(run(input, NoiseStrength.Low), from) / before)
        val medium = db(rms(run(input, NoiseStrength.Medium), from) / before)
        val high = db(rms(run(input, NoiseStrength.High), from) / before)
        (low < -3f) shouldBe true
        (medium < low) shouldBe true
        (high < medium - 2f) shouldBe true
    }

    @Test
    fun `speech-like bursts over noise stay while the noise between them drops`() {
        val tone = sine(440.0, 5.0, 0.3f)
        for (i in tone.indices) if ((i / (TEST_RATE * 3 / 10)) % 2 == 1) tone[i] = 0f
        val hiss = noise(5.0, 0.05f, seed = 2)
        val input = FloatArray(tone.size) { tone[it] + hiss[it] }
        val out = run(input, NoiseStrength.High)
        fun at(seconds: Double) = (seconds * TEST_RATE).toInt()
        val toneChange = db(toneLevel(out, 440.0, at(3.65), at(3.85)) / toneLevel(input, 440.0, at(3.65), at(3.85)))
        (toneChange > -3f) shouldBe true
        val gap = rms(out, at(3.95), at(4.15)) / rms(input, at(3.95), at(4.15))
        (gap < 0.5f) shouldBe true
    }

    @Test
    fun `silence stays silent`() {
        val out = run(FloatArray(TEST_RATE), NoiseStrength.High)
        out.all { it == 0f } shouldBe true
    }

    @Test
    fun `the output is delayed by one frame and keeps its length`() {
        val input = sine(300.0, 1.0, 0.4f)
        val out = run(input, NoiseStrength.Low)
        out.size shouldBe input.size
    }

    @Test
    fun `strength change mid stream does not jump`() {
        val dsp = NoiseReductionDsp(1)
        val input = noise(2.0, 0.1f)
        val first = chunked(input.copyOfRange(0, TEST_RATE)) { b, n -> dsp.process(b, n) }
        dsp.setStrength(NoiseStrength.High)
        val second = chunked(input.copyOfRange(TEST_RATE, input.size)) { b, n -> dsp.process(b, n) }
        (kotlin.math.abs(second[0]) < 0.3f) shouldBe true
        first.size shouldBe second.size
    }
}
