package com.petruvideo.core.media.text

import com.petruvideo.core.model.TextShadow
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

/** The sums behind rotated and shadowed text (feature 036), kept apart from drawing so they can be tested. */
internal object TextGeometry {
    private const val SHADOW_BLUR_SHARE = 0.15f
    private const val SHADOW_DISTANCE_SHARE = 0.25f
    private const val PERCENT = 100f
    private const val RIGHT_ANGLE = 90
    private const val HALF_TURN = 180

    /** The size of the smallest box that holds a [width] by [height] block turned by [degrees]. */
    fun turnedSize(width: Int, height: Int, degrees: Int): Pair<Int, Int> {
        val turn = ((degrees % HALF_TURN) + HALF_TURN) % HALF_TURN
        if (turn == 0) return width to height
        if (turn == RIGHT_ANGLE) return height to width
        val radians = Math.toRadians(degrees.toDouble())
        val c = abs(cos(radians))
        val s = abs(sin(radians))
        val w = ceil(width * c + height * s - EPSILON).toInt().coerceAtLeast(1)
        val h = ceil(width * s + height * c - EPSILON).toInt().coerceAtLeast(1)
        return w to h
    }

    /** How blurred the shadow is, in pixels, for text of [textSize] pixels. */
    fun shadowBlur(textSize: Float, shadow: TextShadow): Float =
        textSize * SHADOW_BLUR_SHARE * shadow.softnessPercent / PERCENT

    /** How far the shadow sits from the text, in pixels. */
    fun shadowDistance(textSize: Float, shadow: TextShadow): Float =
        textSize * SHADOW_DISTANCE_SHARE * shadow.distancePercent / PERCENT

    /** The shadow's pixel offset: direction 0 is to the right and 90 is straight down. */
    fun shadowOffset(textSize: Float, shadow: TextShadow): Pair<Float, Float> {
        val radians = Math.toRadians(shadow.directionDegrees.toDouble())
        val distance = shadowDistance(textSize, shadow)
        return (distance * cos(radians)).toFloat() to (distance * sin(radians)).toFloat()
    }

    /** Whole pixels the shadow can reach past the text on any side; 0 when there is no shadow. */
    fun shadowReach(textSize: Float, shadow: TextShadow): Int =
        if (!shadow.enabled) 0 else ceil(shadowBlur(textSize, shadow) * 2f + shadowDistance(textSize, shadow)).toInt()

    private const val EPSILON = 1e-6
}
