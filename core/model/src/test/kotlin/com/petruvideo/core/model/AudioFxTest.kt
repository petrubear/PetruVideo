package com.petruvideo.core.model

import io.kotest.matchers.shouldBe
import org.junit.Test

class AudioFxTest {
    @Test
    fun `defaults change nothing`() {
        val fx = AudioFx()
        fx.fade.isNeutral shouldBe true
        fx.noise.enabled shouldBe false
        fx.eq.isFlat shouldBe true
        fx.voice.anyOn shouldBe false
        fx.inRange shouldBe true
    }

    @Test
    fun `each fade is limited to half the clip`() {
        Fade(3_000, 9_000).limitedTo(8_000) shouldBe Fade(3_000, 4_000)
        Fade(500, 500).limitedTo(8_000) shouldBe Fade(500, 500)
        Fade(40_000, 0).limitedTo(120_000) shouldBe Fade(FADE_MAX_MS, 0)
    }

    @Test
    fun `eq presets match and custom is detected`() {
        EqPresets.all.forEach { EqPresets.match(EqPresets.settings(it)) shouldBe it }
        Equalizer(EqPreset.Flat, 8, 0, -2).normalized().preset shouldBe EqPreset.Custom
        EqPresets.settings(EqPreset.BassBoost).preset shouldBe EqPreset.BassBoost
    }

    @Test
    fun `eq bands are clamped`() {
        Equalizer(lowDb = 40, midDb = -40, highDb = 3).normalized() shouldBe Equalizer(EqPreset.Custom, 12, -12, 3)
    }

    @Test
    fun `voice settings are clamped and kept when off`() {
        val voice = VoiceFx(Echo(false, 5_000, 9), Robot(false, 300), Pitch(true, -40), Bass(false, -5)).normalized()
        voice.echo shouldBe Echo(false, ECHO_DELAY_MAX_MS, ECHO_REPEATS_MAX)
        voice.robot.intensity shouldBe 100
        voice.pitch.semitones shouldBe -12
        voice.bass.amount shouldBe 0
        voice.inRange shouldBe true
    }
}
