package com.petruvideo.core.model.engine

import com.petruvideo.core.model.Fingerprint
import com.petruvideo.core.model.KEYFRAME_MAX
import com.petruvideo.core.model.Keyframe
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.OverlayClip
import com.petruvideo.core.model.Pose
import com.petruvideo.core.model.TextClip
import com.petruvideo.core.model.Timeline
import io.kotest.matchers.shouldBe
import org.junit.Test

class KeyframeCommandsTest {
    private val pip = MediaItem(
        "m", MediaKind.Video, "content://m", "m.mp4", 10_000, 1280, 720, 30f, false,
        Fingerprint(1, 1, 10_000, "video/mp4"),
    )
    private val timeline = Timeline(
        "p", media = mapOf("m" to pip),
        overlayLane = listOf(OverlayClip("o", "m", 0, 4_000, 1_000)),
        textLane = listOf(TextClip("t", "Hi", 0, 3_000)),
    )

    private fun applied(c: Command, t: Timeline = timeline) = (c.apply(t) as CommandResult.Applied).timeline
    private val Timeline.o get() = overlayLane.single()
    private val Timeline.t get() = textLane.single()

    @Test
    fun `add keyframe holds the shown values and the clip looks the same`() {
        val t = applied(AddKeyframe("o", 2_000))
        t.o.keyframes shouldBe listOf(Keyframe(1_000, timeline.o.basePose))
        t.o.poseAt(0) shouldBe timeline.o.basePose
        AddKeyframe("o", 2_000).apply(t) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        AddKeyframe("o", 9_000).apply(timeline) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        AddKeyframe("zz", 0).apply(timeline) shouldBe CommandResult.Rejected(RejectReason.NotFound)
    }

    @Test
    fun `a gesture between keyframes makes a keyframe with the shown values and the change`() {
        val start = applied(SetPoseAt("o", 1_000, Pose(100, 100, 200, 100)))
        val two = applied(SetPoseAt("o", 3_000, Pose(900, 700, 400, 50)), start)
        two.o.keyframes.map { it.timeMs } shouldBe listOf(0L, 2_000L)
        two.o.poseAt(1_000) shouldBe Pose(500, 400, 300, 75)
        val mid = applied(SetOverlayPlacement("o", 400, 400, 300, atMs = 2_000), two)
        mid.o.keyframes.map { it.timeMs } shouldBe listOf(0L, 1_000L, 2_000L)
        mid.o.keyframes[1].pose shouldBe Pose(400, 400, 300, 75)
        val fade = applied(SetOverlayOpacity("o", 10, atMs = 2_000), mid)
        fade.o.keyframes[1].pose.opacityPercent shouldBe 10
        fade.o.keyframes[1].pose.xPermille shouldBe 400
    }

    @Test
    fun `old placement commands are never ignored on an animated clip`() {
        val animated = applied(AddKeyframe("o", 1_000))
        SetOverlayPlacement("o", 1, 2, 300).apply(animated) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        SetOverlayOpacity("o", 20).apply(animated) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        applied(SetOverlayOpacity("o", 20)).o.opacityPercent shouldBe 20
        val text = applied(AddKeyframe("t", 0))
        SetTextPlacement("t", 1, 2, 80).apply(text) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        applied(SetTextPlacement("t", 100, 200, 80, atMs = 1_000), text).t.keyframes.size shouldBe 2
        applied(SetTextPlacement("t", 100, 200, 80)).t.xPermille shouldBe 100
    }

    @Test
    fun `the plain values follow the first keyframe and deleting the last keeps its look`() {
        val a = applied(SetPoseAt("t", 0, Pose(100, 200, 80, 100)))
        a.t.xPermille shouldBe 100
        val gone = applied(DeleteKeyframe("t", 0), a)
        gone.t.keyframes shouldBe emptyList()
        gone.t.basePose shouldBe Pose(100, 200, 80, 100)
        DeleteKeyframe("t", 5).apply(a) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
    }

