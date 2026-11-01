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

enum class NoiseStrength(val label: String) { Low("Low"), Medium("Medium"), High("High") }

data class NoiseReduction(val enabled: Boolean = false, val strength: NoiseStrength = NoiseStrength.Medium)

enum class EqPreset(val label: String) {
    Flat("Flat"),
    VoiceBoost("Voice boost"),
    BassBoost("Bass boost"),
    TrebleBoost("Treble boost"),
    Warm("Warm"),
    Telephone("Telephone"),
    Custom("Custom"),
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

/** Everything a clip's sound gets beyond speed, volume, mute and reverb (feature 005). Defaults change nothing. */
data class AudioFx(
    val fade: Fade = Fade(),
    val noise: NoiseReduction = NoiseReduction(),
    val eq: Equalizer = Equalizer(),
    val voice: VoiceFx = VoiceFx(),
) {
    val inRange: Boolean get() = fade.inRange && eq.inRange && voice.inRange
}

fun AudioFx.withoutFadeIn() = copy(fade = fade.copy(inMs = 0))

fun AudioFx.withoutFadeOut() = copy(fade = fade.copy(outMs = 0))
