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

class TrimCommandsTest {
    private val base = Timeline(
        projectId = "p",
        format = ProjectFormat(1920, 1080, 30),
        media = mapOf(
            "m1" to testVideo("m1"),
            "m2" to testVideo("m2"),
            "ph" to testPhoto("ph"),
            "au" to testAudio("au"),
        ),
        videoLane = listOf(
            VideoClip("c1", "m1", 1_000, 6_000, attachedAudio = true),
            VideoClip("c2", "m2", 0, 4_000, attachedAudio = true),
            VideoClip("p1", "ph", 0, 5_000, attachedAudio = false),
        ),
        audioLane = listOf(
            AudioClip("a1", "au", 2_000, 6_000, timelineStartMs = 3_000, origin = AudioOrigin.Added),
            AudioClip("a2", "au", 0, 1_000, timelineStartMs = 8_000, origin = AudioOrigin.Added),
        ),
    )

    private fun applied(result: CommandResult) = result.shouldBeInstanceOf<CommandResult.Applied>().timeline

    @Test
    fun `trimming the start shortens the clip and later clips shift`() {
        val t = applied(TrimStart("c1", 2_000).apply(base))
        t.videoLane[0].lengthMs shouldBe 4_000
        t.videoStartOf("c2") shouldBe 4_000
        TimelineInvariants.violations(t).shouldBeEmpty()
    }

    @Test
    fun `trimmed footage can be restored by dragging back`() {
        val trimmed = applied(TrimEnd("c1", 3_000).apply(base))
        val restored = applied(TrimEnd("c1", 6_000).apply(trimmed))
        restored.videoLane[0] shouldBe base.videoLane[0]
    }

    @Test
    fun `end can reach the end of the source but not beyond`() {
        applied(TrimEnd("c1", 10_000).apply(base)).videoLane[0].sourceEndMs shouldBe 10_000
        TrimEnd("c1", 10_001).apply(base) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        TrimStart("c1", -1).apply(base) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
    }

    @Test
    fun `a clip cannot go below 100 ms`() {
        TrimStart("c1", 5_950).apply(base) shouldBe CommandResult.Rejected(RejectReason.BelowMinimumLength)
        TrimEnd("c1", 1_099).apply(base) shouldBe CommandResult.Rejected(RejectReason.BelowMinimumLength)
        applied(TrimEnd("c1", 1_100).apply(base)).videoLane[0].lengthMs shouldBe 100
    }

    @Test
    fun `photos can be lengthened and shortened between 0_5 and 60 seconds`() {
        applied(TrimEnd("p1", 12_000).apply(base)).videoLane[2].lengthMs shouldBe 12_000
        applied(TrimEnd("p1", 500).apply(base)).videoLane[2].lengthMs shouldBe 500
        applied(TrimStart("p1", -3_000).apply(base)).videoLane[2].lengthMs shouldBe 8_000
        TrimEnd("p1", 499).apply(base) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        TrimEnd("p1", 60_001).apply(base) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
    }

    @Test
    fun `audio start trim keeps the content in place`() {
        val clip = applied(TrimStart("a1", 3_000).apply(base)).audioLane.first { it.id == "a1" }
        clip.timelineStartMs shouldBe 4_000
        clip.timelineEndMs shouldBe 7_000
    }

    @Test
    fun `audio trim may run over a neighbour but not into a fourth layer`() {
        TrimEnd("a1", 9_000).apply(base).shouldBeInstanceOf<CommandResult.Applied>()
        TrimStart("a1", 0).apply(base).shouldBeInstanceOf<CommandResult.Applied>()
        TrimStart("a1", 500).apply(base.copy(audioLane = base.audioLane.map {
            if (it.id == "a1") it.copy(timelineStartMs = 100) else it
        })) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        val crowded = base.copy(
            audioLane = base.audioLane + listOf("f1", "f2", "f3").map {
                AudioClip(it, "au", 0, 8_000, timelineStartMs = 2_000, origin = AudioOrigin.Added)
            },
        )
        TrimEnd("a1", 9_000).apply(crowded) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
    }

    @Test
    fun `unknown clip is rejected`() {
        TrimStart("nope", 0).apply(base) shouldBe CommandResult.Rejected(RejectReason.NotFound)
    }
}
