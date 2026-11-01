package com.petruvideo.core.model

import com.petruvideo.core.model.engine.AddCaptions
import com.petruvideo.core.model.engine.CommandResult
import io.kotest.matchers.shouldBe
import kotlin.math.abs
import org.junit.Test

class CaptionsTest {
    private fun media(id: String, audio: Boolean = true) = MediaItem(
        id, MediaKind.Video, "content://$id", "$id.mp4", 30_000, 1280, 720, 30f, audio,
        Fingerprint(1, 1, 30_000, "video/mp4"),
    )

    /** Speech (0.5) between the given source ranges in ms, silence elsewhere, 20 bins per second, 30 s. */
    private fun speech(vararg ranges: LongRange) =
        FloatArray(600) { i -> if (ranges.any { (i * 50L) in it }) 0.5f else 0f }

    private fun timeline(clip: VideoClip = VideoClip("a", "m", 0, 10_000, true), vararg more: VideoClip) = Timeline(
        "p", media = mapOf("m" to media("m"), "n" to media("n", audio = false)),
        videoLane = listOf(clip) + more,
        textLane = listOf(TextClip("typed", "Title", 0, 2_000)),
    )

    private fun plan(t: Timeline, peaks: FloatArray?) = CaptionPlanner.plan(t) { peaks }

    private fun near(a: Long, b: Long) = abs(a - b) <= 60

    @Test
    fun `phrases sit between pauses and short sounds are dropped`() {
        val p = plan(timeline(), speech(1_000L..2_500L, 4_000L..4_100L, 6_000L..8_000L)).phrases
        p.size shouldBe 2
        (near(p[0].startMs, 1_000) && near(p[0].endMs, 2_550)) shouldBe true
        (near(p[1].startMs, 6_000) && near(p[1].endMs, 8_050)) shouldBe true
    }

    @Test
    fun `a short pause inside a sentence does not split it`() {
        val p = plan(timeline(), speech(1_000L..2_000L, 2_200L..3_000L)).phrases
        p.size shouldBe 1
    }

    @Test
    fun `a long stretch of speech is cut into readable phrases`() {
        val p = plan(timeline(VideoClip("a", "m", 0, 20_000, true)), speech(0L..19_000L)).phrases
        (p.size >= 3) shouldBe true
        p.all { it.endMs - it.startMs <= 8_050 } shouldBe true
        p.zipWithNext().all { (a, b) -> b.startMs >= a.endMs } shouldBe true
    }

    @Test
    fun `trim, position, and speed move the phrase on the timeline`() {
        val second = VideoClip("b", "m", 2_000, 10_000, true, speedMilli = 2_000)
        val t = timeline(VideoClip("a", "m", 0, 4_000, true), second)
        // The first clip lasts 4 s; the second starts there and plays source 2..10 s at 2x.
        val p = plan(t, speech(6_000L..8_000L)).phrases
        p.size shouldBe 1
        (near(p[0].startMs, 4_000 + 2_000) && near(p[0].endMs, 4_000 + 3_000)) shouldBe true
        p[0].clipId shouldBe "b"
    }

    @Test
    fun `muted, silent, reversed, curved and soundless clips are not used and some are counted`() {
        val loud = speech(1_000L..3_000L)
        plan(timeline(VideoClip("a", "m", 0, 10_000, true, muted = true)), loud).phrases.size shouldBe 0
        plan(timeline(VideoClip("a", "m", 0, 10_000, true, volumePercent = 0)), loud).phrases.size shouldBe 0
        plan(timeline(VideoClip("a", "m", 0, 10_000, false)), loud).phrases.size shouldBe 0
        val reversed = plan(timeline(VideoClip("a", "m", 0, 10_000, true, reversed = true)), loud)
        reversed.phrases.size shouldBe 0
        reversed.skippedClips shouldBe 1
        plan(timeline(), null).skippedClips shouldBe 1
    }

    @Test
    fun `at most 200 captions per run`() {
        val clip = VideoClip("a", "m", 0, 29_000, true)
        val many = FloatArray(600) { i -> if ((i / 4) % 2 == 0) 0.5f else 0f }
        val plan = plan(timeline(clip), many)
        (plan.phrases.size <= CAPTION_MAX) shouldBe true
    }

    @Test
    fun `AddCaptions adds caption style text, replaces only captions, and is one step`() {
        val old = TextClip("old", "Earlier", 100, 1_000, caption = true)
        val t = timeline().copy(textLane = listOf(TextClip("typed", "Title", 0, 2_000), old))
        val lines = listOf(
            CaptionLine(1_000, 2_500, "Hello there"), CaptionLine(3_000, 3_100, "too short"),
            CaptionLine(4_000, 5_000, " "),
        )
        var n = 0
        val result = (AddCaptions(lines, replace = true) { "n${n++}" }.apply(t) as CommandResult.Applied).timeline
        result.textLane.map { it.text } shouldBe listOf("Title", "Hello there")
        val caption = result.textLane.last()
        caption.caption shouldBe true
        caption.yPermille shouldBe CAPTION_Y
        caption.style.background.enabled shouldBe true
        val kept = (AddCaptions(lines, replace = false).apply(t) as CommandResult.Applied).timeline
        kept.textLane.size shouldBe 3
    }

    @Test
    fun `nothing to add and nothing to remove is refused`() {
        val t = timeline()
        AddCaptions(emptyList(), replace = true).apply(t) shouldBe
            CommandResult.Rejected(com.petruvideo.core.model.engine.RejectReason.OutOfRange)
    }
}
