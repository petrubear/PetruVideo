package com.petruvideo.core.model

import io.kotest.matchers.shouldBe
import org.junit.Test

class ReverbTest {
    @Test
    fun `defaults are off with the medium room numbers`() {
        Reverb() shouldBe Reverb(false, ReverbPreset.MediumRoom, 50, 1_500, 50, 20, 30)
        Reverb().inRange shouldBe true
    }

    @Test
    fun `presets have their table values`() {
        ReverbPresets.settings(ReverbPreset.Hall) shouldBe Reverb(false, ReverbPreset.Hall, 80, 3_500, 40, 30, 35)
        ReverbPresets.settings(ReverbPreset.Cave, enabled = true) shouldBe
            Reverb(true, ReverbPreset.Cave, 95, 7_000, 30, 60, 45)
        ReverbPresets.all.size shouldBe 5
    }

    @Test
    fun `matching numbers give the preset and others give Custom`() {
        ReverbPresets.match(ReverbPresets.settings(ReverbPreset.Plate)) shouldBe ReverbPreset.Plate
        ReverbPresets.match(ReverbPresets.settings(ReverbPreset.Plate).copy(damping = 21)) shouldBe ReverbPreset.Custom
    }

    @Test
    fun `normalizing clamps every number and recomputes the preset`() {
        val wild = Reverb(true, ReverbPreset.Hall, 500, 50, -3, 900, 101).normalized()
        wild shouldBe Reverb(true, ReverbPreset.Custom, 100, 200, 0, 200, 100)
        Reverb(true, ReverbPreset.Hall, 80, 3_500, 40, 30, 35).normalized().preset shouldBe ReverbPreset.Hall
    }
}
