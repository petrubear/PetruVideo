package com.petruvideo.core.media.waveform

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.petruvideo.core.model.MediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Downsampled audio peaks (0..1, [PEAKS_PER_SECOND] per second) for waveform drawing. Computed once per media
 * item by decoding the audio track, then cached on disk keyed by the media fingerprint (research 7).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WaveformPeaksGenerator(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher,
) {
    @Inject
    constructor(@ApplicationContext context: Context) :
        this(context, Dispatchers.IO.limitedParallelism(DECODE_PARALLELISM))

    private val decodes = AtomicInteger()

    /** How many times audio was actually decoded (cache hits do not count); exposed for tests. */
    val decodeCount: Int get() = decodes.get()

    /** Peaks for the item's sound, or null when it has none or cannot be decoded. */
    suspend fun peaks(item: MediaItem): FloatArray? {
        if (!item.hasAudio) return null
        val cache = cacheFile(item)
        return withContext(dispatcher) {
            readCache(cache) ?: runCatching { decode(Uri.parse(item.uri)) }.getOrNull()?.also {
                decodes.incrementAndGet()
                writeCache(cache, it)
            }
        }
    }

    private fun cacheFile(item: MediaItem): File {
        val f = item.fingerprint
        val dir = File(context.cacheDir, "waveforms").apply { mkdirs() }
        // The address is part of the key: stems of a voice split have the same size, time and length.
        val address = item.uri.hashCode().toUInt().toString(16)
        return File(dir, "${f.sizeBytes}-${f.lastModified}-${f.durationMs}-$address.peaks")
    }

    private fun readCache(file: File): FloatArray? = runCatching {
        if (!file.exists()) return null
        val bytes = ByteBuffer.wrap(file.readBytes()).order(ByteOrder.LITTLE_ENDIAN)
        FloatArray(bytes.remaining() / Float.SIZE_BYTES) { bytes.float }
    }.getOrNull()

    private fun writeCache(file: File, peaks: FloatArray) {
        val bytes = ByteBuffer.allocate(peaks.size * Float.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        peaks.forEach { bytes.putFloat(it) }
        file.writeBytes(bytes.array())
    }

    private suspend fun decode(uri: Uri): FloatArray? {
        val extractor = MediaExtractor()
        extractor.setDataSource(context, uri, null)
        try {
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return null
            extractor.selectTrack(track)
            return decodeTrack(extractor, extractor.getTrackFormat(track))
        } finally {
            extractor.release()
        }
    }

    private suspend fun decodeTrack(extractor: MediaExtractor, format: MediaFormat): FloatArray {
        val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
        codec.configure(format, null, null, 0)
        codec.start()
        val acc = PeakAccumulator(
            format.getInteger(MediaFormat.KEY_SAMPLE_RATE),
            format.getInteger(MediaFormat.KEY_CHANNEL_COUNT),
        )
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        try {
            while (!outputDone) {
                currentCoroutineContext().ensureActive()
                if (!inputDone) inputDone = feed(codec, extractor)
                val index = codec.dequeueOutputBuffer(info, TIMEOUT_US)
                if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    acc.sampleRate = codec.outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    acc.channels = codec.outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                } else if (index >= 0) {
                    acc.add(codec.getOutputBuffer(index)!!.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer())
                    codec.releaseOutputBuffer(index, false)
                    outputDone = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                }
            }
        } finally {
            codec.stop()
            codec.release()
        }
        return acc.finish()
    }

    /** Folds 16-bit PCM into one peak (the loudest sample of any channel) per 1/[PEAKS_PER_SECOND] second. */
    private class PeakAccumulator(var sampleRate: Int, var channels: Int) {
        private val peaks = ArrayList<Float>()
        private var windowMax = 0
        private var windowCount = 0

        fun add(samples: ShortBuffer) {
            val framesPerPeak = sampleRate / PEAKS_PER_SECOND
            while (samples.remaining() >= channels) {
                repeat(channels) { windowMax = maxOf(windowMax, abs(samples.get().toInt())) }
                if (++windowCount >= framesPerPeak) flush()
            }
        }

        fun finish(): FloatArray {
            if (windowCount > 0) flush()
            return FloatArray(peaks.size) { peaks[it].coerceIn(0f, 1f) }
        }

        private fun flush() {
            peaks += windowMax / PCM_MAX
            windowMax = 0
            windowCount = 0
        }
    }

    /** Queues one input buffer; returns true once the end of the stream has been queued. */
    private fun feed(codec: MediaCodec, extractor: MediaExtractor): Boolean {
        val index = codec.dequeueInputBuffer(TIMEOUT_US)
        if (index < 0) return false
        val size = extractor.readSampleData(codec.getInputBuffer(index)!!, 0)
        return if (size < 0) {
            codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            true
        } else {
            codec.queueInputBuffer(index, 0, size, extractor.sampleTime, 0)
            extractor.advance()
            false
        }
    }

    companion object {
        const val PEAKS_PER_SECOND = 20
        private const val DECODE_PARALLELISM = 2
        private const val TIMEOUT_US = 10_000L
        private const val PCM_MAX = 32_768f
    }
}
