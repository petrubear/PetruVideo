package com.petruvideo.core.model.engine

import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.Fingerprint
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.Timeline
import io.kotest.matchers.shouldBe
import org.junit.Test

class OverlayCommandsTest {
    private fun media(id: String, kind: MediaKind, duration: Long?, missing: Boolean = false) = MediaItem(
        id, kind, "content://$id", id, duration, 1920, 1080, 30f, false, Fingerprint(1, 1, duration, "video/mp4"),
        if (missing) Availability.Missing else Availability.Available,
    )

    private val video = media("v", MediaKind.Video, 30_000)
    private val photo = media("p", MediaKind.Photo, null)
    private val empty = Timeline("p")

    private fun applied(c: Command, t: Timeline) = (c.apply(t) as CommandResult.Applied).timeline

    private fun withVideo() = applied(AddOverlay(video, 2_000, { "o1" }), empty)

    @Test
    fun `add puts a video up to ten seconds and a photo five seconds and keeps the media`() {
        val v = withVideo()
        v.overlayLane.single().durationMs shouldBe 10_000
        v.overlayLane.single().startMs shouldBe 2_000
        v.media.keys shouldBe setOf("v")
        val shortVideo = applied(AddOverlay(media("s", MediaKind.Video, 3_000), 0, { "o" }), empty)
        shortVideo.overlayLane.single().durationMs shouldBe 3_000
        applied(AddOverlay(photo, 0, { "o2" }), empty).overlayLane.single().durationMs shouldBe 5_000
        v.totalDurationMs shouldBe 12_000
    }

    @Test
    fun `audio and missing media are rejected`() {
        AddOverlay(media("a", MediaKind.Audio, 5_000), 0).apply(empty) shouldBe
            CommandResult.Rejected(RejectReason.OutOfRange)
        AddOverlay(media("m", MediaKind.Video, 5_000, missing = true), 0).apply(empty) shouldBe
            CommandResult.Rejected(RejectReason.MissingMedia)
    }

    @Test
    fun `placement is clamped and opacity changes and no change is rejected`() {
        val t = withVideo()
        val placed = applied(SetOverlayPlacement("o1", 5_000, -3, 5), t).overlayLane.single()
        placed.xPermille shouldBe 1000
        placed.yPermille shouldBe 0
        placed.widthPermille shouldBe 50
        applied(SetOverlayOpacity("o1", 40), t).overlayLane.single().opacityPercent shouldBe 40
        SetOverlayOpacity("o1", 100).apply(t) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        MoveOverlay("nope", 1).apply(t) shouldBe CommandResult.Rejected(RejectReason.NotFound)
    }

    @Test
    fun `trim keeps a video inside its file and lets a photo grow`() {
        val t = withVideo()
        val trimmed = applied(TrimOverlay("o1", 3_000, 8_000), t).overlayLane.single()
        trimmed.startMs shouldBe 3_000
        trimmed.sourceStartMs shouldBe 1_000
        trimmed.durationMs shouldBe 5_000
        TrimOverlay("o1", 2_000, 2_100).apply(t) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        TrimOverlay("o1", 2_000, 45_000).apply(t) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        TrimOverlay("o1", 500, 5_000).apply(t) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        val p = applied(AddOverlay(photo, 0, { "p1" }), empty)
        applied(TrimOverlay("p1", 0, 9_000), p).overlayLane.single().durationMs shouldBe 9_000
    }

    @Test
    fun `split cuts the source and the timeline together and duplicate and delete work`() {
        val t = withVideo()
        val ids = sequenceOf("a", "b").iterator()
        val (a, b) = applied(Split("o1", 6_000, ids::next), t).overlayLane
        a.startMs shouldBe 2_000
        a.sourceEndMs shouldBe 4_000
        b.startMs shouldBe 6_000
        b.sourceStartMs shouldBe 4_000
        b.sourceEndMs shouldBe 10_000
        val dup = applied(Duplicate("o1") { "d" }, t)
        dup.overlayLane.map { it.startMs } shouldBe listOf(2_000L, 12_000L)
        applied(Delete("d"), dup).overlayLane.size shouldBe 1
        val p = applied(AddOverlay(photo, 0, { "p1" }), empty)
        val photoIds = sequenceOf("x", "y").iterator()
        val (pa, pb) = applied(Split("p1", 2_000, photoIds::next), p).overlayLane
        pa.durationMs shouldBe 2_000
        pb.durationMs shouldBe 3_000
        pb.sourceStartMs shouldBe 0
    }

    @Test
    fun `removing missing media removes its overlays and relinking keeps them inside the new file`() {
        val t = withVideo()
        val removed = applied(RemoveMissingMedia("v"), t)
        removed.overlayLane shouldBe emptyList()
        removed.media.containsKey("v") shouldBe false
        val shorter = media("v", MediaKind.Video, 4_000)
        val relinked = applied(RelinkMedia("v", shorter), t).overlayLane.single()
        (relinked.sourceEndMs <= 4_000) shouldBe true
        (relinked.durationMs >= 100) shouldBe true
    }
}
