package com.petruvideo.core.media.text

import android.graphics.Paint
import com.petruvideo.core.model.STICKER_SIZE
import com.petruvideo.core.model.StickerCatalog
import com.petruvideo.core.model.TextClip
import io.kotest.matchers.shouldBe
import org.junit.Test

/** SC-002 of feature 020: every symbol the phone says it can draw really draws as a visible picture. */
class StickerGlyphTest {
    @Test
    fun everySymbolThePhoneCanDrawProducesVisiblePixels() {
        val paint = Paint()
        val offered = StickerCatalog.all.filter { paint.hasGlyph(it.glyph) }
        (offered.size >= StickerCatalog.all.size / 2) shouldBe true
        val empty = offered.filter { sticker ->
            val clip = TextClip("s", sticker.glyph, 0, 3_000, sizePermille = STICKER_SIZE)
            val bitmap = TextRenderer.render(clip, 640, 360)
            var visible = 0
            for (y in 0 until bitmap.height step 2) for (x in 0 until bitmap.width step 2) {
                if (android.graphics.Color.alpha(bitmap.getPixel(x, y)) > 40) visible++
            }
            visible < MIN_VISIBLE
        }
        empty.map { it.label } shouldBe emptyList()
    }

    private companion object {
        const val MIN_VISIBLE = 40
    }
}
