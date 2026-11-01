package com.petruvideo.core.media.composition

import android.util.Pair
import androidx.annotation.OptIn
import androidx.media3.common.OverlaySettings
import androidx.media3.common.VideoCompositorSettings
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.StaticOverlaySettings
import com.petruvideo.core.model.OverlayClip
import com.petruvideo.core.model.TEXT_PERMILLE
import com.petruvideo.core.model.TransitionKind

/**
 * Where each overlay sequence is drawn over the main video: the center, the width as a share of the frame width, and
 * the opacity. Overlay [k] is input k; the main video comes after them and is drawn as it is. The output has the
 * frame size [frameWidth] by [frameHeight]. The clips can be replaced while playing, so a drag shows at once.
 */
@OptIn(UnstableApi::class)
class OverlayLayout(
    clips: List<OverlayClip>,
    private val frameWidth: Int,
    private val frameHeight: Int,
    private val plan: TransitionPlan? = null,
) : VideoCompositorSettings {
    @Volatile private var byInput: Map<Int, OverlayClip> = clips.mapIndexed { i, c -> i to c }.toMap()
    @Volatile private var inputSizes: List<Size> = emptyList()

    /** Replaces the placement of one clip (the others are kept). */
    fun update(clip: OverlayClip) {
        byInput = byInput.mapValues { (_, old) -> if (old.id == clip.id) clip else old }
    }

    override fun getOutputSize(inputSizes: List<Size>): Size {
        this.inputSizes = inputSizes
        return Size(frameWidth, frameHeight)
    }

    override fun getOverlaySettings(inputId: Int, presentationTimeUs: Long): OverlaySettings {
        if (plan != null && inputId >= plan.firstInput) return mainSettings(inputId, presentationTimeUs, plan)
        val clip = byInput[inputId] ?: return StaticOverlaySettings.Builder().build()
        val x = clip.xPermille.toFloat() / TEXT_PERMILLE * 2f - 1f
        val y = 1f - clip.yPermille.toFloat() / TEXT_PERMILLE * 2f
        // An overlay is drawn at its own pixel size relative to the frame, so the scale also corrects for that.
        val own = inputSizes.getOrNull(inputId)?.width?.takeIf { it > 0 } ?: frameWidth
        val scale = clip.widthPermille.toFloat() / TEXT_PERMILLE * frameWidth / own
        // Outside its own time the input is a gap (a 16 by 16 placeholder), which must not be drawn.
        val timeMs = presentationTimeUs / MICROS_PER_MS
        val alpha = if (timeMs in clip.startMs until clip.endMs) clip.opacityPercent / PERCENT else 0f
        return object : OverlaySettings {
            override fun getAlphaScale() = alpha
            override fun getBackgroundFrameAnchor(): Pair<Float, Float> = Pair(x, y)
            override fun getOverlayFrameAnchor(): Pair<Float, Float> = Pair(0f, 0f)
            override fun getScale(): Pair<Float, Float> = Pair(scale, scale)
        }
    }

    /**
     * The two main inputs: each is shown only while one of its clips plays, and a transition window moves them. The
     * input with the higher number is drawn on top, so it carries the fade when one is needed.
     */
    private fun mainSettings(inputId: Int, presentationTimeUs: Long, plan: TransitionPlan): OverlaySettings {
        val timeMs = presentationTimeUs / MICROS_PER_MS
        val window = plan.windows.firstOrNull {
            timeMs in it.startMs until it.endMs && (inputId == it.from || inputId == it.to)
        }
        val active = plan.ranges[inputId].orEmpty().any { timeMs in it }
        if (window == null) return placed(if (active) 1f else 0f, 0f, 0f, 1f)
        val p = (timeMs - window.startMs).toFloat() / (window.endMs - window.startMs)
        val outgoing = inputId == window.from
        val onTop = inputId == minOf(window.from, window.to)
        val own = if (outgoing) 1f - p else p
        return when (window.kind) {
            TransitionKind.Dissolve -> placed(if (onTop) own else 1f, 0f, 0f, 1f)
            TransitionKind.FadeBlack -> placed(
                (if (outgoing) 1f - 2f * p else 2f * p - 1f).coerceAtLeast(0f), 0f, 0f, 1f,
            )
            TransitionKind.Slide -> placed(1f, if (outgoing) -2f * p else 2f * (1f - p), 0f, 1f)
            TransitionKind.Cover -> placed(1f, if (!onTop) 0f else if (outgoing) -2f * p else 2f * (1f - p), 0f, 1f)
            TransitionKind.Zoom -> placed(
                if (onTop) own else 1f, 0f, 0f, 1f + ZOOM_SPAN * (if (outgoing) p else 1f - p),
            )
        }
    }

    /** Full-frame settings for a main input: [alpha], the center moved by ([x], [y]) in frame halves, and [zoom]. */
    private fun placed(alpha: Float, x: Float, y: Float, zoom: Float): OverlaySettings {
        val own = inputSizes.firstOrNull()?.width?.takeIf { it > 0 } ?: frameWidth
        val scale = zoom * frameWidth / own
        return object : OverlaySettings {
            override fun getAlphaScale() = alpha
            override fun getBackgroundFrameAnchor(): Pair<Float, Float> = Pair(x, y)
            override fun getOverlayFrameAnchor(): Pair<Float, Float> = Pair(0f, 0f)
            override fun getScale(): Pair<Float, Float> = Pair(scale, scale)
        }
    }

    private companion object {
        const val ZOOM_SPAN = 0.3f

        const val PERCENT = 100f
        const val MICROS_PER_MS = 1_000L
    }
}
