package com.petruvideo.core.model

const val FILTER_ADJUST_MAX = 100
const val FILTER_INTENSITY_DEFAULT = 100

enum class FilterLook(val label: String) {
    None("None"),
    Vivid("Vivid"),
    Warm("Warm"),
    Cool("Cool"),
    BlackAndWhite("Black and white"),
    Vintage("Vintage"),
    Faded("Faded"),
}

/** A clip's look (with its strength) and five manual adjustments; the defaults leave the picture unchanged. */
data class ClipFilter(
    val look: FilterLook = FilterLook.None,
    val intensity: Int = FILTER_INTENSITY_DEFAULT,
    val brightness: Int = 0,
    val contrast: Int = 0,
    val saturation: Int = 0,
    val warmth: Int = 0,
    val vignette: Int = 0,
    val exposure: Int = 0,
    val gamma: Int = 0,
    val highlights: Int = 0,
    val shadows: Int = 0,
    val tint: Int = 0,
    val hue: Int = 0,
    val sharpen: Int = 0,
) {
    val lookActive: Boolean get() = look != FilterLook.None && intensity > 0
    val adjustmentsNeutral: Boolean
        get() = brightness == 0 && contrast == 0 && saturation == 0 && warmth == 0 && vignette == 0 &&
            extraNeutral
    /** The seven adjustments of feature 023 are all at their neutral value. */
    val extraNeutral: Boolean
        get() = exposure == 0 && gamma == 0 && highlights == 0 && shadows == 0 && tint == 0 && hue == 0 && sharpen == 0
    val isNeutral: Boolean get() = !lookActive && adjustmentsNeutral

    fun normalized() = copy(
        intensity = intensity.coerceIn(0, 100),
        brightness = brightness.coerceIn(-FILTER_ADJUST_MAX, FILTER_ADJUST_MAX),
        contrast = contrast.coerceIn(-FILTER_ADJUST_MAX, FILTER_ADJUST_MAX),
        saturation = saturation.coerceIn(-FILTER_ADJUST_MAX, FILTER_ADJUST_MAX),
        warmth = warmth.coerceIn(-FILTER_ADJUST_MAX, FILTER_ADJUST_MAX),
        vignette = vignette.coerceIn(0, FILTER_ADJUST_MAX),
        exposure = exposure.coerceIn(-FILTER_ADJUST_MAX, FILTER_ADJUST_MAX),
        gamma = gamma.coerceIn(-FILTER_ADJUST_MAX, FILTER_ADJUST_MAX),
        highlights = highlights.coerceIn(-FILTER_ADJUST_MAX, FILTER_ADJUST_MAX),
        shadows = shadows.coerceIn(-FILTER_ADJUST_MAX, FILTER_ADJUST_MAX),
        tint = tint.coerceIn(-FILTER_ADJUST_MAX, FILTER_ADJUST_MAX),
        hue = hue.coerceIn(-FILTER_ADJUST_MAX, FILTER_ADJUST_MAX),
        sharpen = sharpen.coerceIn(0, FILTER_ADJUST_MAX),
    )

    val inRange: Boolean get() = normalized() == this

    /** One canonical non-neutral value, so the editor can tell "a filter is on" from "its numbers changed". */
    fun structural(): ClipFilter = if (isNeutral) ClipFilter() else ACTIVE

    companion object {
        val ACTIVE = ClipFilter(look = FilterLook.Vivid)
    }
}
