package com.petruvideo.core.model.export

import io.kotest.matchers.shouldBe
import org.junit.Test

class BitrateTableTest {
    @Test
    fun `recommended bitrates at 30 fps`() {
        BitrateTable.videoBitrate(Resolution.R720, FrameRate.F30, Quality.Recommended) shouldBe 6_000_000
        BitrateTable.videoBitrate(Resolution.R1080, FrameRate.F30, Quality.Recommended) shouldBe 12_000_000
        BitrateTable.videoBitrate(Resolution.R2K, FrameRate.F30, Quality.Recommended) shouldBe 20_000_000
        BitrateTable.videoBitrate(Resolution.R4K, FrameRate.F30, Quality.Recommended) shouldBe 45_000_000
    }

    @Test
    fun `quality scales the bitrate`() {
        BitrateTable.videoBitrate(Resolution.R1080, FrameRate.F30, Quality.Smaller) shouldBe 7_200_000
        BitrateTable.videoBitrate(Resolution.R1080, FrameRate.F30, Quality.Higher) shouldBe 18_000_000
    }

    @Test
    fun `frame rate scales the bitrate with an exponent below one`() {
        val at24 = BitrateTable.videoBitrate(Resolution.R1080, FrameRate.F24, Quality.Recommended)
        val at60 = BitrateTable.videoBitrate(Resolution.R1080, FrameRate.F60, Quality.Recommended)
        (at24 in 9_000_000..11_000_000) shouldBe true
        (at60 in 19_000_000..21_000_000) shouldBe true
    }

    @Test
    fun `estimated size is video plus audio bitrate times the duration`() {
        // (12 + 0.128) Mbit/s for 60 s = 90.96 MB
        BitrateTable.estimatedBytes(12_000_000, 60_000) shouldBe 90_960_000
        BitrateTable.estimatedBytes(12_000_000, 0) shouldBe 0
    }

    @Test
    fun `a higher resolution never has a lower bitrate`() {
        val rates = Resolution.entries.map { BitrateTable.videoBitrate(it, FrameRate.F30, Quality.Recommended) }
        rates shouldBe rates.sorted()
    }
}
