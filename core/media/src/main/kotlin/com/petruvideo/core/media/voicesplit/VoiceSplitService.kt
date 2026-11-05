package com.petruvideo.core.media.voicesplit

import android.content.Context
import android.net.Uri
import com.petruvideo.core.media.probe.MediaProber
import com.petruvideo.core.media.reverse.Pcm
import com.petruvideo.core.media.reverse.SoundCodec
import com.petruvideo.core.model.MediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import com.petruvideo.core.media.R

/** The two audio files of a split as media items: the voice and everything else. */
class SplitStems(val voice: MediaItem, val backing: MediaItem)

/** Separates the voice from the rest of a clip's sound (feature 027). */
fun interface VoiceSplitter {
    /**
     * Splits [startMs] to [endMs] of [media] into a voice and everything else, reporting the share done (0 to 100) to
     * [onProgress]; null when the sound cannot be read or the split fails. Cancelling the calling coroutine stops it
     * and leaves no files behind.
     */
    suspend fun split(media: MediaItem, startMs: Long, endMs: Long, onProgress: (Int) -> Unit): SplitStems?
}

/** Used where no separation is available. */
object NoVoiceSplitter : VoiceSplitter {
    override suspend fun split(media: MediaItem, startMs: Long, endMs: Long, onProgress: (Int) -> Unit): SplitStems? =
        null
}

/**
 * Runs the bundled UMX-HQ vocals model over the clip in overlapping blocks (so the model always has context and the
 * seams are cross-faded), keeps the voice as estimated, and makes the rest by taking the voice away from the original.
 */
