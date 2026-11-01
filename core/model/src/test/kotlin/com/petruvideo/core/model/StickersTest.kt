package com.petruvideo.core.model

import com.petruvideo.core.model.engine.AddCaptions
import com.petruvideo.core.model.engine.AddSticker
import com.petruvideo.core.model.engine.CommandResult
import io.kotest.matchers.shouldBe
import org.junit.Test

class StickersTest {
    private val timeline = Timeline("p", videoLane = listOf(VideoClip("a", "m", 0, 10_000, false)))

    @Test
    fun `every category has stickers and the labels and glyphs are unique`() {
        StickerCategory.entries.all { StickerCatalog.of(it).isNotEmpty() } shouldBe true
        StickerCatalog.all.map { it.glyph }.toSet().size shouldBe StickerCatalog.all.size
        StickerCatalog.all.map { it.category to it.label }.toSet().size shouldBe StickerCatalog.all.size
    }

    @Test
    fun `only shapes carry a colour`() {
        StickerCatalog.all.all { (it.color != null) == (it.category == StickerCategory.Shapes) } shouldBe true
    }

    @Test
    fun `a sticker becomes a centered large text layer of three seconds that is not an auto caption`() {
        val star = StickerCatalog.of(StickerCategory.Shapes).first { it.label == "Star" }
        val t = (AddSticker(2_000, star) { "s1" }.apply(timeline) as CommandResult.Applied).timeline
        val clip = t.textLane.single()
        clip.text shouldBe "★"
        clip.startMs shouldBe 2_000
        clip.durationMs shouldBe 3_000
        clip.sizePermille shouldBe STICKER_SIZE
        clip.style.colorArgb shouldBe star.color
        clip.caption shouldBe false
        clip.xPermille shouldBe 500
        clip.yPermille shouldBe 500
    }

    @Test
    fun `near the project end the layer is shortened and an emoji keeps the default colour`() {
        val face = StickerCatalog.of(StickerCategory.Faces).first()
        val t = (AddSticker(9_000, face).apply(timeline) as CommandResult.Applied).timeline
        t.textLane.single().durationMs shouldBe 1_000
        t.textLane.single().style.colorArgb shouldBe TextStyle().colorArgb
    }

    @Test
    fun `replacing auto captions leaves stickers alone`() {
        val face = StickerCatalog.all.first()
        val withSticker = (AddSticker(0, face).apply(timeline) as CommandResult.Applied).timeline
        val lines = listOf(CaptionLine(1_000, 2_000, "Hi"))
        val captioned = (AddCaptions(lines, replace = true).apply(withSticker) as CommandResult.Applied).timeline
        captioned.textLane.map { it.text } shouldBe listOf(face.glyph, "Hi")
    }
}
