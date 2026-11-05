package com.petruvideo.core.model

const val FADE_MAX_MS = 30_000L
const val EQ_BAND_MAX_DB = 12
const val ECHO_DELAY_MIN_MS = 50
const val ECHO_DELAY_MAX_MS = 1_000
const val ECHO_REPEATS_MAX = 5
const val PITCH_MAX_SEMITONES = 12
const val FX_PERCENT_MAX = 100

/** Fade in and out lengths in timeline time. Stored as set; the effective lengths follow the clip's length. */
data class Fade(val inMs: Long = 0, val outMs: Long = 0) {
    /** Each fade at most half the clip, so the two never overlap. */
    fun limitedTo(clipLengthMs: Long): Fade {
        val half = (clipLengthMs / 2).coerceAtLeast(0)
        return Fade(inMs.coerceIn(0, minOf(FADE_MAX_MS, half)), outMs.coerceIn(0, minOf(FADE_MAX_MS, half)))
    }

    val inRange: Boolean get() = inMs in 0..FADE_MAX_MS && outMs in 0..FADE_MAX_MS
    val isNeutral: Boolean get() = inMs == 0L && outMs == 0L
}

enum class NoiseStrength { Low, Medium, High }

data class NoiseReduction(val enabled: Boolean = false, val strength: NoiseStrength = NoiseStrength.Medium)

enum class EqPreset {
    Flat,
    VoiceBoost,
    BassBoost,
    TrebleBoost,
    Warm,
    Telephone,
    Custom,
}

/** Three band gains in dB; [preset] says which preset the numbers match (or Custom). Flat means no processing. */
data class Equalizer(
    val preset: EqPreset = EqPreset.Flat,
    val lowDb: Int = 0,
    val midDb: Int = 0,
    val highDb: Int = 0,
) {
    fun normalized(): Equalizer {
        val clamped = copy(
            lowDb = lowDb.coerceIn(-EQ_BAND_MAX_DB, EQ_BAND_MAX_DB),
            midDb = midDb.coerceIn(-EQ_BAND_MAX_DB, EQ_BAND_MAX_DB),
            highDb = highDb.coerceIn(-EQ_BAND_MAX_DB, EQ_BAND_MAX_DB),
        )
        return clamped.copy(preset = EqPresets.match(clamped))
    }

    val inRange: Boolean get() = normalized().copy(preset = preset) == this
    val isFlat: Boolean get() = lowDb == 0 && midDb == 0 && highDb == 0
}

object EqPresets {
    private val values = mapOf(
        EqPreset.Flat to Triple(0, 0, 0),
        EqPreset.VoiceBoost to Triple(-3, 4, 2),
        EqPreset.BassBoost to Triple(8, 0, -1),
        EqPreset.TrebleBoost to Triple(-1, 0, 8),
        EqPreset.Warm to Triple(4, 1, -4),
        EqPreset.Telephone to Triple(-12, 6, -8),
    )

    val all: List<EqPreset> = values.keys.toList()

    /** The numbers of [preset]; Custom has none of its own and returns Flat's. */
    fun settings(preset: EqPreset): Equalizer {
        val (low, mid, high) = values[preset] ?: values.getValue(EqPreset.Flat)
        return Equalizer(preset.takeIf { it != EqPreset.Custom } ?: EqPreset.Flat, low, mid, high)
    }

    fun match(eq: Equalizer): EqPreset =
        values.entries.firstOrNull { it.value == Triple(eq.lowDb, eq.midDb, eq.highDb) }?.key ?: EqPreset.Custom
}

data class Echo(val enabled: Boolean = false, val delayMs: Int = 250, val repeats: Int = 3)

data class Robot(val enabled: Boolean = false, val intensity: Int = 60)

data class Pitch(val enabled: Boolean = false, val semitones: Int = 4)

data class Bass(val enabled: Boolean = false, val amount: Int = 50)

/** The four voice effects; each has its own on/off state and keeps its settings when off. They stack. */
data class VoiceFx(
    val echo: Echo = Echo(),
    val robot: Robot = Robot(),
    val pitch: Pitch = Pitch(),
    val bass: Bass = Bass(),
) {
    fun normalized() = VoiceFx(
        echo = echo.copy(
            delayMs = echo.delayMs.coerceIn(ECHO_DELAY_MIN_MS, ECHO_DELAY_MAX_MS),
            repeats = echo.repeats.coerceIn(1, ECHO_REPEATS_MAX),
        ),
        robot = robot.copy(intensity = robot.intensity.coerceIn(0, FX_PERCENT_MAX)),
        pitch = pitch.copy(semitones = pitch.semitones.coerceIn(-PITCH_MAX_SEMITONES, PITCH_MAX_SEMITONES)),
        bass = bass.copy(amount = bass.amount.coerceIn(0, FX_PERCENT_MAX)),
    )

    val inRange: Boolean get() = normalized() == this
    val anyOn: Boolean get() = echo.enabled || robot.enabled || pitch.enabled || bass.enabled
}

const val COMP_THRESHOLD_MIN_DB = -60
const val COMP_RATIO_MIN_TENTHS = 10
const val COMP_RATIO_MAX_TENTHS = 200
const val COMP_ATTACK_MIN_MS = 1
const val COMP_ATTACK_MAX_MS = 200
const val COMP_RELEASE_MIN_MS = 20
const val COMP_RELEASE_MAX_MS = 1_000
const val COMP_LEVEL_MAX_DB = 12

