package com.petruvideo.core.model

import io.kotest.matchers.shouldBe
import org.junit.Test
import kotlin.math.abs

class CompressorTest {
    @Test
    fun `the default is the Voice preset, switched off`() {
        val c = Compressor()
        c.enabled shouldBe false
        c.preset shouldBe CompressorPreset.Voice
        c.normalized().preset shouldBe CompressorPreset.Voice
        CompressorPresets.settings(CompressorPreset.Voice).copy(enabled = false) shouldBe c
        c.inRange shouldBe true
    }

    @Test
    fun `each named preset matches itself and has its own level`() {
        CompressorPresets.all.forEach {
            val settings = CompressorPresets.settings(it, enabled = true)
            settings.preset shouldBe it
            settings.normalized().preset shouldBe it
        }
        CompressorPresets.settings(CompressorPreset.Gentle).levelDb shouldBe 0
        CompressorPresets.settings(CompressorPreset.Podcast).levelDb shouldBe 3
    }

    @Test
    fun `strength zero and one hundred are the ends of the shortcut`() {
        val none = Compressor().withStrength(0)
        none.thresholdDb shouldBe 0
        none.ratioTenths shouldBe COMP_RATIO_MIN_TENTHS
        none.strength shouldBe 0
        val full = Compressor().withStrength(100)
        full.thresholdDb shouldBe -40
        full.ratioTenths shouldBe 80
        full.strength shouldBe 100
    }

    @Test
    fun `strength round trips within one point and follows the advanced values`() {
        for (s in 0..100) abs(Compressor().withStrength(s).strength - s) shouldBeAtMost 1
        val deeper = Compressor().withStrength(50).copy(thresholdDb = -40)
        (deeper.strength > Compressor().withStrength(50).strength) shouldBe true
    }

    @Test
    fun `changing any value away from a preset makes it Custom`() {
        val voice = CompressorPresets.settings(CompressorPreset.Voice, enabled = true)
        voice.copy(levelDb = voice.levelDb + 1).normalized().preset shouldBe CompressorPreset.Custom
        voice.copy(releaseMs = 300).normalized().preset shouldBe CompressorPreset.Custom
    }

    @Test
    fun `values are clamped to their ranges`() {
        val c = Compressor(true, CompressorPreset.Custom, -99, 999, 0, 5_000, 40).normalized()
        c.thresholdDb shouldBe COMP_THRESHOLD_MIN_DB
        c.ratioTenths shouldBe COMP_RATIO_MAX_TENTHS
        c.attackMs shouldBe COMP_ATTACK_MIN_MS
        c.releaseMs shouldBe COMP_RELEASE_MAX_MS
        c.levelDb shouldBe COMP_LEVEL_MAX_DB
        Compressor(thresholdDb = 5).inRange shouldBe false
    }

    @Test
    fun `it is audible only when on and doing something`() {
        Compressor().audible shouldBe false
        Compressor(enabled = true).audible shouldBe true
        Compressor(enabled = true).withStrength(0).copy(levelDb = 0).audible shouldBe false
        Compressor(enabled = true).withStrength(0).copy(levelDb = 3).audible shouldBe true
    }

    @Test
    fun `a default AudioFx is in range and keeps the compressor off`() {
        AudioFx().compressor.enabled shouldBe false
        AudioFx().inRange shouldBe true
    }

    private infix fun Int.shouldBeAtMost(max: Int) = (this <= max) shouldBe true
}
