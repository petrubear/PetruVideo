package com.petruvideo.core.media.preview

import com.petruvideo.core.model.Reverb
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh

/**
 * A Freeverb-style reverb for mono or stereo float audio: a pre-delay, 8 damped feedback combs in parallel and 4
 * all-pass filters in series per channel, mixed with the dry signal. All buffers are allocated up front, so
 * [process] never allocates. Settings change while running; mix, decay, and damping are smoothed over a few
 * milliseconds. Room size changes the delay lengths and therefore is applied only by [setRoomSize].
 */
class ReverbDsp(private val sampleRate: Int, private val channels: Int) {
    private val scale = sampleRate / REFERENCE_RATE
    private val maxScale = MAX_ROOM_SCALE
    private val predelayCap = (MAX_PREDELAY_MS * sampleRate / 1000) + 1

    private class Comb(capacity: Int) {
        val buffer = FloatArray(capacity)
        var length = capacity
        var index = 0
        var filter = 0f
        var feedback = 0f
    }

    private class AllPass(val buffer: FloatArray) {
        var index = 0
    }

    private val combs = Array(channels) { ch ->
        Array(COMB_DELAYS.size) { i ->
            Comb(((COMB_DELAYS[i] + ch * STEREO_SPREAD) * scale * maxScale).toInt() + 1)
        }
    }
    private val allPasses = Array(channels) { ch ->
        Array(ALLPASS_DELAYS.size) { i ->
            AllPass(FloatArray(((ALLPASS_DELAYS[i] + ch * STEREO_SPREAD) * scale).toInt() + 1))
        }
    }
    private val predelay = Array(channels) { FloatArray(predelayCap) }
    private var predelayIndex = 0

    private var roomPercent = 50
    private var decaySeconds = 1.5f
    private var targetDamping = 0.5f * MAX_DAMPING
    private var damping = targetDamping
    private var predelaySamples = 0
    private var targetMix = 0.3f
    private var mix = targetMix
    private var targetFeedbackScale = 1f

    init {
        setRoomSize(roomPercent)
        updateFeedback()
    }

    /** Applies every setting except room size, which needs [setRoomSize]. */
    fun setSettings(reverb: Reverb) {
        decaySeconds = reverb.decayMs / MS_PER_SECOND
        targetDamping = reverb.damping / PERCENT * MAX_DAMPING
        predelaySamples = (reverb.preDelayMs * sampleRate / 1000).coerceIn(0, predelayCap - 1)
        targetMix = reverb.mix / PERCENT
        updateFeedback()
    }

    /** Changes the comb delay lengths; call when the user releases the control or when building the clip. */
    fun setRoomSize(percent: Int) {
        roomPercent = percent
        val factor = MIN_ROOM_SCALE + (maxScale - MIN_ROOM_SCALE) * percent / PERCENT
        for (ch in combs) {
            for ((i, comb) in ch.withIndex()) {
                val delay = (COMB_DELAYS[i] + (if (ch === combs[0]) 0 else STEREO_SPREAD)) * scale * factor
                comb.length = delay.toInt().coerceIn(1, comb.buffer.size)
                if (comb.index >= comb.length) comb.index = 0
            }
        }
        updateFeedback()
    }

    /** Clears the filters, e.g. after a seek, so no old tail is heard. */
    fun reset() {
        for (ch in combs) for (c in ch) { c.buffer.fill(0f); c.filter = 0f; c.index = 0 }
        for (ch in allPasses) for (a in ch) { a.buffer.fill(0f); a.index = 0 }
        for (p in predelay) p.fill(0f)
        predelayIndex = 0
        mix = targetMix
        damping = targetDamping
    }

    /** Feedback that makes each comb fall 60 dB in the decay time: g = 10^(-3 * delay / (decay * rate)). */
    private fun updateFeedback() {
        for (ch in combs) {
            for (c in ch) {
                c.feedback = 10f.pow(-3f * c.length / (decaySeconds * sampleRate)).coerceAtMost(MAX_FEEDBACK)
            }
        }
        targetFeedbackScale = 1f
    }

    /** Processes [frames] interleaved frames in place. */
    fun process(samples: FloatArray, frames: Int) {
        val mixStart = mix
        val dampStart = damping
        val mixStep = (targetMix - mixStart) / frames
        val dampStep = (targetDamping - dampStart) / frames
        for (f in 0 until frames) {
            mix = mixStart + mixStep * (f + 1)
            damping = dampStart + dampStep * (f + 1)
            val wetGain = sin(mix * HALF_PI) * WET_GAIN
            val dryGain = cos(mix * HALF_PI)
            for (ch in 0 until channels) {
                val at = f * channels + ch
                samples[at] = sample(ch, samples[at], dryGain, wetGain)
            }
            predelayIndex = (predelayIndex + 1) % predelayCap
        }
    }

    private fun sample(ch: Int, dry: Float, dryGain: Float, wetGain: Float): Float {
        val line = predelay[ch]
        line[predelayIndex % predelayCap] = dry
        val input = line[(predelayIndex - predelaySamples + predelayCap) % predelayCap] * INPUT_GAIN
        var wet = 0f
        for (c in combs[ch]) {
            val out = c.buffer[c.index]
            c.filter = out * (1f - damping) + c.filter * damping
            c.buffer[c.index] = input + c.filter * c.feedback
            if (++c.index >= c.length) c.index = 0
            wet += out
        }
        for (a in allPasses[ch]) {
            val buffered = a.buffer[a.index]
            val out = -wet + buffered
            a.buffer[a.index] = wet + buffered * ALLPASS_FEEDBACK
            if (++a.index >= a.buffer.size) a.index = 0
            wet = out
        }
        return limit(dry * dryGain + wet * wetGain)
    }

    companion object {
        private const val REFERENCE_RATE = 44_100f
        private const val MAX_ROOM_SCALE = 1.4f
        private const val MIN_ROOM_SCALE = 0.6f
        private const val MAX_PREDELAY_MS = 200
        private const val STEREO_SPREAD = 23
        private const val INPUT_GAIN = 0.05f
        private const val WET_GAIN = 3f
        private const val ALLPASS_FEEDBACK = 0.5f
        private const val MAX_DAMPING = 0.8f
        private const val MAX_FEEDBACK = 0.999f
        private const val PERCENT = 100f
        private const val MS_PER_SECOND = 1000f
        private const val KNEE = 0.9f
        private val HALF_PI = (PI / 2).toFloat()
        private val COMB_DELAYS = intArrayOf(1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617)
        private val ALLPASS_DELAYS = intArrayOf(556, 441, 341, 225)

        /** Keeps peaks inside full scale with a soft knee instead of hard clipping. */
        fun limit(x: Float): Float {
            val a = abs(x)
            if (a <= KNEE) return x
            val y = KNEE + (1f - KNEE) * tanh((a - KNEE) / (1f - KNEE))
            return if (x < 0) -y else y
        }
    }
}
