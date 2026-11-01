package com.petruvideo.feature.editor

import com.petruvideo.core.model.AudioFx
import com.petruvideo.core.model.EqPreset
import com.petruvideo.core.model.EqPresets
import com.petruvideo.core.model.Echo
import com.petruvideo.core.model.Fade
import com.petruvideo.core.model.NoiseReduction
import com.petruvideo.core.model.VoiceFx
import com.petruvideo.feature.editor.ui.badgeParts
import com.petruvideo.feature.editor.ui.collapseBadges
import io.kotest.matchers.shouldBe
import org.junit.Test

class ClipBadgeTest {
    @Test
    fun `neutral clips have no badges`() {
        badgeParts(1000, 100, false, AudioFx()) shouldBe emptyList()
    }

    @Test
    fun `each tool adds its own badge`() {
        val fx = AudioFx(
            fade = Fade(100, 0),
            noise = NoiseReduction(true),
            eq = EqPresets.settings(EqPreset.Warm),
            voice = VoiceFx(echo = Echo(true)),
        )
        badgeParts(2000, 40, true, fx) shouldBe listOf("2x", "40%", "Rv", "Fd", "Nr", "Eq", "Vx")
    }

    @Test
    fun `extra badges collapse into a count`() {
        collapseBadges(listOf("2x", "40%")) shouldBe listOf("2x", "40%")
        collapseBadges(listOf("2x", "40%", "Rv")) shouldBe listOf("2x", "40%", "Rv")
        collapseBadges(listOf("2x", "40%", "Rv", "Fd", "Nr")) shouldBe listOf("2x", "40%", "+3")
    }
}