    @Test
    fun `moving stops at neighbours and a copy replaces the keyframe already there`() {
        val t = applied(SetPoseAt("o", 3_000, Pose(1, 1, 100, 1)), applied(SetPoseAt("o", 1_000, Pose(9, 9, 200, 9))))
        MoveKeyframe("o", 0, 2_000).apply(t) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        applied(MoveKeyframe("o", 0, 1_960), t).o.keyframes[0].timeMs shouldBe 1_960L
        val copied = applied(CopyKeyframe("o", 0, 2_000), t)
        copied.o.keyframes.map { it.timeMs } shouldBe listOf(0L, 1_000L, 2_000L)
        val replaced = applied(CopyKeyframe("o", 0, 3_000), t)
        replaced.o.keyframes.map { it.timeMs } shouldBe listOf(0L, 2_000L)
        replaced.o.keyframes[1].pose shouldBe Pose(9, 9, 200, 9)
    }

    @Test
    fun `motion is set per keyframe`() {
        val t = applied(AddKeyframe("o", 1_000))
        applied(SetKeyframeMotion("o", 0, true), t).o.keyframes[0].ease shouldBe true
        SetKeyframeMotion("o", 0, false).apply(t) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
    }

    @Test
    fun `at most fifty keyframes and the limit is reported`() {
        val many = List(KEYFRAME_MAX) { Keyframe(it * 70L, timeline.o.basePose) }
        val t = timeline.copy(overlayLane = listOf(timeline.o.withKeyframes(many)))
        AddKeyframe("o", 1_000 + 3_500).apply(t) shouldBe CommandResult.Rejected(RejectReason.LimitReached)
        SetPoseAt("o", 1_000 + 3_555, timeline.o.basePose).apply(t) shouldBe
            CommandResult.Rejected(RejectReason.LimitReached)
        applied(SetPoseAt("o", 1_000 + 70, Pose(5, 5, 100, 5)), t).o.keyframes.size shouldBe KEYFRAME_MAX
        TimelineInvariants.violations(t).none { "too many" in it } shouldBe true
    }

    @Test
    fun `split gives each half the animation of its own time and trim keeps the edges`() {
        val whole = applied(
            SetPoseAt("o", 5_000, Pose(900, 900, 500, 0)),
            applied(SetPoseAt("o", 1_000, Pose(100, 100, 100, 100))),
        )
        var n = 0
        val split = applied(Split("o", 3_000, { "n${n++}" }), whole)
        val (first, second) = split.overlayLane
        first.durationMs shouldBe 2_000
        first.poseAt(0) shouldBe whole.o.poseAt(0)
        first.poseAt(2_000) shouldBe whole.o.poseAt(2_000)
        second.poseAt(0) shouldBe whole.o.poseAt(2_000)
        second.poseAt(2_000) shouldBe whole.o.poseAt(4_000)
        val trimmed = applied(TrimOverlay("o", 2_000, 4_000), whole)
        trimmed.o.keyframes.first().timeMs shouldBe 0
        trimmed.o.poseAt(0) shouldBe whole.o.poseAt(1_000)
        trimmed.o.poseAt(2_000) shouldBe whole.o.poseAt(3_000)
        val text = applied(SetPoseAt("t", 2_500, Pose(1, 1, 200, 100)), applied(SetPoseAt("t", 0, Pose(9, 9, 60, 100))))
        val trimmedText = applied(TrimText("t", 500, 2_000), text)
        trimmedText.t.poseAt(0) shouldBe text.t.poseAt(500)
        applied(Duplicate("o"), whole).overlayLane.all { it.keyframes == whole.o.keyframes } shouldBe true
    }

    @Test
    fun `invariants flag broken keyframe data`() {
        val close = listOf(Keyframe(0, Pose(1, 1, 100, 1)), Keyframe(10, Pose(2, 2, 100, 2)))
        val bad = timeline.copy(overlayLane = listOf(timeline.o.copy(keyframes = close)))
        TimelineInvariants.violations(bad).any { "less than a frame" in it } shouldBe true
        TimelineInvariants.violations(bad).any { "follow its first keyframe" in it } shouldBe true
    }
}
