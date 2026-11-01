package com.petruvideo.core.media.text

import com.petruvideo.core.model.TextAlign
import com.petruvideo.core.model.TextBackground
import com.petruvideo.core.model.TextClip
import com.petruvideo.core.model.TextOutline
import com.petruvideo.core.model.TextStyle
import io.kotest.matchers.shouldBe
import org.junit.Test

class TextRendererTest {
    private val clip = TextClip("a", "TITLE", 0, 2_000, sizePermille = 100)

    private fun opaquePixels(bitmap: android.graphics.Bitmap): Int {
        var n = 0
        for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) if (bitmap.getPixel(x, y) ushr 24 != 0) n++
        return n
    }

    @Test
    fun wordsRenderAsAWideBitmapInsideTheFrameMargin() {
        val bitmap = TextRenderer.render(clip, 1280, 720)
        check(bitmap.width > bitmap.height * 2) { "bitmap ${bitmap.width}x${bitmap.height}" }
        (bitmap.width <= 1280 * 0.9) shouldBe true
        (bitmap.height <= 720 * 0.9) shouldBe true
        check(opaquePixels(bitmap) > 500) { "bitmap ${bitmap.width}x${bitmap.height} opaque ${opaquePixels(bitmap)}" }
    }

    @Test
    fun noWordsGiveAMinimalTransparentBitmap() {
        val bitmap = TextRenderer.render(clip.copy(text = ""), 1280, 720)
        bitmap.width shouldBe 1
        opaquePixels(bitmap) shouldBe 0
    }

    @Test
    fun longTextWrapsInsideTheMargin() {
        val long = clip.copy(text = "The quick brown fox jumps over the lazy dog ".repeat(6), sizePermille = 80)
        val bitmap = TextRenderer.render(long, 1280, 720)
        (bitmap.width <= 1280 * 0.9) shouldBe true
        (bitmap.height <= 720 * 0.9) shouldBe true
        (bitmap.height > 80) shouldBe true
    }

    @Test
    fun theSizeIsRelativeToTheFrameHeight() {
        val small = TextRenderer.render(clip, 640, 360)
        val big = TextRenderer.render(clip, 1280, 720)
        (big.width.toFloat() / small.width in 1.9f..2.1f) shouldBe true
        (big.height.toFloat() / small.height in 1.9f..2.1f) shouldBe true
    }

    @Test
    fun backgroundAndOutlineAddPixelsAndAlignmentMovesTheText() {
        val plain = TextRenderer.render(clip, 1280, 720)
        val boxed = TextRenderer.render(clip.copy(style = TextStyle(background = TextBackground(true))), 1280, 720)
        val outlined = TextRenderer.render(clip.copy(style = TextStyle(outline = TextOutline(true, widthPermille = 30))), 1280, 720)
        (opaquePixels(boxed) > opaquePixels(plain) * 2) shouldBe true
        (opaquePixels(outlined) > opaquePixels(plain)) shouldBe true
        val wrapped = clip.copy(text = "aaaa bbbb cccc dddd eeee ffff gggg hhhh iiii jjjj kkkk llll mmmm nnnn oooo pppp", sizePermille = 150)
        val left = TextRenderer.render(wrapped.copy(style = TextStyle(align = TextAlign.Left)), 1280, 720)
        val right = TextRenderer.render(wrapped.copy(style = TextStyle(align = TextAlign.Right)), 1280, 720)
        left.width shouldBe right.width
    }

    @Test
    fun rightToLeftEmojiAndAccentsDoNotCrash() {
        for (text in listOf("שלום עולם", "مرحبا بالعالم", "你好，世界", "Café ñandú 😀🎉", "a\nb\n\nc")) {
            val bitmap = TextRenderer.render(clip.copy(text = text), 1280, 720)
            (bitmap.width in 1..1280 && bitmap.height in 1..720) shouldBe true
        }
    }

    @Test
    fun placementStaysInsideTheMargin() {
        val corner = clip.copy(xPermille = 0, yPermille = 1_000)
        val box = TextRenderer.place(corner, 400, 100, 1280, 720)
        (box.centerX - 200 >= 1280 * 0.05f - 0.5f) shouldBe true
        (box.centerY + 50 <= 720 * 0.95f + 0.5f) shouldBe true
        val tooBig = TextRenderer.place(clip, 1270, 100, 1280, 720)
        tooBig.centerX shouldBe 640f
    }
}
