package com.petruvideo.core.data.db

import com.petruvideo.core.model.TextClip
import com.petruvideo.core.model.TextShadow
import com.petruvideo.core.model.TextStyle
import io.kotest.matchers.shouldBe
import org.junit.Test

class TextStyleMapperTest {
    @Test
    fun `a text keeps its rotation shadow and spacing through the database`() {
        val style = TextStyle(
            shadow = TextShadow(true, 0xFF112233.toInt(), 70, 55, 25, 135),
            rotationDegrees = -37,
            letterSpacingPercent = 22,
            lineSpacingPercent = 130,
        )
        val clip = TextClip("t", "Hello", 100, 2_000, style = style)
        clip.toEntity("p").toModel() shouldBe clip
    }

    @Test
    fun `a plain text has the neutral values in its columns`() {
        val entity = TextClip("t", "Hello", 0, 1_000).toEntity("p")
        entity.rotation shouldBe 0
        entity.shEnabled shouldBe false
        entity.letterSpacing shouldBe 0
        entity.lineSpacing shouldBe 100
        entity.toModel().style shouldBe TextStyle()
    }
}
