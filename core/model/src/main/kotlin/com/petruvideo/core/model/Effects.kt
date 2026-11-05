package com.petruvideo.core.model

const val TRANSITION_MIN_MS = 200L
const val TRANSITION_MAX_MS = 2_000L
const val TRANSITION_STEP_MS = 100L
const val TRANSITION_DEFAULT_MS = 1_000L
const val EFFECT_STRENGTH_DEFAULT = 50
const val EFFECT_MAX = 3

enum class TransitionKind {
    Dissolve,
    FadeBlack,
    Slide,
    Cover,
    Zoom,

    // Added in feature 035; appended so the five above keep their place.
    Spin,
    Flip,
    PushUp,
    PushDown,
    PushRight,
    ZoomOut,
    WhipPan,
    Drop,
    DipZoom,
    TiltSlide,
}

/** A blend between a clip and the next one; the next clip starts [lengthMs] earlier. */
data class Transition(val kind: TransitionKind, val lengthMs: Long = TRANSITION_DEFAULT_MS) {
    val inRange: Boolean
        get() = lengthMs in TRANSITION_MIN_MS..TRANSITION_MAX_MS && lengthMs % TRANSITION_STEP_MS == 0L
}

enum class EffectKind {
    Blur,
    Pixelate,
    Mirror,
    Shake,
    Glitch,
    RgbSplit,
    Glow,
    Vhs,
    Grain,

    // Added in feature 035; appended so the nine above keep their place.
    ZoomPulse,
    Flash,
    LightLeak,
    Letterbox,
    ZoomBlur,
    Fisheye,
    Ripple,
    Strobe,
    OldFilm,
    Cartoon,
}

data class ClipEffect(val kind: EffectKind, val strength: Int = EFFECT_STRENGTH_DEFAULT) {
    fun normalized() = copy(strength = strength.coerceIn(0, 100))
    val inRange: Boolean get() = strength in 0..100
}

/** The shown length of the transition after the clip at [index] of [lane]: limited to the neighbours, 0 if none. */
fun transitionLengthAfter(lane: List<VideoClip>, index: Int): Long {
    val transition = lane.getOrNull(index)?.transitionOut ?: return 0L
    val next = lane.getOrNull(index + 1) ?: return 0L
    val length = minOf(transition.lengthMs, transitionLimitMs(lane[index].lengthMs, next.lengthMs))
    return if (length < TRANSITION_MIN_MS) 0L else length
}

/** The longest transition two neighbours allow: half of the shorter one, rounded down to the step. */
fun transitionLimitMs(firstMs: Long, secondMs: Long): Long =
    minOf(TRANSITION_MAX_MS, minOf(firstMs, secondMs) / 2 / TRANSITION_STEP_MS * TRANSITION_STEP_MS)
