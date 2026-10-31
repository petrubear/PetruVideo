package com.petruvideo.core.media.preview

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.petruvideo.core.model.Reverb
import java.nio.ByteBuffer

/**
 * Runs [ReverbDsp] on 16-bit mono or stereo PCM for one clip. Settings can change while playing ([update]); a
 * clip with reverb off passes straight through. No tail is emitted after the input ends, so the effect stops at
 * the clip's end, and the filters are cleared on every flush (seek or new item).
 */
@OptIn(UnstableApi::class)
class ReverbAudioProcessor(initial: Reverb) : BaseAudioProcessor() {
    @Volatile private var target: Reverb = initial
    @Volatile private var roomVersion = 0
    private var appliedRoomVersion = -1
    private var appliedTarget: Reverb? = null
    private var wasEnabled = false
    private var dsp: ReverbDsp? = null
    private var work = FloatArray(0)
    private var bytes = ByteArray(0)

    /** Changes the settings; [applyRoomSize] also changes the room size (the user released the control). */
    fun update(reverb: Reverb, applyRoomSize: Boolean = false) {
        target = reverb
        if (applyRoomSize) roomVersion++
    }

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        val supported = inputAudioFormat.encoding == C.ENCODING_PCM_16BIT && inputAudioFormat.channelCount in 1..2
        if (!supported) return AudioFormat.NOT_SET
        dsp = ReverbDsp(inputAudioFormat.sampleRate, inputAudioFormat.channelCount)
        appliedRoomVersion = -1
        appliedTarget = null
        return inputAudioFormat
    }

    override fun onFlush() {
        dsp?.reset()
        wasEnabled = false
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val size = inputBuffer.remaining()
        // Copy the input first: the pipeline may hand back this processor's own output buffer.
        if (bytes.size < size) bytes = ByteArray(size)
        inputBuffer.get(bytes, 0, size)
        val output = replaceOutputBuffer(size)
        val settings = target
        val engine = dsp
        if (engine == null || !settings.enabled) {
            output.put(bytes, 0, size)
            output.flip()
            wasEnabled = false
            return
        }
        if (!wasEnabled) engine.reset()
        wasEnabled = true
        if (appliedTarget != settings) {
            engine.setSettings(settings)
            appliedTarget = settings
        }
        if (appliedRoomVersion != roomVersion) {
            engine.setRoomSize(settings.roomSize)
            appliedRoomVersion = roomVersion
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
