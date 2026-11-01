package com.petruvideo.core.model.engine

import com.petruvideo.core.model.ClipEffect
import com.petruvideo.core.model.EffectKind
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VideoClip
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.Test

class StackedEffectsTest {
    private val timeline = Timeline("p", videoLane = listOf(VideoClip("a", "m", 0, 4_000, true)))

    private fun applied(c: Command, t: Timeline = timeline) = (c.apply(t) as CommandResult.Applied).timeline
    private val Timeline.effects get() = videoLane[0].effects

    @Test
    fun `effects stack in the order they are added and a repeated kind changes its strength`() {
        var t = applied(SetClipEffect("a", EffectKind.Blur, 30))
        t = applied(SetClipEffect("a", EffectKind.Grain, 60), t)
        t = applied(SetClipEffect("a", EffectKind.Blur, 80), t)
        t.effects shouldBe listOf(ClipEffect(EffectKind.Blur, 80), ClipEffect(EffectKind.Grain, 60))
        TimelineInvariants.violations(t).filterNot { "unknown media" in it }.shouldBeEmpty()
    }

    @Test
    fun `at most three effects`() {
        var t = timeline
        for (k in listOf(EffectKind.Blur, EffectKind.Glow, EffectKind.Vhs)) t = applied(SetClipEffect("a", k, 50), t)
        SetClipEffect("a", EffectKind.Grain, 50).apply(t) shouldBe CommandResult.Rejected(RejectReason.LimitReached)
        (SetClipEffect("a", EffectKind.Glow, 10).apply(t) is CommandResult.Applied) shouldBe true
    }

    @Test
    fun `remove takes out one kind or all of them`() {
        var t = applied(SetClipEffect("a", EffectKind.Blur, 30))
        t = applied(SetClipEffect("a", EffectKind.Glow, 60), t)
        applied(ResetClipEffect("a", EffectKind.Blur), t).effects shouldBe listOf(ClipEffect(EffectKind.Glow, 60))
        applied(ResetClipEffect("a"), t).effects.shouldBeEmpty()
        ResetClipEffect("a", EffectKind.Mirror).apply(t) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
    }

    @Test
    fun `split and duplicate keep all of them`() {
        var t = applied(SetClipEffect("a", EffectKind.Blur, 30))
        t = applied(SetClipEffect("a", EffectKind.Glow, 60), t)
        applied(Split("a", 2_000), t).videoLane.map { it.effects } shouldBe List(2) { t.effects }
        applied(Duplicate("a"), t).videoLane[1].effects shouldBe t.effects
    }
}
