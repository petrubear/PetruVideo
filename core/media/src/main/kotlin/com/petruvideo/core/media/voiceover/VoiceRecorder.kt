package com.petruvideo.core.media.voiceover

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface RecorderState {
    data object Idle : RecorderState

    /** [level] is 0 to 1, shaped so quiet speech still moves a meter. */
    data class Recording(val elapsedMs: Long, val level: Float) : RecorderState
}

/** Records the microphone to a file; one recording at a time, owned outside the screen so it survives rotation. */
interface VoiceRecorder {
    val state: StateFlow<RecorderState>

    /** Called with the time recorded so far when the system ends the recording (microphone lost, error). */
    var onInterrupted: (elapsedMs: Long) -> Unit

    /** Opens the microphone and starts writing [file]; false when it cannot be opened (for example it is in use). */
    fun start(file: File): Boolean

    /** Stops and returns the recorded time in milliseconds, or 0 when nothing was recording. */
    fun stop(): Long
}

/**
 * [MediaRecorder] writing AAC in an ADTS stream: the stream has no trailer, so a take cut short by a crash still
 * plays up to the cut (research 1). The level is the peak since the last poll.
 */
@Singleton
class MediaVoiceRecorder @Inject constructor(@ApplicationContext private val context: Context) : VoiceRecorder {
    private val handler = Handler(Looper.getMainLooper())
    private val _state = MutableStateFlow<RecorderState>(RecorderState.Idle)
    override val state: StateFlow<RecorderState> = _state.asStateFlow()
    override var onInterrupted: (Long) -> Unit = {}

    private var recorder: MediaRecorder? = null
    private var startedAt = 0L

    private val poll = object : Runnable {
        override fun run() {
            val active = recorder ?: return
            val peak = runCatching { active.maxAmplitude }.getOrDefault(0)
            _state.value = RecorderState.Recording(elapsed(), sqrt(peak / PEAK_MAX).coerceIn(0f, 1f))
            handler.postDelayed(this, POLL_MS)
        }
    }

    private fun elapsed() = SystemClock.elapsedRealtime() - startedAt

    override fun start(file: File): Boolean {
        if (recorder != null) return false
        file.parentFile?.mkdirs()
        val next = newRecorder()
        val opened = runCatching {
            next.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(SAMPLE_RATE)
                setAudioChannels(1)
                setAudioEncodingBitRate(BITRATE)
                setOutputFile(file.absolutePath)
                setOnErrorListener { _, _, _ -> interrupted() }
                setOnInfoListener { _, _, _ -> interrupted() }
                prepare()
                start()
            }
        }.isSuccess
        if (!opened) {
            // The microphone is in use, missing, or refused: the recorder cannot be used, so it is released.
            discard(next)
            return false
        }
        recorder = next
        startedAt = SystemClock.elapsedRealtime()
        _state.value = RecorderState.Recording(0, 0f)
        handler.postDelayed(poll, POLL_MS)
        return true
    }

    override fun stop(): Long {
        val active = recorder ?: return 0L
        val time = elapsed()
        handler.removeCallbacks(poll)
        recorder = null
        // stop() throws when no audio frame arrived yet; the file is then empty and the take is too short anyway.
        runCatching { active.stop() }
        discard(active)
        _state.value = RecorderState.Idle
        return time
    }

    private fun interrupted() {
        val time = stop()
        onInterrupted(time)
    }

    private fun discard(active: MediaRecorder) {
        runCatching { active.reset() }
        runCatching { active.release() }
    }

    @Suppress("DEPRECATION")
    private fun newRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()

    private companion object {
        const val SAMPLE_RATE = 44_100
        const val BITRATE = 96_000
        const val POLL_MS = 50L
        const val PEAK_MAX = 32_767f
    }
}
