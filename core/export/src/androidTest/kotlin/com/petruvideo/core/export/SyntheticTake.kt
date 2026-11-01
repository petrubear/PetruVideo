package com.petruvideo.core.export

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import java.io.ByteArrayOutputStream
import kotlin.math.PI
import kotlin.math.sin

/**
 * A voice-over take made without the microphone: a sine wave encoded to AAC at the recorder's settings (44.1 kHz, mono,
 * 96 kbps) and framed as an ADTS stream, the file format the recorder writes (feature 013).
 */
object SyntheticTake {
    private const val SAMPLE_RATE = 44_100
    private const val BITRATE = 96_000
    private const val FRAME_SAMPLES = 1_024
    private const val TIMEOUT_US = 10_000L
    private const val ADTS_HEADER = 7
    private const val FREQ_INDEX_44100 = 4

    fun bytes(ms: Long): ByteArray {
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, SAMPLE_RATE, 1).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, BITRATE)
        }
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()
        val out = ByteArrayOutputStream()
        val info = MediaCodec.BufferInfo()
        val total = (SAMPLE_RATE * ms / 1000).toInt()
        var fed = 0
        var inputDone = false
        var outputDone = false
        while (!outputDone) {
            if (!inputDone) {
                val index = codec.dequeueInputBuffer(TIMEOUT_US)
                if (index >= 0) {
                    val buffer = codec.getInputBuffer(index)!!
                    val samples = minOf(buffer.capacity() / 2, total - fed, FRAME_SAMPLES * 2)
                    buffer.clear()
                    for (i in 0 until samples) {
                        buffer.putShort((sin(2 * PI * 440 * (fed + i) / SAMPLE_RATE) * 12_000).toInt().toShort())
                    }
                    fed += samples
                    inputDone = fed >= total
                    val flags = if (inputDone) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                    codec.queueInputBuffer(index, 0, samples * 2, fed * 1_000_000L / SAMPLE_RATE, flags)
                }
            }
            val outIndex = codec.dequeueOutputBuffer(info, TIMEOUT_US)
            if (outIndex >= 0) {
                val buffer = codec.getOutputBuffer(outIndex)!!
                if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0 && info.size > 0) {
                    val frame = ByteArray(info.size)
                    buffer.position(info.offset)
                    buffer.get(frame)
                    out.write(header(frame.size))
                    out.write(frame)
                }
                outputDone = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                codec.releaseOutputBuffer(outIndex, false)
            }
        }
        codec.stop()
        codec.release()
        return out.toByteArray()
    }

    private fun header(payload: Int): ByteArray {
        val length = payload + ADTS_HEADER
        return byteArrayOf(
            0xFF.toByte(), 0xF1.toByte(),
            ((1 shl 6) or (FREQ_INDEX_44100 shl 2)).toByte(),
            ((1 shl 6) or (length shr 11)).toByte(),
            ((length shr 3) and 0xFF).toByte(),
            (((length and 7) shl 5) or 0x1F).toByte(),
            0xFC.toByte(),
        )
    }
}
