package com.petruvideo.feature.editor

import com.petruvideo.core.model.TextClip
import com.petruvideo.feature.editor.ui.dragStep
import io.kotest.matchers.shouldBe
import org.junit.Test

class TextDragTest {
    private val clip = TextClip("t", "x", 0, 1_000)

    @Test
    fun `near the center it snaps and shows the guides`() {
        val step = dragStep(clip, 505f, 497f, 1f, 1_000, 500, 8f)
        step.clip.xPermille shouldBe 500
        step.clip.yPermille shouldBe 500
        step.guideX shouldBe true
        step.guideY shouldBe true
        step.rawX shouldBe 505f
    }

    @Test
    fun `away from the center it follows the finger and stays inside the frame`() {
        val step = dragStep(clip, 200f, 1_200f, 1f, 1_000, 500, 8f)
        step.clip.xPermille shouldBe 200
        step.clip.yPermille shouldBe 1_000
        step.guideX shouldBe false
        dragStep(clip, -50f, 300f, 1f, 1_000, 500, 8f).clip.xPermille shouldBe 0
    }

    @Test
    fun `pinching scales within the size limits`() {
        dragStep(clip, 100f, 100f, 2f, 1_000, 500, 8f).clip.sizePermille shouldBe 120
        dragStep(clip, 100f, 100f, 100f, 1_000, 500, 8f).clip.sizePermille shouldBe 300
        dragStep(clip, 100f, 100f, 0.01f, 1_000, 500, 8f).clip.sizePermille shouldBe 20
    }
}
