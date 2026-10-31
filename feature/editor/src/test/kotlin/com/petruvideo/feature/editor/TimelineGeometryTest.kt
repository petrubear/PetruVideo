package com.petruvideo.feature.editor

import com.petruvideo.core.model.AudioClip
import com.petruvideo.core.model.AudioOrigin
import com.petruvideo.core.model.VideoClip
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.floats.shouldBeExactly
import io.kotest.matchers.shouldBe
import org.junit.Test

class TimelineGeometryTest {
    private val geometry = TimelineGeometry(pxPerSecond = 100f)

    private val lane = listOf(
        VideoClip("a", "m", 0, 2_000, true),
        VideoClip("b", "m", 0, 3_000, true),
        VideoClip("c", "m", 0, 1_000, true),
    )

    @Test
    fun `time and pixels convert both ways`() {
        geometry.timeToPx(1_500) shouldBeExactly 150f
        geometry.pxToTime(150f) shouldBe 1_500L
        geometry.pxToTime(geometry.timeToPx(12_345)) shouldBe 12_345L
    }

    @Test
    fun `scroll offset equals the playhead position so the playhead stays centered`() {
        geometry.scrollForPlayhead(2_000) shouldBeExactly 200f
        geometry.playheadForScroll(200f, totalMs = 6_000) shouldBe 2_000L
    }

    @Test
    fun `scrolling past either end clamps the playhead to the timeline`() {
        geometry.playheadForScroll(-50f, totalMs = 6_000) shouldBe 0L
        geometry.playheadForScroll(9_999f, totalMs = 6_000) shouldBe 6_000L
    }

    @Test
    fun `clip rectangles follow the lane order and lengths`() {
        geometry.videoRects(lane).map { Triple(it.clipId, it.startPx, it.widthPx) } shouldContainExactly listOf(
            Triple("a", 0f, 200f),
            Triple("b", 200f, 300f),
            Triple("c", 500f, 100f),
        )
    }

    @Test
    fun `audio rectangles use their own start position`() {
        val clip = AudioClip("x", "m", 0, 2_000, timelineStartMs = 1_000, origin = AudioOrigin.Added)
        geometry.audioRect(clip).let { it.startPx to it.widthPx } shouldBe (100f to 200f)
    }

    @Test
    fun `hit testing finds the clip under a pixel, with boundaries belonging to the next clip`() {
        geometry.videoClipAt(lane, 0f) shouldBe "a"
        geometry.videoClipAt(lane, 199f) shouldBe "a"
        geometry.videoClipAt(lane, 200f) shouldBe "b"
        geometry.videoClipAt(lane, 599f) shouldBe "c"
        geometry.videoClipAt(lane, 600f) shouldBe null
        geometry.videoClipAt(lane, -1f) shouldBe null
    }

    @Test
    fun `dropping a dragged clip picks the index by the clip centers`() {
        // Dragging clip "a" (index 0): where the dragged clip's center lands decides its new index.
        geometry.dropIndex(lane, draggedIndex = 0, centerPx = 50f) shouldBe 0
        geometry.dropIndex(lane, draggedIndex = 0, centerPx = 300f) shouldBe 1
        geometry.dropIndex(lane, draggedIndex = 0, centerPx = 580f) shouldBe 2
        geometry.dropIndex(lane, draggedIndex = 2, centerPx = 10f) shouldBe 0
        geometry.dropIndex(lane, draggedIndex = 1, centerPx = 9_000f) shouldBe 2
    }

    @Test
    fun `ruler ticks are spaced at least 48 px apart and the major ones are labelled`() {
        val ticks = geometry.rulerTicks(totalMs = 12_000)
        ticks.first().timeMs shouldBe 0L
        ticks.zipWithNext().all { (a, b) -> geometry.timeToPx(b.timeMs - a.timeMs) >= 48f } shouldBe true
        ticks.filter { it.label != null }.map { it.label } shouldContainExactly listOf("0:00", "0:05", "0:10")
    }

    @Test
    fun `zooming out increases the tick step`() {
        val zoomedOut = TimelineGeometry(pxPerSecond = 10f).rulerTicks(totalMs = 120_000)
        (zoomedOut[1].timeMs - zoomedOut[0].timeMs) shouldBe 5_000L
        TimelineGeometry(pxPerSecond = 200f).rulerTicks(totalMs = 3_000)[1].timeMs shouldBe 500L
    }

    @Test
    fun `the highlighted ruler second is the whole second of the playhead`() {
        geometry.currentSecond(0) shouldBe 0
        geometry.currentSecond(2_999) shouldBe 2
        geometry.currentSecond(3_000) shouldBe 3
    }

    @Test
    fun `content width is the timeline length plus half a viewport each side`() {
        geometry.contentWidthPx(totalMs = 6_000, viewportPx = 400f) shouldBeExactly 1_000f
    }
}
