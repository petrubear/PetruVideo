package com.petruvideo.core.media.reverse

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * Writes a backwards copy of one range of a video (feature 017). The range is first made all-intra with Media3 (every
 * frame a sync frame, none depends on another), so its compressed frames can be copied into a new file in reverse order
 * without decoding them; the sound is decoded, reversed, and encoded again. The copy is a normal MP4.
 */
@OptIn(UnstableApi::class)
class ReverseRenderer(private val context: Context) {
    /** Progress is 0 to 100. Returns false when the video cannot be read; cancellation deletes every partial file. */
    suspend fun render(
        uri: String,
        startMs: Long,
        endMs: Long,
        withSound: Boolean,
        output: File,
        onProgress: (Int) -> Unit,
    ): Boolean {
        val work = File(output.parentFile, "${output.name}.work").apply { mkdirs() }
        val intra = File(work, "intra.mp4")
        return try {
            val intraOk = makeAllIntra(uri, startMs, endMs, intra) { onProgress(it * INTRA_SHARE / PERCENT) }
            if (!intraOk) return false
            val sound = if (withSound) withContext(Dispatchers.Default) { reversedSound(uri, startMs, endMs) } else null
            onProgress(INTRA_SHARE + SOUND_SHARE)
            currentCoroutineContext().ensureActive()
            val written = withContext(Dispatchers.IO) { remux(intra, sound, output) }
            if (written) onProgress(PERCENT)
            written
        } catch (e: CancellationException) {
            output.delete()
            throw e
        } finally {
            work.deleteRecursively()
        }
    }

    private suspend fun makeAllIntra(
        uri: String,
        startMs: Long,
        endMs: Long,
        file: File,
        onProgress: (Int) -> Unit,
    ): Boolean = withContext(Dispatchers.Main.immediate) {
        suspendCancellableCoroutine { continuation ->
            val handler = Handler(Looper.getMainLooper())
            val item = MediaItem.Builder().setUri(Uri.parse(uri))
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(startMs)
                        .setEndPositionMs(endMs)
                        .build(),
                ).build()
            val encoders = DefaultEncoderFactory.Builder(context)
                .setRequestedVideoEncoderSettings(
                    VideoEncoderSettings.Builder().setiFrameIntervalSeconds(0f).setBitrate(INTRA_BITRATE).build(),
                )
                .setEnableFallback(true)
                .build()
            var polling = true
            lateinit var transformer: Transformer
            val listener = object : Transformer.Listener {
                override fun onCompleted(composition: androidx.media3.transformer.Composition, result: ExportResult) {
                    polling = false
                    if (continuation.isActive) continuation.resume(true)
                }

                override fun onError(
                    composition: androidx.media3.transformer.Composition,
                    result: ExportResult,
                    exception: ExportException,
                ) {
                    polling = false
                    if (continuation.isActive) continuation.resume(false)
                }
            }
            transformer = Transformer.Builder(context)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .setEncoderFactory(encoders)
                .addListener(listener)
                .build()
            val holder = ProgressHolder()
            val poll = object : Runnable {
                override fun run() {
                    if (!polling) return
                    if (transformer.getProgress(holder) != Transformer.PROGRESS_STATE_NOT_STARTED) {
                        onProgress(holder.progress)
                    }
                    handler.postDelayed(this, POLL_MS)
                }
            }
            continuation.invokeOnCancellation {
                polling = false
                handler.post { transformer.cancel() }
            }
            runCatching {
                transformer.start(EditedMediaItem.Builder(item).setRemoveAudio(true).build(), file.absolutePath)
            }.onSuccess { handler.post(poll) }.onFailure { continuation.resume(false) }
        }
    }

    /** The encoded reversed sound of the range, or null when there is none or it cannot be read. */
    private suspend fun reversedSound(uri: String, startMs: Long, endMs: Long): ReversedSound? {
        val pcm = SoundCodec.decode(context, Uri.parse(uri), startMs * MICROS, endMs * MICROS) ?: return null
        currentCoroutineContext().ensureActive()
        return SoundCodec.encode(pcm.reversed())
    }

    /** Copies the intra file's frames backwards into [output], with [sound] woven in by time. */
    private suspend fun remux(intra: File, sound: ReversedSound?, output: File): Boolean {
        val extractor = MediaExtractor()
        val muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var started = false
        try {
            extractor.setDataSource(intra.absolutePath)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
            } ?: return false
            val format = extractor.getTrackFormat(track)
            extractor.selectTrack(track)
            if (format.containsKey(MediaFormat.KEY_ROTATION)) {
                muxer.setOrientationHint(format.getInteger(MediaFormat.KEY_ROTATION))
            }
            val videoOut = muxer.addTrack(format)
            val audioOut = sound?.let { muxer.addTrack(it.format) } ?: -1
            muxer.start()
            started = true
            val times = sampleTimes(extractor)
            if (times.isEmpty()) return false
            writeInterleaved(extractor, muxer, videoOut, audioOut, times, sound)
            return true
        } finally {
            extractor.release()
            if (started) runCatching { muxer.stop() }
            muxer.release()
            if (!started) output.delete()
        }
    }

    private fun sampleTimes(extractor: MediaExtractor): LongArray {
        val times = ArrayList<Long>()
        while (extractor.sampleTime >= 0) {
            times += extractor.sampleTime
            if (!extractor.advance()) break
        }
        return times.toLongArray()
    }

    private suspend fun writeInterleaved(
        extractor: MediaExtractor,
        muxer: MediaMuxer,
        videoOut: Int,
        audioOut: Int,
        times: LongArray,
        sound: ReversedSound?,
    ) {
        val last = times.last()
        val buffer = ByteBuffer.allocateDirect(MAX_SAMPLE).order(ByteOrder.nativeOrder())
        val info = MediaCodec.BufferInfo()
        var audioIndex = 0
        for (i in times.indices.reversed()) {
            currentCoroutineContext().ensureActive()
            val newTime = last - times[i]
            while (sound != null && audioIndex < sound.chunks.size && sound.chunks[audioIndex].timeUs <= newTime) {
                writeChunk(muxer, audioOut, sound.chunks[audioIndex++])
            }
            extractor.seekTo(times[i], MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            buffer.clear()
            val size = extractor.readSampleData(buffer, 0)
            if (size < 0) continue
            info.set(0, size, newTime, MediaCodec.BUFFER_FLAG_KEY_FRAME)
            muxer.writeSampleData(videoOut, buffer, info)
        }
        while (sound != null && audioIndex < sound.chunks.size) writeChunk(muxer, audioOut, sound.chunks[audioIndex++])
    }

    private fun writeChunk(muxer: MediaMuxer, track: Int, chunk: SoundChunk) {
        val info = MediaCodec.BufferInfo().apply { set(0, chunk.bytes.size, chunk.timeUs, 0) }
        muxer.writeSampleData(track, ByteBuffer.wrap(chunk.bytes), info)
    }

    private companion object {
        const val MICROS = 1_000L
        const val PERCENT = 100
        const val INTRA_SHARE = 60
        const val SOUND_SHARE = 20
        const val INTRA_BITRATE = 40_000_000
        const val POLL_MS = 200L
        const val MAX_SAMPLE = 8 * 1024 * 1024
    }
}

