package com.petruvideo.core.media.captions

import io.kotest.matchers.shouldBe
import org.junit.Test

class PhraseAudioTest {
    @Test
    fun `stereo is averaged to mono`() {
        DecodedPhraseAudio.mono(shortArrayOf(100, 300, -50, 50), 2).toList() shouldBe listOf<Short>(200, 0)
        DecodedPhraseAudio.mono(shortArrayOf(1, 2, 3), 1).toList() shouldBe listOf<Short>(1, 2, 3)
    }

    @Test
    fun `resampling to 16 kHz keeps the length in time and the shape`() {
        val tone = ShortArray(48_000) { (kotlin.math.sin(2 * Math.PI * 440 * it / 48_000) * 10_000).toInt().toShort() }
        val out = DecodedPhraseAudio.resample(tone, 48_000)
        out.size shouldBe 16_000
        // Zero crossings of a 440 Hz tone: about 880 per second whatever the rate.
        val crossings = out.toList().zipWithNext().count { (a, b) -> (a < 0) != (b < 0) }
        (kotlin.math.abs(crossings - 880) < 10) shouldBe true
        DecodedPhraseAudio.resample(out, 16_000) shouldBe out
    }
}
