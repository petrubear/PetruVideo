package com.petruvideo.core.media.text

import com.petruvideo.core.model.AnimKind
import com.petruvideo.core.model.TextAnimation
import com.petruvideo.core.model.TextClip
import io.kotest.matchers.shouldBe
import org.junit.Test

class TextAnimatorTest {
    private val base = TextClip("a", "hi", 1_000, 4_000)

    @Test
    fun `hidden outside the window and plain inside without animations`() {
        TextAnimator.at(base, 999).visible shouldBe false
        TextAnimator.at(base, 5_000).visible shouldBe false
        TextAnimator.at(base, 1_000) shouldBe TextLook(true, 1f, 0f, 1f)
        TextAnimator.at(base, 3_000) shouldBe TextLook(true, 1f, 0f, 1f)
    }

    @Test
    fun `fade in rises from zero and fade out falls to zero`() {
        val c = base.copy(animIn = TextAnimation(AnimKind.Fade, 1_000), animOut = TextAnimation(AnimKind.Fade, 1_000))
        TextAnimator.at(c, 1_000).alpha shouldBe 0f
        (TextAnimator.at(c, 1_500).alpha in 0.45f..0.55f) shouldBe true
        TextAnimator.at(c, 2_000).alpha shouldBe 1f
        (TextAnimator.at(c, 4_999).alpha < 0.01f) shouldBe true
    }

    @Test
    fun `slides move vertically and pop scales`() {
        val c = base.copy(
            animIn = TextAnimation(AnimKind.SlideUp, 500), animOut = TextAnimation(AnimKind.SlideDown, 500),
        )
        (TextAnimator.at(c, 1_000).offsetY > 0.07f) shouldBe true
        TextAnimator.at(c, 3_000).offsetY shouldBe 0f
        (TextAnimator.at(c, 4_990).offsetY > 0.07f) shouldBe true
        val pop = base.copy(animIn = TextAnimation(AnimKind.Pop, 500))
        (TextAnimator.at(pop, 1_000).scale < 0.65f) shouldBe true
        TextAnimator.at(pop, 1_600).scale shouldBe 1f
    }

    @Test
    fun `short clips limit the animations to half each`() {
        val long = TextAnimation(AnimKind.Fade, 1_500)
        val c = TextClip("a", "x", 0, 400, animIn = long, animOut = long)
        TextAnimator.at(c, 200).alpha shouldBe 1f
        (TextAnimator.at(c, 100).alpha < 1f) shouldBe true
    }

    @Test
    fun `alpha never leaves zero to one`() {
        val c = base.copy(animIn = TextAnimation(AnimKind.Pop, 1_500), animOut = TextAnimation(AnimKind.Pop, 1_500))
        for (t in 1_000L until 5_000L step 37) TextAnimator.at(c, t).alpha.let { (it in 0f..1f) shouldBe true }
    }
}