/** The named compressor settings; [Custom] is any other combination. */
enum class CompressorPreset {
    Gentle,
    Voice,
    Podcast,
    Punchy,
    Custom,
}

/**
 * Evens out loudness (feature 034). [thresholdDb] is where compression starts, [ratioTenths] is the ratio times ten
 * (30 means 3:1), [levelDb] is the manual makeup gain. Strength is a shortcut over threshold and ratio, not a
 * separate stored value. Turning it off keeps the settings; the defaults are the Voice preset.
 */
data class Compressor(
    val enabled: Boolean = false,
    val preset: CompressorPreset = CompressorPreset.Voice,
    val thresholdDb: Int = -22,
    val ratioTenths: Int = 49,
    val attackMs: Int = 10,
    val releaseMs: Int = 120,
    val levelDb: Int = 2,
) {
    /** 0..100: half from how low the threshold is, half from how high the ratio is. */
    val strength: Int
        get() {
            val fromThreshold = (-thresholdDb / STRENGTH_THRESHOLD_SPAN).coerceIn(0f, 1f)
            val fromRatio = ((ratioTenths - COMP_RATIO_MIN_TENTHS) / STRENGTH_RATIO_SPAN).coerceIn(0f, 1f)
            return Math.round(HALF_STRENGTH * (fromThreshold + fromRatio))
        }

    /** The same compressor with threshold and ratio set from a 0..100 strength. */
    fun withStrength(percent: Int): Compressor = withStrengthUnmatched(percent).normalized()

    /** Like [withStrength] but without finding the preset, so the preset table can be built from it. */
    internal fun withStrengthUnmatched(percent: Int): Compressor {
        val p = percent.coerceIn(0, FX_PERCENT_MAX) / FX_PERCENT_MAX.toFloat()
        return copy(
            thresholdDb = -Math.round(p * STRENGTH_THRESHOLD_SPAN),
            ratioTenths = COMP_RATIO_MIN_TENTHS + Math.round(p * STRENGTH_RATIO_SPAN),
        )
    }

    fun normalized(): Compressor {
        val clamped = copy(
            thresholdDb = thresholdDb.coerceIn(COMP_THRESHOLD_MIN_DB, 0),
            ratioTenths = ratioTenths.coerceIn(COMP_RATIO_MIN_TENTHS, COMP_RATIO_MAX_TENTHS),
            attackMs = attackMs.coerceIn(COMP_ATTACK_MIN_MS, COMP_ATTACK_MAX_MS),
            releaseMs = releaseMs.coerceIn(COMP_RELEASE_MIN_MS, COMP_RELEASE_MAX_MS),
            levelDb = levelDb.coerceIn(-COMP_LEVEL_MAX_DB, COMP_LEVEL_MAX_DB),
        )
        return clamped.copy(preset = CompressorPresets.match(clamped))
    }

    val inRange: Boolean get() = normalized().copy(preset = preset) == this

    /** True when it changes the sound: on, and either compressing or adding level. */
    val audible: Boolean get() = enabled && (ratioTenths > COMP_RATIO_MIN_TENTHS && thresholdDb < 0 || levelDb != 0)

    private companion object {
        const val STRENGTH_THRESHOLD_SPAN = 40f
        const val STRENGTH_RATIO_SPAN = 70f
        const val HALF_STRENGTH = 50f
    }
}

object CompressorPresets {
    private fun named(strength: Int, levelDb: Int, attackMs: Int, releaseMs: Int) =
        Compressor(false, CompressorPreset.Custom, 0, COMP_RATIO_MIN_TENTHS, attackMs, releaseMs, levelDb)
            .withStrengthUnmatched(strength).copy(attackMs = attackMs, releaseMs = releaseMs, levelDb = levelDb)

    private val values = mapOf(
        CompressorPreset.Gentle to named(30, 0, 30, 250),
        CompressorPreset.Voice to named(55, 2, 10, 120),
        CompressorPreset.Podcast to named(75, 3, 8, 100),
        CompressorPreset.Punchy to named(65, 2, 25, 80),
    )

    val all: List<CompressorPreset> = values.keys.toList()

    /** The preset's settings with its own level, keeping whether the compressor is on. */
    fun settings(preset: CompressorPreset, enabled: Boolean = false): Compressor =
        (values[preset] ?: values.getValue(CompressorPreset.Voice)).copy(enabled = enabled, preset = preset)

    fun match(c: Compressor): CompressorPreset =
        values.entries.firstOrNull { (_, v) ->
            v.thresholdDb == c.thresholdDb && v.ratioTenths == c.ratioTenths && v.attackMs == c.attackMs &&
                v.releaseMs == c.releaseMs && v.levelDb == c.levelDb
        }?.key ?: CompressorPreset.Custom
}

/** Everything a clip's sound gets beyond speed, volume, mute and reverb (feature 005). Defaults change nothing. */
data class AudioFx(
    val fade: Fade = Fade(),
    val noise: NoiseReduction = NoiseReduction(),
    val eq: Equalizer = Equalizer(),
    val voice: VoiceFx = VoiceFx(),
    val compressor: Compressor = Compressor(),
) {
    val inRange: Boolean get() = fade.inRange && eq.inRange && voice.inRange && compressor.inRange
}

fun AudioFx.withoutFadeIn() = copy(fade = fade.copy(inMs = 0))

fun AudioFx.withoutFadeOut() = copy(fade = fade.copy(outMs = 0))
