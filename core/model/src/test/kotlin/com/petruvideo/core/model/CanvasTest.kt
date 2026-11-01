package com.petruvideo.core.model

import com.petruvideo.core.model.engine.CommandResult
import com.petruvideo.core.model.engine.RejectReason
import com.petruvideo.core.model.engine.SetCanvas
import io.kotest.matchers.shouldBe
import org.junit.Test

class CanvasTest {
    private val timeline = Timeline("p")

    @Test
    fun `a project starts with a black background`() {
        timeline.canvas shouldBe CanvasBackground.Black
    }

    @Test
    fun `the background is set in one step and the same choice is refused`() {
        val blur = CanvasBackground.Blur(BlurStrength.Strong)
        val t = (SetCanvas(blur).apply(timeline) as CommandResult.Applied).timeline
        t.canvas shouldBe blur
        SetCanvas(blur).apply(t) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        val colour = (SetCanvas(CanvasBackground.Colour(0xFFE53935.toInt())).apply(t) as CommandResult.Applied).timeline
        colour.canvas shouldBe CanvasBackground.Colour(0xFFE53935.toInt())
    }

    @Test
    fun `blur radii grow with the strength`() {
        (BlurStrength.Light.radiusPermille < BlurStrength.Medium.radiusPermille) shouldBe true
        (BlurStrength.Medium.radiusPermille < BlurStrength.Strong.radiusPermille) shouldBe true
    }
}
