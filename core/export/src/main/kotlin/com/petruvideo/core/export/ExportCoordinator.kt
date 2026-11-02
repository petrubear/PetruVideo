package com.petruvideo.core.export

import com.petruvideo.core.data.repo.ExportRepository
import com.petruvideo.core.data.repo.StoredJob
import com.petruvideo.core.media.pressure.PressureLevel
import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.export.BitrateTable
import com.petruvideo.core.model.export.ExportFailure
import com.petruvideo.core.model.export.ExportRecord
import com.petruvideo.core.model.export.ExportSettings
import com.petruvideo.core.model.export.ExportState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** What a render will cost: the real video bitrate, the estimated size, and the length. */
data class ExportPlan(val videoBitrate: Int, val estimatedBytes: Long, val durationMs: Long)

/** One export, from the snapshot taken at the start to its result. */
data class ExportJob(
    val id: String,
    val projectId: String,
    val projectName: String,
    val settings: ExportSettings,
    val plan: ExportPlan,
    val startedAt: Long,
    val state: ExportState,
)

/** Why an export cannot start now. */
sealed interface Blocker {
    data object NoClips : Blocker
    data class MissingMedia(val name: String) : Blocker
    data class NotEnoughStorage(val neededBytes: Long) : Blocker
    data object Busy : Blocker
}

/**
 * Runs one export at a time: takes a snapshot of the project, renders it, saves it to the gallery, and publishes the
 * state for the screen and the notification. A killed process does not resume; [ExportRecovery] reports it next time.
 */
class ExportCoordinator(
    private val runner: ExportRunner,
    private val saver: ExportSaver,
    private val repository: ExportRepository,
    private val encoders: EncoderCapabilities,
    private val storage: StorageInfo,
    private val cacheDir: File,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { java.util.UUID.randomUUID().toString() },
) {
    private val _job = MutableStateFlow<ExportJob?>(null)
    val job: StateFlow<ExportJob?> = _job
    private var running: Job? = null
    private var stoppedByPressure = false

    /** The bitrate the encoder will really get (the table value inside the encoder's range) and the size estimate. */
    fun plan(timeline: Timeline, settings: ExportSettings): ExportPlan {
        val wanted = BitrateTable.videoBitrate(settings.resolution, settings.frameRate, settings.quality)
        val range = encoders.bitrateRange
        val bitrate = wanted.coerceIn(range.first, range.last)
        val duration = timeline.totalDurationMs
        return ExportPlan(bitrate, BitrateTable.estimatedBytes(bitrate, duration), duration)
    }

    fun check(timeline: Timeline, plan: ExportPlan): Blocker? {
        if (_job.value?.state?.active == true) return Blocker.Busy
        if (timeline.videoLane.isEmpty() && timeline.audioLane.isEmpty() && timeline.textLane.isEmpty()) {
            return Blocker.NoClips
        }
        missingName(timeline)?.let { return Blocker.MissingMedia(it) }
        val needed = plan.estimatedBytes * COPIES + MARGIN_BYTES
        return if (storage.freeBytes() < needed) Blocker.NotEnoughStorage(needed) else null
    }

    private fun missingName(timeline: Timeline): String? {
        val used = timeline.videoLane.map { it.mediaItemId } + timeline.audioLane.map { it.mediaItemId }
        val items = used.mapNotNull { timeline.media[it] }
        return items.firstOrNull { it.availability == Availability.Missing }?.displayName
    }

    /** Starts rendering a copy of [timeline]; returns the blocker instead when it cannot start. */
    fun start(projectId: String, projectName: String, timeline: Timeline, settings: ExportSettings): Blocker? {
        val plan = plan(timeline, settings)
        check(timeline, plan)?.let { return it }
        val job = ExportJob(newId(), projectId, projectName, settings, plan, clock(), ExportState.Waiting)
        _job.value = job
        stoppedByPressure = false
        running = scope.launch { render(job, timeline) }
        return null
    }

    fun cancel() {
        running?.cancel()
    }

    /** Called by the pressure monitor: at critical pressure an active render stops with a clear reason. */
    fun onPressure(level: PressureLevel) {
        if (level == PressureLevel.Critical && _job.value?.state?.active == true) {
            stoppedByPressure = true
            running?.cancel()
        }
    }

    /** Forgets a finished or failed job (the screen was left) so a new one starts clean. */
    fun clear() {
        if (_job.value?.state?.active != true) _job.value = null
    }

    private suspend fun render(job: ExportJob, snapshot: Timeline) {
        val file = File(cacheDir, "${job.id}.mp4").also { it.parentFile?.mkdirs() }
        try {
            set(job, ExportState.Rendering(0))
            val request = ExportRequest(snapshot, job.settings, job.plan.videoBitrate, file)
            when (val result = runner.run(request) { progress -> setProgress(job, progress) }) {
                is RunResult.Error -> finish(job, ExportState.Failed(ExportFailure.EncoderError(result.detail)))
                is RunResult.Done -> publish(job, file, result.durationMs)
            }
        } catch (e: CancellationException) {
            withContext(NonCancellable) {
                val end = if (stoppedByPressure) ExportState.Failed(ExportFailure.Pressure) else ExportState.Cancelled
                finish(job, end)
            }
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            // A failure in the render or in saving its result ends the job instead of reaching the app scope.
            withContext(NonCancellable) {
                runCatching { finish(job, ExportState.Failed(ExportFailure.EncoderError(e.message.orEmpty()))) }
            }
        } finally {
            withContext(NonCancellable) { file.delete() }
        }
    }

    private suspend fun publish(job: ExportJob, file: File, durationMs: Long) {
        val saved = saver.save(file, "${job.projectName.ifBlank { "PetruVideo" }}-${job.startedAt}.mp4")
        if (saved != null) {
            repository.addRecord(
                ExportRecord(job.id, job.projectName, saved.uri, durationMs, saved.bytes, job.settings, clock()),
            )
        }
        finish(
            job,
            if (saved == null) {
                ExportState.Failed(ExportFailure.SaveFailed)
            } else {
                ExportState.Finished(saved.uri, saved.bytes, durationMs)
            },
        )
    }

    private fun setProgress(job: ExportJob, progress: Int) {
        _job.update { current ->
            if (current?.id == job.id && current.state is ExportState.Rendering) {
                current.copy(state = ExportState.Rendering(progress.coerceIn(0, MAX_PROGRESS)))
            } else {
                current
            }
        }
    }

    private suspend fun set(job: ExportJob, state: ExportState) {
        _job.update { current -> if (current?.id == job.id) current.copy(state = state) else current }
        repository.saveJob(StoredJob(job.projectId, job.id, job.startedAt, state))
    }

    private suspend fun finish(job: ExportJob, state: ExportState) = set(job, state)

    private companion object {
        const val COPIES = 2L
        const val MARGIN_BYTES = 50L * 1024 * 1024
        const val MAX_PROGRESS = 100
    }
}

/** At app start: a job left waiting or rendering (the app was killed) is reported as interrupted and its files go. */
class ExportRecovery(private val repository: ExportRepository, private val cacheDir: File) {
    suspend fun run(): Int {
        cacheDir.listFiles()?.forEach { it.delete() }
        return repository.markInterrupted()
    }
}
