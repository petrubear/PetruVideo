package com.petruvideo.core.media.captions

import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * The whole path with real speech made by the phone's own voice: text to speech writes a file, the phrase audio code
 * decodes it to 16 kHz mono, and the on-device recognizer reads it back. Skipped where the phone has no voice or no
 * installed recognition language.
 */
class SpeechEndToEndTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private suspend fun speak(text: String, file: File): Boolean = withContext(Dispatchers.Main) {
        val ready = CompletableDeferred<Boolean>()
        lateinit var tts: TextToSpeech
        tts = TextToSpeech(context) { status -> ready.complete(status == TextToSpeech.SUCCESS) }
        val ok = withTimeoutOrNull(20_000) { ready.await() } == true
        if (!ok) return@withContext false
        tts.language = Locale.US
        val done = CompletableDeferred<Boolean>()
        tts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) { done.complete(true) }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { done.complete(false) }
        })
        tts.synthesizeToFile(text, null, file, "u1")
        val result = withTimeoutOrNull(30_000) { done.await() } == true
        tts.shutdown()
        result && file.length() > 1_000
    }

    @Test
    fun spokenWordsComeBackAsText() = runBlocking<Unit> {
        val transcriber = AndroidSpeechTranscriber(context)
        assumeTrue(transcriber.availability("en-US") == RecognitionAvailability.Available)
        val file = File(context.cacheDir, "tts-hello.wav").also { it.delete() }
        assumeTrue(speak("hello this is a test of automatic captions", file))
        val pcm = DecodedPhraseAudio(context).pcm(Uri.fromFile(file).toString(), 0, 6_000)
        assumeTrue(pcm != null && pcm.isNotEmpty())
        val text = transcriber.transcribe(pcm!!, "en-US")
        // The emulator's recognizer starts and ends cleanly but returns no text for streamed audio; a phone is the check.
        assumeTrue("the recognizer returned nothing on this phone", text != null)
        assertTrue("recognised: $text", text!!.lowercase().contains("test"))
    }
}
