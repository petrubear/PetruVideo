package com.petruvideo.core.media.captions

import com.petruvideo.core.media.waveform.WaveformPeaksGenerator
import com.petruvideo.core.model.CaptionLine
import com.petruvideo.core.model.CaptionPlanner
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.Timeline
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** What a run of Auto captions found; [lines] are ready for the `AddCaptions` command. */
class CaptionResult(
    val lines: List<CaptionLine>,
    val skippedPhrases: Int,
    val skippedClips: Int,
    val truncated: Boolean,
)

/** What the editor needs from Auto captions. */
interface CaptionMaker {
    suspend fun availability(languageTag: String): RecognitionAvailability

    suspend fun languages(): List<String>

    /** Recognises the speech of the project's main track; progress is 0 to 100; cancel by cancelling the caller. */
    suspend fun make(timeline: Timeline, languageTag: String, onProgress: (Int) -> Unit): CaptionResult
}

/** The caption maker used where captions are not wired in (tests): unavailable. */
object NoCaptionMaker : CaptionMaker {
    override suspend fun availability(languageTag: String): RecognitionAvailability =
        RecognitionAvailability.NotOnThisPhone

    override suspend fun languages(): List<String> = emptyList()

    override suspend fun make(timeline: Timeline, languageTag: String, onProgress: (Int) -> Unit) =
        CaptionResult(emptyList(), 0, 0, false)
}

/** Gives the loudness of a media item's sound (waveform peaks). */
fun interface PeaksSource {
    suspend fun peaks(item: MediaItem): FloatArray?
}

/** Finds phrases with [CaptionPlanner], cuts their sound, and recognises them one at a time on the phone. */
@Singleton
class CaptionService(
    private val peaks: PeaksSource,
    private val audio: PhraseAudio,
    private val transcriber: SpeechTranscriber,
) : CaptionMaker {
    @Inject
    constructor(
        generator: WaveformPeaksGenerator,
        audio: DecodedPhraseAudio,
        transcriber: AndroidSpeechTranscriber,
    ) : this({ generator.peaks(it) }, audio, transcriber)

    override suspend fun availability(languageTag: String) = transcriber.availability(languageTag)

    override suspend fun languages() = transcriber.installedLanguages()

    override suspend fun make(timeline: Timeline, languageTag: String, onProgress: (Int) -> Unit): CaptionResult {
        val loudness = HashMap<String, FloatArray?>()
        for (clip in timeline.videoLane) {
            val item = timeline.media[clip.mediaItemId] ?: continue
            if (clip.mediaItemId !in loudness) loudness[clip.mediaItemId] = peaks.peaks(item)
        }
        val plan = CaptionPlanner.plan(timeline) { loudness[it] }
        val lines = ArrayList<CaptionLine>()
        var skipped = 0
        for ((index, phrase) in plan.phrases.withIndex()) {
            currentCoroutineContext().ensureActive()
            val clip = timeline.videoLane.first { it.id == phrase.clipId }
            val uri = timeline.media.getValue(clip.mediaItemId).uri
            val sound = audio.pcm(uri, phrase.sourceStartMs, phrase.sourceEndMs)
            val text = sound?.let { transcriber.transcribe(it, languageTag) }
            if (text.isNullOrBlank()) skipped++ else lines += CaptionLine(phrase.startMs, phrase.endMs, text)
            onProgress((index + 1) * PERCENT / plan.phrases.size)
        }
        return CaptionResult(lines, skipped, plan.skippedClips, plan.truncated)
    }

    private companion object {
        const val PERCENT = 100
    }
}
