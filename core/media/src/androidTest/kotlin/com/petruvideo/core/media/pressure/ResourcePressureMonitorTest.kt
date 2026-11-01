package com.petruvideo.core.media.pressure

import android.content.ComponentCallbacks2
import android.os.PowerManager
import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ResourcePressureMonitorTest {
    @Test
    fun startsNormalWithFullQuality() {
        val monitor = ResourcePressureMonitor()
        monitor.level.value shouldBe PressureLevel.Normal
        monitor.quality.value shouldBe PreviewQuality.Full
    }

    @Test
    fun theUsersSwitchKeepsQualityReducedWithoutAnyPressureOrNotice() = runBlocking<Unit> {
        val monitor = ResourcePressureMonitor()
        monitor.notices.test {
            monitor.setForcedReduced(true)
            monitor.quality.value shouldBe PreviewQuality.Reduced
            monitor.level.value shouldBe PressureLevel.Normal
            monitor.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW)
            awaitItem() shouldBe PressureNotice.QualityReduced
            monitor.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE)
            monitor.level.value shouldBe PressureLevel.Normal
            monitor.quality.value shouldBe PreviewQuality.Reduced
            awaitItem() shouldBe PressureNotice.QualityRestored
            monitor.setForcedReduced(false)
            monitor.quality.value shouldBe PreviewQuality.Full
            expectNoEvents()
        }
    }

    @Test
    fun turningTheSwitchOffKeepsReducedWhilePressureRemains() {
        val monitor = ResourcePressureMonitor()
        monitor.setForcedReduced(true)
        monitor.onThermalStatus(PowerManager.THERMAL_STATUS_MODERATE)
        monitor.setForcedReduced(false)
        monitor.quality.value shouldBe PreviewQuality.Reduced
    }

    @Test
    fun memoryAndThermalSignalsMapToLevels() {
        PressureLevel.from(ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW, null) shouldBe PressureLevel.Reduced
        PressureLevel.from(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL, null) shouldBe PressureLevel.Critical
        PressureLevel.from(null, PowerManager.THERMAL_STATUS_LIGHT) shouldBe PressureLevel.Normal
        PressureLevel.from(null, PowerManager.THERMAL_STATUS_MODERATE) shouldBe PressureLevel.Reduced
        PressureLevel.from(null, PowerManager.THERMAL_STATUS_SEVERE) shouldBe PressureLevel.Critical
        PressureLevel.from(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN, PowerManager.THERMAL_STATUS_NONE) shouldBe
            PressureLevel.Normal
    }

    @Test
    fun reducedPressureLowersResolutionFrameRateAndThumbnailSizeAndSaysSo() = runBlocking<Unit> {
        val monitor = ResourcePressureMonitor()
        monitor.notices.test {
            monitor.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW)
            monitor.level.value shouldBe PressureLevel.Reduced
            monitor.quality.value shouldBe PreviewQuality.Reduced
            (PreviewQuality.Reduced.previewScale < PreviewQuality.Full.previewScale) shouldBe true
            (PreviewQuality.Reduced.previewFps < PreviewQuality.Full.previewFps) shouldBe true
            (PreviewQuality.Reduced.thumbnailPx < PreviewQuality.Full.thumbnailPx) shouldBe true
            awaitItem() shouldBe PressureNotice.QualityReduced
        }
    }

    @Test
    fun criticalPressureAsksPlaybackToPauseAndRecoveryRestoresQuality() = runBlocking<Unit> {
        val monitor = ResourcePressureMonitor()
        monitor.notices.test {
            monitor.onThermalStatus(PowerManager.THERMAL_STATUS_SEVERE)
            monitor.level.value shouldBe PressureLevel.Critical
            awaitItem() shouldBe PressureNotice.PlaybackPaused

            monitor.onThermalStatus(PowerManager.THERMAL_STATUS_NONE)
            monitor.level.value shouldBe PressureLevel.Normal
            monitor.quality.value shouldBe PreviewQuality.Full
            awaitItem() shouldBe PressureNotice.QualityRestored
        }
    }

    @Test
    fun repeatedSignalsAtTheSameLevelDoNotRepeatTheNotice() = runBlocking<Unit> {
        val monitor = ResourcePressureMonitor()
        monitor.notices.test {
            monitor.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW)
            monitor.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW)
            awaitItem() shouldBe PressureNotice.QualityReduced
            expectNoEvents()
        }
    }
}
