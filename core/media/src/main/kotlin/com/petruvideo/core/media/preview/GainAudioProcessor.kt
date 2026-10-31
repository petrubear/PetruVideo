package com.petruvideo.core.media.preview

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import kotlin.math.abs
import kotlin.math.tanh

/**
 * Scales 16-bit PCM by a gain that can change while playing, so a volume drag is heard within one buffer without
 * rebuilding the composition. Above full scale a soft knee limits the peaks instead of clipping harshly.
 */
@OptIn(UnstableApi::class)
class GainAudioProcessor(percent: Int) : BaseAudioProcessor() {
    @Volatile
    private var gain: Float = percent / PERCENT

    fun setPercent(percent: Int) {
        gain = percent / PERCENT
    }

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat =
        if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT) inputAudioFormat else AudioFormat.NOT_SET

    override fun queueInput(inputBuffer: java.nio.ByteBuffer) {
        val output = replaceOutputBuffer(inputBuffer.remaining())
        val current = gain
        while (inputBuffer.remaining() >= 2) {
            output.putShort(scale(inputBuffer.getShort(), current))
        }
        output.flip()
    }

    companion object {
        private const val PERCENT = 100f
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
