package com.petruvideo.core.model.engine

import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.AudioClip
import com.petruvideo.core.model.AudioOrigin
import com.petruvideo.core.model.PHOTO_DEFAULT_MS
import com.petruvideo.core.model.ProjectFormat
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VideoClip
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Test

class AddClipsTest {
    private var counter = 0
    private val ids = { "id-${counter++}" }

    private val empty = Timeline(projectId = "p")

    /** Three 4 s video clips: boundaries at 0, 4000, 8000 and 12000. */
    private fun withThreeClips(): Timeline {
        val media = (1..3).associate { "m$it" to testVideo("m$it") }
        return empty.copy(
            format = ProjectFormat(1920, 1080, 30),
            media = media,
            videoLane = (1..3).map { VideoClip("c$it", "m$it", 0, 4_000, attachedAudio = true) },
        )
    }

    private fun applied(result: CommandResult) = result.shouldBeInstanceOf<CommandResult.Applied>()

    private fun addVideos(t: Timeline, playhead: Long, vararg items: com.petruvideo.core.model.MediaItem) =
        applied(AddVideoClips(items.map { AddVideoItem(it) }, playhead, ids).apply(t))

    @Test
    fun `an empty timeline gets the clips in selection order from the start`() {
        val result = addVideos(empty, 0, testVideo("a"), testPhoto("b"), testVideo("c"))
        result.timeline.videoLane.map { it.mediaItemId } shouldContainExactly listOf("a", "b", "c")
        result.timeline.media.keys shouldBe setOf("a", "b", "c")
    }

    @Test
    fun `a playhead inside a clip inserts after that clip`() {
        val result = addVideos(withThreeClips(), playhead = 5_000, testVideo("new"))
        result.timeline.videoLane.map { it.mediaItemId } shouldContainExactly listOf("m1", "m2", "new", "m3")
    }

    @Test
    fun `a playhead on a boundary inserts at that boundary`() {
        val result = addVideos(withThreeClips(), playhead = 4_000, testVideo("new"))
        result.timeline.videoLane.map { it.mediaItemId } shouldContainExactly listOf("m1", "new", "m2", "m3")
    }

    @Test
    fun `a playhead at zero inserts at the start`() {
        val result = addVideos(withThreeClips(), playhead = 0, testVideo("new"))
        result.timeline.videoLane.first().mediaItemId shouldBe "new"
    }

    @Test
    fun `a playhead at or beyond the end appends`() {
        addVideos(withThreeClips(), 12_000, testVideo("n1")).timeline.videoLane.last().mediaItemId shouldBe "n1"
        addVideos(withThreeClips(), 99_000, testVideo("n2")).timeline.videoLane.last().mediaItemId shouldBe "n2"
    }

    @Test
    fun `attached audio follows the source and photos get the default duration`() {
        val result = addVideos(empty, 0, testVideo("loud"), testVideo("silent", hasAudio = false), testPhoto("pic"))
        val (loud, silent, pic) = result.timeline.videoLane
        loud.attachedAudio shouldBe true
        silent.attachedAudio shouldBe false
        pic.attachedAudio shouldBe false
        pic.sourceStartMs shouldBe 0
        pic.sourceEndMs shouldBe PHOTO_DEFAULT_MS
        loud.sourceEndMs shouldBe 10_000
    }

    @Test
    fun `an explicit trim is used when valid`() {
        val result = applied(AddVideoClips(listOf(AddVideoItem(testVideo("a"), 1_000, 3_000)), 0, ids).apply(empty))
        result.timeline.videoLane.single().let { it.sourceStartMs to it.sourceEndMs } shouldBe (1_000L to 3_000L)
    }

    @Test
    fun `a trim outside the source or below the minimum is rejected`() {
        AddVideoClips(listOf(AddVideoItem(testVideo("a"), 0, 99_000)), 0, ids).apply(empty) shouldBe
            CommandResult.Rejected(RejectReason.OutOfRange)
        AddVideoClips(listOf(AddVideoItem(testVideo("a"), 1_000, 1_050)), 0, ids).apply(empty) shouldBe
            CommandResult.Rejected(RejectReason.BelowMinimumLength)
    }

    @Test
    fun `the project format is set from the first video or photo exactly once`() {
        val first = addVideos(empty, 0, testVideo("a").copy(width = 1080, height = 1920), testVideo("b"))
        first.timeline.format shouldBe ProjectFormat(1080, 1920, 30)

        val second = addVideos(first.timeline, 0, testVideo("c").copy(width = 3840, height = 2160, frameRate = 60f))
        second.timeline.format shouldBe ProjectFormat(1080, 1920, 30)
    }