@Singleton
class VoiceSplitService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: VoiceSplitStore,
    private val probe: MediaProber,
) : VoiceSplitter {
    private val model by lazy { UmxVocalsModel(context) }
    private val stft = Stft()

    override suspend fun split(media: MediaItem, startMs: Long, endMs: Long, onProgress: (Int) -> Unit): SplitStems? {
        val files = splitFiles(media, startMs, endMs, onProgress) ?: return null
        val voice = probe.probe(Uri.fromFile(files.voice), files.voice.name).getOrNull() ?: return null
        val backing = probe.probe(Uri.fromFile(files.backing), files.backing.name).getOrNull() ?: return null
        return SplitStems(
            voice.copy(displayName = context.getString(R.string.media_voice_stem, media.displayName)),
            backing.copy(displayName = context.getString(R.string.media_backing_stem, media.displayName)),
        )
    }

    internal suspend fun splitFiles(
        media: MediaItem,
        startMs: Long,
        endMs: Long,
        onProgress: (Int) -> Unit,
    ): SplitFiles? {
        val files = store.filesFor(media, startMs, endMs)
        if (files.exists) {
            onProgress(DONE)
            return files
        }
        return withContext(Dispatchers.Default) {
            val temporaries = SplitFiles(store.temporary(files.voice), store.temporary(files.backing))
            try {
                runCatching {
                    val mix = decode(media, startMs, endMs) ?: return@runCatching null
                    onProgress(DECODED)
                    write(mix, temporaries, onProgress)
                    check(temporaries.voice.renameTo(files.voice) && temporaries.backing.renameTo(files.backing))
                    onProgress(DONE)
                    files
                }.onFailure { if (it is CancellationException) throw it }.getOrNull()
            } finally {
                temporaries.voice.delete()
                temporaries.backing.delete()
            }
        }
    }

    private suspend fun decode(media: MediaItem, startMs: Long, endMs: Long): ShortArray? =
        withContext(Dispatchers.IO) {
            SoundCodec.decode(context, Uri.parse(media.uri), startMs * MICROS, endMs * MICROS)?.let(::toStereo44k)
        }

    private suspend fun write(mix: ShortArray, into: SplitFiles, onProgress: (Int) -> Unit) {
        WavWriter(into.voice, RATE, 2).use { voice ->
            WavWriter(into.backing, RATE, 2).use { backing -> writeBlocks(mix, voice, backing, onProgress) }
        }
    }

    private suspend fun writeBlocks(mix: ShortArray, voice: WavWriter, backing: WavWriter, onProgress: (Int) -> Unit) {
        val total = mix.size / 2
        val blocks = if (total <= BLOCK) 1 else ceil((total - BLOCK).toDouble() / HOP_BLOCK).toInt() + 1
        var carry: Array<FloatArray>? = null
        var start = 0
        for (block in 0 until blocks) {
            currentCoroutineContext().ensureActive()
            val length = minOf(BLOCK, total - start)
            val last = block == blocks - 1
            val vocals = separate(mix, start, length)
            val emitted = if (last) length else length - OVERLAP
            val out = blend(vocals, emitted, carry.takeIf { block > 0 })
            carry = if (last) null else tail(vocals, length)
            emit(mix, start, out, emitted, voice, backing)
            start += HOP_BLOCK
            onProgress(DECODED + ((block + 1) * (DONE - DECODED - 1)) / blocks)
        }
    }

    private fun emit(
        mix: ShortArray,
        start: Int,
        vocals: Array<FloatArray>,
        count: Int,
        voice: WavWriter,
        backing: WavWriter,
    ) {
        val voiceOut = ShortArray(count * 2)
        val backingOut = ShortArray(count * 2)
        for (i in 0 until count) {
            for (ch in 0..1) {
                val original = mix[(start + i) * 2 + ch].toInt()
                val v = (vocals[ch][i] * SHORT_MAX).roundToInt().coerceIn(SHORT_MIN, SHORT_MAX.toInt())
                voiceOut[i * 2 + ch] = v.toShort()
                backingOut[i * 2 + ch] = (original - v).coerceIn(SHORT_MIN, SHORT_MAX.toInt()).toShort()
            }
        }
        voice.write(voiceOut)
        backing.write(backingOut)
    }

    /** The first [count] samples of a block, with the cross-fade from the previous block's [carry] added in. */
    private fun blend(vocals: Array<FloatArray>, count: Int, carry: Array<FloatArray>?): Array<FloatArray> =
        Array(2) { ch ->
            FloatArray(count) { i ->
                if (carry != null && i < OVERLAP) vocals[ch][i] * (i.toFloat() / OVERLAP) + carry[ch][i] else vocals[ch][i]
            }
        }

    /** The last [OVERLAP] samples of a block faded out, to be blended into the start of the next block. */
    private fun tail(vocals: Array<FloatArray>, length: Int): Array<FloatArray> = Array(2) { ch ->
        FloatArray(OVERLAP) { i -> vocals[ch][length - OVERLAP + i] * (1f - i.toFloat() / OVERLAP) }
    }

    /** The estimated voice of `length` samples of [mix] from [start], one array per channel. */
    private fun separate(mix: ShortArray, start: Int, length: Int): Array<FloatArray> {
        val spectra = Array(2) { ch ->
            stft.forward(FloatArray(length) { mix[(start + it) * 2 + ch] / SHORT_MAX })
        }
        val frames = spectra[0].frames
        val bins = stft.bins
        val input = FloatArray(2 * bins * frames)
        for (ch in 0..1) for (t in 0 until frames) for (k in 0 until bins) {
            input[(ch * bins + k) * frames + t] = hypot(spectra[ch].re[t][k], spectra[ch].im[t][k])
        }
        val estimate = model.estimate(input, bins, frames)
        return Array(2) { ch ->
            val voice = Spectrum(frames, Array(frames) { FloatArray(bins) }, Array(frames) { FloatArray(bins) })
            for (t in 0 until frames) for (k in 0 until bins) {
                val magnitude = input[(ch * bins + k) * frames + t]
                val gain = if (magnitude > MIN_MAGNITUDE) estimate[(ch * bins + k) * frames + t] / magnitude else 0f
                voice.re[t][k] = spectra[ch].re[t][k] * gain
                voice.im[t][k] = spectra[ch].im[t][k] * gain
            }
            stft.inverse(voice, length)
        }
    }

    private companion object {
        const val RATE = 44_100
        const val MICROS = 1_000L
        const val BLOCK = 16 * RATE
        const val OVERLAP = 4 * RATE
        const val HOP_BLOCK = BLOCK - OVERLAP
        const val DECODED = 5
        const val DONE = 100
        const val SHORT_MAX = 32767f
        const val SHORT_MIN = -32768
        const val MIN_MAGNITUDE = 1e-9f
    }
}

/** Stereo 16-bit samples at 44.1 kHz from any channel count and rate (mono is copied to both sides). */
internal fun toStereo44k(pcm: Pcm): ShortArray {
    val inFrames = pcm.samples.size / pcm.channels
    val outFrames = (inFrames.toLong() * 44_100 / pcm.sampleRate).toInt()
    val out = ShortArray(outFrames * 2)
    val step = pcm.sampleRate.toDouble() / 44_100
    for (i in 0 until outFrames) {
        val position = i * step
        val a = position.toInt().coerceAtMost(inFrames - 1)
        val b = (a + 1).coerceAtMost(inFrames - 1)
        val fraction = (position - a).toFloat()
        for (ch in 0..1) {
            val source = if (pcm.channels == 1) 0 else ch
            val x = pcm.samples[a * pcm.channels + source]
            val y = pcm.samples[b * pcm.channels + source]
            out[i * 2 + ch] = (x + (y - x) * fraction).roundToInt().toShort()
        }
    }
    return out
}
