package com.petruvideo.core.media.preview

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.petruvideo.core.model.Fade
import kotlin.math.abs
import kotlin.math.tanh

/**
 * Scales 16-bit PCM by a gain that can change while playing, so a volume drag is heard within one buffer without
 * rebuilding the composition, and applies the clip's fade in and out. The clip position comes from the stream
 * offset given at each flush plus the frames processed since, so a fade is right after a seek. Above full scale a
 * soft knee limits the peaks instead of clipping harshly.
 */
@OptIn(UnstableApi::class)
class GainAudioProcessor(percent: Int, fade: Fade = Fade(), clipLengthMs: Long = 0) : BaseAudioProcessor() {
    @Volatile
    private var gain: Float = percent / PERCENT

    @Volatile
    private var fadeSetting: Fade = fade.limitedTo(clipLengthMs)
    private var lengthMs = clipLengthMs
    private var position = 0L

    fun setPercent(percent: Int) {
        gain = percent / PERCENT
    }

    /** Changes the fades (limited to half the clip) while playing. */
    fun setFade(fade: Fade, clipLengthMs: Long = lengthMs) {
        lengthMs = clipLengthMs
        fadeSetting = fade.limitedTo(clipLengthMs)
    }

    override fun onFlush(streamMetadata: AudioProcessor.StreamMetadata) {
        position = streamMetadata.positionOffsetUs * inputAudioFormat.sampleRate / MICROS
    }

    override fun onFlush() {
        position = 0
    }

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat =
        if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT) inputAudioFormat else AudioFormat.NOT_SET

    override fun queueInput(inputBuffer: java.nio.ByteBuffer) {
        val output = replaceOutputBuffer(inputBuffer.remaining())
        val current = gain
        val fade = fadeSetting
        val rate = inputAudioFormat.sampleRate
        val channels = inputAudioFormat.channelCount.coerceAtLeast(1)
        val inFrames = fade.inMs * rate / MILLIS
        val outFrames = fade.outMs * rate / MILLIS
        val total = lengthMs * rate / MILLIS
        val fading = !fade.isNeutral
        while (inputBuffer.remaining() >= 2 * channels) {
            val frameGain = if (fading) current * FadeCurve.factor(position, inFrames, outFrames, total) else current
            repeat(channels) { output.putShort(scale(inputBuffer.getShort(), frameGain)) }
            position++
        }
        output.flip()
    }

    companion object {
        private const val PERCENT = 100f
        private const val MILLIS = 1000L
        private const val MICROS = 1_000_000L
        private const val FULL_SCALE = 32767f
        private const val KNEE = 0.8f

        /** One sample scaled by [gain]; peaks above [KNEE] of full scale are compressed smoothly. */
        fun scale(sample: Short, gain: Float): Short {
            val x = sample / FULL_SCALE * gain
            val y = if (abs(x) <= KNEE) x else Math.signum(x) * (KNEE + (1 - KNEE) * tanh((abs(x) - KNEE) / (1 - KNEE)))
            return (y * FULL_SCALE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }
}
