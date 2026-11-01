package com.petruvideo.core.export

import com.petruvideo.core.data.repo.ExportRepository
import com.petruvideo.core.data.repo.StoredJob
import com.petruvideo.core.media.pressure.PressureLevel
import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.Fingerprint
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.ProjectFormat
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VideoClip
import com.petruvideo.core.model.export.DeviceLimits
import com.petruvideo.core.model.export.ExportFailure
import com.petruvideo.core.model.export.ExportSettings
import com.petruvideo.core.model.export.ExportState
import com.petruvideo.core.model.export.FrameRate
import com.petruvideo.core.model.export.Quality
import com.petruvideo.core.model.export.Resolution
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExportCoordinatorTest {
    private class FakeRepo : ExportRepository {
        val jobs = mutableListOf<StoredJob>()
        val records = mutableListOf<com.petruvideo.core.model.export.ExportRecord>()
        override suspend fun addRecord(record: com.petruvideo.core.model.export.ExportRecord) {
            records += record
        }
        override suspend fun settings(projectId: String) = null
        override suspend fun saveSettings(projectId: String, settings: ExportSettings) = Unit
        override suspend fun job(projectId: String) = jobs.lastOrNull { it.projectId == projectId }
        override suspend fun saveJob(job: StoredJob) { jobs += job }
        override suspend fun markInterrupted() = 0
    }

    private class FakeRunner(val gate: CompletableDeferred<RunResult> = CompletableDeferred()) : ExportRunner {
        var request: ExportRequest? = null
        override suspend fun run(request: ExportRequest, onProgress: (Int) -> Unit): RunResult {
            this.request = request
            request.output.writeText("video")
            onProgress(40)
            return gate.await()
        }
    }

    private class FakeSaver(var result: SavedVideo? = SavedVideo("content://media/9", 5)) : ExportSaver {
        override suspend fun save(file: File, displayName: String) = result
    }

    private val encoders = object : EncoderCapabilities {
        override val limits = DeviceLimits(2160, 60)
        override val bitrateRange = 1_000_000..30_000_000
    }
    private val settings = ExportSettings(Resolution.R1080, FrameRate.F30, Quality.Recommended)

    private fun media(id: String, missing: Boolean = false) = MediaItem(
        id, MediaKind.Video, "content://$id", "$id.mp4", 10_000, 1920, 1080, 30f, true,
        Fingerprint(1, 1, 10_000, "video/mp4"), if (missing) Availability.Missing else Availability.Available,
    )

    private fun timeline(missing: Boolean = false) = Timeline(
        "p", ProjectFormat(1920, 1080, 30), mapOf("a" to media("a", missing)),
        videoLane = listOf(VideoClip("c1", "a", 0, 10_000, true)),
    )

    private class Env(val coordinator: ExportCoordinator, val runner: FakeRunner, val repo: FakeRepo, val dir: File)

    private fun TestScope.env(free: Long = Long.MAX_VALUE, saver: FakeSaver = FakeSaver()): Env {
        val dir = File.createTempFile("export", "").apply { delete(); mkdirs() }
        val runner = FakeRunner()
        val repo = FakeRepo()
        var n = 0
        val coordinator = ExportCoordinator(
            runner, saver, repo, encoders, { free }, dir, scopeOf(this), { 1000L }, { "job${n++}" },
        )
        return Env(coordinator, runner, repo, dir)
    }

    private fun scopeOf(scope: TestScope) =
        kotlinx.coroutines.CoroutineScope(StandardTestDispatcher(scope.testScheduler))

    @Test
    fun `a job goes Waiting to Rendering to Finished and cleans its file`() = runTest {
        val e = env()
        e.coordinator.start("p", "Trip", timeline(), settings) shouldBe null
        e.coordinator.job.value!!.state shouldBe ExportState.Waiting
        advanceUntilIdle()
        e.coordinator.job.value!!.state shouldBe ExportState.Rendering(40)
        e.runner.gate.complete(RunResult.Done(5, 10_000))
        advanceUntilIdle()
        e.coordinator.job.value!!.state shouldBe ExportState.Finished("content://media/9", 5, 10_000)
        File(e.dir, "job0.mp4").exists() shouldBe false
        e.repo.jobs.last().state.shouldBeInstanceOf<ExportState.Finished>()
    }

    @Test
    fun `a finished export adds one record and a failed or cancelled one adds none`() = runTest {
        val e = env()
        e.coordinator.start("p", "Trip", timeline(), settings) shouldBe null
        advanceUntilIdle()
        e.runner.gate.complete(RunResult.Done(5, 10_000))
        advanceUntilIdle()
        val record = e.repo.records.single()
        record.projectName shouldBe "Trip"
        record.uri shouldBe "content://media/9"
        record.bytes shouldBe 5
        record.durationMs shouldBe 10_000
        record.settings shouldBe settings
        record.finishedAt shouldBe 1000L
        record.formatLabel shouldBe "1080p, 30 fps, Recommended"

        val failing = env()
        failing.coordinator.start("p", "Trip", timeline(), settings)
        advanceUntilIdle()
        failing.runner.gate.complete(RunResult.Error("boom"))
        advanceUntilIdle()
        failing.repo.records shouldBe emptyList()

        val cancelled = env()
        cancelled.coordinator.start("p", "Trip", timeline(), settings)
        advanceUntilIdle()
        cancelled.coordinator.cancel()
        advanceUntilIdle()
        cancelled.repo.records shouldBe emptyList()

        val unsaved = env(saver = FakeSaver(null))
        unsaved.coordinator.start("p", "Trip", timeline(), settings)
        advanceUntilIdle()
        unsaved.runner.gate.complete(RunResult.Done(5, 10_000))
        advanceUntilIdle()
        unsaved.repo.records shouldBe emptyList()
    }

    @Test
    fun `the plan uses the table bitrate inside the encoder range and the estimate follows it`() = runTest {
        val e = env()
        val plan = e.coordinator.plan(timeline(), settings.copy(resolution = Resolution.R4K))
        plan.videoBitrate shouldBe 30_000_000
        plan.estimatedBytes shouldBe ((30_000_000 + 128_000) * 10.0 / 8).toLong()
    }

    @Test
    fun `only one export runs at a time`() = runTest {
        val e = env()
        e.coordinator.start("p", "Trip", timeline(), settings) shouldBe null
        advanceUntilIdle()
        e.coordinator.start("p", "Trip", timeline(), settings) shouldBe Blocker.Busy
    }

    @Test
    fun `the render uses a copy of the project taken at the start`() = runTest {
        val e = env()
        val original = timeline()
        e.coordinator.start("p", "Trip", original, settings)
        advanceUntilIdle()
        e.runner.request!!.timeline shouldBe original
    }

    @Test
    fun `cancel ends in Cancelled and removes the temporary file`() = runTest {
        val e = env()
        e.coordinator.start("p", "Trip", timeline(), settings)
        advanceUntilIdle()
        e.coordinator.cancel()
        advanceUntilIdle()
        e.coordinator.job.value!!.state shouldBe ExportState.Cancelled
        File(e.dir, "job0.mp4").exists() shouldBe false
    }

    @Test
    fun `critical pressure stops the render with the Pressure reason`() = runTest {
        val e = env()
        e.coordinator.start("p", "Trip", timeline(), settings)
        advanceUntilIdle()
        e.coordinator.onPressure(PressureLevel.Reduced)
        e.coordinator.job.value!!.state shouldBe ExportState.Rendering(40)
        e.coordinator.onPressure(PressureLevel.Critical)
        advanceUntilIdle()
        e.coordinator.job.value!!.state shouldBe ExportState.Failed(ExportFailure.Pressure)
    }

    @Test
    fun `an encoder error and a failed save end in Failed`() = runTest {
        val e = env()
        e.coordinator.start("p", "Trip", timeline(), settings)
        advanceUntilIdle()
        e.runner.gate.complete(RunResult.Error("ERROR_CODE_ENCODING_FAILED"))
        advanceUntilIdle()
        val failure = ExportFailure.EncoderError("ERROR_CODE_ENCODING_FAILED")
        e.coordinator.job.value!!.state shouldBe ExportState.Failed(failure)

        val f = env(saver = FakeSaver(null))
        f.coordinator.start("p", "Trip", timeline(), settings)
        advanceUntilIdle()
        f.runner.gate.complete(RunResult.Done(5, 1))
        advanceUntilIdle()
        f.coordinator.job.value!!.state shouldBe ExportState.Failed(ExportFailure.SaveFailed)
    }

    @Test
    fun `blockers for no clips, missing media, and too little storage`() = runTest {
        val e = env()
        e.coordinator.start("p", "Trip", Timeline("p"), settings) shouldBe Blocker.NoClips
        e.coordinator.start("p", "Trip", timeline(missing = true), settings) shouldBe Blocker.MissingMedia("a.mp4")
        val low = env(free = 1_000)
        low.coordinator.start("p", "Trip", timeline(), settings).shouldBeInstanceOf<Blocker.NotEnoughStorage>()
        low.coordinator.job.value shouldBe null
    }

    @Test
    fun `recovery marks leftovers and removes cache files`() = runTest {
        val dir = File.createTempFile("export", "").apply { delete(); mkdirs() }
        File(dir, "old.mp4").writeText("x")
        var marked = 0
        val repo = object : ExportRepository by FakeRepo() {
            override suspend fun markInterrupted(): Int { marked++; return 1 }
        }
        ExportRecovery(repo, dir).run() shouldBe 1
        marked shouldBe 1
        File(dir, "old.mp4").exists() shouldBe false
    }
}
