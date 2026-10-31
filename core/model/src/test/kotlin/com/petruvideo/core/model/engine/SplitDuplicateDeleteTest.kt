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

class SplitDuplicateDeleteTest {
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
            VideoClip("c1", "m1", 1_000, 7_000, attachedAudio = true),
            VideoClip("c2", "m2", 0, 4_000, attachedAudio = false),
            VideoClip("p1", "ph", 0, 5_000, attachedAudio = false),
        ),
        audioLane = listOf(
            AudioClip("a1", "au", 0, 4_000, timelineStartMs = 8_000, origin = AudioOrigin.Added),
            AudioClip("d1", "m1", 1_000, 7_000, timelineStartMs = 0, origin = AudioOrigin.Detached),
        ).sortedBy { it.timelineStartMs },
    )
    private var counter = 0
    private val ids: () -> String = { "n${counter++}" }

    private fun applied(result: CommandResult) = result.shouldBeInstanceOf<CommandResult.Applied>().timeline

    @Test
    fun `split halves are contiguous and keep attached audio`() {
        val t = applied(Split("c1", 2_500, ids).apply(base))
        t.videoLane.size shouldBe 4
        val (a, b) = t.videoLane
        a.sourceStartMs shouldBe 1_000
        a.sourceEndMs shouldBe 3_500
        b.sourceStartMs shouldBe 3_500
        b.sourceEndMs shouldBe 7_000
        a.attachedAudio shouldBe true
        b.attachedAudio shouldBe true
        t.totalDurationMs shouldBe base.totalDurationMs
        TimelineInvariants.violations(t).shouldBeEmpty()
    }

    @Test
    fun `split must be strictly inside and leave 100 ms on each side`() {
        Split("c1", 0, ids).apply(base) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        Split("c1", 6_000, ids).apply(base) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        Split("c1", 50, ids).apply(base) shouldBe CommandResult.Rejected(RejectReason.BelowMinimumLength)
        Split("c1", 5_950, ids).apply(base) shouldBe CommandResult.Rejected(RejectReason.BelowMinimumLength)
        Split("nope", 1_000, ids).apply(base) shouldBe CommandResult.Rejected(RejectReason.NotFound)
    }

    @Test
    fun `splitting a photo makes two photos that add up`() {
        val t = applied(Split("p1", 6_000 + 4_000 + 2_000, ids).apply(base))
        t.videoLane.map { it.lengthMs } shouldContainExactly listOf(6_000L, 4_000L, 2_000L, 3_000L)
    }

    @Test
    fun `splitting audio keeps the position of both halves`() {
        val t = applied(Split("a1", 9_000, ids).apply(base))
        val halves = t.audioLane.filter { it.mediaItemId == "au" }
        halves.map { it.timelineStartMs } shouldContainExactly listOf(8_000L, 9_000L)
        halves.map { it.lengthMs } shouldContainExactly listOf(1_000L, 3_000L)
        TimelineInvariants.violations(t).shouldBeEmpty()
    }

    @Test
    fun `split then rejoining the halves gives the original range`() {
        val t = applied(Split("c2", 6_000 + 1_500, ids).apply(base))
        val halves = t.videoLane.filter { it.mediaItemId == "m2" }
        halves.first().sourceStartMs shouldBe base.videoLane[1].sourceStartMs
        halves.last().sourceEndMs shouldBe base.videoLane[1].sourceEndMs
        halves.first().sourceEndMs shouldBe halves.last().sourceStartMs
    }

    @Test
    fun `duplicate puts the copy right after a video clip`() {
        val t = applied(Duplicate("c1", ids).apply(base))
        t.videoLane.size shouldBe 4
        t.videoLane[1].copy(id = "c1") shouldBe base.videoLane[0]
        t.videoLane[1].id shouldBe "n0"
    }

    @Test
    fun `duplicate of audio goes to the next free position`() {
        val t = applied(Duplicate("a1", ids).apply(base))
        val copy = t.audioLane.first { it.id == "n0" }
        copy.timelineStartMs shouldBe 12_000
        TimelineInvariants.violations(t).shouldBeEmpty()
    }

    @Test
    fun `delete removes a video clip with its attached audio but not detached audio`() {
        val t = applied(Delete("c1").apply(base))
        t.videoLane.map { it.id } shouldContainExactly listOf("c2", "p1")
        t.audioLane shouldBe base.audioLane
    }

    @Test
    fun `delete removes an audio clip`() {
        applied(Delete("d1").apply(base)).audioLane.map { it.id } shouldContainExactly listOf("a1")
        Delete("nope").apply(base) shouldBe CommandResult.Rejected(RejectReason.NotFound)
    }
}
