package com.petruvideo.core.model.engine

import com.petruvideo.core.model.AudioClip
import com.petruvideo.core.model.AudioOrigin
import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.EqPreset
import com.petruvideo.core.model.Equalizer
import com.petruvideo.core.model.Fade
import com.petruvideo.core.model.Fingerprint
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.NoiseReduction
import com.petruvideo.core.model.NoiseStrength
import com.petruvideo.core.model.Pitch
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VideoClip
import com.petruvideo.core.model.VoiceFx
import io.kotest.matchers.shouldBe
import org.junit.Test

class AudioFxCommandsTest {
    private fun media(id: String, kind: MediaKind = MediaKind.Video) = MediaItem(
        id, kind, "content://$id", id, 20_000,
        1920.takeIf { kind != MediaKind.Audio }, 1080.takeIf { kind != MediaKind.Audio },
        30f, true, Fingerprint(1, 1, 20_000, "video/mp4"), Availability.Available,
    )

    private val timeline = Timeline(
        "p",
        media = mapOf("v" to media("v"), "ph" to media("ph", MediaKind.Photo), "a" to media("a", MediaKind.Audio)),
        videoLane = listOf(
            VideoClip("c1", "v", 0, 8_000, attachedAudio = true),
            VideoClip("c2", "v", 0, 4_000, attachedAudio = false),
            VideoClip("c3", "ph", 0, 3_000, attachedAudio = false),
        ),
        audioLane = listOf(AudioClip("a1", "a", 0, 6_000, 0, origin = AudioOrigin.Added)),
    )

    private fun applied(command: Command, on: Timeline = timeline) =
        (command.apply(on) as CommandResult.Applied)

    @Test
    fun `fade is limited to half the clip with a notice`() {
        val result = applied(SetFade("c1", Fade(5_000, 1_000)))
        result.timeline.videoLane[0].fx.fade shouldBe Fade(4_000, 1_000)
        result.notices shouldBe listOf(Notice.FadeLimited)
        applied(SetFade("a1", Fade(1_000, 2_000))).notices shouldBe emptyList()
    }

    @Test
    fun `settings apply to video clips with sound and audio clips`() {
        applied(SetNoiseReduction("c1", NoiseReduction(true, NoiseStrength.High)))
            .timeline.videoLane[0].fx.noise shouldBe NoiseReduction(true, NoiseStrength.High)
        applied(ApplyEqPreset("a1", EqPreset.BassBoost)).timeline.audioLane[0].fx.eq.preset shouldBe EqPreset.BassBoost
        applied(SetEqualizer("a1", Equalizer(EqPreset.Flat, 5, 0, 0))).timeline.audioLane[0].fx.eq.preset shouldBe
            EqPreset.Custom
        applied(SetVoiceFx("a1", VoiceFx(pitch = Pitch(true, 40))))
            .timeline.audioLane[0].fx.voice.pitch.semitones shouldBe 12
    }

    @Test
    fun `clips without sound are rejected`() {
        for (id in listOf("c2", "c3")) {
            SetFade(id, Fade(100, 100)).apply(timeline) shouldBe CommandResult.Rejected(RejectReason.NoAttachedAudio)
            SetNoiseReduction(id, NoiseReduction(true)).apply(timeline) shouldBe
                CommandResult.Rejected(RejectReason.NoAttachedAudio)
        }
        SetFade("nope", Fade(1, 1)).apply(timeline) shouldBe CommandResult.Rejected(RejectReason.NotFound)
    }

    @Test
    fun `no change is rejected and other clips are untouched`() {
        SetFade("c1", Fade()).apply(timeline) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        val result = applied(SetFade("c1", Fade(500, 0)))
        result.timeline.videoLane[1] shouldBe timeline.videoLane[1]
        result.timeline.audioLane shouldBe timeline.audioLane
    }

    @Test
    fun `split moves the fades to the outer edges and copies the rest`() {
        val faded = applied(SetFade("c1", Fade(1_000, 2_000))).timeline
        val withNoise = applied(SetNoiseReduction("c1", NoiseReduction(true)), faded)
        val split = applied(Split("c1", 3_000, newClipId = sequenceOf("x", "y").iterator()::next), withNoise.timeline)
        val (first, second) = split.timeline.videoLane
        first.fx.fade shouldBe Fade(1_000, 0)
        second.fx.fade shouldBe Fade(0, 2_000)
        first.fx.noise.enabled shouldBe true
        second.fx.noise.enabled shouldBe true
    }

    @Test
    fun `duplicate and detach keep every setting`() {
        val set = applied(ApplyEqPreset("c1", EqPreset.Warm)).timeline
        val copy = applied(Duplicate("c1", newClipId = { "d" }), set)
        copy.timeline.videoLane[1].fx.eq.preset shouldBe EqPreset.Warm
        val detached = applied(DetachAudio("c1", newClipId = { "e" }), set)
        detached.timeline.audioLane.first { it.id == "e" }.fx.eq.preset shouldBe EqPreset.Warm
    }

    @Test
    fun `undo restores the exact timeline`() {
        val before = timeline
        val after = applied(SetFade("c1", Fade(500, 500))).timeline
        (after == before) shouldBe false
        before shouldBe timeline
    }
}
