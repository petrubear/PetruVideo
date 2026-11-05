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
        val timeMs = presentationTimeUs / MICROS_PER_MS
        // The shown values come from the clip's keyframes (or its plain values) at this moment of the clip.
        val pose = clip.poseAt(timeMs - clip.startMs)
        val x = pose.xPermille.toFloat() / TEXT_PERMILLE * 2f - 1f
        val y = 1f - pose.yPermille.toFloat() / TEXT_PERMILLE * 2f
        // An overlay is drawn at its own pixel size relative to the frame, so the scale also corrects for that.
        val own = inputSizes.getOrNull(inputId)?.width?.takeIf { it > 0 } ?: frameWidth
        val scale = pose.sizePermille.toFloat() / TEXT_PERMILLE * frameWidth / own
        // Outside its own time the input is a gap (a 16 by 16 placeholder), which must not be drawn.
        val alpha = if (timeMs in clip.startMs until clip.endMs) pose.opacityPercent / PERCENT else 0f
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
        if (window == null) return placed(inputId, if (active) 1f else 0f, 0f, 0f, 1f)
        val p = (timeMs - window.startMs).toFloat() / (window.endMs - window.startMs)
        val outgoing = inputId == window.from
        val onTop = inputId == minOf(window.from, window.to)
        return placed(inputId, TransitionPoses.of(window.kind, p, outgoing, onTop))
    }

    /** Full-frame settings for a main input that is shown as [alpha] and not moved or resized. */
    private fun placed(inputId: Int, alpha: Float, x: Float, y: Float, zoom: Float): OverlaySettings =
        placed(inputId, TransitionPose(alpha, x, y, zoom))

    /** Full-frame settings for a main input drawn as [pose]. */
    private fun placed(inputId: Int, pose: TransitionPose): OverlaySettings {
        // The input's own size: the other main input may be a 16 by 16 gap while this one plays.
        val own = inputSizes.getOrNull(inputId)?.width?.takeIf { it > 0 } ?: frameWidth
        val fit = frameWidth.toFloat() / own
        return object : OverlaySettings {
            override fun getAlphaScale() = pose.alpha
            override fun getBackgroundFrameAnchor(): Pair<Float, Float> = Pair(pose.x, pose.y)
            override fun getOverlayFrameAnchor(): Pair<Float, Float> = Pair(0f, 0f)
            override fun getScale(): Pair<Float, Float> = Pair(pose.zoomX * fit, pose.zoomY * fit)
            override fun getRotationDegrees() = pose.rotationDegrees
        }
    }

    private companion object {
        const val PERCENT = 100f
        const val MICROS_PER_MS = 1_000L
    }
}
