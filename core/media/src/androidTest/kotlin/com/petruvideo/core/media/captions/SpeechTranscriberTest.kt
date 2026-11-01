package com.petruvideo.core.media.captions

import androidx.test.platform.app.InstrumentationRegistry
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.Test

class SpeechTranscriberTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /** Whatever the phone has, the answer is one of the known states, and "available" means a language is installed. */
    @Test
    fun availabilityIsAKnownAnswerAndAvailableMeansALanguageIsInstalled() = runBlocking<Unit> {
        val transcriber = AndroidSpeechTranscriber(context)
        val installed = transcriber.installedLanguages()
        when (val answer = transcriber.availability("en-US")) {
            RecognitionAvailability.Available -> installed.isNotEmpty() shouldBe true
            is RecognitionAvailability.LanguageMissing -> answer.wanted shouldBe "en-US"
            RecognitionAvailability.NeedsNewerAndroid, RecognitionAvailability.NotOnThisPhone ->
                installed.isEmpty() shouldBe true
        }
    }
}
