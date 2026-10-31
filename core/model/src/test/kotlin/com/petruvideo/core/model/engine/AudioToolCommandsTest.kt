package com.petruvideo.core.model.engine

import com.petruvideo.core.model.AudioClip
import com.petruvideo.core.model.AudioOrigin
import com.petruvideo.core.model.ProjectFormat
import com.petruvideo.core.model.Reverb
import com.petruvideo.core.model.ReverbPreset
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VideoClip
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Test

class AudioToolCommandsTest {
    private val base = Timeline(
        projectId = "p",
        format = ProjectFormat(1920, 1080, 30),
        media = mapOf("v" to testVideo("v"), "au" to testAudio("au", 20_000), "ph" to testPhoto("ph")),
        videoLane = listOf(
            VideoClip("c1", "v", 0, 10_000, attachedAudio = true),
            VideoClip("c2", "v", 0, 10_000, attachedAudio = false),
            VideoClip("p1", "ph", 0, 5_000, attachedAudio = false),
        ),
        audioLane = listOf(AudioClip("a1", "au", 0, 4_000, timelineStartMs = 0, origin = AudioOrigin.Added)),
    )

    private fun applied(result: CommandResult) = result.shouldBeInstanceOf<CommandResult.Applied>().timeline

    @Test
    fun `mute and unmute a video clip with sound, keeping volume and reverb`() {
        val t = applied(SetReverbEnabled("c1", true).apply(base))
        val muted = applied(SetVideoMuted("c1", true).apply(t))
        muted.videoLane[0].muted shouldBe true
        muted.videoLane[0].volumePercent shouldBe 100
        muted.videoLane[0].reverb.enabled shouldBe true
        applied(SetVideoMuted("c1", false).apply(muted)) shouldBe t
    }

    @Test
    fun `mute rejects clips without sound, photos, no-ops and unknown ids`() {
        SetVideoMuted("c2", true).apply(base) shouldBe CommandResult.Rejected(RejectReason.NoAttachedAudio)
        SetVideoMuted("p1", true).apply(base) shouldBe CommandResult.Rejected(RejectReason.NoAttachedAudio)
        SetVideoMuted("c1", false).apply(base) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        SetVideoMuted("nope", true).apply(base) shouldBe CommandResult.Rejected(RejectReason.NotFound)
    }

    @Test
    fun `reverb turns on and off with settings kept`() {
        val on = applied(SetReverbEnabled("a1", true).apply(base))
        on.audioLane[0].reverb.enabled shouldBe true
        val tuned = applied(SetReverbSettings("a1", on.audioLane[0].reverb.copy(mix = 70)).apply(on))
        val off = applied(SetReverbEnabled("a1", false).apply(tuned))
        off.audioLane[0].reverb.mix shouldBe 70
        applied(SetReverbEnabled("a1", true).apply(off)).audioLane[0].reverb shouldBe tuned.audioLane[0].reverb
    }

    @Test
    fun `a preset sets all numbers and turns reverb on`() {
        val t = applied(ApplyReverbPreset("c1", ReverbPreset.Cave).apply(base))
        val r = t.videoLane[0].reverb
        r.enabled shouldBe true
        r.preset shouldBe ReverbPreset.Cave
        r.decayMs shouldBe 7_000
        ApplyReverbPreset("c1", ReverbPreset.Custom).apply(base) shouldBe
            CommandResult.Rejected(RejectReason.OutOfRange)
    }

    @Test
    fun `settings are clamped, the preset is recomputed and Custom appears`() {
        val on = applied(ApplyReverbPreset("a1", ReverbPreset.Hall).apply(base))
        val custom = applied(SetReverbSettings("a1", on.audioLane[0].reverb.copy(damping = 99, mix = 500)).apply(on))
        val r = custom.audioLane[0].reverb
        r.damping shouldBe 99
        r.mix shouldBe 100
        r.preset shouldBe ReverbPreset.Custom
        r.enabled shouldBe true
        TimelineInvariants.violations(custom).shouldBeEmpty()
        val back = applied(SetReverbSettings("a1", r.copy(damping = 40, mix = 35)).apply(custom))
        back.audioLane[0].reverb.preset shouldBe ReverbPreset.Hall
    }

    @Test
    fun `reverb rejects photos, soundless video, unknown ids and no-ops`() {
        SetReverbEnabled("p1", true).apply(base) shouldBe CommandResult.Rejected(RejectReason.NoAttachedAudio)
        SetReverbEnabled("c2", true).apply(base) shouldBe CommandResult.Rejected(RejectReason.NoAttachedAudio)
        SetReverbEnabled("nope", true).apply(base) shouldBe CommandResult.Rejected(RejectReason.NotFound)
        SetReverbEnabled("a1", false).apply(base) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
    }

    @Test
    fun `other clips are untouched and undo restores the timeline`() {
        val history = EditHistory(base)
        history.apply(ApplyReverbPreset("c1", ReverbPreset.Hall))
        history.apply(SetVideoMuted("c1", true))
        history.current.videoLane[1] shouldBe base.videoLane[1]
        history.current.audioLane shouldBe base.audioLane
        history.undo()
        history.undo()
        history.current shouldBe base
    }

    @Test
    fun `detach copies mute and reverb to the audio clip and clears the video mute`() {
        val withReverb = applied(ApplyReverbPreset("c1", ReverbPreset.Plate).apply(base))
        val t = applied(SetVideoMuted("c1", true).apply(withReverb))
        val result = applied(DetachAudio("c1", newClipId = { "d" }).apply(t))
        val audio = result.audioLane.first { it.id == "d" }
        audio.muted shouldBe true
        audio.reverb.preset shouldBe ReverbPreset.Plate
        result.videoLane[0].muted shouldBe false
    }

    @Test
    fun `split and duplicate keep mute and reverb`() {
        val withReverb = applied(ApplyReverbPreset("c1", ReverbPreset.Hall).apply(base))
        val t = applied(SetVideoMuted("c1", true).apply(withReverb))
        val ids = listOf("x", "y").iterator()
        val split = applied(Split("c1", 4_000, newClipId = { ids.next() }).apply(t))
        split.videoLane.take(2).forEach {
            it.muted shouldBe true
            it.reverb shouldBe t.videoLane[0].reverb
        }
        val dup = applied(Duplicate("c1", newClipId = { "z" }).apply(t))
        dup.videoLane[1].reverb shouldBe t.videoLane[0].reverb
        dup.videoLane[1].muted shouldBe true
    }
}
