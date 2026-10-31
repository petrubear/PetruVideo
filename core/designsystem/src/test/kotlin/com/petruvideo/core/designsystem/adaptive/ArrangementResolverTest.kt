package com.petruvideo.core.designsystem.adaptive

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Test

class ArrangementResolverTest {
    private val hingeH = HingeBounds(left = 0, top = 900, right = 2000, bottom = 960)
    private val hingeV = HingeBounds(left = 1000, top = 0, right = 1060, bottom = 1800)

    private fun input(
        widthDp: Int,
        heightDp: Int = 800,
        fold: FoldState = FoldState.None,
        orientation: FoldOrientation? = null,
        hinge: HingeBounds? = null,
        separating: Boolean = false,
    ) = ArrangementInput(widthDp, heightDp, fold, orientation, hinge, separating)

    @Test
    fun `compact width resolves to Compact without wide content`() {
        ArrangementResolver.resolve(input(widthDp = 411)) shouldBe Arrangement.Compact(wideContent = false)
        ArrangementResolver.resolve(input(widthDp = 599)) shouldBe Arrangement.Compact(wideContent = false)
    }

    @Test
    fun `medium width 600 to 839 resolves to Compact with wide content`() {
        ArrangementResolver.resolve(input(widthDp = 600)) shouldBe Arrangement.Compact(wideContent = true)
        ArrangementResolver.resolve(input(widthDp = 839)) shouldBe Arrangement.Compact(wideContent = true)
    }

    @Test
    fun `expanded width from 840 dp resolves to Expanded when flat`() {
        ArrangementResolver.resolve(input(widthDp = 840)) shouldBe Arrangement.Expanded(hinge = null)
        ArrangementResolver.resolve(input(widthDp = 960, fold = FoldState.Flat)) shouldBe Arrangement.Expanded(null)
    }

    @Test
    fun `half-opened horizontal fold resolves to Tabletop with the hinge bounds`() {
        val result = ArrangementResolver.resolve(
            input(
                widthDp = 900,
                fold = FoldState.HalfOpened,
                orientation = FoldOrientation.Horizontal,
                hinge = hingeH,
                separating = true,
            ),
        )
        result shouldBe Arrangement.Tabletop(hinge = hingeH)
    }

    @Test
    fun `half-opened horizontal fold without hinge bounds is not Tabletop`() {
        val result = ArrangementResolver.resolve(
            input(widthDp = 900, fold = FoldState.HalfOpened, orientation = FoldOrientation.Horizontal, hinge = null),
        )
        result.shouldBeInstanceOf<Arrangement.Expanded>()
    }

    @Test
    fun `separating vertical hinge in book posture returns the hinge for avoidance`() {
        val result = ArrangementResolver.resolve(
            input(
                widthDp = 900,
                fold = FoldState.HalfOpened,
                orientation = FoldOrientation.Vertical,
                hinge = hingeV,
                separating = true,
            ),
        )
        result shouldBe Arrangement.Expanded(hinge = hingeV)
    }

    @Test
    fun `flat unfolded with a non-separating fold does not report a hinge`() {
        val result = ArrangementResolver.resolve(
            input(
                widthDp = 900,
                fold = FoldState.Flat,
                orientation = FoldOrientation.Vertical,
                hinge = hingeV,
                separating = false,
            ),
        )
        result shouldBe Arrangement.Expanded(hinge = null)
    }

    @Test
    fun `cover screen resolves to Compact`() {
        ArrangementResolver.resolve(input(widthDp = 360, heightDp = 780)) shouldBe Arrangement.Compact(false)
    }

    @Test
    fun `the arrangement is chosen from window size and posture, not the device`() {
        ArrangementResolver.resolve(input(widthDp = 700, fold = FoldState.Flat)) shouldBe Arrangement.Compact(true)
    }
}
