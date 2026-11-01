package com.petruvideo.core.model.engine

import com.petruvideo.core.model.AudioClip
import com.petruvideo.core.model.AudioOrigin
import com.petruvideo.core.model.ClipFilter
import com.petruvideo.core.model.FilterLook
import com.petruvideo.core.model.TextClip
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VideoClip
import io.kotest.matchers.shouldBe
import org.junit.Test

class FilterCommandsTest {
    private val timeline = Timeline(
        "p",
        videoLane = listOf(VideoClip("a", "m", 0, 4_000, true), VideoClip("b", "m", 0, 3_000, false)),
        audioLane = listOf(AudioClip("au", "m", 0, 1_000, 0, origin = AudioOrigin.Added)),
        textLane = listOf(TextClip("tx", "t", 0, 1_000)),
    )

    private fun applied(c: Command, t: Timeline = timeline) = (c.apply(t) as CommandResult.Applied).timeline

    private fun Timeline.filterOf(id: String) = videoLane.first { it.id == id }.filter

    @Test
    fun `a look keeps the adjustments and resets the strength`() {
        val t = applied(SetFilterAdjustments("a", ClipFilter(brightness = 30, vignette = 20)))
        val withLook = applied(SetFilterIntensity("a", 40), applied(SetFilterLook("a", FilterLook.Warm), t))
        withLook.filterOf("a") shouldBe ClipFilter(FilterLook.Warm, 40, brightness = 30, vignette = 20)
        applied(SetFilterLook("a", FilterLook.Cool), withLook).filterOf("a") shouldBe
            ClipFilter(FilterLook.Cool, 100, brightness = 30, vignette = 20)
    }

    @Test
    fun `values are clamped and no change is rejected`() {
        applied(SetFilterAdjustments("a", ClipFilter(brightness = 900, vignette = -5))).filterOf("a") shouldBe
            ClipFilter(brightness = 100, vignette = 0)
        SetFilterLook("a", FilterLook.None).apply(timeline) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        ResetFilterAdjustments("a").apply(timeline) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
    }

    @Test
    fun `reset keeps the look and remove clears everything`() {
        val withLook = applied(SetFilterLook("a", FilterLook.Vintage))
        val t = applied(SetFilterAdjustments("a", ClipFilter(contrast = 50)), withLook)
        applied(ResetFilterAdjustments("a"), t).filterOf("a") shouldBe ClipFilter(FilterLook.Vintage)
        applied(RemoveFilter("a"), t).filterOf("a") shouldBe ClipFilter()
    }

    @Test
    fun `audio and text clips are rejected`() {
        SetFilterLook("au", FilterLook.Vivid).apply(timeline) shouldBe CommandResult.Rejected(RejectReason.NotFound)
        SetFilterLook("tx", FilterLook.Vivid).apply(timeline) shouldBe CommandResult.Rejected(RejectReason.NotFound)
    }

    @Test
    fun `apply to all copies one filter to every picture clip`() {
        val t = applied(SetFilterLook("a", FilterLook.Faded))
        val all = applied(ApplyFilterToAll("a"), t)
        all.videoLane.map { it.filter } shouldBe listOf(ClipFilter(FilterLook.Faded), ClipFilter(FilterLook.Faded))
        ApplyFilterToAll("a").apply(all) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        ApplyFilterToAll("a").apply(timeline.copy(videoLane = timeline.videoLane.take(1))) shouldBe
            CommandResult.Rejected(RejectReason.OutOfRange)
    }

    @Test
    fun `split and duplicate keep the filter`() {
        val t = applied(SetFilterLook("a", FilterLook.Cool))
        val ids = sequenceOf("x", "y").iterator()
        applied(Split("a", 2_000, ids::next), t).videoLane.take(2).map { it.filter.look } shouldBe
            listOf(FilterLook.Cool, FilterLook.Cool)
        applied(Duplicate("a") { "d" }, t).videoLane[1].filter.look shouldBe FilterLook.Cool
    }
}
