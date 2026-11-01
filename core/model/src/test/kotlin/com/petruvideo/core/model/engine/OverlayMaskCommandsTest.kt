package com.petruvideo.core.model.engine

import com.petruvideo.core.model.MaskKind
import com.petruvideo.core.model.OverlayClip
import com.petruvideo.core.model.OverlayMask
import com.petruvideo.core.model.Timeline
import io.kotest.matchers.shouldBe
import org.junit.Test

class OverlayMaskCommandsTest {
    private val timeline = Timeline("p", overlayLane = listOf(OverlayClip("o", "m", 0, 6_000, 1_000)))

    private fun applied(c: Command, t: Timeline = timeline) = (c.apply(t) as CommandResult.Applied).timeline

    @Test
    fun `an overlay starts unmasked and the mask is set clamped in one step`() {
        timeline.overlayLane[0].mask shouldBe OverlayMask()
        applied(SetOverlayMask("o", OverlayMask(MaskKind.Circle, 500, -3, true))).overlayLane[0].mask shouldBe
            OverlayMask(MaskKind.Circle, 100, 0, true)
    }

    @Test
    fun `the same mask and an unknown clip are rejected`() {
        SetOverlayMask("o", OverlayMask()).apply(timeline) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        SetOverlayMask("x", OverlayMask(MaskKind.Linear)).apply(timeline) shouldBe
            CommandResult.Rejected(RejectReason.NotFound)
    }

    @Test
    fun `split and duplicate keep the mask`() {
        val masked = applied(SetOverlayMask("o", OverlayMask(MaskKind.Rectangle, 40, 10, false)))
        applied(Split("o", 3_000), masked).overlayLane.map { it.mask } shouldBe
            List(2) { OverlayMask(MaskKind.Rectangle, 40, 10, false) }
        applied(Duplicate("o"), masked).overlayLane[1].mask shouldBe masked.overlayLane[0].mask
    }
}
