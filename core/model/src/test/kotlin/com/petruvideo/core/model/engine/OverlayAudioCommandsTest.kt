package com.petruvideo.core.model.engine

import com.petruvideo.core.model.AudioClip
import com.petruvideo.core.model.AudioFx
import com.petruvideo.core.model.AudioOrigin
import com.petruvideo.core.model.Fade
import com.petruvideo.core.model.Fingerprint
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.OverlayClip
import com.petruvideo.core.model.Echo
import com.petruvideo.core.model.Equalizer
import com.petruvideo.core.model.NoiseReduction
import com.petruvideo.core.model.Reverb
import com.petruvideo.core.model.VoiceFx
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VideoClip
import io.kotest.matchers.shouldBe
import org.junit.Test

class OverlayAudioCommandsTest {
    private fun media(id: String, hasAudio: Boolean = true) = MediaItem(
        id, MediaKind.Video, "content://$id", "$id.mp4", 20_000, 1280, 720, 30f, hasAudio,
        Fingerprint(1, 1, 20_000, "video/mp4"),
    )

    private val timeline = Timeline(
        "p", media = mapOf("m" to media("m"), "q" to media("q", hasAudio = false)),
        videoLane = listOf(VideoClip("v", "m", 0, 10_000, true)),
        overlayLane = listOf(
            OverlayClip("o", "m", 0, 6_000, 1_000, attachedAudio = true),
            OverlayClip("silent", "q", 0, 6_000, 8_000),
        ),
    )

    private fun applied(c: Command, t: Timeline = timeline) = (c.apply(t) as CommandResult.Applied).timeline

    private fun rejected(c: Command, t: Timeline = timeline) = (c.apply(t) as CommandResult.Rejected).reason

    private fun overlay(t: Timeline, id: String = "o") = t.overlayLane.first { it.id == id }

    @Test
    fun `volume changes only the chosen overlay and is clamped`() {
        val t = applied(SetVolume("o", 60))
        overlay(t).volumePercent shouldBe 60
        overlay(t, "silent").volumePercent shouldBe 100
        overlay(applied(SetVolume("o", 900))).volumePercent shouldBe 200
    }

    @Test
    fun `volume on a silent overlay or with no change is rejected`() {
        rejected(SetVolume("silent", 50)) shouldBe RejectReason.NoAttachedAudio
        rejected(SetVolume("o", 100)) shouldBe RejectReason.OutOfRange
    }

    @Test
    fun `mute and unmute an overlay without touching its picture`() {
        val muted = applied(SetMuted("o", true))
        overlay(muted).muted shouldBe true
        overlay(muted).widthPermille shouldBe overlay(timeline).widthPermille
        overlay(applied(SetMuted("o", false), muted)).muted shouldBe false
        rejected(SetMuted("o", false)) shouldBe RejectReason.OutOfRange
        rejected(SetMuted("silent", true)) shouldBe RejectReason.NoAttachedAudio
    }

    @Test
    fun `sound can be turned on for an overlay whose video has sound and off again`() {
        val off = applied(SetOverlaySound("o", false))
        overlay(off).attachedAudio shouldBe false
        val old = timeline.copy(overlayLane = listOf(OverlayClip("o", "m", 0, 6_000, 1_000)))
        overlay(applied(SetOverlaySound("o", true), old)).attachedAudio shouldBe true
    }

    @Test
    fun `sound cannot be turned on for a video without sound`() {
        rejected(SetOverlaySound("silent", true)) shouldBe RejectReason.NoAttachedAudio
        rejected(SetOverlaySound("o", true)) shouldBe RejectReason.OutOfRange
        rejected(SetOverlaySound("missing", true)) shouldBe RejectReason.NotFound
    }

    @Test
    fun `effects change only the chosen overlay`() {
        val faded = applied(SetFade("o", Fade(500, 300)))
        overlay(faded).fx.fade shouldBe Fade(500, 300)
        overlay(faded, "silent").fx shouldBe AudioFx()
        val eq = Equalizer(lowDb = 4, midDb = 0, highDb = -3)
        overlay(applied(SetEqualizer("o", eq))).fx.eq.lowDb shouldBe 4
        overlay(applied(SetNoiseReduction("o", NoiseReduction(true)))).fx.noise.enabled shouldBe true
        overlay(applied(SetVoiceFx("o", VoiceFx(echo = Echo(true, 250, 3))))).fx.voice.echo.enabled shouldBe true
    }

    @Test
    fun `a fade is limited to half the overlay and a silent overlay is rejected`() {
        overlay(applied(SetFade("o", Fade(5_000, 0)))).fx.fade.inMs shouldBe 3_000
        rejected(SetFade("silent", Fade(100, 0))) shouldBe RejectReason.NoAttachedAudio
    }

    @Test
    fun `reverb is set on one overlay`() {
        val t = applied(SetReverbEnabled("o", true))
        overlay(t).reverb.enabled shouldBe true
        overlay(t, "silent").reverb shouldBe Reverb()
        rejected(SetReverbEnabled("silent", true)) shouldBe RejectReason.NoAttachedAudio
    }

    private fun audio(id: String) =
        media(id).copy(kind = MediaKind.Audio, width = null, height = null, frameRate = null)

    private val sounding = timeline.copy(
        overlayLane = listOf(
            OverlayClip("o", "m", 2_000, 6_000, 1_000, attachedAudio = true, volumePercent = 70, muted = true),
        ),
    )

    @Test
    fun `detaching an overlay makes an audio clip at its start and silences the overlay`() {
        val t = applied(DetachAudio("o") { "a1" }, sounding)
        overlay(t).attachedAudio shouldBe false
        val clip = t.audioLane.single()
        clip.id shouldBe "a1"
        clip.origin shouldBe AudioOrigin.Detached
        clip.mediaItemId shouldBe "m"
        clip.sourceStartMs shouldBe 2_000
        clip.sourceEndMs shouldBe 6_000
        clip.timelineStartMs shouldBe 1_000
        clip.volumePercent shouldBe 70
        clip.muted shouldBe true
    }

    @Test
    fun `detaching a silent overlay is rejected and a full audio lane refuses without moving the sound`() {
        rejected(DetachAudio("silent")) shouldBe RejectReason.NoAttachedAudio
        val full = sounding.copy(
            media = sounding.media + ("a" to audio("a")),
            audioLane = (0 until 3).map { AudioClip("x$it", "a", 0, 8_000, 0, false, AudioOrigin.Added) },
        )
        rejected(DetachAudio("o"), full) shouldBe RejectReason.LimitReached
    }

    @Test
    fun `splitting the voice of an overlay makes two clips at its start and silences it`() {
        val voice = audio("voice")
        val backing = audio("backing")
        val ids = listOf("v1", "b1").iterator()
        val t = applied(SplitVoice("o", voice, backing) { ids.next() }, sounding)
        overlay(t).attachedAudio shouldBe false
        t.audioLane.map { it.id }.toSet() shouldBe setOf("v1", "b1")
        t.audioLane.all { it.timelineStartMs == 1_000L && it.volumePercent == 70 } shouldBe true
        t.media.keys.containsAll(listOf("voice", "backing")) shouldBe true
    }

    @Test
    fun `the voice split rules cover overlays`() {
        VoiceSplitRules.reasonNot(sounding, "o") shouldBe null
        VoiceSplitRules.reasonNot(timeline, "silent") shouldBe VoiceSplitBlock.NoSound
    }
}
