package com.petruvideo.core.media.filter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.Pair
import androidx.annotation.OptIn
import androidx.media3.common.OverlaySettings
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.RgbMatrix
import com.petruvideo.core.model.ClipFilter
import com.petruvideo.core.model.ColorMath
import kotlin.math.ceil
import kotlin.math.hypot

/** The color transform of one clip; the matrix can be replaced while playing, so a slider drag needs no rebuild. */
@OptIn(UnstableApi::class)
class LiveRgbMatrix(filter: ClipFilter) : RgbMatrix {
    @Volatile private var matrix: FloatArray = ColorMath.matrix(filter)

    fun update(filter: ClipFilter) {
        matrix = ColorMath.matrix(filter)
    }

    override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray = matrix
}

/** Dark edges of one clip: a radial gradient at a quarter of the frame size, scaled up; the strength is alpha. */
@OptIn(UnstableApi::class)
class VignetteOverlay(filter: ClipFilter) : BitmapOverlay() {
    @Volatile private var alpha: Float = ColorMath.vignetteAlpha(filter)
    @Volatile private var frame = Size(1, 1)
    private var cached: Bitmap? = null
    private var cachedFor: Size? = null

    fun update(filter: ClipFilter) {
        alpha = ColorMath.vignetteAlpha(filter)
    }

    override fun configure(videoSize: Size) {
        frame = videoSize
    }

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val size = frame
        val have = cached
        if (have != null && cachedFor == size) return have
        val w = ceil(size.width / DOWNSCALE).toInt().coerceAtLeast(1)
        val h = ceil(size.height / DOWNSCALE).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val radius = hypot(w / 2f, h / 2f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w / 2f, h / 2f, radius,
                intArrayOf(0x00000000, 0x00000000, 0xFF000000.toInt()),
                floatArrayOf(0f, CLEAR_UNTIL, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        Canvas(bitmap).drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        cached = bitmap
        cachedFor = size
        return bitmap
    }

    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
        val size = frame
        val bitmap = getBitmap(presentationTimeUs)
        val sx = size.width.toFloat() / bitmap.width
        val sy = size.height.toFloat() / bitmap.height
        val strength = alpha
        return object : OverlaySettings {
            override fun getAlphaScale() = strength
            override fun getOverlayFrameAnchor(): Pair<Float, Float> = Pair(0f, 0f)
            override fun getBackgroundFrameAnchor(): Pair<Float, Float> = Pair(0f, 0f)
            override fun getScale(): Pair<Float, Float> = Pair(sx, sy)
        }
    }

    private companion object {
        const val DOWNSCALE = 4f
        const val CLEAR_UNTIL = 0.5f
    }
}

/** The effects of one filtered clip and the way to change them live. */
@OptIn(UnstableApi::class)
class ClipFilterEffects(filter: ClipFilter) {
    private val rgb = LiveRgbMatrix(filter)
    private val vignette = VignetteOverlay(filter)

    /** In drawing order: the color transform, then the vignette. */
    val effects: List<androidx.media3.effect.GlEffect> = listOf(rgb, OverlayEffect(listOf(vignette)))

    fun update(filter: ClipFilter) {
        rgb.update(filter)
        vignette.update(filter)
    }
}
