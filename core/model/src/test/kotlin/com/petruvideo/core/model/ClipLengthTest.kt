package com.petruvideo.core.model

import io.kotest.matchers.shouldBe
import org.junit.Test

class ClipLengthTest {
    private val clip = VideoClip("c", "m", 1_000, 11_000, attachedAudio = true)

    @Test
    fun `defaults are 1x and 100 percent`() {
        clip.speedMilli shouldBe 1000
        clip.volumePercent shouldBe 100
        clip.sourceLengthMs shouldBe 10_000
        clip.lengthMs shouldBe 10_000
    }

    @Test
    fun `timeline length is source length divided by speed, rounded`() {
        clip.copy(speedMilli = 2000).lengthMs shouldBe 5_000
        clip.copy(speedMilli = 500).lengthMs shouldBe 20_000
        clip.copy(speedMilli = 250).lengthMs shouldBe 40_000
        clip.copy(speedMilli = 3000).lengthMs shouldBe 3_333
        clip.copy(sourceEndMs = 1_001, speedMilli = 3000).lengthMs shouldBe 0
    }

    @Test
    fun `audio clip end follows the derived length`() {
        val audio = AudioClip("a", "m", 0, 4_000, timelineStartMs = 1_000, origin = AudioOrigin.Added, speedMilli = 500)
        audio.lengthMs shouldBe 8_000
        audio.timelineEndMs shouldBe 9_000
    }
}
