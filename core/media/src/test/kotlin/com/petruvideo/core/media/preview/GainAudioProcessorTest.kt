package com.petruvideo.core.media.preview

import io.kotest.matchers.shouldBe
import org.junit.Test

class GainAudioProcessorTest {
    @Test
    fun `unity gain keeps samples within one step`() {
        listOf<Short>(0, 100, -100, 20000, -20000).forEach {
            (GainAudioProcessor.scale(it, 1f) - it).let { d -> d >= -1 && d <= 1 } shouldBe true
        }
    }

    @Test
    fun `half gain halves a quiet sample and zero silences`() {
        GainAudioProcessor.scale(10000, 0.5f) shouldBe 5000.toShort()
        GainAudioProcessor.scale(12345, 0f) shouldBe 0.toShort()
    }

    @Test
    fun `boost above full scale is limited and never wraps`() {
        val loud = GainAudioProcessor.scale(32767, 2f)
        (loud <= Short.MAX_VALUE) shouldBe true
        (loud > 26000) shouldBe true
        GainAudioProcessor.scale(-32768, 2f) shouldBe GainAudioProcessor.scale(32767, 2f).let { (-it).toShort() }
    }
}