/** An encoded chunk of reversed sound and its time from the start of the file. */
class SoundChunk(val bytes: ByteArray, val timeUs: Long)

/** The AAC format and chunks of a reversed sound. */
class ReversedSound(val format: MediaFormat, val chunks: List<SoundChunk>)

/** Decoded 16-bit sound of a range: [samples] holds [channels] interleaved values per frame. */
class Pcm(val samples: ShortArray, val sampleRate: Int, val channels: Int) {
    /** The same sound played backwards, frame by frame so channels stay in place. */
    fun reversed(): Pcm {
        val frames = samples.size / channels
        val out = ShortArray(samples.size)
        for (f in 0 until frames) {
            System.arraycopy(samples, f * channels, out, (frames - 1 - f) * channels, channels)
        }
        return Pcm(out, sampleRate, channels)
    }
}

/** Decoding and encoding of sound for [ReverseRenderer]. */
internal object SoundCodec {
    private const val TIMEOUT_US = 10_000L
    private const val AAC_BITRATE = 128_000

    fun decode(context: Context, uri: Uri, startUs: Long, endUs: Long): Pcm? {
        val extractor = MediaExtractor()
        return try {
            runCatching {
                extractor.setDataSource(context, uri, null)
                val track = (0 until extractor.trackCount).firstOrNull {
                    extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
                }
                track?.let {
                    val format = extractor.getTrackFormat(it)
                    extractor.selectTrack(it)
                    extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                    decodeRange(extractor, format, startUs, endUs)
                }
            }.getOrNull()
        } finally {
            extractor.release()
        }
    }

