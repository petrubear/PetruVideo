package com.petruvideo.core.media.composition

import com.petruvideo.core.model.SPEED_UNIT
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.TransitionKind

/** True when the video lane has a transition that is long enough to show. */
fun hasTransitions(timeline: Timeline): Boolean =
    timeline.videoLane.indices.any { timeline.transitionLengthAfter(it) > 0 }

/** True when the composition needs the multiple-input video graph (overlays or transitions). */
fun usesMultipleInputs(timeline: Timeline): Boolean = timeline.overlayLane.isNotEmpty() || hasTransitions(timeline)

/** The timeline with the sound of every overlay turned off, for a preview that cannot play it (feature 029). */
fun Timeline.withoutOverlaySound(): Timeline =
    if (overlayLane.none { it.attachedAudio }) {
        this
    } else {
        copy(overlayLane = overlayLane.map { it.copy(attachedAudio = false) })
    }

/**
 * The timeline with every transition replaced by a hard cut at the same moment: the clip before it ends where the
 * transition starts. The preview plays this, because two video inputs freeze on some phones' hardware decoders; the
 * export still blends. Clips with a speed curve or played backwards keep their length.
 */
fun withTransitionsAsCuts(timeline: Timeline): Timeline {
    if (!hasTransitions(timeline)) return timeline
    val lane = timeline.videoLane.mapIndexed { i, clip ->
        val fade = timeline.transitionLengthAfter(i)
        val trimmable = fade > 0 && clip.speedCurve == null && !clip.reversed
        val end = if (trimmable) clip.sourceEndMs - fade * clip.speedMilli / SPEED_UNIT else clip.sourceEndMs
        clip.copy(sourceEndMs = end, transitionOut = null)
    }
    return timeline.copy(videoLane = lane)
}

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
