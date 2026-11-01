package com.petruvideo.core.model

import io.kotest.matchers.shouldBe
import kotlin.math.abs
import org.junit.Test

class SpeedCurveTest {
    private val flat2x = SpeedCurve(listOf(SpeedPoint(0, 2_000), SpeedPoint(4_000, 2_000)))
    private val ramp = SpeedCurve(listOf(SpeedPoint(0, 1_000), SpeedPoint(3_000, 500), SpeedPoint(6_000, 2_000)))

    private fun near(a: Long, b: Long, tolerance: Long) = abs(a - b) <= tolerance

    @Test
    fun `a flat curve is the constant speed`() {
        flat2x.timelineMs shouldBe 2_000
        flat2x.sourceAt(1_000) shouldBe 2_000
        flat2x.timelineAt(2_000) shouldBe 1_000
        flat2x.speedAt(1_234.0) shouldBe 2.0
    }

    @Test
    fun `the speed changes linearly between points and the end speeds hold`() {
        ramp.speedAt(0.0) shouldBe 1.0
        ramp.speedAt(1_500.0) shouldBe 0.75
        ramp.speedAt(4_500.0) shouldBe 1.25
        ramp.speedAt(99_999.0) shouldBe 2.0
    }

    @Test
    fun `source and timeline mappings are inverses within a step`() {
        for (t in 0..ramp.timelineMs step 250) {
            val back = ramp.timelineAt(ramp.sourceAt(t))
            near(back, t, 3) shouldBe true
        }
        ramp.sourceAt(ramp.timelineMs) shouldBe 6_000
        ramp.sourceAt(0) shouldBe 0
    }

    @Test
    fun `the length matches the integral of one over the speed`() {
        // For a linear speed between s0 and s1 over d ms the time is d * ln(s1 / s0) / (s1 - s0).
        fun segment(d: Double, s0: Double, s1: Double) = d * kotlin.math.ln(s1 / s0) / (s1 - s0)
        val exact = segment(3_000.0, 1.0, 0.5) + segment(3_000.0, 0.5, 2.0)
        near(ramp.timelineMs, exact.toLong(), 60) shouldBe true
    }

    @Test
    fun `the steps cover the clip exactly and are short on the timeline`() {
        ramp.steps.first().startMs shouldBe 0
        ramp.steps.last().endMs shouldBe 6_000
        ramp.steps.zipWithNext().all { (a, b) -> a.endMs == b.startMs } shouldBe true
        ramp.steps.all { it.timelineMs <= 600.0 } shouldBe true
    }

    @Test
    fun `windowing keeps the speed at every moment and the parts add up to the whole`() {
        val first = ramp.windowed(0, 2_500)
        val second = ramp.windowed(2_500, 6_000)
        first.sourceLengthMs shouldBe 2_500
        second.sourceLengthMs shouldBe 3_500
        for (p in listOf(0.0, 800.0, 2_400.0)) (abs(first.speedAt(p) - ramp.speedAt(p)) < 0.002) shouldBe true
        for (p in listOf(0.0, 1_000.0, 3_400.0)) (abs(second.speedAt(p) - ramp.speedAt(p + 2_500)) < 0.01) shouldBe true
        near(first.timelineMs + second.timelineMs, ramp.timelineMs, 40) shouldBe true
    }

    @Test
    fun `a window beyond the curve holds the end speed`() {
        val longer = ramp.windowed(0, 8_000)
        longer.sourceLengthMs shouldBe 8_000
        longer.speedAt(7_500.0) shouldBe 2.0
    }

    @Test
    fun `presets scale to the clip and stay in range`() {
        for (preset in SpeedPreset.entries) {
            val curve = SpeedCurve.preset(preset, 6_000)
            curve.isValid shouldBe true
            curve.sourceLengthMs shouldBe 6_000
        }
        (SpeedCurve.preset(SpeedPreset.Bullet, 6_000).timelineMs > 6_000L) shouldBe true
        (SpeedCurve.preset(SpeedPreset.FlashIn, 6_000).timelineMs > 6_000L) shouldBe true
    }

    @Test
    fun `text form round trips and bad text is no curve`() {
        SpeedCurve.decode(SpeedCurve.encode(ramp)) shouldBe ramp
        SpeedCurve.decode(null) shouldBe null
        SpeedCurve.decode("garbage") shouldBe null
        SpeedCurve.decode("0:1000") shouldBe null
    }

    @Test
    fun `a clip with a curve is as long as the curve and shows the source it says`() {
        val clip = VideoClip("a", "m", 1_000, 7_000, true, speedCurve = ramp)
        clip.lengthMs shouldBe ramp.timelineMs
        clip.sourceTimeAt(0) shouldBe 1_000
        clip.sourceTimeAt(clip.lengthMs) shouldBe 7_000
        clip.copy(reversed = true).sourceTimeAt(0) shouldBe 7_000
    }

    @Test
    fun `points can be moved, added and removed within the rules`() {
        val moved = ramp.withPointMoved(1, 3_500, 250)!!
        moved.points[1] shouldBe SpeedPoint(3_500, 250)
        ramp.withPointMoved(1, 9_000, 99_999)!!.points[1] shouldBe SpeedPoint(5_900, 10_000)
        ramp.withPointMoved(0, 500, 400)!!.points[0] shouldBe SpeedPoint(0, 400)
        ramp.withPointMoved(2, 1_000, 400)!!.points[2].positionMs shouldBe 6_000
        ramp.withPointMoved(7, 1, 1) shouldBe null
        val added = ramp.withPointAdded(1_500)!!
        added.points.size shouldBe 4
        added.points[1] shouldBe SpeedPoint(1_500, 750)
        ramp.withPointAdded(3_050) shouldBe null
        ramp.withPointAdded(0) shouldBe null
        ramp.withPointRemoved(1)!!.points.size shouldBe 2
        ramp.withPointRemoved(0) shouldBe null
        flat2x.withPointRemoved(1) shouldBe null
    }

    @Test
    fun `at most 12 points`() {
        var curve = SpeedCurve(listOf(SpeedPoint(0, 1_000), SpeedPoint(20_000, 1_000)))
        var at = 500L
        while (curve.points.size < CURVE_POINTS_MAX) { curve = curve.withPointAdded(at)!!; at += 500 }
        curve.withPointAdded(at) shouldBe null
    }
}
