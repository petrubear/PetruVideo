package com.petruvideo.core.model

import io.kotest.matchers.shouldBe
import org.junit.Test

class TimeFormatTest {
    @Test
    fun `formats minutes and seconds`() {
        formatDuration(0) shouldBe "0:00"
        formatDuration(8_000) shouldBe "0:08"
        formatDuration(65_000) shouldBe "1:05"
        formatDuration(102_000) shouldBe "1:42"
    }

    @Test
    fun `rounds down to the second and adds hours past one hour`() {
        formatDuration(1_999) shouldBe "0:01"
        formatDuration(3_600_000) shouldBe "1:00:00"
        formatDuration(3_725_000) shouldBe "1:02:05"
    }

    @Test
    fun `negative values clamp to zero`() {
        formatDuration(-5) shouldBe "0:00"
    }
}
