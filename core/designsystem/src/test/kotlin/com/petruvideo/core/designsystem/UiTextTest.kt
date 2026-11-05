package com.petruvideo.core.designsystem

import io.kotest.matchers.shouldBe
import org.junit.Test

class UiTextTest {
    @Test
    fun `a resource text keeps its id and arguments and compares by value`() {
        UiText.Res(7, "a", 2) shouldBe UiText.Res(7, listOf("a", 2))
        UiText.Res(7).args shouldBe emptyList()
    }

    @Test
    fun `a plural keeps its count and arguments`() {
        val text = UiText.Plural(3, 2, listOf("x"))
        text.count shouldBe 2
        text.args shouldBe listOf("x")
    }

    @Test
    fun `a raw text is kept as written`() {
        UiText.Raw("Mi proyecto").value shouldBe "Mi proyecto"
    }
}
