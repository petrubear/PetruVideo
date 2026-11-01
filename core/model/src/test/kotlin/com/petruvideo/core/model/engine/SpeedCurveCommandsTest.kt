package com.petruvideo.core.model.engine

import com.petruvideo.core.model.Fingerprint
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.SpeedCurve
import com.petruvideo.core.model.SpeedPoint
import com.petruvideo.core.model.SpeedPreset
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VideoClip
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlin.math.abs
import org.junit.Test

class SpeedCurveCommandsTest {
    private val video = MediaItem(
        "m", MediaKind.Video, "content://m", "m.mp4", 60_000, 1920, 1080, 30f, true,
        Fingerprint(1, 1, 60_000, "video/mp4"),
    )
    private val photo = MediaItem(
        "p", MediaKind.Photo, "content://p", "p.jpg", null, 1920, 1080, null, false,
        Fingerprint(1, 1, 0, "image/jpeg"),
    )
    private val clip = VideoClip("a", "m", 2_000, 8_000, true)
    private val timeline = Timeline(
        "t", media = mapOf("m" to video, "p" to photo),
        videoLane = listOf(clip, VideoClip("ph", "p", 0, 3_000, false), VideoClip("b", "m", 0, 4_000, true)),
    )

    private fun applied(c: Command, t: Timeline = timeline) = (c.apply(t) as CommandResult.Applied).timeline
    private fun Timeline.clip(id: String) = videoLane.first { it.id == id }

    @Test
    fun `a preset sets a curve, changes the length, and reset returns to 1x`() {
        val t = applied(ApplySpeedPreset("a", SpeedPreset.Bullet))
        val c = t.clip("a")
        c.speedCurve!!.sourceLengthMs shouldBe 6_000
        (c.lengthMs > 6_000L) shouldBe true
        TimelineInvariants.violations(t).shouldBeEmpty()
        val back = applied(ResetSpeedCurve("a"), t).clip("a")
        back.speedCurve shouldBe null
        back.lengthMs shouldBe 6_000
        ResetSpeedCurve("a").apply(timeline) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
    }

    @Test
    fun `photos and a curve that does not fit are refused and constant speed is refused while a curve is on`() {
        ApplySpeedPreset("ph", SpeedPreset.Hero).apply(timeline) shouldBe
            CommandResult.Rejected(RejectReason.OutOfRange)
        val wrong = SpeedCurve(listOf(SpeedPoint(0, 1_000), SpeedPoint(5_000, 1_000)))
        SetSpeedCurve("a", wrong).apply(timeline) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        val t = applied(ApplySpeedPreset("a", SpeedPreset.Hero))
        SetSpeed("a", 2_000).apply(t) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        ApplySpeedPreset("zz", SpeedPreset.Hero).apply(timeline) shouldBe CommandResult.Rejected(RejectReason.NotFound)
    }

    @Test
    fun `a curve that would make the clip too short is refused`() {
        val shortClip = VideoClip("s", "m", 0, 600, true)
        val tiny = timeline.copy(videoLane = listOf(shortClip))
        val fast = SpeedCurve(listOf(SpeedPoint(0, 10_000), SpeedPoint(600, 10_000)))
        SetSpeedCurve("s", fast).apply(tiny) shouldBe CommandResult.Rejected(RejectReason.BelowMinimumLength)
    }

    @Test
    fun `splitting a clip with a curve keeps the speed at every moment`() {
        val t = applied(ApplySpeedPreset("a", SpeedPreset.Hero))
        val original = t.clip("a")
        val cut = original.lengthMs / 2
        val split = applied(Split("a", cut), t)
        val first = split.videoLane[0]
        val second = split.videoLane[1]
        (abs(first.lengthMs + second.lengthMs - original.lengthMs) <= 40) shouldBe true
        first.speedCurve!!.sourceLengthMs + second.speedCurve!!.sourceLengthMs shouldBe original.sourceLengthMs
        first.sourceEndMs shouldBe second.sourceStartMs
        TimelineInvariants.violations(split).shouldBeEmpty()
    }

    @Test
    fun `splitting a reversed clip with a curve cuts the curve in play order`() {
        val reversed = timeline.copy(videoLane = listOf(clip.copy(reversed = true)) + timeline.videoLane.drop(1))
        val t = applied(ApplySpeedPreset("a", SpeedPreset.Montage), reversed)
        val split = applied(Split("a", t.clip("a").lengthMs / 2), t)
        val (first, second) = split.videoLane
        first.reversed shouldBe true
        first.sourceStartMs shouldBe second.sourceEndMs
        TimelineInvariants.violations(split).shouldBeEmpty()
    }

    @Test
    fun `trimming a clip with a curve cuts the curve and keeps the clip valid`() {
        val t = applied(ApplySpeedPreset("a", SpeedPreset.Bullet))
        val trimmed = applied(TrimStart("a", 3_000), t).clip("a")
        trimmed.speedCurve!!.sourceLengthMs shouldBe trimmed.sourceLengthMs
        trimmed.sourceLengthMs shouldBe 5_000
        val end = applied(TrimEnd("a", 6_000), t).clip("a")
        end.speedCurve!!.sourceLengthMs shouldBe 4_000
        TimelineInvariants.violations(applied(TrimEnd("a", 6_000), t)).shouldBeEmpty()
    }

    @Test
    fun `duplicate and reverse keep the curve`() {
        val t = applied(ApplySpeedPreset("a", SpeedPreset.FlashOut))
        applied(Duplicate("a"), t).videoLane[1].speedCurve shouldBe t.clip("a").speedCurve
        val r = applied(SetReversed("a", true), t).clip("a")
        r.speedCurve shouldBe t.clip("a").speedCurve
        r.lengthMs shouldBe t.clip("a").lengthMs
    }

    @Test
    fun `a freeze in a clip with a curve gives parts that keep their curves`() {
        val t = applied(ApplySpeedPreset("a", SpeedPreset.Montage))
        val still = MediaItem(
            "s", MediaKind.Photo, "file://s.jpg", "s.jpg", null, 1920, 1080, null, false,
            Fingerprint(1, 1, 0, "image/jpeg"),
        )
        val frozen = applied(FreezeFrame("a", t.clip("a").lengthMs / 2, 2_000, still), t)
        (frozen.videoLane[0].speedCurve != null) shouldBe true
        (frozen.videoLane[2].speedCurve != null) shouldBe true
        frozen.videoLane[1].speedCurve shouldBe null
        TimelineInvariants.violations(frozen).shouldBeEmpty()
    }

    @Test
    fun `relinking to a shorter file cuts the curve with the clip`() {
        val t = applied(ApplySpeedPreset("a", SpeedPreset.Hero))
        val shorter = video.copy(durationMs = 5_000)
        val relinked = applied(RelinkMedia("m", shorter), t).clip("a")
        relinked.speedCurve!!.sourceLengthMs shouldBe relinked.sourceLengthMs
    }
}
