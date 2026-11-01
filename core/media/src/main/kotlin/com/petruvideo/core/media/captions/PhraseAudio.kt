package com.petruvideo.core.media.captions

import android.content.Context
import android.net.Uri
import com.petruvideo.core.media.reverse.SoundCodec
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Gives the sound of one phrase of a media file as 16 kHz mono 16-bit PCM, the form the recognizer wants. */
fun interface PhraseAudio {
    suspend fun pcm(uri: String, startMs: Long, endMs: Long): ShortArray?
}

/** Decodes the range with the sound decoder, mixes it to mono and resamples it to 16 kHz. */
class DecodedPhraseAudio @Inject constructor(
    @ApplicationContext private val context: Context,
) : PhraseAudio {
    override suspend fun pcm(uri: String, startMs: Long, endMs: Long): ShortArray? = withContext(Dispatchers.Default) {
        SoundCodec.decode(context, Uri.parse(uri), startMs * MICROS_PER_MS, endMs * MICROS_PER_MS)?.let {
            resample(mono(it.samples, it.channels), it.sampleRate)
        }
    }

    internal companion object {
        const val TARGET_RATE = 16_000
        private const val MICROS_PER_MS = 1_000L

        fun mono(samples: ShortArray, channels: Int): ShortArray {
            if (channels <= 1) return samples
            return ShortArray(samples.size / channels) { frame ->
                var sum = 0
                for (c in 0 until channels) sum += samples[frame * channels + c]
                (sum / channels).toShort()
            }
        }

        /** Linear resampling of mono [samples] from [rate] to 16 kHz. */
        fun resample(samples: ShortArray, rate: Int): ShortArray {
            if (rate == TARGET_RATE || samples.isEmpty()) return samples
            val out = ShortArray((samples.size.toLong() * TARGET_RATE / rate).toInt())
            val step = rate.toDouble() / TARGET_RATE
            for (i in out.indices) {
                val position = i * step
                val index = position.toInt().coerceAtMost(samples.lastIndex)
                val next = (index + 1).coerceAtMost(samples.lastIndex)
                val fraction = position - index
                out[i] = (samples[index] + (samples[next] - samples[index]) * fraction).toInt().toShort()
            }
            return out
        }
    }
}
