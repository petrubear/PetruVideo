package com.petruvideo.core.media.dsp

import com.petruvideo.core.model.AudioFx

/**
 * The per-clip chain of noise reduction, equalizer, compressor, bass, pitch, robot, and echo, in that order. Stages
 * that are off are skipped. [setFx] can be called while processing; [heavyAllowed] false bypasses noise
 * reduction, pitch, and echo (the costly stages) when the preview is under pressure. The compressor is cheap and
 * is never bypassed.
 */
class VoiceFxChain(sampleRate: Int, private val channels: Int) {
    private val noise = NoiseReductionDsp(channels)
    private val eq = EqualizerDsp(sampleRate, channels)
    private val compressor = CompressorDsp(sampleRate, channels)
    private val bass = BassDsp(sampleRate, channels)
    private val pitch = PitchShiftDsp(sampleRate, channels)
    private val robot = RobotDsp(sampleRate, channels)
    private val echo = EchoDsp(sampleRate, channels)

    @Volatile private var fx = AudioFx()
    @Volatile private var heavyAllowed = true
    private var noiseWasOn = false
    private var echoWasOn = false

    fun setFx(value: AudioFx, heavyAllowed: Boolean = true) {
        fx = value
        this.heavyAllowed = heavyAllowed
        noise.setStrength(value.noise.strength)
        eq.setGains(value.eq.lowDb, value.eq.midDb, value.eq.highDb)
        compressor.setSettings(value.compressor)
        bass.setAmount(if (value.voice.bass.enabled) value.voice.bass.amount else 0)
        pitch.setSemitones(if (value.voice.pitch.enabled && heavyAllowed) value.voice.pitch.semitones else 0)
        robot.setIntensity(if (value.voice.robot.enabled) value.voice.robot.intensity else 0)
        echo.setSettings(value.voice.echo.delayMs, value.voice.echo.repeats)
    }

    /** True when at least one stage changes the sound, so a processor can skip the work entirely. */
    val active: Boolean
        get() {
            val now = fx
            val heavy = heavyAllowed
            return (now.noise.enabled && heavy) || !now.eq.isFlat || now.compressor.audible || now.voice.bass.enabled ||
                (now.voice.pitch.enabled && heavy) || now.voice.robot.enabled || (now.voice.echo.enabled && heavy)
        }

    fun reset() {
        noise.reset(); eq.reset(); compressor.reset(); bass.reset(); pitch.reset(); robot.reset(); echo.reset()
        noiseWasOn = false
        echoWasOn = false
    }

    fun process(buffer: FloatArray, frames: Int) {
        val now = fx
        val heavy = heavyAllowed
        val noiseOn = now.noise.enabled && heavy
        if (noiseOn && !noiseWasOn) noise.reset()
        noiseWasOn = noiseOn
        if (noiseOn) noise.process(buffer, frames)
        eq.process(buffer, frames)
        compressor.process(buffer, frames)
        bass.process(buffer, frames)
        pitch.process(buffer, frames)
        robot.process(buffer, frames)
        val echoOn = now.voice.echo.enabled && heavy
        if (echoOn && !echoWasOn) echo.reset()
        echoWasOn = echoOn
        if (echoOn) echo.process(buffer, frames)
    }
}
