package com.petruvideo.feature.export

import com.petruvideo.core.data.repo.ExportRepository
import com.petruvideo.core.data.repo.LoadedProject
import com.petruvideo.core.data.repo.ProjectFilter
import com.petruvideo.core.data.repo.ProjectRepository
import com.petruvideo.core.data.repo.ProjectSummary
import com.petruvideo.core.data.repo.SettingsRepository
import com.petruvideo.core.data.repo.StoredJob
import com.petruvideo.core.model.AppSettings
import com.petruvideo.core.model.ThemeChoice
import kotlinx.coroutines.flow.MutableStateFlow
import com.petruvideo.core.data.session.ProjectSession
import com.petruvideo.core.export.Blocker
import com.petruvideo.core.export.EncoderCapabilities
import com.petruvideo.core.export.ExportCoordinator
import com.petruvideo.core.export.ExportRequest
import com.petruvideo.core.export.ExportRunner
import com.petruvideo.core.export.ExportSaver
import com.petruvideo.core.export.RunResult
import com.petruvideo.core.export.SavedVideo
import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.Fingerprint
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.Project
import com.petruvideo.core.model.ProjectFormat
import com.petruvideo.core.model.ProjectStatus
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VideoClip
import com.petruvideo.core.model.export.DeviceLimits
import com.petruvideo.core.model.export.ExportFailure
import com.petruvideo.core.model.export.ExportSettings
import com.petruvideo.core.model.export.ExportState
import com.petruvideo.core.model.export.FrameRate
import com.petruvideo.core.model.export.Quality
import com.petruvideo.core.model.export.Resolution
import com.petruvideo.core.model.export.Unavailable
import io.kotest.matchers.shouldBe
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExportViewModelTest {
    @After
    fun tearDown() = Dispatchers.resetMain()

    private class Projects(val loaded: LoadedProject?) : ProjectRepository {
        override fun observeProjects(filter: ProjectFilter, query: String): Flow<List<ProjectSummary>> =
            flowOf(emptyList())
        override suspend fun loadProject(id: String) = loaded
        override suspend fun createProject(): Project = error("unused")
        override suspend fun saveTimeline(projectId: String, timeline: Timeline) = Unit
        override suspend fun duplicateProject(id: String): Project? = null
        override suspend fun deleteProject(id: String) = Unit
        override suspend fun rename(id: String, name: String) = Unit
        override suspend fun setStatus(id: String, status: ProjectStatus) = Unit
        override suspend fun setThumbnail(id: String, ref: String?) = Unit
    }

    private class Exports(var saved: ExportSettings? = null, var last: StoredJob? = null) : ExportRepository {
        override suspend fun settings(projectId: String) = saved
        override suspend fun saveSettings(projectId: String, settings: ExportSettings) { saved = settings }
        override suspend fun job(projectId: String) = last
        override suspend fun saveJob(job: StoredJob) { last = job }
        override suspend fun markInterrupted() = 0
    }

    private class Runner(val gate: CompletableDeferred<RunResult> = CompletableDeferred()) : ExportRunner {
        override suspend fun run(request: ExportRequest, onProgress: (Int) -> Unit): RunResult {
            onProgress(25)
            return gate.await()
        }
    }

    private val saver = object : ExportSaver {
        override suspend fun save(file: File, displayName: String) = SavedVideo("content://media/3", 7)
    }

    private fun media(w: Int, h: Int, fps: Float, missing: Boolean = false) = MediaItem(
        "a", MediaKind.Video, "content://a", "a.mp4", 10_000, w, h, fps, true,
        Fingerprint(1, 1, 10_000, "video/mp4"), if (missing) Availability.Missing else Availability.Available,
    )

    private fun loaded(item: MediaItem, clips: Boolean = true) = LoadedProject(
        Project("p", "Trip", 0, 0),
        Timeline(
            "p", ProjectFormat(1920, 1080, 30), mapOf("a" to item),
            videoLane = if (clips) listOf(VideoClip("c1", "a", 0, 10_000, true)) else emptyList(),
        ),
    )

    private class Env(val vm: ExportViewModel, val runner: Runner, val exports: Exports, var started: Int)

    private fun TestScope.env(
        project: LoadedProject,
        device: DeviceLimits = DeviceLimits(2160, 60),
        exports: Exports = Exports(),
        app: AppSettings = AppSettings(),
    ): Env {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val session = ProjectSession(Projects(project), backgroundScope)
        val runner = Runner()
        val encoders = object : EncoderCapabilities {
            override val limits = device
            override val bitrateRange = 1_000_000..60_000_000
        }
        val cacheDir = File.createTempFile("export", "").apply { delete(); mkdirs() }
        val coordinator = ExportCoordinator(
            runner, saver, exports, encoders, { Long.MAX_VALUE }, cacheDir,
            CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
        )
        lateinit var env: Env
        val settingsRepo = object : SettingsRepository {
            override val settings = MutableStateFlow(app)
            override fun setDefaultQuality(quality: Quality) = Unit
            override fun setLowerPreview(on: Boolean) = Unit
            override fun setTheme(theme: ThemeChoice) = Unit
        }
        val vm = ExportViewModel(session, coordinator, encoders, exports, { env.started++ }, settingsRepo)
        env = Env(vm, runner, exports, 0)
        vm.open("p")
        return env
    }

    private val Env.state get() = vm.state.value

    @Test
    fun `defaults come from the footage, the device, and the project frame rate`() = runTest {
        val e = env(loaded(media(1920, 1080, 30f)))
        e.state.settings shouldBe ExportSettings(Resolution.R1080, FrameRate.F30, Quality.Recommended)
        e.state.videoBitrate shouldBe 12_000_000
        e.state.estimatedBytes shouldBe ((12_000_000 + 128_000) * 10.0 / 8).toLong()
        e.state.blocker shouldBe null
    }

    @Test
    fun `options above the footage are dimmed with the reason`() = runTest {
        val e = env(loaded(media(1920, 1080, 30f)))
        e.state.resolutions.first { it.value == Resolution.R4K }.unavailable shouldBe Unavailable.AboveFootage
        e.state.frameRates.first { it.value == FrameRate.F60 }.unavailable shouldBe Unavailable.AboveFootage
        e.vm.onEvent(ExportEvent.SelectResolution(Resolution.R4K))
        e.state.settings!!.resolution shouldBe Resolution.R1080
    }

    @Test
    fun `4K footage on a capable device allows 4K and updates bitrate and estimate`() = runTest {
        val e = env(loaded(media(3840, 2160, 60f)))
        e.vm.onEvent(ExportEvent.SelectResolution(Resolution.R4K))
        e.vm.onEvent(ExportEvent.SelectQuality(Quality.Higher))
        e.state.settings shouldBe ExportSettings(Resolution.R4K, FrameRate.F30, Quality.Higher)
        e.state.videoBitrate shouldBe 60_000_000
        e.exports.saved shouldBe e.state.settings
    }

    @Test
    fun `a device that cannot encode 4K dims it with its own reason`() = runTest {
        val e = env(loaded(media(3840, 2160, 30f)), device = DeviceLimits(1080, 30))
        e.state.resolutions.first { it.value == Resolution.R4K }.unavailable shouldBe Unavailable.NotSupported
        e.state.settings!!.resolution shouldBe Resolution.R1080
    }

    @Test
    fun `saved settings come back and are lowered into the current caps`() = runTest {
        val saved = ExportSettings(Resolution.R4K, FrameRate.F60, Quality.Smaller)
        val e = env(loaded(media(1920, 1080, 30f)), exports = Exports(saved))
        e.state.settings shouldBe ExportSettings(Resolution.R1080, FrameRate.F30, Quality.Smaller)
    }

    @Test
    fun `the Settings default quality starts a new project and a saved choice wins`() = runTest {
        val fresh = env(loaded(media(1920, 1080, 30f)), app = AppSettings(defaultQuality = Quality.Smaller))
        fresh.state.settings shouldBe ExportSettings(Resolution.R1080, FrameRate.F30, Quality.Smaller)
        fresh.state.videoBitrate shouldBe 7_200_000
        val saved = ExportSettings(Resolution.R1080, FrameRate.F30, Quality.Higher)
        val own = env(loaded(media(1920, 1080, 30f)), exports = Exports(saved), app = AppSettings(Quality.Smaller))
        own.state.settings shouldBe saved
    }

    @Test
    fun `an empty project and missing media block the export with a reason`() = runTest {
        env(loaded(media(1920, 1080, 30f), clips = false)).state.blocker shouldBe Blocker.NoClips
        env(loaded(media(1920, 1080, 30f, missing = true))).state.blocker shouldBe Blocker.MissingMedia("a.mp4")
    }

    @Test
    fun `start begins a render, starts the service, and finishing offers Open and Share`() = runTest {
        val e = env(loaded(media(1920, 1080, 30f)))
        e.vm.onEvent(ExportEvent.Start)
        e.started shouldBe 1
        e.state.job!!.state shouldBe ExportState.Rendering(25)
        e.state.rendering shouldBe true
        e.runner.gate.complete(RunResult.Done(7, 10_000))
        e.state.job!!.state shouldBe ExportState.Finished("content://media/3", 7, 10_000)
        e.state.rendering shouldBe false
    }

    @Test
    fun `cancel stops the render`() = runTest {
        val e = env(loaded(media(1920, 1080, 30f)))
        e.vm.onEvent(ExportEvent.Start)
        e.vm.onEvent(ExportEvent.Cancel)
        e.state.job!!.state shouldBe ExportState.Cancelled
    }

    @Test
    fun `an interrupted last export is reported once`() = runTest {
        val last = StoredJob("p", "j", 0, ExportState.Failed(ExportFailure.Interrupted))
        val e = env(loaded(media(1920, 1080, 30f)), exports = Exports(last = last))
        e.state.interrupted shouldBe true
        e.vm.onEvent(ExportEvent.DismissInterrupted)
        e.state.interrupted shouldBe false
    }
}
