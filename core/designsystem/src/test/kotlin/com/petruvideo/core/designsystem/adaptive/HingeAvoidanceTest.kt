package com.petruvideo.core.designsystem.adaptive

import io.kotest.matchers.shouldBe
import org.junit.Test

class HingeAvoidanceTest {
    private val vertical = HingeBounds(left = 1_000, top = 0, right = 1_040, bottom = 1_800)
    private val horizontal = HingeBounds(left = 0, top = 900, right = 2_000, bottom = 940)

    @Test
    fun `vertical and horizontal hinges are told apart`() {
        HingeAvoidance.isVertical(vertical) shouldBe true
        HingeAvoidance.isVertical(horizontal) shouldBe false
    }

    @Test
    fun `the split leaves the hinge gap empty and uses the whole window`() {
        val (left, gap, right) = HingeAvoidance.verticalSplit(vertical, 2_040)
        left shouldBe 1_000
        gap shouldBe 40
        right shouldBe 1_000
        HingeAvoidance.intersects(0, 0, left, 1_800, vertical) shouldBe false
        HingeAvoidance.intersects(left + gap, 0, 2_040, 1_800, vertical) shouldBe false
    }

    @Test
    fun `content crossing the hinge is detected`() {
        HingeAvoidance.intersects(900, 100, 1_100, 200, vertical) shouldBe true
        HingeAvoidance.intersects(0, 880, 2_000, 960, horizontal) shouldBe true
        HingeAvoidance.intersects(0, 0, 2_000, 900, horizontal) shouldBe false
    }
}
