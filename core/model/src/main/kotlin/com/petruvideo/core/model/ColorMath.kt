package com.petruvideo.core.model

/**
 * The color transform of a [ClipFilter] as a 4x4 matrix in column-major order acting on [r g b 1], with the offsets
 * in elements 12 to 14 (the layout Media3's `RgbMatrix` uses). Order: look (blended by intensity), saturation,
 * contrast, brightness, warmth. Pure, so the preview tiles and the export use the same numbers.
 */
object ColorMath {
    private const val LR = 0.2126f
    private const val LG = 0.7152f
    private const val LB = 0.0722f
    private const val BRIGHTNESS_RANGE = 0.35f
    private const val CONTRAST_RANGE = 0.8f
    private const val SATURATION_RANGE = 1f
    private const val WARMTH_RANGE = 0.15f
    private const val VIGNETTE_MAX_ALPHA = 0.85f

    fun identity() = floatArrayOf(1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f)

    fun matrix(filter: ClipFilter): FloatArray {
        var m = identity()
        if (filter.lookActive) m = blend(look(filter.look), filter.intensity / 100f)
        if (filter.saturation != 0) m = mul(saturation(1f + filter.saturation / 100f * SATURATION_RANGE), m)
        if (filter.contrast != 0) m = mul(contrast(1f + filter.contrast / 100f * CONTRAST_RANGE), m)
        if (filter.brightness != 0) m = mul(offset(filter.brightness / 100f * BRIGHTNESS_RANGE), m)
        if (filter.warmth != 0) {
            val w = filter.warmth / 100f * WARMTH_RANGE
            m = mul(scale(1f + w, 1f, 1f - w), m)
        }
        return m
    }

    /** Opacity of the dark edges, 0 to 0.85 for a vignette of 0 to 100. */
    fun vignetteAlpha(filter: ClipFilter): Float = filter.vignette.coerceIn(0, 100) / 100f * VIGNETTE_MAX_ALPHA

    private fun look(look: FilterLook): FloatArray = when (look) {
        FilterLook.None -> identity()
        FilterLook.Vivid -> mul(contrast(1.1f), saturation(1.35f))
        FilterLook.Warm -> mul(offset(0.02f, 0f, -0.02f), scale(1.08f, 1f, 0.92f))
        FilterLook.Cool -> mul(offset(-0.01f, 0f, 0.02f), scale(0.92f, 1f, 1.08f))
        FilterLook.BlackAndWhite -> mul(contrast(1.05f), saturation(0f))
        FilterLook.Vintage -> mul(offset(0.05f), mul(contrast(0.9f), blend(sepia(), 0.6f)))
        FilterLook.Faded -> mul(offset(0.06f), mul(saturation(0.85f), contrast(0.85f)))
    }

    private fun sepia() = rows(
        listOf(0.393f, 0.769f, 0.189f, 0.349f, 0.686f, 0.168f, 0.272f, 0.534f, 0.131f),
    )

    /** [m] mixed with the identity: 0 gives the identity, 1 gives [m]. */
    fun blend(m: FloatArray, t: Float): FloatArray {
        val id = identity()
        return FloatArray(16) { id[it] + (m[it] - id[it]) * t }
    }

    /** A 4x4 column-major matrix from the nine numbers of a 3x3 color matrix given row by row. */
    private fun rows(v: List<Float>) = floatArrayOf(
        v[0], v[3], v[6], 0f, v[1], v[4], v[7], 0f, v[2], v[5], v[8], 0f, 0f, 0f, 0f, 1f,
    )

    private fun saturation(s: Float): FloatArray {
        val inv = 1f - s
        return rows(
            listOf(
                LR * inv + s, LG * inv, LB * inv,
                LR * inv, LG * inv + s, LB * inv,
                LR * inv, LG * inv, LB * inv + s,
            ),
        )
    }

    private fun scale(r: Float, g: Float, b: Float) = rows(listOf(r, 0f, 0f, 0f, g, 0f, 0f, 0f, b))

    private fun contrast(k: Float) = scale(k, k, k).also {
        val shift = 0.5f * (1f - k)
        it[12] = shift
        it[13] = shift
        it[14] = shift
    }

    private fun offset(r: Float, g: Float = r, b: Float = r) = identity().also {
        it[12] = r
        it[13] = g
        it[14] = b
    }

    /** The matrix that applies [b] first and then [a] (column-major 4x4 product a x b). */
    fun mul(a: FloatArray, b: FloatArray): FloatArray {
        val out = FloatArray(16)
        for (col in 0 until 4) {
            for (row in 0 until 4) {
                var sum = 0f
                for (k in 0 until 4) sum += a[k * 4 + row] * b[col * 4 + k]
                out[col * 4 + row] = sum
            }
        }
        return out
    }
}
