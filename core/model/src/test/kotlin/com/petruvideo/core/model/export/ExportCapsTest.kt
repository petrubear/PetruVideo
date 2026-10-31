package com.petruvideo.core.model.export

import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.Fingerprint
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.ProjectFormat
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VideoClip
import io.kotest.matchers.shouldBe
import org.junit.Test

class ExportCapsTest {
    private val device = DeviceLimits(maxShortSide = 2160, maxFps = 60)

    private fun media(id: String, w: Int, h: Int, fps: Float?, kind: MediaKind = MediaKind.Video) = MediaItem(
        id, kind, "content://$id", "$id.mp4", if (kind == MediaKind.Photo) null else 10_000, w, h, fps, true,
        Fingerprint(1, 1, 10_000, "video/mp4"), Availability.Available,
    )

    private fun timeline(vararg items: MediaItem, projectFps: Int = 30) = Timeline(
        projectId = "p",
        format = ProjectFormat(1920, 1080, projectFps),
        media = items.associateBy { it.id },
        videoLane = items.map { VideoClip("c-${it.id}", it.id, 0, 5_000, attachedAudio = true) },
    )

    @Test
    fun `1080p 30 footage caps at 1080p and 30`() {
        val caps = ExportCaps.from(timeline(media("a", 1920, 1080, 29.97f)))
        caps shouldBe ExportCaps(Resolution.R1080, FrameRate.F30)
    }

    @Test
    fun `a 4K 60 clip among 1080p clips lifts both caps`() {
        val caps = ExportCaps.from(timeline(media("a", 1920, 1080, 30f), media("b", 3840, 2160, 60f)))
        caps shouldBe ExportCaps(Resolution.R4K, FrameRate.F60)
    }

    @Test
    fun `portrait footage counts by its short side`() {
        ExportCaps.from(timeline(media("a", 1080, 1920, 30f))).maxResolution shouldBe Resolution.R1080
    }

    @Test
    fun `photos count by pixel size and at the project frame rate`() {
        val caps = ExportCaps.from(timeline(media("p", 4000, 3000, null, MediaKind.Photo), projectFps = 24))
        caps shouldBe ExportCaps(Resolution.R4K, FrameRate.F24)
    }

    @Test
    fun `small footage still allows the lowest options`() {
        ExportCaps.from(timeline(media("a", 640, 360, 15f))) shouldBe ExportCaps(Resolution.R720, FrameRate.F24)
        ExportCaps.from(Timeline("p")) shouldBe ExportCaps(Resolution.R720, FrameRate.F24)
    }

    @Test
    fun `options above the footage or the device are unavailable with a reason`() {
        val caps = ExportCaps(Resolution.R4K, FrameRate.F60)
        val limited = DeviceLimits(maxShortSide = 1080, maxFps = 30)
        val res = caps.resolutionOptions(limited)
        res.first { it.value == Resolution.R4K }.unavailable shouldBe Unavailable.NotSupported
        res.first { it.value == Resolution.R1080 }.allowed shouldBe true
        ExportCaps(Resolution.R1080, FrameRate.F30).resolutionOptions(device)
            .first { it.value == Resolution.R2K }.unavailable shouldBe Unavailable.AboveFootage
        caps.frameRateOptions(limited).first { it.value == FrameRate.F60 }.unavailable shouldBe Unavailable.NotSupported
    }

    private fun allowed(cap: FrameRate, device: DeviceLimits) =
        ExportCaps(Resolution.R1080, cap).frameRateOptions(device).filter { it.allowed }.map { it.value }

    @Test
    fun `a lower frame rate is offered only when it divides the footage rate`() {
        val device = DeviceLimits(2160, 60)
        allowed(FrameRate.F60, device) shouldBe listOf(FrameRate.F30, FrameRate.F60)
        allowed(FrameRate.F30, device) shouldBe listOf(FrameRate.F30)
        ExportCaps(Resolution.R1080, FrameRate.F30).frameRateOptions(device)
            .first { it.value == FrameRate.F24 }.unavailable shouldBe Unavailable.NotForFootage
        allowed(FrameRate.F24, device) shouldBe listOf(FrameRate.F24)
    }

    @Test
    fun `defaults are at most 1080p, the nearest allowed frame rate, and Recommended`() {
        ExportCaps(Resolution.R4K, FrameRate.F60).defaults(projectFps = 30, device = device) shouldBe
            ExportSettings(Resolution.R1080, FrameRate.F30, Quality.Recommended)
        ExportCaps(Resolution.R720, FrameRate.F24).defaults(30, device) shouldBe
            ExportSettings(Resolution.R720, FrameRate.F24, Quality.Recommended)
        val caps = ExportCaps(Resolution.R4K, FrameRate.F60)
        caps.defaults(projectFps = 50, device = device).frameRate shouldBe FrameRate.F60
    }

    @Test
    fun `a saved choice is lowered into new caps`() {
        val saved = ExportSettings(Resolution.R4K, FrameRate.F60, Quality.Higher)
        ExportCaps(Resolution.R1080, FrameRate.F30).clamp(saved, device) shouldBe
            ExportSettings(Resolution.R1080, FrameRate.F30, Quality.Higher)
    }
}
