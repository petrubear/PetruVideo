package com.petruvideo.core.media.preview

import com.petruvideo.core.model.Reverb
import com.petruvideo.core.model.ReverbPreset
import io.kotest.matchers.shouldBe
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random
import org.junit.Test

class ReverbDspTest {
    private val rate = 44_100

    private fun reverb(decayMs: Int = 1_000, mix: Int = 100, damping: Int = 0, room: Int = 50, preDelayMs: Int = 0) =
        Reverb(true, ReverbPreset.Custom, room, decayMs, damping, preDelayMs, mix)

    private fun dsp(r: Reverb, channels: Int = 1) = ReverbDsp(rate, channels).also {
        it.setSettings(r)
        it.setRoomSize(r.roomSize)
        it.reset()
    }

    private fun rms(a: FloatArray, from: Int, to: Int): Float {
        var s = 0.0
        for (i in from until to) s += a[i] * a[i]
        return sqrt(s / (to - from)).toFloat()
    }

    private fun impulseResponse(r: Reverb, seconds: Int): FloatArray {
        val out = FloatArray(rate * seconds)
        out[0] = 1f
        dsp(r).process(out, out.size)
        return out
    }

    @Test
    fun `the tail falls roughly 60 dB in the decay time`() {
        val ir = impulseResponse(reverb(decayMs = 1_000), seconds = 4)
        val early = rms(ir, 0, rate / 5)
        val atDecay = rms(ir, rate, rate + rate / 10)
        val late = rms(ir, rate * 3, rate * 3 + rate / 10)
        (early > 0f) shouldBe true
        (atDecay < early) shouldBe true
        (late < early * 0.01f) shouldBe true
    }

    @Test
    fun `a longer decay keeps more tail`() {
        val short = impulseResponse(reverb(decayMs = 500), 3)
        val long = impulseResponse(reverb(decayMs = 4_000), 3)
        (rms(long, rate * 2, rate * 2 + 4_000) > rms(short, rate * 2, rate * 2 + 4_000) * 5f) shouldBe true
    }

    @Test
    fun `mix 0 returns the input unchanged`() {
        val random = Random(1)
        val input = FloatArray(10_000) { random.nextFloat() * 1.6f - 0.8f }
        val out = input.copyOf()
        dsp(reverb(mix = 0)).process(out, out.size)
        for (i in input.indices) (abs(out[i] - input[i]) < 1e-6f) shouldBe true
    }

    @Test
    fun `output stays inside full scale for loud input`() {
        val random = Random(2)
        val out = FloatArray(rate * 2) { if (random.nextBoolean()) 1f else -1f }
        dsp(reverb(decayMs = 10_000, mix = 80)).process(out, out.size)
        var peak = 0f
        for (x in out) peak = maxOf(peak, abs(x))
        (peak <= 1f) shouldBe true
    }

    @Test
    fun `stereo channels are processed independently without crossing over`() {
        val frames = rate
        val out = FloatArray(frames * 2)
        out[0] = 1f
        dsp(reverb(), channels = 2).process(out, frames)
        var left = 0f
        var right = 0f
        for (i in 0 until frames) {
            left = maxOf(left, abs(out[i * 2]))
            right = maxOf(right, abs(out[i * 2 + 1]))
        }
        (left > 0f) shouldBe true
        right shouldBe 0f
    }

    @Test
    fun `reset clears the tail`() {
        val d = dsp(reverb())
        val burst = FloatArray(rate) { if (it < 100) 0.8f else 0f }
        d.process(burst, burst.size)
        d.reset()
        val silence = FloatArray(rate)
        d.process(silence, silence.size)
        silence.all { it == 0f } shouldBe true
    }

    @Test
    fun `changing mix and damping mid stream does not jump`() {
        val d = dsp(reverb(mix = 20))
        val random = Random(3)
        val a = FloatArray(4_000) { random.nextFloat() * 0.2f - 0.1f }
        d.process(a, a.size)
        d.setSettings(reverb(mix = 90, damping = 90))
        val b = FloatArray(4_000) { random.nextFloat() * 0.2f - 0.1f }
        val last = a.last()
        d.process(b, b.size)
        (abs(b.first() - last) < 0.5f) shouldBe true
    }

    @Test
    fun `pre-delay holds back the wet signal`() {
        val r = reverb(preDelayMs = 100, mix = 100)
        val ir = impulseResponse(r, 1)
        val silentUntil = rate / 10 - 50
        for (i in 1 until silentUntil) ir[i] shouldBe 0f
    }

    @Test
    fun `limit is transparent below the knee and bounded above`() {
        ReverbDsp.limit(0.5f) shouldBe 0.5f
        (ReverbDsp.limit(5f) <= 1f) shouldBe true
        ReverbDsp.limit(-5f) shouldBe -ReverbDsp.limit(5f)
    }
}
