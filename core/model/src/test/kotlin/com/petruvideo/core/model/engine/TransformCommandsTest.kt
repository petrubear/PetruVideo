package com.petruvideo.core.model.engine

import com.petruvideo.core.model.ClipTransform
import com.petruvideo.core.model.CropBox
import com.petruvideo.core.model.Fingerprint
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VideoClip
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.Test

class TransformCommandsTest {
    private fun media(id: String, w: Int = 1920, h: Int = 1080) = MediaItem(
        id, MediaKind.Video, "content://$id", "$id.mp4", 20_000, w, h, 30f, true,
        Fingerprint(1, 1, 20_000, "video/mp4"),
    )

    private val timeline = Timeline(
        "p", media = mapOf("m" to media("m")),
        videoLane = listOf(VideoClip("a", "m", 0, 4_000, true), VideoClip("b", "m", 0, 4_000, true)),
    )

    private fun applied(c: Command, t: Timeline = timeline) = (c.apply(t) as CommandResult.Applied).timeline
    private fun Timeline.clipA() = videoLane.first { it.id == "a" }

    @Test
    fun `rotate and flip change only the chosen clip and undo restores`() {
        var t = applied(RotateClip("a"))
        t.clipA().transform.quarterTurns shouldBe 1
        t.videoLane[1].transform shouldBe ClipTransform()
        t = applied(FlipClip("a", horizontal = true), t)
        t.clipA().transform.flipH shouldBe true
        val history = EditHistory(timeline)
        history.apply(RotateClip("a"))
        history.undo()
        history.current.videoLane.first().transform shouldBe ClipTransform()
        TimelineInvariants.violations(t).shouldBeEmpty()
    }

    @Test
    fun `a crop is pulled inside the picture and turns with it`() {
        val t = applied(SetCrop("a", CropBox(-5, 100, 900, 5000)))
        t.clipA().transform.crop shouldBe CropBox(0, 100, 900, 1000)
        val turned = applied(RotateClip("a"), applied(SetCrop("a", CropBox(0, 0, 500, 500))))
        turned.clipA().transform.crop shouldBe CropBox(500, 0, 1000, 500)
        SetCrop("a", CropBox(0, 100, 900, 1000)).apply(t) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
    }

    @Test
    fun `reset removes everything and does nothing when there is nothing`() {
        val t = applied(FlipClip("a", false), applied(RotateClip("a")))
        applied(ResetTransform("a"), t).clipA().transform shouldBe ClipTransform()
        ResetTransform("a").apply(timeline) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        RotateClip("zz").apply(timeline) shouldBe CommandResult.Rejected(RejectReason.NotFound)
    }

    @Test
    fun `split duplicate trim and speed keep the transform`() {
        val t = applied(SetCrop("a", CropBox(100, 100, 800, 800)), applied(RotateClip("a")))
        val tr = t.clipA().transform
        applied(Split("a", 2_000), t).videoLane.take(2).all { it.transform == tr } shouldBe true
        applied(Duplicate("a"), t).videoLane[1].transform shouldBe tr
        applied(TrimEnd("a", 2_000), t).clipA().transform shouldBe tr
        applied(SetSpeed("a", 2_000), t).clipA().transform shouldBe tr
    }

    @Test
    fun `relink to the same shape keeps the crop and to another shape resets only the crop`() {
        val t = applied(SetCrop("a", CropBox(100, 100, 800, 800)), applied(RotateClip("a")))
        val same = applied(RelinkMedia("m", media("n")), t)
        same.clipA().transform shouldBe t.clipA().transform
        val other = applied(RelinkMedia("m", media("n", 1080, 1920)), t)
        other.clipA().transform shouldBe t.clipA().transform.copy(crop = CropBox())
    }
}
