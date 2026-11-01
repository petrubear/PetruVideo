package com.petruvideo.core.media.preview

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.petruvideo.core.media.dsp.VoiceFxChain
import com.petruvideo.core.model.AudioFx
import java.nio.ByteBuffer

/**
 * Runs the noise reduction, equalizer, and voice effects of [VoiceFxChain] on 16-bit mono or stereo PCM for one
 * clip. Settings change while playing ([update]). With every stage off the audio passes straight through; the
 * filters are cleared on every flush (seek or new item) and nothing is emitted after the input ends.
 */
@OptIn(UnstableApi::class)
class VoiceFxProcessor(initial: AudioFx, private val heavyAllowed: Boolean = true) : BaseAudioProcessor() {
    @Volatile private var fx: AudioFx = initial
    @Volatile private var heavy = heavyAllowed
    private var chain: VoiceFxChain? = null
    private var applied: AudioFx? = null
    private var appliedHeavy = true
    private var work = FloatArray(0)
    private var bytes = ByteArray(0)

    fun update(value: AudioFx, heavyAllowed: Boolean = heavy) {
        fx = value
        heavy = heavyAllowed
    }

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        val supported = inputAudioFormat.encoding == C.ENCODING_PCM_16BIT && inputAudioFormat.channelCount in 1..2
        if (!supported) return AudioFormat.NOT_SET
        chain = VoiceFxChain(inputAudioFormat.sampleRate, inputAudioFormat.channelCount)
        applied = null
        return inputAudioFormat
    }

    override fun onFlush() {
        chain?.reset()
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val size = inputBuffer.remaining()
        // Copy the input first: the pipeline may hand back this processor's own output buffer.
        if (bytes.size < size) bytes = ByteArray(size)
        inputBuffer.get(bytes, 0, size)
        val output = replaceOutputBuffer(size)
        val engine = chain
        val settings = fx
        val allowed = heavy
        if (engine != null && (applied != settings || appliedHeavy != allowed)) {
            engine.setFx(settings, allowed)
            applied = settings
            appliedHeavy = allowed
        }
        if (engine == null || !engine.active) {
            output.put(bytes, 0, size)
            output.flip()
            return
        }
        val samples = size / 2
        if (work.size < samples) work = FloatArray(samples)
        val view = ByteBuffer.wrap(bytes, 0, size).order(output.order())
        for (i in 0 until samples) work[i] = view.getShort() / SCALE
        engine.process(work, samples / inputAudioFormat.channelCount)
        for (i in 0 until samples) {
            val scaled = (work[i] * SCALE).toInt()
            output.putShort(scaled.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort())
        }
        output.flip()
    }

    private companion object {
        const val SCALE = 32768f
    }
}
