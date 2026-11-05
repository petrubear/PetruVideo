package com.petruvideo.core.media.composition

import com.petruvideo.core.model.TransitionKind

/**
 * Where one of the two main clips is drawn during a transition: its [alpha], its center moved by ([x], [y]) in frame
 * halves, its size scale on each axis, and its [rotationDegrees]. Progress runs from 0 (the first clip fills the
 * frame) to 1 (the second clip does).
 */
data class TransitionPose(
    val alpha: Float,
    val x: Float = 0f,
    val y: Float = 0f,
    val zoomX: Float = 1f,
    val zoomY: Float = zoomX,
    val rotationDegrees: Float = 0f,
)

/** The pose of each main clip at each moment of each transition kind; one place for the math of all of them. */
object TransitionPoses {
    private const val ZOOM_SPAN = 0.3f
    private const val DIP_ZOOM_SPAN = 0.2f
    private const val SPIN_TURN = 180f
    private const val SPIN_SMALLEST = 0.3f
    private const val TILT_DEGREES = 15f
    private const val DROP_DEGREES = 25f
    private const val ZOOM_OUT_SPAN = 0.3f
    private const val MIN_SCALE = 0.001f

    /**
     * [progress] is 0 to 1 over the window, [outgoing] says which clip this is, and [onTop] is true for the input the
     * compositor draws above the other, which carries the fade when one is needed.
     */
    fun of(kind: TransitionKind, progress: Float, outgoing: Boolean, onTop: Boolean): TransitionPose {
        val own = if (outgoing) 1f - progress else progress
        val fade = if (onTop) own else 1f
        return when (kind) {
            TransitionKind.Dissolve -> TransitionPose(fade)
            TransitionKind.FadeBlack -> TransitionPose(dip(progress, outgoing))
            TransitionKind.Slide -> TransitionPose(1f, x = sideOffset(progress, outgoing))
            TransitionKind.Cover -> TransitionPose(1f, x = if (onTop) sideOffset(progress, outgoing) else 0f)
            TransitionKind.Zoom ->
                TransitionPose(fade, zoomX = 1f + ZOOM_SPAN * (if (outgoing) progress else 1f - progress))
            else -> added(kind, progress, outgoing, fade)
        }
    }

    private fun added(kind: TransitionKind, p: Float, outgoing: Boolean, fade: Float): TransitionPose = when (kind) {
        TransitionKind.Spin -> TransitionPose(
            fade,
            zoomX = if (outgoing) 1f - (1f - SPIN_SMALLEST) * p else SPIN_SMALLEST + (1f - SPIN_SMALLEST) * p,
            rotationDegrees = if (outgoing) -SPIN_TURN * p else SPIN_TURN * (1f - p),
        )
        TransitionKind.Flip -> {
            val half = if (outgoing) 1f - 2f * p else 2f * p - 1f
            val shown = if (outgoing) p < HALF else p >= HALF
            TransitionPose(if (shown) 1f else 0f, zoomX = half.coerceAtLeast(MIN_SCALE), zoomY = 1f)
        }
        TransitionKind.PushUp -> TransitionPose(1f, y = if (outgoing) 2f * p else -2f * (1f - p))
        TransitionKind.PushDown -> TransitionPose(1f, y = if (outgoing) -2f * p else 2f * (1f - p))
        TransitionKind.PushRight -> TransitionPose(1f, x = -sideOffset(p, outgoing))
        TransitionKind.ZoomOut -> TransitionPose(
            fade,
            zoomX = if (outgoing) 1f - ZOOM_OUT_SPAN * p else 1f - ZOOM_OUT_SPAN * (1f - p),
        )
        TransitionKind.WhipPan -> TransitionPose(1f, x = sideOffset(whip(p), outgoing))
        TransitionKind.Drop -> TransitionPose(
            fade,
            y = if (outgoing) -2f * p * p else 0f,
            rotationDegrees = if (outgoing) DROP_DEGREES * p else 0f,
        )
        TransitionKind.DipZoom -> TransitionPose(
            dip(p, outgoing),
            zoomX = 1f + DIP_ZOOM_SPAN * (if (outgoing) p else 1f - p),
        )
        TransitionKind.TiltSlide -> TransitionPose(
            1f,
            x = sideOffset(p, outgoing),
            rotationDegrees = if (outgoing) TILT_DEGREES * p else -TILT_DEGREES * (1f - p),
        )
        else -> error("not an added transition: $kind")
    }

    /** Where a clip sits when the two slide left together: the first leaves left, the second arrives from the right. */
    private fun sideOffset(p: Float, outgoing: Boolean) = if (outgoing) -2f * p else 2f * (1f - p)

    /** Fades to black over the first half and back in over the second. */
    private fun dip(p: Float, outgoing: Boolean) = (if (outgoing) 1f - 2f * p else 2f * p - 1f).coerceAtLeast(0f)

    /** Slow at both ends and quick in the middle (smoother-step), so the pan feels whipped. */
    private fun whip(p: Float) = p * p * p * (p * (p * 6f - 15f) + 10f)

    private const val HALF = 0.5f
}
