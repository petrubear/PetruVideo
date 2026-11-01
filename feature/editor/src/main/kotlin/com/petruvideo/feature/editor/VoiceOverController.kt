package com.petruvideo.feature.editor

import com.petruvideo.core.media.voiceover.RecorderState
import com.petruvideo.core.media.voiceover.VoiceOverStore
import com.petruvideo.core.media.voiceover.VoiceRecorder
import com.petruvideo.core.model.VOICE_OVER_MIN_MS
import com.petruvideo.core.model.VoiceOverStop
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** What the recording bar shows: the count-in digit, or the running take with its timer and level. */
sealed interface VoiceOverUi {
    data class CountIn(val secondsLeft: Int) : VoiceOverUi

    data class Recording(val elapsedMs: Long, val level: Float) : VoiceOverUi
}

/** How a take ended. */
sealed interface VoiceOverResult {
    data class Take(val file: File, val durationMs: Long, val startMs: Long, val reason: VoiceOverStop) :
        VoiceOverResult

    data object TooShort : VoiceOverResult

    data object MicBusy : VoiceOverResult
}

/**
 * The voice-over flow (spec User Story 1): a three second count-in, then the recorder and the preview start together,
 * and the take ends on Stop, on its limit, or when the system interrupts it. Every end keeps what was recorded.
 */
class VoiceOverController @Inject constructor(
    private val recorder: VoiceRecorder,
    val files: VoiceOverStore,
) {
    private val _ui = MutableStateFlow<VoiceOverUi?>(null)
    val ui: StateFlow<VoiceOverUi?> = _ui.asStateFlow()

    private var job: Job? = null
    private var take: Take? = null

    /** True from the count-in until the take has ended. */
    val active: Boolean get() = _ui.value != null

    private class Take(
        val file: File,
        val startMs: Long,
        val maxMs: Long,
        val limit: VoiceOverStop,
        val onDone: (VoiceOverResult) -> Unit,
    )

    /**
     * Counts in, then records from [startMs] for at most [maxMs] (which ends with [limit]). [onRecording] runs once the
     * microphone is open, so playback starts right after it (research 3); [onDone] gets the outcome.
     */
    fun begin(
        scope: CoroutineScope,
        projectId: String,
        startMs: Long,
        maxMs: Long,
        limit: VoiceOverStop,
        onRecording: () -> Unit,
        onDone: (VoiceOverResult) -> Unit,
    ) {
        if (active) return
        _ui.value = VoiceOverUi.CountIn(COUNT_IN)
        job = scope.launch {
            for (second in COUNT_IN downTo 1) {
                _ui.value = VoiceOverUi.CountIn(second)
                delay(SECOND_MS)
            }
            val file = files.newFile()
            files.openTake(projectId, startMs, file)
            recorder.onInterrupted = { ms -> end(ms, VoiceOverStop.Interrupted) }
            if (!recorder.start(file)) {
                files.closeTake()
                file.delete()
                _ui.value = null
                onDone(VoiceOverResult.MicBusy)
                return@launch
            }
            take = Take(file, startMs, maxMs, limit, onDone)
            _ui.value = VoiceOverUi.Recording(0, 0f)
            onRecording()
            watch()
        }
    }

    private suspend fun watch() {
        recorder.state.collect { state ->
            val current = take
            if (state is RecorderState.Recording && current != null) {
                _ui.value = VoiceOverUi.Recording(state.elapsedMs, state.level)
                if (state.elapsedMs >= current.maxMs) end(recorder.stop(), current.limit)
            }
        }
    }

    /** Ends the take the user stopped (or the screen left, with [reason] Interrupted); keeps what was recorded. */
    fun stop(reason: VoiceOverStop = VoiceOverStop.User) {
        if (take != null) end(recorder.stop(), reason)
    }

    /** Cancels a count-in that has not started recording; a running take is ended with [stop] instead. */
    fun cancel() {
        if (take != null) return
        job?.cancel()
        job = null
        _ui.value = null
    }

    private fun end(measuredMs: Long, reason: VoiceOverStop) {
        val current = take ?: return
        take = null
        recorder.onInterrupted = {}
        job?.cancel()
        job = null
        files.closeTake()
        _ui.value = null
        val length = minOf(measuredMs, current.maxMs)
        if (length < VOICE_OVER_MIN_MS) {
            current.file.delete()
            current.onDone(VoiceOverResult.TooShort)
        } else {
            current.onDone(VoiceOverResult.Take(current.file, length, current.startMs, reason))
        }
    }

    private companion object {
        const val COUNT_IN = 3
        const val SECOND_MS = 1_000L
    }
}
