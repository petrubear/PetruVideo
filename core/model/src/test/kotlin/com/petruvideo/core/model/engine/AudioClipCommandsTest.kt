package com.petruvideo.core.model.engine

import com.petruvideo.core.model.AudioClip
import com.petruvideo.core.model.AudioOrigin
import com.petruvideo.core.model.ProjectFormat
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VideoClip
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Test

class AudioClipCommandsTest {
    private val base = Timeline(
        projectId = "p",
        format = ProjectFormat(1920, 1080, 30),
        media = mapOf(
            "au" to testAudio("au", 10_000),
            "short" to testAudio("short", 2_000),
            "long" to testAudio("long", 20_000),
            "v" to testVideo("v"),
        ),
        audioLane = listOf(
            AudioClip("a1", "au", 0, 4_000, timelineStartMs = 1_000, origin = AudioOrigin.Added),
            AudioClip("a2", "au", 0, 3_000, timelineStartMs = 8_000, origin = AudioOrigin.Detached),
        ),
    )

    private fun applied(result: CommandResult) = result.shouldBeInstanceOf<CommandResult.Applied>()

    @Test
    fun `moving to free space just moves`() {
        val result = applied(MoveAudioClip("a1", 4_000).apply(base))
        result.timeline.audioLane.first { it.id == "a1" }.timelineStartMs shouldBe 4_000
        result.notices.shouldBeEmpty()
    }

    @Test
    fun `moving onto another clip snaps after it with a notice`() {
        val result = applied(MoveAudioClip("a1", 7_000).apply(base))
        result.timeline.audioLane.first { it.id == "a1" }.timelineStartMs shouldBe 11_000
        result.notices shouldContainExactly listOf(Notice.Snapped)
        TimelineInvariants.violations(result.timeline).shouldBeEmpty()
    }

    @Test
    fun `bad moves are rejected`() {
        MoveAudioClip("a1", -1).apply(base) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        MoveAudioClip("a1", 1_000).apply(base) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        MoveAudioClip("nope", 0).apply(base) shouldBe CommandResult.Rejected(RejectReason.NotFound)
    }

    @Test
    fun `mute and unmute toggle the flag`() {
        val muted = applied(SetMuted("a1", true).apply(base)).timeline
        muted.audioLane.first { it.id == "a1" }.muted shouldBe true
        applied(SetMuted("a1", false).apply(muted)).timeline shouldBe base
        SetMuted("a1", false).apply(base) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
    }

    @Test
    fun `replacement keeps position and length`() {
        val result = applied(ReplaceAudio("a1", testAudio("long", 20_000)).apply(base))
        val clip = result.timeline.audioLane.first { it.id == "a1" }
        clip.mediaItemId shouldBe "long"
        clip.timelineStartMs shouldBe 1_000
        clip.lengthMs shouldBe 4_000
        clip.origin shouldBe AudioOrigin.Added
        result.notices.shouldBeEmpty()
    }

    @Test
    fun `a shorter replacement shortens the clip and tells the user`() {
        val result = applied(ReplaceAudio("a1", testAudio("short", 2_000)).apply(base))
        result.timeline.audioLane.first { it.id == "a1" }.lengthMs shouldBe 2_000
        result.notices shouldContainExactly listOf(Notice.LengthChanged)
        TimelineInvariants.violations(result.timeline).shouldBeEmpty()
    }

    @Test
    fun `replacement must be available audio`() {
        ReplaceAudio("a1", testVideo("v")).apply(base) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        ReplaceAudio("a1", testAudio("x").copy(availability = com.petruvideo.core.model.Availability.Missing))
            .apply(base) shouldBe CommandResult.Rejected(RejectReason.MissingMedia)
        ReplaceAudio("nope", testAudio("x")).apply(base) shouldBe CommandResult.Rejected(RejectReason.NotFound)
    }
}
