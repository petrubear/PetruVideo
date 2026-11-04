package com.petruvideo.core.model

import com.petruvideo.core.model.engine.AddOverlay
import com.petruvideo.core.model.engine.CommandResult
import com.petruvideo.core.model.engine.Split
import io.kotest.matchers.shouldBe
import org.junit.Test

class OverlayAudioModelTest {
    private fun media(kind: MediaKind = MediaKind.Video, hasAudio: Boolean = true) = MediaItem(
        "m", kind, "content://m", "m.mp4", 20_000, 1280, 720, 30f, hasAudio, Fingerprint(1, 1, 20_000, "video/mp4"),
    )

    private fun added(media: MediaItem): OverlayClip {
        val result = AddOverlay(media, 0) { "o" }.apply(Timeline("p")) as CommandResult.Applied
        return result.timeline.overlayLane.single()
    }

    @Test
    fun `an overlay built directly is silent`() {
        OverlayClip("o", "m", 0, 5_000, 0).attachedAudio shouldBe false
    }

    @Test
    fun `a new overlay of a video with sound has sound on at full volume`() {
        val clip = added(media())
        clip.attachedAudio shouldBe true
        clip.muted shouldBe false
        clip.volumePercent shouldBe VOLUME_DEFAULT_PERCENT
    }

    @Test
    fun `a new overlay of a photo or a silent video has no sound`() {
        added(media(MediaKind.Photo, hasAudio = false)).attachedAudio shouldBe false
        added(media(hasAudio = false)).attachedAudio shouldBe false
    }

    @Test
    fun `normalizing clamps the volume and limits the fade to half the clip`() {
        val clip = OverlayClip(
            "o", "m", 0, 2_000, 0, volumePercent = 500, fx = AudioFx(fade = Fade(5_000, 5_000)),
        ).normalized()
        clip.volumePercent shouldBe VOLUME_MAX_PERCENT
        clip.fx.fade shouldBe Fade(1_000, 1_000)
    }

    @Test
    fun `splitting an overlay gives both parts the same sound settings`() {
        val clip = OverlayClip(
            "o", "m", 0, 4_000, 0, attachedAudio = true, volumePercent = 70, muted = true,
            reverb = Reverb(enabled = true),
        )
        val timeline = Timeline("p", media = mapOf("m" to media()), overlayLane = listOf(clip))
        val parts = (Split("o", 2_000).apply(timeline) as CommandResult.Applied).timeline.overlayLane
        parts.size shouldBe 2
        parts.forEach {
            it.attachedAudio shouldBe true
            it.volumePercent shouldBe 70
            it.muted shouldBe true
            it.reverb shouldBe clip.reverb
        }
    }
}