    private fun decodeRange(extractor: MediaExtractor, format: MediaFormat, startUs: Long, endUs: Long): Pcm? {
        val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
        codec.configure(format, null, null, 0)
        codec.start()
        var rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val out = ShortList()
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        try {
            while (!outputDone) {
                if (!inputDone) inputDone = feed(extractor, codec, endUs)
                val index = codec.dequeueOutputBuffer(info, TIMEOUT_US)
                if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    rate = codec.outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    channels = codec.outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                } else if (index >= 0) {
                    collect(codec.getOutputBuffer(index)!!, info, startUs, endUs, rate, channels, out)
                    codec.releaseOutputBuffer(index, false)
                    outputDone = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                }
            }
        } finally {
            codec.stop()
            codec.release()
        }
        return out.toArray().takeIf { it.isNotEmpty() }?.let { Pcm(it, rate, channels) }
    }

    /** Queues one input sample; true once the end of the range or the file was queued. */
    private fun feed(extractor: MediaExtractor, codec: MediaCodec, endUs: Long): Boolean {
        val index = codec.dequeueInputBuffer(TIMEOUT_US)
        if (index < 0) return false
        val buffer = codec.getInputBuffer(index)!!
        val size = extractor.readSampleData(buffer, 0)
        if (size < 0 || extractor.sampleTime > endUs) {
            codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            return true
        }
        codec.queueInputBuffer(index, 0, size, extractor.sampleTime, 0)
        extractor.advance()
        return false
    }

    private fun collect(
        buffer: ByteBuffer,
        info: MediaCodec.BufferInfo,
        startUs: Long,
        endUs: Long,
        rate: Int,
        channels: Int,
        out: ShortList,
    ) {
        val shorts = buffer.order(ByteOrder.nativeOrder()).asShortBuffer()
        var frame = 0L
        while (shorts.remaining() >= channels) {
            val timeUs = info.presentationTimeUs + frame * MICROS_PER_SECOND / rate
            val keep = timeUs in startUs until endUs
            repeat(channels) { shorts.get().also { sample -> if (keep) out.add(sample) } }
            frame++
        }
    }

    fun encode(pcm: Pcm): ReversedSound? {
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, pcm.sampleRate, pcm.channels).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, AAC_BITRATE)
        }
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()
        val state = EncodeState()
        val info = MediaCodec.BufferInfo()
        try {
            while (!state.done) pump(codec, pcm, state, info)
        } finally {
            codec.stop()
            codec.release()
        }
        return state.format?.let { ReversedSound(it, state.chunks) }
    }

    /** What an encode has so far: how much sound went in, the chunks that came out, and whether it is finished. */
    private class EncodeState {
        var fed = 0
        var format: MediaFormat? = null
        var done = false
        val chunks = ArrayList<SoundChunk>()
    }

    private fun pump(codec: MediaCodec, pcm: Pcm, state: EncodeState, info: MediaCodec.BufferInfo) {
        if (state.fed < pcm.samples.size) state.fed = feedPcm(codec, pcm, state.fed)
        val out = codec.dequeueOutputBuffer(info, TIMEOUT_US)
        if (out == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) state.format = codec.outputFormat
        if (out >= 0) {
            takeChunk(codec, out, info)?.let { state.chunks += it }
            state.done = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
        }
    }

    /** Queues the next piece of [pcm] from [fed] on; returns how many samples have been queued. */
    private fun feedPcm(codec: MediaCodec, pcm: Pcm, fed: Int): Int {
        val index = codec.dequeueInputBuffer(TIMEOUT_US)
        if (index < 0) return fed
        val buffer = codec.getInputBuffer(index)!!.order(ByteOrder.nativeOrder())
        buffer.clear()
        val count = minOf(buffer.capacity() / 2, pcm.samples.size - fed)
        for (i in 0 until count) buffer.putShort(pcm.samples[fed + i])
        val last = fed + count >= pcm.samples.size
        codec.queueInputBuffer(
            index, 0, count * 2, fed / pcm.channels * MICROS_PER_SECOND / pcm.sampleRate,
            if (last) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0,
        )
        return fed + count
    }

    /** Output buffer [index] as a chunk and released, or null for codec config and empty buffers. */
    private fun takeChunk(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo): SoundChunk? {
        var chunk: SoundChunk? = null
        if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0 && info.size > 0) {
            val bytes = ByteArray(info.size)
            val data = codec.getOutputBuffer(index)!!
            data.position(info.offset)
            data.get(bytes)
            chunk = SoundChunk(bytes, info.presentationTimeUs)
        }
        codec.releaseOutputBuffer(index, false)
        return chunk
    }

    private const val MICROS_PER_SECOND = 1_000_000L
}

/** A growing list of shorts without boxing. */
private class ShortList {
    private var data = ShortArray(INITIAL)
    private var size = 0

    fun add(value: Short) {
        if (size == data.size) data = data.copyOf(size * 2)
        data[size++] = value
    }

    fun toArray(): ShortArray = data.copyOf(size)

    private companion object {
        const val INITIAL = 1 shl 16
    }
}
