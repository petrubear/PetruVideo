package com.petruvideo.core.model

import io.kotest.matchers.shouldBe
import org.junit.Test

class ReverseTest {
    private val clip = VideoClip("a", "m", 1_000, 5_000, true)

    @Test
    fun `a forward clip shows the source from its start and a reversed one from its end`() {
        clip.sourceTimeAt(0) shouldBe 1_000
        clip.sourceTimeAt(1_500) shouldBe 2_500
        clip.copy(reversed = true).sourceTimeAt(0) shouldBe 5_000
        clip.copy(reversed = true).sourceTimeAt(1_500) shouldBe 3_500
        clip.copy(reversed = true).sourceTimeAt(99_999) shouldBe 1_000
    }

    @Test
    fun `speed scales the distance travelled in the source`() {
        val fast = clip.copy(speedMilli = 2_000)
        fast.sourceTimeAt(1_000) shouldBe 3_000
        fast.copy(reversed = true).sourceTimeAt(1_000) shouldBe 3_000
        fast.copy(reversed = true).sourceTimeAt(0) shouldBe 5_000
    }
}
