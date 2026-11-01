package com.petruvideo.core.media.composition

import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.TransitionKind

/** True when the video lane has a transition that is long enough to show. */
fun hasTransitions(timeline: Timeline): Boolean =
    timeline.videoLane.indices.any { timeline.transitionLengthAfter(it) > 0 }

/** True when the composition needs the multiple-input video graph (overlays or transitions). */
fun usesMultipleInputs(timeline: Timeline): Boolean = timeline.overlayLane.isNotEmpty() || hasTransitions(timeline)

/** One transition window on the timeline: the clip on input [from] gives way to the clip on input [to]. */
data class TransitionWindow(
    val startMs: Long,
    val endMs: Long,
    val kind: TransitionKind,
    val from: Int,
    val to: Int,
)

/**
 * When the video lane's clips play and where transitions blend them. Clips alternate between two sequences, which
 * are inputs [firstInput] and [firstInput] + 1 of the compositor; a clip is on input `firstInput + index % 2`.
 */
class TransitionPlan(val firstInput: Int, val ranges: Map<Int, List<LongRange>>, val windows: List<TransitionWindow>) {
    companion object {
        fun of(timeline: Timeline, firstInput: Int): TransitionPlan {
            val ranges = mutableMapOf(firstInput to mutableListOf<LongRange>(), firstInput + 1 to mutableListOf())
            val windows = mutableListOf<TransitionWindow>()
            var start = 0L
            timeline.videoLane.forEachIndexed { i, clip ->
                val input = firstInput + i % 2
                ranges.getValue(input) += start until start + clip.lengthMs
                val length = timeline.transitionLengthAfter(i)
                val kind = clip.transitionOut?.kind
                val next = start + clip.lengthMs - length
                if (length > 0 && kind != null) {
                    windows += TransitionWindow(next, next + length, kind, input, firstInput + (i + 1) % 2)
                }
                start = next
            }
            return TransitionPlan(firstInput, ranges, windows)
        }
    }
}
