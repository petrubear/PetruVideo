package com.petruvideo.core.model.engine

import com.petruvideo.core.model.CHROMA_BLUE
import com.petruvideo.core.model.CHROMA_GREEN
import com.petruvideo.core.model.ChromaKey
import com.petruvideo.core.model.Fingerprint
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.OverlayClip
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VideoClip
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.Test

class ChromaKeyCommandsTest {
    private val media = MediaItem(
        "m", MediaKind.Video, "content://m", "m.mp4", 20_000, 1280, 720, 30f, true,
        Fingerprint(1, 1, 20_000, "video/mp4"),
    )
    private val timeline = Timeline(
        "p", media = mapOf("m" to media),
        videoLane = listOf(VideoClip("v", "m", 0, 10_000, true)),
        overlayLane = listOf(OverlayClip("o", "m", 0, 6_000, 1_000)),
    )

    private fun applied(c: Command, t: Timeline = timeline) = (c.apply(t) as CommandResult.Applied).timeline

    @Test
    fun `a new overlay is not keyed`() {
        timeline.overlayLane[0].chroma shouldBe ChromaKey()
        timeline.overlayLane[0].chroma.enabled shouldBe false
    }

    @Test
    fun `setting the key is one step and is clamped`() {
        val t = applied(SetChromaKey("o", ChromaKey(true, CHROMA_BLUE, 150, -5, 30)))
        t.overlayLane[0].chroma shouldBe ChromaKey(true, CHROMA_BLUE, 100, 0, 30)
        TimelineInvariants.violations(t).shouldBeEmpty()
    }

    @Test
    fun `the same key and an unknown clip are rejected`() {
        SetChromaKey("o", ChromaKey()).apply(timeline) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        SetChromaKey("x", ChromaKey(true)).apply(timeline) shouldBe CommandResult.Rejected(RejectReason.NotFound)
    }

    @Test
    fun `split and duplicate keep the key`() {
        val keyed = applied(SetChromaKey("o", ChromaKey(true, CHROMA_GREEN, 70, 10, 90)))
        val split = applied(Split("o", 3_000), keyed)
        split.overlayLane.map { it.chroma } shouldBe List(2) { ChromaKey(true, CHROMA_GREEN, 70, 10, 90) }
        val copy = applied(Duplicate("o"), keyed)
        copy.overlayLane[1].chroma shouldBe keyed.overlayLane[0].chroma
    }

    @Test
    fun `a key outside its ranges is an invariant violation`() {
        val bad = timeline.copy(overlayLane = listOf(timeline.overlayLane[0].copy(chroma = ChromaKey(strength = 500))))
        TimelineInvariants.violations(bad).size shouldBe 1
    }
}
