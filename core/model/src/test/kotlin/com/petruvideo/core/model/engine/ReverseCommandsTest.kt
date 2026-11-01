package com.petruvideo.core.model.engine

import com.petruvideo.core.model.Fingerprint
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VideoClip
import io.kotest.matchers.shouldBe
import org.junit.Test

class ReverseCommandsTest {
    private val video = MediaItem(
        "m", MediaKind.Video, "content://m", "m.mp4", 60_000, 1920, 1080, 30f, true,
        Fingerprint(1, 1, 60_000, "video/mp4"),
    )
    private val photo = MediaItem(
        "p", MediaKind.Photo, "content://p", "p.jpg", null, 1920, 1080, null, false,
        Fingerprint(1, 1, 0, "image/jpeg"),
    )
    private val clip = VideoClip("a", "m", 10_000, 16_000, true)
    private val timeline = Timeline(
        "t", media = mapOf("m" to video, "p" to photo),
        videoLane = listOf(clip, VideoClip("ph", "p", 0, 3_000, false), VideoClip("long", "m", 0, 31_000, true)),
    )

    private fun applied(c: Command, t: Timeline = timeline) = (c.apply(t) as CommandResult.Applied).timeline
    private fun Timeline.clip(id: String) = videoLane.first { it.id == id }

    @Test
    fun `reverse is on for video clips up to 30 seconds and can be turned off again`() {
        val on = applied(SetReversed("a", true))
        on.clip("a").reversed shouldBe true
        applied(SetReversed("a", false), on).clip("a").reversed shouldBe false
        SetReversed("a", true).apply(on) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
    }

    @Test
    fun `photos and clips over 30 seconds cannot be reversed`() {
        SetReversed("ph", true).apply(timeline) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        SetReversed("long", true).apply(timeline) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        SetReversed("zz", true).apply(timeline) shouldBe CommandResult.Rejected(RejectReason.NotFound)
    }

    @Test
    fun `splitting a reversed clip keeps both parts reversed with the later source first`() {
        val t = applied(Split("a", 2_000), applied(SetReversed("a", true)))
        val (first, second) = t.videoLane
        first.reversed shouldBe true
        second.reversed shouldBe true
        first.sourceStartMs shouldBe 14_000
        first.sourceEndMs shouldBe 16_000
        second.sourceStartMs shouldBe 10_000
        second.sourceEndMs shouldBe 14_000
    }

    @Test
    fun `trimming a reversed clip moves the opposite edge of the source`() {
        val on = applied(SetReversed("a", true))
        // The start handle moved 1 s later: forward semantics 11 000, so the source end drops to 15 000.
        val start = applied(TrimStart("a", 11_000), on).clip("a")
        start.sourceStartMs shouldBe 10_000
        start.sourceEndMs shouldBe 15_000
        // The end handle moved 1 s earlier: forward semantics 15 000, so the source start rises to 11 000.
        val end = applied(TrimEnd("a", 15_000), on).clip("a")
        end.sourceStartMs shouldBe 11_000
        end.sourceEndMs shouldBe 16_000
    }

    @Test
    fun `duplicate keeps the reversed flag`() {
        val t = applied(Duplicate("a"), applied(SetReversed("a", true)))
        t.videoLane[1].reversed shouldBe true
    }
}
