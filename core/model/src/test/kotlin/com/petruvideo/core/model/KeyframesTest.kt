package com.petruvideo.core.model

import io.kotest.matchers.shouldBe
import org.junit.Test

class KeyframesTest {
    private val base = Pose(750, 250, 350, 100)
    private val a = Keyframe(0, Pose(100, 100, 200, 100))
    private val b = Keyframe(2_000, Pose(900, 500, 400, 50))

    @Test
    fun `no keyframes give the plain values`() {
        Keyframes.poseAt(base, emptyList(), 1_234) shouldBe base
    }

    @Test
    fun `before the first and after the last the nearest keyframe holds`() {
        val frames = listOf(Keyframe(500, a.pose), Keyframe(1_500, b.pose))
        Keyframes.poseAt(base, frames, 0) shouldBe a.pose
        Keyframes.poseAt(base, frames, 500) shouldBe a.pose
        Keyframes.poseAt(base, frames, 1_500) shouldBe b.pose
        Keyframes.poseAt(base, frames, 9_000) shouldBe b.pose
    }

    @Test
    fun `linear blends at constant speed`() {
        val frames = listOf(a, b)
        Keyframes.poseAt(base, frames, 1_000) shouldBe Pose(500, 300, 300, 75)
        Keyframes.poseAt(base, frames, 500) shouldBe Pose(300, 200, 250, 88)
    }

    @Test
    fun `ease starts and ends slowly and meets linear in the middle`() {
        val frames = listOf(a.copy(ease = true), b)
        Keyframes.poseAt(base, frames, 1_000) shouldBe Pose(500, 300, 300, 75)
        // At a quarter of the way ease has covered 15.6% of the distance, linear 25%.
        Keyframes.poseAt(base, frames, 500).xPermille shouldBe 225
    }

    @Test
    fun `a keyframe is changed within a frame of its time or added in order, up to the limit`() {
        val frames = listOf(a, b)
        val changed = Keyframes.withPoseAt(frames, 10, Pose(1, 2, 3, 4))!!
        changed.map { it.timeMs } shouldBe listOf(0L, 2_000L)
        changed[0].pose shouldBe Pose(1, 2, 3, 4)
        Keyframes.withPoseAt(frames, 1_000, base)!!.map { it.timeMs } shouldBe listOf(0L, 1_000L, 2_000L)
        val full = List(KEYFRAME_MAX) { Keyframe(it * 100L, base) }
        Keyframes.withPoseAt(full, 4_950, base) shouldBe null
        Keyframes.withPoseAt(full, 100, Pose(0, 0, 50, 0))!!.size shouldBe KEYFRAME_MAX
    }

    @Test
    fun `a window keeps inside keyframes shifted and holds the values at the new edges`() {
        val frames = listOf(a, Keyframe(1_000, Pose(500, 300, 300, 75)), b)
        val right = Keyframes.window(base, frames, 500, 2_000)
        right.map { it.timeMs } shouldBe listOf(0L, 500L, 1_500L)
        right[0].pose shouldBe Pose(300, 200, 250, 88)
        right[2].pose shouldBe b.pose
        val left = Keyframes.window(base, frames, 0, 1_500)
        left.map { it.timeMs } shouldBe listOf(0L, 1_000L, 1_500L)
        left.last().pose shouldBe Keyframes.poseAt(base, frames, 1_500)
        Keyframes.window(base, emptyList(), 0, 1_000) shouldBe emptyList()
    }

    @Test
    fun `a window reaching past the clip holds the last values at the end`() {
        val frames = listOf(a, b)
        val wider = Keyframes.window(base, frames, -500, 2_500)
        wider.first().pose shouldBe a.pose
        wider.last().timeMs shouldBe 3_000L
        wider.last().pose shouldBe b.pose
    }

    @Test
    fun `playing an animation frame by frame never jumps`() {
        for (ease in listOf(false, true)) {
            val frames = listOf(a.copy(ease = ease), b)
            var last = Keyframes.poseAt(base, frames, 0)
            var biggest = 0
            for (t in 33L..2_000L step 33L) {
                val now = Keyframes.poseAt(base, frames, t)
                biggest = maxOf(biggest, kotlin.math.abs(now.xPermille - last.xPermille))
                last = now
            }
            // The path is 800 permille long over 60 frames; a step above 10% of it would be a visible jump.
            (biggest < 80) shouldBe true
        }
    }

    @Test
    fun `normalizing sorts, drops duplicate times, and clamps`() {
        val messy = listOf(
            Keyframe(5_000, Pose(2_000, -5, 9_999, 300)),
            Keyframe(0, a.pose),
            Keyframe(0, b.pose),
        )
        val fixed = Keyframes.normalized(messy, 2_000, 50..1_000)
        fixed.map { it.timeMs } shouldBe listOf(0L, 2_000L)
        fixed[0].pose shouldBe a.pose
        fixed[1].pose shouldBe Pose(1_000, 0, 1_000, 100)
    }

    @Test
    fun `an overlay with keyframes shows them and plain values follow the first keyframe`() {
        val clip = OverlayClip("o", "m", 0, 4_000, 1_000).withKeyframes(listOf(a, b))
        clip.xPermille shouldBe 100
        clip.widthPermille shouldBe 200
        clip.poseAt(1_000) shouldBe Pose(500, 300, 300, 75)
        clip.inRange shouldBe true
        val cleared = clip.withKeyframes(emptyList(), b.pose)
        cleared.basePose shouldBe b.pose
        cleared.poseAt(0) shouldBe b.pose
    }

    @Test
    fun `a text clip with keyframes shows them at full opacity base`() {
        val end = Keyframe(3_000, b.pose.copy(sizePermille = 250))
        val clip = TextClip("t", "Hi", 0, 3_000).withKeyframes(listOf(a, end))
        clip.poseAt(0).sizePermille shouldBe 200
        TextClip("t", "Hi", 0, 3_000).poseAt(10).opacityPercent shouldBe 100
        clip.inRange shouldBe true
    }
}
