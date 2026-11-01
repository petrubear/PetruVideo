package com.petruvideo.core.model

import com.petruvideo.core.model.engine.TextRows
import io.kotest.matchers.shouldBe
import org.junit.Test

class TextTest {
    private fun clip(id: String, start: Long, duration: Long) = TextClip(id, "t", start, duration)

    @Test
    fun `defaults are in range and end is start plus duration`() {
        val c = clip("a", 1_000, 5_000)
        c.inRange shouldBe true
        c.endMs shouldBe 6_000
        c.style.font shouldBe TextFont.Sans
        c.xPermille shouldBe 500
    }

    @Test
    fun `every range is clamped`() {
        val c = TextClip(
            "a", "x".repeat(900), -5, 1, 2_000, -3, 9_999,
            TextStyle(background = TextBackground(true, 0, 300), outline = TextOutline(true, 0, 900)),
            TextAnimation(AnimKind.Fade, 10), TextAnimation(AnimKind.Pop, 90_000),
        ).normalized()
        c.text.length shouldBe TEXT_MAX_CHARS
        c.startMs shouldBe 0
        c.durationMs shouldBe TEXT_MIN_MS
        c.xPermille shouldBe 1000
        c.yPermille shouldBe 0
        c.sizePermille shouldBe TEXT_SIZE_MAX
        c.style.background.opacityPercent shouldBe 100
        c.style.outline.widthPermille shouldBe TEXT_OUTLINE_MAX
        c.animIn.lengthMs shouldBe ANIM_MIN_MS
        c.animOut.lengthMs shouldBe ANIM_MAX_MS
    }

    @Test
    fun `animations are limited to half the clip each and none counts as zero`() {
        val c = clip("a", 0, 1_000).copy(animIn = TextAnimation(AnimKind.Fade, 900), animOut = TextAnimation(AnimKind.Pop, 400))
        c.effectiveAnimMs() shouldBe (500L to 400L)
        c.copy(animIn = TextAnimation()).effectiveAnimMs() shouldBe (0L to 400L)
        c.animIn.lengthMs shouldBe 900
    }

    @Test
    fun `the project length follows the last text end`() {
        Timeline("p", textLane = listOf(clip("a", 2_000, 3_000), clip("b", 0, 1_000))).totalDurationMs shouldBe 5_000
    }

    @Test
    fun `overlapping clips get separate rows by first fit`() {
        val rows = TextRows.assign(listOf(clip("a", 0, 4_000), clip("b", 1_000, 2_000), clip("c", 4_000, 1_000), clip("d", 2_000, 3_000)))
        rows["a"] shouldBe 0
        rows["b"] shouldBe 1
        rows["c"] shouldBe 0
        rows["d"] shouldBe 2
        TextRows.rowCount(emptyList()) shouldBe 0
    }
}
