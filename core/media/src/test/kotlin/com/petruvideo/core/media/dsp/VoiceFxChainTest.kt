package com.petruvideo.core.media.dsp

import com.petruvideo.core.model.AudioFx
import com.petruvideo.core.model.Echo
import com.petruvideo.core.model.NoiseReduction
import com.petruvideo.core.model.Pitch
import com.petruvideo.core.model.VoiceFx
import io.kotest.matchers.shouldBe
import org.junit.Test

class VoiceFxChainTest {
    private fun run(chain: VoiceFxChain, input: FloatArray) = chunked(input) { b, n -> chain.process(b, n) }

    @Test
    fun `neutral settings are inactive and leave the sound untouched`() {
        val chain = VoiceFxChain(TEST_RATE, 1).also { it.setFx(AudioFx()) }
        chain.active shouldBe false
        val input = sine(440.0, 0.5)
        run(chain, input) shouldBe input
    }

    @Test
    fun `stages that are on make the chain active and change the sound`() {
        val fx = AudioFx(voice = VoiceFx(echo = Echo(true, 100, 2)))
        val chain = VoiceFxChain(TEST_RATE, 1).also { it.setFx(fx) }
        chain.active shouldBe true
        val input = FloatArray(TEST_RATE).also { it[0] = 0.5f }
        (run(chain, input)[TEST_RATE / 10] > 0.2f) shouldBe true
    }

    @Test
    fun `under pressure noise reduction pitch and echo are bypassed`() {
        val fx = AudioFx(
            noise = NoiseReduction(true),
            voice = VoiceFx(echo = Echo(true), pitch = Pitch(true, 5)),
        )
        val chain = VoiceFxChain(TEST_RATE, 1).also { it.setFx(fx, heavyAllowed = false) }
        chain.active shouldBe false
        val input = sine(300.0, 0.5)
        run(chain, input) shouldBe input
    }

    @Test
    fun `stacked effects stay inside full scale`() {
        val fx = AudioFx(voice = VoiceFx(echo = Echo(true, 80, 5), pitch = Pitch(true, 7)))
        val chain = VoiceFxChain(TEST_RATE, 2).also { it.setFx(fx) }
        val loud = FloatArray(TEST_RATE * 2) { if ((it / 2) % 50 < 25) 0.95f else -0.95f }
        chunked(loud, 2) { b, n -> chain.process(b, n) }.all { kotlin.math.abs(it) <= 1f } shouldBe true
    }
}
