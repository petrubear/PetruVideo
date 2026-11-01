package com.petruvideo.core.media.captions

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.speech.RecognitionListener
import android.speech.RecognitionSupport
import android.speech.RecognitionSupportCallback
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/** Whether Auto captions can recognise speech on this phone in a language. */
sealed interface RecognitionAvailability {
    data object Available : RecognitionAvailability

    /** The phone runs an Android version older than 13, the first with on-device recognition. */
    data object NeedsNewerAndroid : RecognitionAvailability

    /** The phone has no on-device recognizer at all. */
    data object NotOnThisPhone : RecognitionAvailability

    /** The recognizer is there but [wanted] is not installed; [installed] lists the languages that are. */
    data class LanguageMissing(val wanted: String, val installed: List<String>) : RecognitionAvailability
}

/** Turns a piece of speech into text, only on the phone (feature 019). */
interface SpeechTranscriber {
    /** Whether speech in [languageTag] can be recognised on the phone now. */
    suspend fun availability(languageTag: String): RecognitionAvailability

    /** The languages installed for on-device recognition (language tags), empty when none or unsupported. */
    suspend fun installedLanguages(): List<String>

    /** The words of [pcm] (16-bit, 16 kHz, mono) in [languageTag], or null when nothing could be recognised. */
    suspend fun transcribe(pcm: ShortArray, languageTag: String): String?
}

/** Android's on-device recognizer, fed from a stream so no microphone is used and nothing goes to the network. */
class AndroidSpeechTranscriber @Inject constructor(
    @ApplicationContext private val context: Context,
) : SpeechTranscriber {
    override suspend fun availability(languageTag: String): RecognitionAvailability {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return RecognitionAvailability.NeedsNewerAndroid
        val supported = withContext(Dispatchers.Main) { SpeechRecognizer.isOnDeviceRecognitionAvailable(context) }
        if (!supported) return RecognitionAvailability.NotOnThisPhone
        val installed = installedLanguages()
        val wanted = Locale.forLanguageTag(languageTag).language
        val present = installed.any { Locale.forLanguageTag(it).language == wanted }
        return if (present) {
            RecognitionAvailability.Available
        } else {
            RecognitionAvailability.LanguageMissing(languageTag, installed)
        }
    }

    override suspend fun installedLanguages(): List<String> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return emptyList()
        return withContext(Dispatchers.Main) { installedOnMain() }
    }

    override suspend fun transcribe(pcm: ShortArray, languageTag: String): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        return withContext(Dispatchers.Main) { recognise(pcm, languageTag) }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun installedOnMain(): List<String> = suspendCancellableCoroutine { continuation ->
        val recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        continuation.invokeOnCancellation { recognizer.destroy() }
        recognizer.checkRecognitionSupport(
            intent, context.mainExecutor,
            object : RecognitionSupportCallback {
                override fun onSupportResult(support: RecognitionSupport) {
                    recognizer.destroy()
                    if (continuation.isActive) continuation.resume(support.installedOnDeviceLanguages)
                }

                override fun onError(error: Int) {
                    recognizer.destroy()
                    if (continuation.isActive) continuation.resume(emptyList())
                }
            },
        )
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun recognise(pcm: ShortArray, languageTag: String): String? = suspendCancellableCoroutine { c ->
        val recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        val pipe = ParcelFileDescriptor.createPipe()
        c.invokeOnCancellation {
            recognizer.destroy()
            pipe.forEach { it.close() }
        }
        recognizer.setRecognitionListener(listener(recognizer) { text -> if (c.isActive) c.resume(text) })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, pipe[0])
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, 1)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, android.media.AudioFormat.ENCODING_PCM_16BIT)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, SAMPLE_RATE)
        }
        recognizer.startListening(intent)
        Thread { paced(pipe[1], pcm + ShortArray(TRAILING_SILENCE)) }.start()
    }

    /** Writes the audio into the pipe in 100 ms pieces at about twice real time, like a microphone would. */
    private fun paced(write: ParcelFileDescriptor, pcm: ShortArray) {
        ParcelFileDescriptor.AutoCloseOutputStream(write).use { out ->
            val piece = SAMPLE_RATE / 10
            var at = 0
            while (at < pcm.size) {
                val end = minOf(at + piece, pcm.size)
                val ok = runCatching { out.write(littleEndian(pcm.copyOfRange(at, end))) }.isSuccess
                if (!ok) return
                Thread.sleep(PACE_MS)
                at = end
            }
        }
    }

    private fun littleEndian(pcm: ShortArray): ByteArray {
        val buffer = ByteBuffer.allocate(pcm.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        pcm.forEach { buffer.putShort(it) }
        return buffer.array()
    }

    private fun listener(recognizer: SpeechRecognizer, done: (String?) -> Unit) = object : RecognitionListener {
        override fun onResults(results: Bundle?) {
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
            recognizer.destroy()
            done(text?.takeIf { it.isNotBlank() })
        }

        override fun onError(error: Int) {
            android.util.Log.w("PetruCaptions", "recognizer error $error")
            recognizer.destroy()
            done(null)
        }

        override fun onReadyForSpeech(params: Bundle?) = Unit

        override fun onBeginningOfSpeech() = Unit

        override fun onRmsChanged(rmsdB: Float) = Unit

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() = Unit

        override fun onPartialResults(partialResults: Bundle?) = Unit

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private companion object {
        const val SAMPLE_RATE = 16_000

        /** Delay after each 100 ms piece: twice real time, as a compromise between speed and a live-like stream. */
        const val PACE_MS = 50L

        /** A second of silence after the phrase, so the recognizer sees where the speech ends. */
        const val TRAILING_SILENCE = SAMPLE_RATE
    }
}
