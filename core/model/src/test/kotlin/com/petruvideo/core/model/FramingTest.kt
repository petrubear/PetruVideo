package com.petruvideo.core.model

import io.kotest.matchers.shouldBe
import org.junit.Test

class FramingTest {
    @Test
    fun `the neutral look fills the frame and is untouched by the cover rule`() {
        Framing.clamp(FRAMING_NEUTRAL) shouldBe FRAMING_NEUTRAL
    }

    @Test
    fun `zoom is limited to 100 to 400 percent`() {
        Framing.clamp(Pose(500, 500, 500, 100)).sizePermille shouldBe 1_000
        Framing.clamp(Pose(500, 500, 9_000, 100)).sizePermille shouldBe 4_000
    }

    @Test
    fun `pan keeps the shown window inside the clip`() {
        // At 200% the window is half the clip, so its center stays between 25% and 75%.
        Framing.clamp(Pose(0, 1_000, 2_000, 100)) shouldBe Pose(250, 750, 2_000, 100)
        // At 100% nothing can move.
        Framing.clamp(Pose(100, 900, 1_000, 100)) shouldBe FRAMING_NEUTRAL
    }

    @Test
    fun `a clip without keyframes shows the neutral look and keyframes drive it`() {
        val clip = VideoClip("a", "m", 0, 4_000, true)
        clip.poseAt(1_000) shouldBe FRAMING_NEUTRAL
        val pushed = clip.withKeyframes(
            listOf(Keyframe(0, FRAMING_NEUTRAL), Keyframe(2_000, Pose(750, 500, 2_000, 100))),
        )
        pushed.poseAt(1_000) shouldBe Pose(625, 500, 1_500, 100)
        pushed.poseAt(5_000).sizePermille shouldBe 2_000
    }

    @Test
    fun `an interpolated pose never shows an edge`() {
        val clip = VideoClip("a", "m", 0, 4_000, true).withKeyframes(
            listOf(Keyframe(0, Pose(500, 500, 1_000, 100)), Keyframe(4_000, Pose(500, 500, 4_000, 100))),
        )
        for (t in 0..4_000 step 100) Framing.clamp(clip.poseAt(t.toLong())) shouldBe clip.poseAt(t.toLong())
    }
}
