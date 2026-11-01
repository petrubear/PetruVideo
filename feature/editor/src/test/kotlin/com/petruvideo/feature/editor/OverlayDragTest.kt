package com.petruvideo.feature.editor

import com.petruvideo.core.model.OverlayClip
import com.petruvideo.feature.editor.ui.overlayDragStep
import io.kotest.matchers.shouldBe
import org.junit.Test

class OverlayDragTest {
    private val clip = OverlayClip("o", "m", 0, 2_000, 0)

    @Test
    fun `near the center it snaps with guides and elsewhere it follows the finger inside the frame`() {
        val snapped = overlayDragStep(clip, 503f, 498f, 1f, 1_000f, 500f, 8f)
        snapped.clip.xPermille shouldBe 500
        snapped.clip.yPermille shouldBe 500
        snapped.guideX shouldBe true
        val free = overlayDragStep(clip, 120f, 1_400f, 1f, 1_000f, 500f, 8f)
        free.clip.xPermille shouldBe 120
        free.clip.yPermille shouldBe 1_000
        free.guideX shouldBe false
    }

    @Test
    fun `pinching keeps the width inside its limits`() {
        overlayDragStep(clip, 100f, 100f, 100f, 1_000f, 500f, 8f).clip.widthPermille shouldBe 1_000
        overlayDragStep(clip, 100f, 100f, 0.01f, 1_000f, 500f, 8f).clip.widthPermille shouldBe 50
        overlayDragStep(clip, 100f, 100f, 2f, 1_000f, 500f, 8f).clip.widthPermille shouldBe 700
    }
}
