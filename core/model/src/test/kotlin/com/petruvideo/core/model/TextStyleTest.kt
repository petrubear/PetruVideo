package com.petruvideo.core.model

import io.kotest.matchers.shouldBe
import org.junit.Test

class TextStyleTest {
    @Test
    fun `the defaults leave the text as it was`() {
        val style = TextStyle()
        style.shadow.enabled shouldBe false
        style.rotationDegrees shouldBe 0
        style.letterSpacingPercent shouldBe 0
        style.lineSpacingPercent shouldBe 100
        style.inRange shouldBe true
        style.normalized() shouldBe style
    }

    @Test
    fun `every new value is clamped to its range`() {
        val wild = TextStyle(
            shadow = TextShadow(
                true, opacityPercent = 500, softnessPercent = -4, distancePercent = 900, directionDegrees = -90,
            ),
            rotationDegrees = 400,
            letterSpacingPercent = -50,
            lineSpacingPercent = 999,
        ).normalized()
        wild.shadow.opacityPercent shouldBe 100
        wild.shadow.softnessPercent shouldBe 0
        wild.shadow.distancePercent shouldBe 100
        wild.shadow.directionDegrees shouldBe 270
        wild.rotationDegrees shouldBe TEXT_ROTATION_MAX
        wild.letterSpacingPercent shouldBe TEXT_LETTER_SPACING_MIN
        wild.lineSpacingPercent shouldBe TEXT_LINE_SPACING_MAX
        TextStyle(rotationDegrees = 200).inRange shouldBe false
        TextStyle(lineSpacingPercent = 10).inRange shouldBe false
    }

    @Test
    fun `a direction is kept within a full turn`() {
        TextStyle(shadow = TextShadow(directionDegrees = 725)).normalized().shadow.directionDegrees shouldBe 5
    }

    @Test
    fun `rotation snaps near the straight angles and nowhere else`() {
        snapRotation(2) shouldBe 0
        snapRotation(-3) shouldBe 0
        snapRotation(88) shouldBe 90
        snapRotation(-92) shouldBe -90
        snapRotation(178) shouldBe 180
        snapRotation(-179) shouldBe -180
        snapRotation(4) shouldBe 4
        snapRotation(45) shouldBe 45
        snapRotation(-120) shouldBe -120
    }
}
