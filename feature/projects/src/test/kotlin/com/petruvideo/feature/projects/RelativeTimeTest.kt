package com.petruvideo.feature.projects

import io.kotest.matchers.shouldBe
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import org.junit.Test

class RelativeTimeTest {
    private val zone = ZoneId.of("UTC")
    private fun at(month: Int, day: Int, hour: Int, minute: Int = 0) =
        ZonedDateTime.of(2026, month, day, hour, minute, 0, 0, zone).toInstant().toEpochMilli()

    private fun format(then: Long, now: Long) = RelativeTime.ago(then, now, zone, Locale.US)

    @Test
    fun `under a minute is just now`() {
        format(at(10, 30, 12, 0), at(10, 30, 12, 0) + 30_000) shouldBe RelativeTime.Ago.JustNow
    }

    @Test
    fun `minutes ago`() {
        format(at(10, 30, 11, 58), at(10, 30, 12, 0)) shouldBe RelativeTime.Ago.Minutes(2)
    }

    @Test
    fun `hours ago on the same day`() {
        format(at(10, 30, 9, 0), at(10, 30, 12, 0)) shouldBe RelativeTime.Ago.Hours(3)
    }

    @Test
    fun `previous calendar day is yesterday`() {
        format(at(10, 29, 23, 0), at(10, 30, 1, 0)) shouldBe RelativeTime.Ago.Yesterday
    }

    @Test
    fun `older dates show month and day`() {
        format(at(10, 24, 9, 0), at(10, 30, 12, 0)) shouldBe RelativeTime.Ago.Date("Oct 24")
    }
}
