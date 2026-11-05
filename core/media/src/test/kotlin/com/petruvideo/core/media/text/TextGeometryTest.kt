package com.petruvideo.core.media.text

import com.petruvideo.core.model.TextShadow
import io.kotest.matchers.shouldBe
import org.junit.Test
import kotlin.math.abs

class TextGeometryTest {
    @Test
    fun `no turn keeps the size and a right angle swaps it`() {
        TextGeometry.turnedSize(200, 100, 0) shouldBe (200 to 100)
        TextGeometry.turnedSize(200, 100, 180) shouldBe (200 to 100)
        TextGeometry.turnedSize(200, 100, -180) shouldBe (200 to 100)
        TextGeometry.turnedSize(200, 100, 90) shouldBe (100 to 200)
        TextGeometry.turnedSize(200, 100, -90) shouldBe (100 to 200)
    }

    @Test
    fun `a turn in between holds the whole block`() {
        val (w, h) = TextGeometry.turnedSize(200, 100, 45)
        // (200 + 100) * 0.7071 = 212.1
        (w in 212..213) shouldBe true
        (h in 212..213) shouldBe true
        val (w30, h30) = TextGeometry.turnedSize(200, 100, 30)
        (w30 > 200 && h30 > 100) shouldBe true
        TextGeometry.turnedSize(200, 100, 30) shouldBe TextGeometry.turnedSize(200, 100, -30)
    }

    private fun shadow(direction: Int) = TextShadow(enabled = true, distancePercent = 100, directionDegrees = direction)

    @Test
    fun `the shadow goes the way its direction says`() {
        val right = TextGeometry.shadowOffset(100f, shadow(0))
        (right.first > 0f && abs(right.second) < 0.001f) shouldBe true
        val down = TextGeometry.shadowOffset(100f, shadow(90))
        (down.second > 0f && abs(down.first) < 0.001f) shouldBe true
        (TextGeometry.shadowOffset(100f, shadow(180)).first < 0f) shouldBe true
        (TextGeometry.shadowOffset(100f, shadow(270)).second < 0f) shouldBe true
        val defaultOffset = TextGeometry.shadowOffset(100f, TextShadow(enabled = true))
        (defaultOffset.first > 0f && defaultOffset.second > 0f) shouldBe true
    }

    @Test
    fun `the reach is zero without a shadow and grows with distance and softness`() {
        TextGeometry.shadowReach(100f, TextShadow(enabled = false, distancePercent = 100)) shouldBe 0
        val near =
            TextGeometry.shadowReach(100f, TextShadow(enabled = true, distancePercent = 10, softnessPercent = 0))
        val far =
            TextGeometry.shadowReach(100f, TextShadow(enabled = true, distancePercent = 100, softnessPercent = 0))
        val soft =
            TextGeometry.shadowReach(100f, TextShadow(enabled = true, distancePercent = 10, softnessPercent = 100))
        (far > near) shouldBe true
        (soft > near) shouldBe true
    }

    @Test
    fun `the reach covers the offset and the blur so nothing is cut`() {
        val s = TextShadow(enabled = true, distancePercent = 100, softnessPercent = 100)
        val offset = TextGeometry.shadowOffset(100f, s)
        val farthest = maxOf(abs(offset.first), abs(offset.second)) + TextGeometry.shadowBlur(100f, s)
        (TextGeometry.shadowReach(100f, s) >= farthest) shouldBe true
    }
}
