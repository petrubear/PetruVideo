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
class OverlayLayout(clips: List<OverlayClip>, private val frameWidth: Int, private val frameHeight: Int) :
    VideoCompositorSettings {
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

    private companion object {

        const val PERCENT = 100f
        const val MICROS_PER_MS = 1_000L
    }
}
