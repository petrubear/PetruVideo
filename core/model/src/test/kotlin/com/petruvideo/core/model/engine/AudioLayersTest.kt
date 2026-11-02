package com.petruvideo.core.model.engine

import com.petruvideo.core.model.AudioClip
import com.petruvideo.core.model.AudioOrigin
import io.kotest.matchers.shouldBe
import org.junit.Test

class AudioLayersTest {
    private fun clip(id: String, start: Long, length: Long) =
        AudioClip(id, "m", 0, length, start, origin = AudioOrigin.Added)

    @Test
    fun `a clip fits while fewer than three others play at any moment of it`() {
        val lane = listOf(clip("a", 0, 10_000), clip("b", 0, 10_000))
        AudioPlacement.fits(lane, 2_000, 3_000) shouldBe true
        AudioPlacement.fits(lane + clip("c", 4_000, 10_000), 2_000, 2_000) shouldBe true
        AudioPlacement.fits(lane + clip("c", 4_000, 10_000), 2_000, 5_000) shouldBe false
        AudioPlacement.fits(lane + clip("c", 4_000, 10_000), 2_000, 5_000, ignoreClipId = "c") shouldBe true
    }

    @Test
    fun `the next free start waits for a layer to free up`() {
        val lane = listOf(clip("a", 0, 6_000), clip("b", 0, 8_000), clip("c", 0, 9_000))
        AudioPlacement.nextFreeStart(lane, 1_000, 2_000) shouldBe 6_000
        AudioPlacement.nextFreeStart(lane.take(2), 1_000, 2_000) shouldBe 1_000
        AudioPlacement.nextFreeStart(emptyList(), -5, 1_000) shouldBe 0
    }

    @Test
    fun `the first full moment is where three other clips already play`() {
        val lane = listOf(clip("a", 0, 10_000), clip("b", 2_000, 10_000), clip("c", 5_000, 10_000))
        AudioPlacement.firstFullMoment(lane, 0, ignoreClipId = null) shouldBe 5_000
        AudioPlacement.firstFullMoment(lane, 0, ignoreClipId = "c") shouldBe null
    }

    @Test
    fun `rows put each clip on the first row that has finished`() {
        val lane = listOf(
            clip("a", 0, 4_000), clip("b", 1_000, 4_000), clip("c", 4_000, 2_000), clip("d", 2_000, 1_000),
        )
        AudioPlacement.rows(lane) shouldBe mapOf("a" to 0, "b" to 1, "d" to 2, "c" to 0)
    }
}
