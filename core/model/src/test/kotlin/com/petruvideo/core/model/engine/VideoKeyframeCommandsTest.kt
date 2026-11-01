package com.petruvideo.core.model.engine

import com.petruvideo.core.model.FRAMING_NEUTRAL
import com.petruvideo.core.model.Fingerprint
import com.petruvideo.core.model.Keyframe
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.Pose
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VideoClip
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.Test

class VideoKeyframeCommandsTest {
    private val media = MediaItem(
        "m", MediaKind.Video, "content://m", "m.mp4", 20_000, 1280, 720, 30f, true,
        Fingerprint(1, 1, 20_000, "video/mp4"),
    )
    private val timeline = Timeline(
        "p", media = mapOf("m" to media),
        videoLane = listOf(VideoClip("a", "m", 0, 4_000, true), VideoClip("b", "m", 0, 4_000, true)),
    )

    private fun applied(c: Command, t: Timeline = timeline) = (c.apply(t) as CommandResult.Applied).timeline
    private val Timeline.b get() = videoLane[1]

    @Test
    fun `add keyframe on the second clip uses clip time and holds the neutral look`() {
        val t = applied(AddKeyframe("b", 5_000))
        t.b.keyframes shouldBe listOf(Keyframe(1_000, FRAMING_NEUTRAL))
        t.videoLane[0].keyframes.shouldBeEmpty()
    }

    @Test
    fun `the first gesture away from the start keeps the look before it`() {
        val t = applied(SetPoseAt("a", 2_000, Pose(700, 500, 2_000, 100)))
        t.videoLane[0].keyframes.map { it.timeMs } shouldBe listOf(0L, 2_000L)
        t.videoLane[0].poseAt(0) shouldBe FRAMING_NEUTRAL
        t.videoLane[0].poseAt(1_000) shouldBe Pose(600, 500, 1_500, 100)
        t.videoLane[0].poseAt(3_000) shouldBe Pose(700, 500, 2_000, 100)
    }

    @Test
    fun `a gesture at the start makes one keyframe`() {
        val t = applied(SetPoseAt("a", 0, Pose(500, 500, 1_000, 40)))
        t.videoLane[0].keyframes.size shouldBe 1
    }

    @Test
    fun `a pose that would show an edge is pulled back`() {
        val t = applied(SetPoseAt("a", 0, Pose(0, 0, 2_000, 100)))
        t.videoLane[0].keyframes.single().pose shouldBe Pose(250, 250, 2_000, 100)
        TimelineInvariants.violations(t).shouldBeEmpty()
    }

    @Test
    fun `move copy motion and delete work and deleting the last returns the neutral look`() {
        var t = applied(SetPoseAt("a", 1_000, Pose(600, 500, 1_500, 100)))
        t = applied(SetPoseAt("a", 3_000, Pose(700, 500, 2_000, 100)), t)
        t = applied(SetKeyframeMotion("a", 3_000, true), t)
        t.videoLane[0].keyframes.last().ease shouldBe true
        t = applied(MoveKeyframe("a", 3_000, 3_500), t)
        t = applied(CopyKeyframe("a", 3_500, 2_000), t)
        t.videoLane[0].keyframes.map { it.timeMs } shouldBe listOf(0L, 1_000L, 2_000L, 3_500L)
        for (rel in listOf(0L, 1_000L, 2_000L, 3_500L)) t = applied(DeleteKeyframe("a", rel), t)
        t.videoLane[0].keyframes.shouldBeEmpty()
        t.videoLane[0].poseAt(2_000) shouldBe FRAMING_NEUTRAL
    }

    @Test
    fun `at most 50 keyframes`() {
        var t = timeline
        for (i in 0 until 50) t = applied(SetPoseAt("a", i * 70L, Pose(500, 500, 1_000, 100)), t)
        SetPoseAt("a", 3_900, Pose(500, 500, 1_000, 50)).apply(t) shouldBe
            CommandResult.Rejected(RejectReason.LimitReached)
    }
}

class VideoKeyframeEditsTest {
    private val media = MediaItem(
        "m", MediaKind.Video, "content://m", "m.mp4", 20_000, 1280, 720, 30f, true,
        Fingerprint(1, 1, 20_000, "video/mp4"),
    )
    private val animated = VideoClip("a", "m", 0, 4_000, true).withKeyframes(
        listOf(Keyframe(0, FRAMING_NEUTRAL), Keyframe(4_000, Pose(750, 500, 2_000, 100))),
    )
    private val timeline = Timeline("p", media = mapOf("m" to media), videoLane = listOf(animated))

    private fun applied(c: Command, t: Timeline = timeline) = (c.apply(t) as CommandResult.Applied).timeline

    @Test
    fun `split continues the movement without a jump`() {
        val t = applied(Split("a", 2_000))
        val (first, second) = t.videoLane
        first.poseAt(first.lengthMs) shouldBe animated.poseAt(2_000)
        second.poseAt(0) shouldBe animated.poseAt(2_000)
        second.poseAt(second.lengthMs) shouldBe animated.poseAt(4_000)
        TimelineInvariants.violations(t).shouldBeEmpty()
    }

    @Test
    fun `trim keeps the look at the new edges`() {
        val end = applied(TrimEnd("a", 2_000))
        end.videoLane[0].poseAt(2_000) shouldBe animated.poseAt(2_000)
        val start = applied(TrimStart("a", 1_000))
        start.videoLane[0].poseAt(0) shouldBe animated.poseAt(1_000)
        start.videoLane[0].poseAt(3_000) shouldBe animated.poseAt(4_000)
        TimelineInvariants.violations(start).shouldBeEmpty()
    }

    @Test
    fun `speed change scales keyframe times so the animation keeps its shape`() {
        val t = applied(SetSpeed("a", 2_000))
        val clip = t.videoLane[0]
        clip.lengthMs shouldBe 2_000
        clip.keyframes.map { it.timeMs } shouldBe listOf(0L, 2_000L)
        clip.poseAt(1_000) shouldBe animated.poseAt(2_000)
        TimelineInvariants.violations(t).shouldBeEmpty()
    }

    @Test
    fun `duplicate copies the keyframes`() {
        val t = applied(Duplicate("a"))
        t.videoLane[1].keyframes shouldBe animated.keyframes
    }
}
