package com.petruvideo.core.media.voicesplit

import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile

/** Writes 16-bit PCM WAV audio a piece at a time; the sizes in the header are filled in on [close]. */
class WavWriter(file: File, private val sampleRate: Int, private val channels: Int) : Closeable {
    private val out = RandomAccessFile(file, "rw").also { it.setLength(0) }
    private var dataBytes = 0L

    init {
        out.write(ByteArray(HEADER_BYTES))
    }

    fun write(samples: ShortArray, count: Int = samples.size) {
        val bytes = ByteArray(count * 2)
        for (i in 0 until count) {
            bytes[2 * i] = (samples[i].toInt() and 0xFF).toByte()
            bytes[2 * i + 1] = (samples[i].toInt() shr 8).toByte()
        }
        out.write(bytes)
        dataBytes += bytes.size
    }

    override fun close() {
        out.seek(0)
        out.write(header())
        out.close()
    }

    private fun header(): ByteArray {
        val byteRate = sampleRate * channels * 2
        val bytes = ByteArray(HEADER_BYTES)
        fun text(at: Int, value: String) = value.forEachIndexed { i, c -> bytes[at + i] = c.code.toByte() }
        fun int(at: Int, value: Long, size: Int) = repeat(size) { bytes[at + it] = (value shr (8 * it)).toByte() }
        text(0, "RIFF")
        int(4, 36 + dataBytes, 4)
        text(8, "WAVE")
        text(12, "fmt ")
        int(16, 16, 4)
        int(20, 1, 2)
        int(22, channels.toLong(), 2)
        int(24, sampleRate.toLong(), 4)
        int(28, byteRate.toLong(), 4)
        int(32, (channels * 2).toLong(), 2)
        int(34, 16, 2)
        text(36, "data")
        int(40, dataBytes, 4)
        return bytes
    }

    private companion object {
        const val HEADER_BYTES = 44
    }
}
