package com.petruvideo.core.media.captions

import com.petruvideo.core.model.Fingerprint
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VideoClip
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CaptionServiceTest {
    private val media = MediaItem(
        "m", MediaKind.Video, "content://m", "m.mp4", 30_000, 1280, 720, 30f, true,
        Fingerprint(1, 1, 30_000, "video/mp4"),
    )
    private val timeline = Timeline(
        "p", media = mapOf("m" to media), videoLane = listOf(VideoClip("a", "m", 0, 10_000, true)),
    )
    private val loud = FloatArray(600) { i -> if ((i * 50) in 1_000..2_500 || (i * 50) in 5_000..7_000) 0.5f else 0f }

    private class FakeTranscriber(val answers: MutableList<String?>) : SpeechTranscriber {
        var calls = 0
        override suspend fun availability(languageTag: String) = RecognitionAvailability.Available
        override suspend fun installedLanguages() = listOf("en-US")
        override suspend fun transcribe(pcm: ShortArray, languageTag: String): String? = answers.getOrNull(calls++)
    }

    private fun service(t: SpeechTranscriber, audio: PhraseAudio = PhraseAudio { _, _, _ -> ShortArray(160) }) =
        CaptionService({ loud }, audio, t)

    @Test
    fun `each phrase becomes a line at its time and unrecognised phrases are counted`() = runTest {
        val progress = mutableListOf<Int>()
        val transcriber = FakeTranscriber(mutableListOf("Hello there", null))
        val result = service(transcriber).make(timeline, "en-US") { progress += it }
        result.lines.size shouldBe 1
        result.lines.single().text shouldBe "Hello there"
        (result.lines.single().startMs in 950..1_050) shouldBe true
        result.skippedPhrases shouldBe 1
        progress shouldBe listOf(50, 100)
    }

    @Test
    fun `a phrase without audio is skipped`() = runTest {
        val silent = PhraseAudio { _, _, _ -> null }
        val result = service(FakeTranscriber(mutableListOf("a", "b")), silent).make(timeline, "en-US") { }
        result.lines.size shouldBe 0
        result.skippedPhrases shouldBe 2
    }

    @Test
    fun `cancelling stops the run before the next phrase`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val transcriber = object : SpeechTranscriber {
            var calls = 0
            override suspend fun availability(languageTag: String) = RecognitionAvailability.Available
            override suspend fun installedLanguages() = emptyList<String>()
            override suspend fun transcribe(pcm: ShortArray, languageTag: String): String? {
                calls++
                gate.await()
                return "x"
            }
        }
        val job = launch { service(transcriber).make(timeline, "en-US") { } }
        testScheduler.advanceUntilIdle()
        job.cancel(CancellationException("test"))
        job.join()
        transcriber.calls shouldBe 1
    }
}
