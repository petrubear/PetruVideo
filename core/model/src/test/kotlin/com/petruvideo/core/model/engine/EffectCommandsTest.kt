package com.petruvideo.core.model.engine

import com.petruvideo.core.model.ClipEffect
import com.petruvideo.core.model.EffectKind
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.Transition
import com.petruvideo.core.model.TransitionKind
import com.petruvideo.core.model.VideoClip
import io.kotest.matchers.shouldBe
import org.junit.Test

class EffectCommandsTest {
    private val timeline = Timeline(
        "p",
        videoLane = listOf(
            VideoClip("a", "m", 0, 4_000, true),
            VideoClip("b", "m", 0, 3_000, true),
            VideoClip("c", "m", 0, 400, true),
        ),
    )

    private fun applied(c: Command, t: Timeline = timeline) = (c.apply(t) as CommandResult.Applied).timeline

    @Test
    fun `a transition shortens the project and moves later clips earlier`() {
        val t = applied(SetTransition("a", TransitionKind.Dissolve, 1_000))
        t.videoLengthMs shouldBe 7_400 - 1_000
        t.videoStartOf("b") shouldBe 3_000
        t.videoStartOf("c") shouldBe 6_000
        applied(RemoveTransition("a"), t).videoLengthMs shouldBe 7_400
    }

    @Test
    fun `the length is limited to half of the shorter neighbour with a notice`() {
        val r = SetTransition("b", TransitionKind.Slide, 2_000).apply(timeline) as CommandResult.Applied
        r.timeline.videoLane[1].transitionOut shouldBe Transition(TransitionKind.Slide, 200)
        r.notices shouldBe listOf(Notice.TransitionLimited)
        val long = SetTransition("a", TransitionKind.Zoom, 2_000).apply(timeline) as CommandResult.Applied
        long.timeline.videoLane[0].transitionOut shouldBe Transition(TransitionKind.Zoom, 1_500)
    }

    @Test
    fun `no neighbour, unknown clip, tiny neighbours and no change are rejected`() {
        SetTransition("c", TransitionKind.Dissolve, 500).apply(timeline) shouldBe
            CommandResult.Rejected(RejectReason.NotFound)
        SetTransition("x", TransitionKind.Dissolve, 500).apply(timeline) shouldBe
            CommandResult.Rejected(RejectReason.NotFound)
        val shortOne = timeline.videoLane[2].copy(sourceEndMs = 300)
        val tiny = timeline.copy(videoLane = listOf(timeline.videoLane[0], shortOne))
        SetTransition("a", TransitionKind.Dissolve, 500).apply(tiny) shouldBe
            CommandResult.Rejected(RejectReason.BelowMinimumLength)
        val t = applied(SetTransition("a", TransitionKind.Dissolve, 500))
        SetTransition("a", TransitionKind.Dissolve, 500).apply(t) shouldBe
            CommandResult.Rejected(RejectReason.OutOfRange)
        RemoveTransition("b").apply(t) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
    }

    @Test
    fun `a stored transition longer than the limit is cut down when a neighbour shrinks`() {
        val t = applied(SetTransition("a", TransitionKind.Dissolve, 1_500))
        val lane = t.videoLane.mapIndexed { i, c -> if (i == 1) c.copy(sourceEndMs = 1_000) else c }
        val shorter = t.copy(videoLane = lane)
        shorter.transitionLengthAfter(0) shouldBe 500
    }

    @Test
    fun `an effect is clamped, replaced, and reset`() {
        val t = applied(SetClipEffect("a", EffectKind.Blur, 150))
        t.videoLane[0].effect shouldBe ClipEffect(EffectKind.Blur, 100)
        applied(SetClipEffect("a", EffectKind.Shake, 20), t).videoLane[0].effect shouldBe
            ClipEffect(EffectKind.Shake, 20)
        SetClipEffect("a", EffectKind.Blur, 100).apply(t) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        applied(ResetClipEffect("a"), t).videoLane[0].effect shouldBe null
        ResetClipEffect("b").apply(t) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
    }

    @Test
    fun `split, duplicate, delete and move keep transitions valid`() {
        val first = applied(SetTransition("a", TransitionKind.Dissolve, 500))
        val t = applied(SetTransition("b", TransitionKind.Cover, 200), first)
        val split = applied(Split("a", 2_000, { "s1" }), t)
        split.videoLane.map { it.transitionOut != null } shouldBe listOf(false, true, true, false)
        val copy = applied(Duplicate("a") { "d" }, t)
        copy.videoLane.map { it.transitionOut != null } shouldBe listOf(false, true, true, false)
        val deleted = applied(Delete("b"), t)
        deleted.videoLane.map { it.transitionOut != null } shouldBe listOf(false, false)
        val moved = applied(MoveVideoClip("c", 0), t)
        moved.videoLane.all { it.transitionOut == null } shouldBe true
    }
}
