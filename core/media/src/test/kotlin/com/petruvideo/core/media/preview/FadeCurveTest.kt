package com.petruvideo.core.media.preview

import io.kotest.matchers.shouldBe
import org.junit.Test
import kotlin.math.abs

class FadeCurveTest {
    private fun at(frame: Long) = FadeCurve.factor(frame, 1_000, 2_000, 10_000)

    @Test
    fun `starts silent ends silent and is full in the middle`() {
        at(0) shouldBe 0f
        at(5_000) shouldBe 1f
        at(10_000) shouldBe 0f
        (abs(at(500) - 0.5f) < 0.01f) shouldBe true
        (abs(at(9_000) - 0.5f) < 0.01f) shouldBe true
    }

    @Test
    fun `no fades means no change`() {
        FadeCurve.factor(0, 0, 0, 10_000) shouldBe 1f
        FadeCurve.factor(9_999, 0, 0, 10_000) shouldBe 1f
    }

    @Test
    fun `the curve is monotonic and has no step larger than the slope allows`() {
        var previous = at(0)
        var biggest = 0f
        for (f in 1..1_000L) {
            val now = at(f)
            (now >= previous) shouldBe true
            biggest = maxOf(biggest, now - previous)
            previous = now
        }
        (biggest < 0.005f) shouldBe true
    }

    @Test
    fun `the position past the clip end is silent`() {
        FadeCurve.factor(12_000, 0, 2_000, 10_000) shouldBe 0f
    }
}