    @Test
    fun `audio only does not set the format`() {
        val result = applied(AddAudioClips(listOf(testAudio("a")), 0, ids).apply(empty))
        result.timeline.format shouldBe null
    }

    @Test
    fun `missing or wrong-kind media is rejected without changing anything`() {
        val missing = testVideo("m").copy(availability = Availability.Missing)
        AddVideoClips(listOf(AddVideoItem(missing)), 0, ids).apply(empty) shouldBe
            CommandResult.Rejected(RejectReason.MissingMedia)
        AddVideoClips(listOf(AddVideoItem(testAudio("a"))), 0, ids).apply(empty) shouldBe
            CommandResult.Rejected(RejectReason.OutOfRange)
        AddAudioClips(listOf(testVideo("v")), 0, ids).apply(empty) shouldBe
            CommandResult.Rejected(RejectReason.OutOfRange)
        AddVideoClips(emptyList(), 0, ids).apply(empty) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
    }

    @Test
    fun `audio files start at the playhead and follow each other in order`() {
        val result = applied(
            AddAudioClips(listOf(testAudio("a", 3_000), testAudio("b", 2_000)), playheadMs = 1_000, ids).apply(empty),
        )
        result.timeline.audioLane.map { Triple(it.mediaItemId, it.timelineStartMs, it.timelineEndMs) }
            .shouldContainExactly(Triple("a", 1_000L, 4_000L), Triple("b", 4_000L, 6_000L))
        result.timeline.audioLane.all { it.origin == AudioOrigin.Added && !it.muted } shouldBe true
        result.notices.shouldBeEmpty()
    }

    @Test
    fun `an audio clip may overlap others and snaps to the next free spot only when the layers are full`() {
        val existing = listOf("x", "y", "z").map {
            AudioClip(it, "ax", 0, 5_000, timelineStartMs = 2_000, origin = AudioOrigin.Added)
        }
        val one = empty.copy(media = mapOf("ax" to testAudio("ax", 10_000)), audioLane = existing.take(1))
        val layered = applied(AddAudioClips(listOf(testAudio("n", 3_000)), playheadMs = 3_000, ids).apply(one))
        layered.timeline.audioLane.first { it.mediaItemId == "n" }.timelineStartMs shouldBe 3_000
        layered.notices.shouldBeEmpty()

        val full = one.copy(audioLane = existing)
        val result = applied(AddAudioClips(listOf(testAudio("n", 3_000)), playheadMs = 3_000, ids).apply(full))
        val added = result.timeline.audioLane.first { it.mediaItemId == "n" }
        added.timelineStartMs shouldBe 7_000
        result.notices shouldContain Notice.Snapped
        TimelineInvariants.violations(result.timeline).shouldBeEmpty()
    }

    @Test
    fun `a clip that fits in a gap before the next clip does not snap`() {
        val existing = AudioClip("x", "ax", 0, 5_000, timelineStartMs = 10_000, origin = AudioOrigin.Added)
        val t = empty.copy(media = mapOf("ax" to testAudio("ax", 10_000)), audioLane = listOf(existing))

        val result = applied(AddAudioClips(listOf(testAudio("n", 3_000)), playheadMs = 2_000, ids).apply(t))

        result.timeline.audioLane.first().timelineStartMs shouldBe 2_000
        result.notices.shouldBeEmpty()
    }

    @Test
    fun `audio may overlap a video's attached sound`() {
        val result = applied(AddAudioClips(listOf(testAudio("music", 20_000)), 0, ids).apply(withThreeClips()))
        result.timeline.audioLane.single().timelineStartMs shouldBe 0
        result.notices.shouldBeEmpty()
    }

    @Test
    fun `results satisfy the invariants and undo restores the timeline`() {
        val history = EditHistory(withThreeClips())
        history.apply(AddVideoClips(listOf(AddVideoItem(testVideo("n")), AddVideoItem(testPhoto("p"))), 4_000, ids))
        history.apply(AddAudioClips(listOf(testAudio("a")), 0, ids))
        TimelineInvariants.violations(history.current).shouldBeEmpty()

        history.undo()
        history.undo()
        history.current shouldBe withThreeClips()
    }

    @Test
    fun `new clips get unique ids`() {
        val result = addVideos(empty, 0, testVideo("a"), testVideo("a2"), testPhoto("b"))
        result.timeline.videoLane.map { it.id }.toSet().size shouldBe 3
    }
}
