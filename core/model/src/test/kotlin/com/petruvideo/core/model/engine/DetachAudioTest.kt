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

class DetachAudioTest {
    private val base = Timeline(
        projectId = "p",
        format = ProjectFormat(1920, 1080, 30),
        media = mapOf("m1" to testVideo("m1"), "m2" to testVideo("m2", hasAudio = false), "au" to testAudio("au")),
        videoLane = listOf(
            VideoClip("c1", "m1", 0, 4_000, attachedAudio = true),
            VideoClip("c2", "m1", 2_000, 7_000, attachedAudio = true),
            VideoClip("c3", "m2", 0, 3_000, attachedAudio = false),
        ),
    )

    private fun applied(result: CommandResult) = result.shouldBeInstanceOf<CommandResult.Applied>()

    @Test
    fun `creates a detached clip with the same range and start and clears the flag`() {
        val result = applied(DetachAudio("c2") { "d" }.apply(base))
        val audio = result.timeline.audioLane.single()
        audio.origin shouldBe AudioOrigin.Detached
        audio.sourceStartMs shouldBe 2_000
        audio.sourceEndMs shouldBe 7_000
        audio.timelineStartMs shouldBe 4_000
        audio.muted shouldBe false
        result.timeline.videoLane[1].attachedAudio shouldBe false
        result.notices.shouldBeEmpty()
        TimelineInvariants.violations(result.timeline).shouldBeEmpty()
    }

    @Test
    fun `rejects a clip without sound or unknown clip`() {
        DetachAudio("c3").apply(base) shouldBe CommandResult.Rejected(RejectReason.NoAttachedAudio)
        val once = applied(DetachAudio("c1").apply(base)).timeline
        DetachAudio("c1").apply(once) shouldBe CommandResult.Rejected(RejectReason.NoAttachedAudio)
        DetachAudio("nope").apply(base) shouldBe CommandResult.Rejected(RejectReason.NotFound)
    }

    @Test
    fun `layers over other audio and snaps only when three clips already play there`() {
        val busy = base.copy(
            audioLane = listOf(AudioClip("a1", "au", 0, 6_000, timelineStartMs = 3_000, origin = AudioOrigin.Added)),
        )
        val over = applied(DetachAudio("c2") { "d" }.apply(busy))
        over.timeline.audioLane.first { it.id == "d" }.timelineStartMs shouldBe base.videoStartOf("c2")
        over.notices.shouldBeEmpty()

        val full = base.copy(
            audioLane = listOf("a1", "a2", "a3").map {
                AudioClip(it, "au", 0, 6_000, timelineStartMs = 3_000, origin = AudioOrigin.Added)
            },
        )
        val result = applied(DetachAudio("c2") { "d" }.apply(full))
        result.timeline.audioLane.first { it.id == "d" }.timelineStartMs shouldBe 9_000
        result.notices shouldContainExactly listOf(Notice.Snapped)
        TimelineInvariants.violations(result.timeline).shouldBeEmpty()
    }

    @Test
    fun `undo restores the original timeline`() {
        val history = EditHistory(base)
        history.apply(DetachAudio("c1"))
        history.undo()
        history.current shouldBe base
    }
}
