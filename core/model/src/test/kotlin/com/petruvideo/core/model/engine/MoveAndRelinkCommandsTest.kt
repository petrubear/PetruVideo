package com.petruvideo.core.model.engine

import com.petruvideo.core.model.AudioClip
import com.petruvideo.core.model.AudioOrigin
import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.ProjectFormat
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VideoClip
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Test

class MoveAndRelinkCommandsTest {
    private val base = Timeline(
        projectId = "p",
        format = ProjectFormat(1920, 1080, 30),
        media = mapOf(
            "m1" to testVideo("m1"),
            "m2" to testVideo("m2", hasAudio = false),
            "m3" to testVideo("m3"),
            "d" to testAudio("d"),
        ),
        videoLane = listOf(
            VideoClip("c1", "m1", 0, 4_000, attachedAudio = true),
            VideoClip("c2", "m2", 0, 3_000, attachedAudio = false),
            VideoClip("c3", "m3", 1_000, 6_000, attachedAudio = true),
        ),
        audioLane = listOf(AudioClip("a1", "d", 0, 2_000, timelineStartMs = 4_500, origin = AudioOrigin.Detached)),
    )

    private fun applied(result: CommandResult) = result.shouldBeInstanceOf<CommandResult.Applied>()

    @Test
    fun `moving a clip reorders the lane and its attached audio follows`() {
        val result = applied(MoveVideoClip("c1", newIndex = 2).apply(base))
        result.timeline.videoLane.map { it.id } shouldContainExactly listOf("c2", "c3", "c1")
        result.timeline.videoLane.last().attachedAudio shouldBe true
        result.timeline.totalDurationMs shouldBe base.totalDurationMs
        TimelineInvariants.violations(result.timeline).shouldBeEmpty()
    }

    @Test
    fun `moving a clip backwards works too`() {
        applied(MoveVideoClip("c3", 0).apply(base)).timeline.videoLane.map { it.id } shouldContainExactly
            listOf("c3", "c1", "c2")
    }

    @Test
    fun `detached audio does not move with the video`() {
        val result = applied(MoveVideoClip("c1", 2).apply(base))
        result.timeline.audioLane shouldBe base.audioLane
    }

    @Test
    fun `bad ids and indexes are rejected`() {
        MoveVideoClip("nope", 0).apply(base) shouldBe CommandResult.Rejected(RejectReason.NotFound)
        MoveVideoClip("c1", 3).apply(base) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        MoveVideoClip("c1", -1).apply(base) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        MoveVideoClip("c1", 0).apply(base) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
    }

    @Test
    fun `move undoes back to the original`() {
        val history = EditHistory(base)
        history.apply(MoveVideoClip("c1", 2))
        history.undo()
        history.current shouldBe base
    }

    @Test
    fun `relink keeps the trim and position when the new file is long enough`() {
        val newItem = testVideo("replacement", durationMs = 20_000)
        val result = applied(RelinkMedia("m1", newItem).apply(base))

        result.timeline.media["m1"]?.uri shouldBe newItem.uri
        result.timeline.media["m1"]?.availability shouldBe Availability.Available
        result.timeline.videoLane.first() shouldBe base.videoLane.first()
        result.notices.shouldBeEmpty()
    }

    @Test
    fun `relink to a shorter file clamps the trim and reports the change`() {
        val result = applied(RelinkMedia("m3", testVideo("short", durationMs = 3_000)).apply(base))
        val clip = result.timeline.videoLane.first { it.id == "c3" }
        clip.sourceStartMs shouldBe 1_000
        clip.sourceEndMs shouldBe 3_000
        result.notices shouldContain Notice.LengthChanged
        TimelineInvariants.violations(result.timeline).shouldBeEmpty()
    }

    @Test
    fun `relink clamps a trim that starts beyond the new end`() {
        val result = applied(RelinkMedia("m3", testVideo("tiny", durationMs = 500)).apply(base))
        val clip = result.timeline.videoLane.first { it.id == "c3" }
        (clip.sourceEndMs <= 500 && clip.lengthMs >= 100 && clip.sourceStartMs >= 0) shouldBe true
    }

    @Test
    fun `relink updates attached audio from the new source`() {
        val silent = testVideo("silent", hasAudio = false)
        applied(RelinkMedia("m1", silent).apply(base)).timeline.videoLane.first().attachedAudio shouldBe false
    }

    @Test
    fun `relink also rebinds audio clips and keeps their position`() {
        val result = applied(RelinkMedia("d", testAudio("newd", durationMs = 1_000)).apply(base))
        val clip = result.timeline.audioLane.single()
        clip.timelineStartMs shouldBe 4_500
        clip.sourceEndMs shouldBe 1_000
        result.notices shouldContain Notice.LengthChanged
    }

    @Test
    fun `relink rejects incompatible kinds, unknown media, and unusable files`() {
        RelinkMedia("m1", testAudio("x")).apply(base) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        RelinkMedia("zzz", testVideo("x")).apply(base) shouldBe CommandResult.Rejected(RejectReason.NotFound)
        RelinkMedia("m1", testVideo("x", durationMs = 50)).apply(base) shouldBe
            CommandResult.Rejected(RejectReason.BelowMinimumLength)
    }

    @Test
    fun `removing missing media drops its clips and the item`() {
        val result = applied(RemoveMissingMedia("m1").apply(base))
        result.timeline.videoLane.map { it.id } shouldContainExactly listOf("c2", "c3")
        ("m1" in result.timeline.media) shouldBe false
        TimelineInvariants.violations(result.timeline).shouldBeEmpty()
    }

    @Test
    fun `removing detached audio media drops its audio clips`() {
        val result = applied(RemoveMissingMedia("d").apply(base))
        result.timeline.audioLane.shouldBeEmpty()
        RemoveMissingMedia("zzz").apply(base) shouldBe CommandResult.Rejected(RejectReason.NotFound)
    }

    @Test
    fun `a photo can be relinked to another photo`() {
        val t = base.copy(
            media = base.media + ("ph" to testPhoto("ph")),
            videoLane = base.videoLane + VideoClip("cp", "ph", 0, 5_000, false),
        )
        val result = applied(RelinkMedia("ph", testPhoto("ph2")).apply(t))
        result.timeline.videoLane.last().sourceEndMs shouldBe 5_000
    }
}
