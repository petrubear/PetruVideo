package com.petruvideo.feature.editor

import com.petruvideo.core.model.AudioClip
import com.petruvideo.core.model.VideoClip
import com.petruvideo.core.model.formatDuration
import com.petruvideo.core.model.transitionLengthAfter
import kotlin.math.roundToLong

data class ClipRect(val clipId: String, val startPx: Float, val widthPx: Float) {
    val endPx: Float get() = startPx + widthPx
    val centerPx: Float get() = startPx + widthPx / 2
}

data class RulerTick(val timeMs: Long, val label: String?)

/**
 * Pure time-to-pixel math for the timeline. The playhead is fixed at the center of the viewport and the content
 * scrolls under it, so the horizontal scroll offset equals the playhead time in pixels (the content has half a
 * viewport of padding on each side).
 */
class TimelineGeometry(val pxPerSecond: Float = DEFAULT_PX_PER_SECOND) {
    private val pxPerMs get() = pxPerSecond / MS_PER_SECOND

    fun timeToPx(ms: Long): Float = ms * pxPerMs

    fun pxToTime(px: Float): Long = (px / pxPerMs).roundToLong()

    fun scrollForPlayhead(playheadMs: Long): Float = timeToPx(playheadMs)

    fun playheadForScroll(scrollPx: Float, totalMs: Long): Long = pxToTime(scrollPx).coerceIn(0L, totalMs)

    fun contentWidthPx(totalMs: Long, viewportPx: Float): Float = timeToPx(totalMs) + viewportPx

    fun videoRects(lane: List<VideoClip>): List<ClipRect> {
        var start = 0f
        return lane.mapIndexed { index, clip ->
            val width = timeToPx(clip.lengthMs)
            // The next clip starts earlier by the transition between them, so the two overlap on the lane.
            ClipRect(clip.id, start, width).also { start += width - timeToPx(transitionLengthAfter(lane, index)) }
        }
    }

    fun audioRect(clip: AudioClip) = ClipRect(clip.id, timeToPx(clip.timelineStartMs), timeToPx(clip.lengthMs))

    /** The video clip under [xPx] (a boundary belongs to the following clip), or null. */
    fun videoClipAt(lane: List<VideoClip>, xPx: Float): String? =
        videoRects(lane).firstOrNull { xPx >= it.startPx && xPx < it.endPx }?.clipId

    /** Index a dragged clip takes when its center is released at [centerPx]: the other clips left of it count. */
    fun dropIndex(lane: List<VideoClip>, draggedIndex: Int, centerPx: Float): Int {
        val others = lane.filterIndexed { index, _ -> index != draggedIndex }
        return videoRects(others).count { it.centerPx < centerPx }
    }

    fun currentSecond(playheadMs: Long): Int = (playheadMs / MS_PER_SECOND).toInt()

    /** Ticks from zero to [totalMs]; the step grows when zoomed out so ticks stay at least 48 px apart. */
    fun rulerTicks(totalMs: Long): List<RulerTick> {
        val step = TICK_STEPS_MS.firstOrNull { timeToPx(it) >= MIN_TICK_GAP_PX } ?: TICK_STEPS_MS.last()
        val major = MAJOR_STEPS_MS.firstOrNull { it % step == 0L && timeToPx(it) >= MIN_LABEL_GAP_PX }
            ?: MAJOR_STEPS_MS.last()
        return generateSequence(0L) { it + step }.takeWhile { it <= totalMs }
            .map { RulerTick(it, if (it % major == 0L) formatDuration(it) else null) }
            .toList()
    }

    companion object {
        const val DEFAULT_PX_PER_SECOND = 80f
        private const val MS_PER_SECOND = 1_000f
        private const val MIN_TICK_GAP_PX = 48f
        private const val MIN_LABEL_GAP_PX = 250f
        private val TICK_STEPS_MS =
            listOf(100L, 200L, 500L, 1_000L, 2_000L, 5_000L, 10_000L, 30_000L, 60_000L, 300_000L)
        private val MAJOR_STEPS_MS = listOf(1_000L, 5_000L, 10_000L, 30_000L, 60_000L, 300_000L, 600_000L, 1_800_000L)
    }
}
