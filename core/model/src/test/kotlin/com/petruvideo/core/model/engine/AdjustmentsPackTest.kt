package com.petruvideo.core.model.engine

import com.petruvideo.core.model.ClipFilter
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VideoClip
import io.kotest.matchers.shouldBe
import org.junit.Test

class AdjustmentsPackTest {
    private val timeline = Timeline(
        "p", videoLane = listOf(VideoClip("a", "m", 0, 4_000, true), VideoClip("b", "m", 0, 3_000, false)),
    )

    private fun applied(c: Command, t: Timeline = timeline) = (c.apply(t) as CommandResult.Applied).timeline
    private fun Timeline.filterOf(id: String) = videoLane.first { it.id == id }.filter

    private val all = ClipFilter(
        exposure = 30, gamma = -20, highlights = -40, shadows = 50, tint = 10, hue = -60, sharpen = 70,
    )

    @Test
    fun `the new adjustments are neutral by default and make a filter active when set`() {
        ClipFilter().isNeutral shouldBe true
        ClipFilter(exposure = 1).isNeutral shouldBe false
        ClipFilter(sharpen = 1).adjustmentsNeutral shouldBe false
    }

    @Test
    fun `all seven are set clamped and reset`() {
        applied(SetFilterAdjustments("a", all)).filterOf("a") shouldBe all
        applied(SetFilterAdjustments("a", ClipFilter(exposure = 900, hue = -900, sharpen = -4))).filterOf("a") shouldBe
            ClipFilter(exposure = 100, hue = -100, sharpen = 0)
        val reset = applied(ResetFilterAdjustments("a"), applied(SetFilterAdjustments("a", all)))
        reset.filterOf("a") shouldBe ClipFilter()
    }

    @Test
    fun `apply to all and remove carry them`() {
        val t = applied(ApplyFilterToAll("a"), applied(SetFilterAdjustments("a", all)))
        t.filterOf("b") shouldBe all
        applied(RemoveFilter("b"), t).filterOf("b") shouldBe ClipFilter()
    }

    @Test
    fun `split and duplicate keep them`() {
        val t = applied(SetFilterAdjustments("a", all))
        applied(Split("a", 2_000), t).videoLane.take(2).map { it.filter } shouldBe listOf(all, all)
        applied(Duplicate("a"), t).videoLane[1].filter shouldBe all
    }
}
