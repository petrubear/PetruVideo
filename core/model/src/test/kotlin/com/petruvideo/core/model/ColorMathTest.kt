package com.petruvideo.core.model

import io.kotest.matchers.shouldBe
import org.junit.Test
import kotlin.math.abs

class ColorMathTest {
    private fun apply(m: FloatArray, r: Float, g: Float, b: Float) = floatArrayOf(
        m[0] * r + m[4] * g + m[8] * b + m[12],
        m[1] * r + m[5] * g + m[9] * b + m[13],
        m[2] * r + m[6] * g + m[10] * b + m[14],
    )

    private fun near(a: Float, b: Float, eps: Float = 0.002f) = abs(a - b) < eps

    @Test
    fun `neutral filters give the identity`() {
        ColorMath.matrix(ClipFilter()).toList() shouldBe ColorMath.identity().toList()
        ColorMath.matrix(ClipFilter(look = FilterLook.Vivid, intensity = 0)).toList() shouldBe
            ColorMath.identity().toList()
        ClipFilter(look = FilterLook.Warm, intensity = 0).isNeutral shouldBe true
        ClipFilter(vignette = 1).isNeutral shouldBe false
    }

    @Test
    fun `black and white gives equal channels at full strength and a halfway mix at half`() {
        val bw = ColorMath.matrix(ClipFilter(look = FilterLook.BlackAndWhite))
        val out = apply(bw, 0.8f, 0.3f, 0.1f)
        near(out[0], out[1]) shouldBe true
        near(out[1], out[2]) shouldBe true
        val halfFilter = ClipFilter(look = FilterLook.BlackAndWhite, intensity = 50)
        val half = apply(ColorMath.matrix(halfFilter), 0.8f, 0.3f, 0.1f)
        (half[0] < 0.8f && half[0] > out[0]) shouldBe true
    }

    @Test
    fun `brightness moves all channels by an offset in the last column`() {
        val m = ColorMath.matrix(ClipFilter(brightness = 100))
        near(m[12], 0.35f) shouldBe true
        near(apply(m, 0.2f, 0.4f, 0.6f)[1], 0.75f) shouldBe true
        near(apply(ColorMath.matrix(ClipFilter(brightness = -100)), 0.2f, 0.4f, 0.6f)[0], -0.15f) shouldBe true
    }

    @Test
    fun `contrast keeps mid gray and saturation zero removes color`() {
        near(apply(ColorMath.matrix(ClipFilter(contrast = 80)), 0.5f, 0.5f, 0.5f)[0], 0.5f) shouldBe true
        val gray = apply(ColorMath.matrix(ClipFilter(saturation = -100)), 0.9f, 0.2f, 0.4f)
        near(gray[0], gray[1]) shouldBe true
        near(gray[1], gray[2]) shouldBe true
    }

    @Test
    fun `warmth raises red and lowers blue and cool does the opposite`() {
        val warm = apply(ColorMath.matrix(ClipFilter(warmth = 100)), 0.5f, 0.5f, 0.5f)
        (warm[0] > 0.5f && warm[2] < 0.5f) shouldBe true
        val cool = apply(ColorMath.matrix(ClipFilter(warmth = -100)), 0.5f, 0.5f, 0.5f)
        (cool[0] < 0.5f && cool[2] > 0.5f) shouldBe true
        val look = apply(ColorMath.matrix(ClipFilter(look = FilterLook.Warm)), 0.5f, 0.5f, 0.5f)
        (look[0] > look[2]) shouldBe true
    }

    @Test
    fun `every look changes the picture and stays finite`() {
        for (look in FilterLook.entries.drop(1)) {
            val m = ColorMath.matrix(ClipFilter(look = look))
            m.all { it.isFinite() } shouldBe true
            (m.toList() != ColorMath.identity().toList()) shouldBe true
        }
    }

    @Test
    fun `ranges are clamped and the structural value tells active from neutral`() {
        ClipFilter(brightness = 900, vignette = -4, intensity = 300).normalized() shouldBe
            ClipFilter(brightness = 100, vignette = 0, intensity = 100)
        ClipFilter(look = FilterLook.Faded, contrast = 5).structural() shouldBe ClipFilter.ACTIVE
        ClipFilter(look = FilterLook.Vivid, brightness = 40).structural() shouldBe ClipFilter.ACTIVE
        ClipFilter().structural() shouldBe ClipFilter()
        near(ColorMath.vignetteAlpha(ClipFilter(vignette = 100)), 0.85f) shouldBe true
    }
}
