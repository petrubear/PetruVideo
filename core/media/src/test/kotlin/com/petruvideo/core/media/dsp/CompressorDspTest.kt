package com.petruvideo.core.media.dsp

import com.petruvideo.core.model.Compressor
import com.petruvideo.core.model.CompressorPreset
import com.petruvideo.core.model.CompressorPresets
import io.kotest.matchers.shouldBe
import org.junit.Test
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

class CompressorDspTest {
    private val quiet = 0.05f
    private val loud = 0.8f

    /** Half a second of a quiet tone followed by half a second of a loud one. */
    private fun quietThenLoud(): FloatArray {
        val a = sine(440.0, 0.5, quiet)
        val b = sine(440.0, 0.5, loud)
        return a + b
    }

    private fun rmsDb(a: FloatArray, from: Int, to: Int): Double {
        var sum = 0.0
        for (i in from until to) sum += a[i].toDouble() * a[i]
        return 20 * log10(sqrt(sum / (to - from)))
    }

    private fun run(settings: Compressor, input: FloatArray, channels: Int = 1): FloatArray {
        val dsp = CompressorDsp(TEST_RATE, channels).also { it.setSettings(settings) }
        return chunked(input, channels) { b, n -> dsp.process(b, n) }
    }

    private fun gapDb(a: FloatArray): Double {
        val half = TEST_RATE / 2
        val settle = TEST_RATE / 5
        return rmsDb(a, half + settle, a.size) - rmsDb(a, settle, half)
    }

    @Test
    fun `the Voice preset shrinks the loudness gap by at least 40 percent`() {
        val input = quietThenLoud()
        val out = run(CompressorPresets.settings(CompressorPreset.Voice, enabled = true), input)
        val before = gapDb(input)
        val after = gapDb(out)
        (after <= before * 0.6) shouldBe true
        (after > 0) shouldBe true
    }

    @Test
    fun `strength zero with no level leaves the sound untouched`() {
        val input = quietThenLoud()
        val none = Compressor(enabled = true).withStrength(0).copy(levelDb = 0)
        run(none, input) shouldBe input
    }

    @Test
    fun `off leaves the sound untouched whatever the settings are`() {
        val input = quietThenLoud()
        run(CompressorPresets.settings(CompressorPreset.Podcast, enabled = false), input) shouldBe input
    }

    @Test
    fun `a stronger setting compresses more`() {
        val input = quietThenLoud()
        val gentle = gapDb(run(Compressor(enabled = true).withStrength(30).copy(levelDb = 0), input))
        val strong = gapDb(run(Compressor(enabled = true).withStrength(90).copy(levelDb = 0), input))
        (strong < gentle) shouldBe true
    }

    @Test
    fun `level alone raises the sound by about its value`() {
        val input = sine(440.0, 0.5, 0.1f)
        val raised = run(Compressor(enabled = true).withStrength(0).copy(levelDb = 6), input)
        val gain = rmsDb(raised, TEST_RATE / 4, raised.size) - rmsDb(input, TEST_RATE / 4, input.size)
        (abs(gain - 6.0) < 0.5) shouldBe true
    }

    @Test
    fun `loud sound with the most level never goes past full scale`() {
        val input = sine(200.0, 0.5, 1.0f)
        val out = run(Compressor(enabled = true).withStrength(0).copy(levelDb = 12), input)
        (out.maxOf { abs(it) } <= 1.0f) shouldBe true
    }

    @Test
    fun `both channels get the same gain`() {
        val mono = quietThenLoud()
        val stereo = FloatArray(mono.size * 2) { mono[it / 2] * if (it % 2 == 0) 1f else 0.5f }
        val out = run(CompressorPresets.settings(CompressorPreset.Voice, enabled = true), stereo, 2)
        for (frame in 0 until mono.size step 997) {
            if (abs(out[frame * 2]) > 0.01f) {
                (abs(out[frame * 2 + 1] / out[frame * 2] - 0.5f) < 0.02f) shouldBe true
            }
        }
    }
}
