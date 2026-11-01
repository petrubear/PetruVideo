package com.petruvideo.core.model

import io.kotest.matchers.shouldBe
import org.junit.Test

class TransformTest {
    @Test
    fun `a new clip has no transform and four turns come back`() {
        ClipTransform().isNeutral shouldBe true
        var t = ClipTransform()
        repeat(4) { t = t.rotated() }
        t shouldBe ClipTransform()
        ClipTransform().rotated().quarterTurns shouldBe 1
    }

    @Test
    fun `two flips of the same axis cancel and the crop mirrors with the picture`() {
        val crop = CropBox(100, 200, 600, 700)
        val t = ClipTransform(crop = crop)
        t.flippedHorizontally().crop shouldBe CropBox(400, 200, 900, 700)
        t.flippedHorizontally().flippedHorizontally() shouldBe t
        t.flippedVertically().crop shouldBe CropBox(100, 300, 600, 800)
    }

    @Test
    fun `a quarter turn moves the box to where its content went`() {
        // The top-left quarter of the picture is the top-right quarter after a clockwise quarter turn.
        CropBox(0, 0, 500, 500).turned() shouldBe CropBox(500, 0, 1000, 500)
        CropBox(0, 0, 500, 500).turned().turned().turned().turned() shouldBe CropBox(0, 0, 500, 500)
    }

    @Test
    fun `turning a mirrored picture swaps the mirrors`() {
        val t = ClipTransform(flipH = true).rotated()
        t shouldBe ClipTransform(quarterTurns = 1, flipV = true)
        var four = ClipTransform(flipH = true, crop = CropBox(100, 100, 500, 800))
        repeat(4) { four = four.rotated() }
        four shouldBe ClipTransform(flipH = true, crop = CropBox(100, 100, 500, 800))
    }

    @Test
    fun `turned size swaps for odd turns`() {
        ClipTransform(quarterTurns = 1).turnedSize(1920, 1080) shouldBe (1080 to 1920)
        ClipTransform(quarterTurns = 2).turnedSize(1920, 1080) shouldBe (1920 to 1080)
    }

    @Test
    fun `a box is kept inside the picture and at least 10 percent`() {
        CropBox(-50, -50, 2000, 2000).normalized() shouldBe CropBox()
        CropBox(500, 500, 520, 510).normalized().inRange shouldBe true
        CropBox(900, 0, 950, 1000).inRange shouldBe false
        CropBox().isFull shouldBe true
    }

    @Test
    fun `a preset shape fits the picture, is centered and keeps its shape`() {
        val box = CropBox.forShape(1f, 1920, 1080)
        // 1:1 on a 16:9 picture: the full height and 56.25% of the width.
        box.heightPermille shouldBe 1000
        (kotlin.math.abs(box.widthPermille - 562) <= 1) shouldBe true
        (kotlin.math.abs(box.left - (1000 - box.right)) <= 1) shouldBe true
        val tall = CropBox.forShape(9f / 16f, 1920, 1080)
        val pixelsW = tall.widthPermille / 1000f * 1920
        val pixelsH = tall.heightPermille / 1000f * 1080
        (kotlin.math.abs(pixelsW / pixelsH - 9f / 16f) < 0.01f) shouldBe true
    }

    private fun pixelShape(box: CropBox, w: Int, h: Int) =
        (box.widthPermille / 1000f * w) / (box.heightPermille / 1000f * h)

    @Test
    fun `a free resize moves the dragged edges and a move keeps the size inside`() {
        val box = CropBox(200, 200, 800, 800)
        box.resized(CropHandle.Right, 100, 999, null, 1920, 1080) shouldBe CropBox(200, 200, 900, 800)
        box.resized(CropHandle.TopLeft, -300, -300, null, 1920, 1080) shouldBe CropBox(0, 0, 800, 800)
        box.moved(500, -500) shouldBe CropBox(400, 0, 1000, 600)
    }

    @Test
    fun `a preset shape is kept while the box is resized from every handle`() {
        val shape = CropShape.Story.aspect!!
        val start = CropBox.forShape(shape, 1920, 1080)
        for (handle in CropHandle.entries) {
            for ((dx, dy) in listOf(60 to 60, -80 to -40, 300 to 300, -500 to 500)) {
                val box = start.resized(handle, dx, dy, shape, 1920, 1080)
                box.inRange shouldBe true
                (kotlin.math.abs(pixelShape(box, 1920, 1080) - shape) < 0.03f) shouldBe true
            }
        }
    }
}
