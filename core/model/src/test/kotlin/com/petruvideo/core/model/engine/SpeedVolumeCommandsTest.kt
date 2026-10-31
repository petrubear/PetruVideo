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

class SpeedVolumeCommandsTest {
    private val base = Timeline(
        projectId = "p",
        format = ProjectFormat(1920, 1080, 30),
        media = mapOf(
            "v" to testVideo("v"),
            "au" to testAudio("au", 20_000),
            "ph" to testPhoto("ph"),
        ),
        videoLane = listOf(
            VideoClip("c1", "v", 0, 10_000, attachedAudio = true),
            VideoClip("c2", "v", 0, 10_000, attachedAudio = true),
            VideoClip("c3", "v", 0, 10_000, attachedAudio = false),
            VideoClip("p1", "ph", 0, 5_000, attachedAudio = false),
        ),
        audioLane = listOf(
            AudioClip("a1", "au", 0, 4_000, timelineStartMs = 0, origin = AudioOrigin.Added),
            AudioClip("a2", "au", 0, 3_000, timelineStartMs = 6_000, origin = AudioOrigin.Added),
        ),
    )

    private fun applied(result: CommandResult) = result.shouldBeInstanceOf<CommandResult.Applied>()

    @Test
    fun `volume applies to attached sound and to audio clips only`() {
        val v = applied(SetVolume("c1", 40).apply(base)).timeline
        v.videoLane.map { it.volumePercent } shouldBe listOf(40, 100, 100, 100)
        val a = applied(SetVolume("a1", 150).apply(base)).timeline
        a.audioLane.map { it.volumePercent } shouldBe listOf(150, 100)
        applied(SetVolume("a1", 500).apply(base)).timeline.audioLane[0].volumePercent shouldBe 200
        applied(SetVolume("a1", 0).apply(base)).timeline.audioLane[0].muted shouldBe false
    }

    @Test
    fun `volume rejects clips without sound, unknown ids, and no-ops`() {
        SetVolume("c3", 40).apply(base) shouldBe CommandResult.Rejected(RejectReason.NoAttachedAudio)
        SetVolume("p1", 40).apply(base) shouldBe CommandResult.Rejected(RejectReason.NoAttachedAudio)
        SetVolume("nope", 40).apply(base) shouldBe CommandResult.Rejected(RejectReason.NotFound)
        SetVolume("c1", 100).apply(base) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
    }

    @Test
    fun `2x halves the length and later clips move earlier`() {
        val result = applied(SetSpeed("c2", 2000).apply(base))
        result.timeline.videoLane[1].lengthMs shouldBe 5_000
        result.timeline.videoStartOf("c3") shouldBe 15_000
        result.notices.shouldBeEmpty()
        TimelineInvariants.violations(result.timeline).shouldBeEmpty()
    }

    @Test
    fun `half speed doubles the length`() {
        val result = applied(SetSpeed("c1", 500).apply(base))
        result.timeline.videoLane[0].lengthMs shouldBe 20_000
        result.timeline.videoStartOf("c2") shouldBe 20_000
    }

    @Test
    fun `speed is limited to the range with a notice`() {
        val fast = applied(SetSpeed("c1", 9000).apply(base))
        fast.timeline.videoLane[0].speedMilli shouldBe 4000
        fast.notices shouldContainExactly listOf(Notice.SpeedLimited)
        val slow = applied(SetSpeed("c1", 100).apply(base))
        slow.timeline.videoLane[0].speedMilli shouldBe 250
        slow.notices shouldContainExactly listOf(Notice.SpeedLimited)
    }

    @Test
    fun `speed keeps the clip at or above the minimum length`() {
        val tiny = base.copy(videoLane = listOf(VideoClip("t", "v", 0, 300, attachedAudio = true)))
        val result = applied(SetSpeed("t", 4000).apply(tiny))
        result.timeline.videoLane[0].lengthMs shouldBe 100
        result.timeline.videoLane[0].speedMilli shouldBe 3000
        result.notices shouldContainExactly listOf(Notice.SpeedLimited)
    }

    @Test
    fun `photos and unknown clips reject speed`() {
        SetSpeed("p1", 2000).apply(base) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        SetSpeed("nope", 2000).apply(base) shouldBe CommandResult.Rejected(RejectReason.NotFound)
        SetSpeed("c1", 1000).apply(base) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
    }

    @Test
    fun `audio speed keeps the start and is limited to avoid the next clip`() {
        val fast = applied(SetSpeed("a1", 2000).apply(base))
        fast.timeline.audioLane[0].lengthMs shouldBe 2_000
        fast.timeline.audioLane[0].timelineStartMs shouldBe 0
        val slow = applied(SetSpeed("a1", 250).apply(base))
        slow.notices shouldContainExactly listOf(Notice.SpeedLimited)
        slow.timeline.audioLane[0].speedMilli shouldBe 667
        (slow.timeline.audioLane[0].timelineEndMs <= 6_000) shouldBe true
        TimelineInvariants.violations(slow.timeline).shouldBeEmpty()
    }

    @Test
    fun `undo restores the identical timeline`() {
        val history = EditHistory(base)
        history.apply(SetSpeed("c1", 2000))
        history.apply(SetVolume("c2", 30))
        history.undo()
        history.undo()
        history.current shouldBe base
    }

    @Test
    fun `split of a sped clip cuts at the playhead and covers the source`() {
        val sped = applied(SetSpeed("c1", 2000).apply(base)).timeline
        val result = applied(Split("c1", 2_000, newClipId = idsOf("x", "y")).apply(sped))
        val (first, second) = result.timeline.videoLane
        first.speedMilli shouldBe 2000
        second.speedMilli shouldBe 2000
        first.sourceEndMs shouldBe 4_000
        second.sourceStartMs shouldBe 4_000
        second.sourceEndMs shouldBe 10_000
    }

    @Test
    fun `detach keeps speed and volume`() {
        val t = applied(SetSpeed("c1", 2000).apply(applied(SetVolume("c1", 40).apply(base)).timeline)).timeline
        val audio = applied(DetachAudio("c1", newClipId = { "d" }).apply(t)).timeline.audioLane.first { it.id == "d" }
        audio.speedMilli shouldBe 2000
        audio.volumePercent shouldBe 40
    }

    private fun idsOf(vararg ids: String): () -> String {
        val it = ids.iterator()
        return { it.next() }
    }
}
