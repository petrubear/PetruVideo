package com.petruvideo.feature.editor

import app.cash.turbine.test
import com.petruvideo.core.data.session.ProjectSession
import com.petruvideo.core.media.pressure.PreviewQuality
import com.petruvideo.core.media.pressure.ResourcePressureMonitor
import com.petruvideo.core.model.Availability
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest {
    @After
    fun tearDown() = Dispatchers.resetMain()

    private class Env(
        val vm: EditorViewModel,
        val engine: FakePreviewEngine,
        val availability: FakeAvailability,
        val thumbnails: FakeThumbnails,
        val pressure: ResourcePressureMonitor,
        val session: ProjectSession,
    )

    private fun TestScope.env(missing: Set<String> = emptySet()): Env {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val repo = FakeRepository(mapOf("p" to sampleLoaded()))
        val session = ProjectSession(repo, backgroundScope)
        val engine = FakePreviewEngine()
        val availability = FakeAvailability(missing)
        val thumbnails = FakeThumbnails()
        val pressure = ResourcePressureMonitor()
        val vm = EditorViewModel(session, engine, availability, thumbnails, pressure)
        vm.open("p")
        return Env(vm, engine, availability, thumbnails, pressure, session)
    }

    private val Env.state get() = vm.state.value

    @Test
    fun `opening shows the header with name, format label and save status`() = runTest {
        val e = env()
        e.state.header?.name shouldBe "Coast road"
        e.state.header?.formatLabel shouldBe "1080p · 30 fps"
        e.state.playback.durationMs shouldBe 12_000
        e.state.isEmpty shouldBe false
    }

    @Test
    fun `the project media list holds the media used by clips`() = runTest {
        env().state.projectMedia.map { it.id } shouldContainExactly listOf("m1", "m2", "m3", "d")
    }

    @Test
    fun `seeking clamps to the timeline, moves the engine, and stores the playhead`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.SeekTo(5_000))
        e.engine.calls.last() shouldBe "seek 5000"
        e.state.playback.positionMs shouldBe 5_000
        e.session.timeline.value?.playheadMs shouldBe 5_000

        e.vm.onEvent(EditorEvent.SeekTo(99_000))
        e.state.playback.positionMs shouldBe 12_000
        e.vm.onEvent(EditorEvent.SeekTo(-5))
        e.state.playback.positionMs shouldBe 0
    }

    @Test
    fun `play and pause drive the engine and pausing stores the position`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.Play)
        e.state.playback.isPlaying shouldBe true

        e.engine.progress(3_500)
        e.state.playback.positionMs shouldBe 3_500

        e.vm.onEvent(EditorEvent.Pause)
        e.state.playback.isPlaying shouldBe false
        e.session.timeline.value?.playheadMs shouldBe 3_500
    }

    @Test
    fun `playing from the end starts again from the beginning`() = runTest {
        val e = env()
        e.engine.progress(12_000, ended = true)
        e.vm.onEvent(EditorEvent.Play)
        e.engine.calls.takeLast(2) shouldContainExactly listOf("seek 0", "play")
    }

    @Test
    fun `stepping a frame moves by one frame of the project frame rate`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.SeekTo(1_000))
        e.vm.onEvent(EditorEvent.StepFrame(1))
        e.state.playback.positionMs shouldBe 1_033
        e.vm.onEvent(EditorEvent.StepFrame(-1))
        e.state.playback.positionMs shouldBe 1_000
    }

    @Test
    fun `selecting a video clip derives its tools, with Detach only when it has attached sound`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.Select(ClipSelection.Video("c1")))
        e.state.clipTools shouldContainExactly listOf(
            ClipTool.Split, ClipTool.Speed, ClipTool.Volume, ClipTool.Detach, ClipTool.Duplicate, ClipTool.Delete,
        )

        e.vm.onEvent(EditorEvent.Select(ClipSelection.Video("c2")))
        e.state.clipTools shouldContainExactly
            listOf(ClipTool.Split, ClipTool.Speed, ClipTool.Volume, ClipTool.Duplicate, ClipTool.Delete)
    }

    @Test
    fun `selecting an audio clip derives its tools and clearing the selection empties them`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.Select(ClipSelection.Audio("a1")))
        e.state.clipTools shouldContainExactly
            listOf(ClipTool.Split, ClipTool.Mute, ClipTool.Replace, ClipTool.Duplicate, ClipTool.Delete)

        e.vm.onEvent(EditorEvent.Select(null))
        e.state.selection shouldBe null
        e.state.clipTools shouldBe emptyList()
    }

    @Test
    fun `selecting a clip that does not exist is ignored`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.Select(ClipSelection.Video("nope")))
        e.state.selection shouldBe null
    }

    @Test
    fun `moving a clip is an undoable edit and undo or redo availability follows`() = runTest {
        val e = env()
        e.state.canUndo shouldBe false
        e.state.canRedo shouldBe false

        e.vm.onEvent(EditorEvent.MoveClip("c1", 2))
        e.state.timeline?.videoLane?.map { it.id } shouldContainExactly listOf("c2", "c3", "c1")
        e.state.canUndo shouldBe true

        e.vm.onEvent(EditorEvent.Undo)
        e.state.timeline?.videoLane?.map { it.id } shouldContainExactly listOf("c1", "c2", "c3")
        e.state.canRedo shouldBe true

        e.vm.onEvent(EditorEvent.Redo)
        e.state.timeline?.videoLane?.map { it.id } shouldContainExactly listOf("c2", "c3", "c1")
    }

    @Test
    fun `the engine reloads when the clips change but not when only the playhead moves`() = runTest {
        val e = env()
        val before = e.engine.loads.size
        e.vm.onEvent(EditorEvent.SeekTo(2_000))
        e.engine.loads.size shouldBe before

        e.vm.onEvent(EditorEvent.MoveClip("c1", 1))
        e.engine.loads.size shouldBe before + 1
    }

    @Test
    fun `tapping an inactive tool shows coming soon until dismissed`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.TapInactiveTool("Effects"))
        e.state.comingSoon shouldBe "Effects"
        e.vm.onEvent(EditorEvent.DismissMessage)
        e.state.comingSoon shouldBe null
    }

    @Test
    fun `fullscreen can be turned on and off and playback is untouched`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.Play)
        e.vm.onEvent(EditorEvent.SetFullscreen(true))
        e.state.fullscreen shouldBe true
        e.state.playback.isPlaying shouldBe true
        e.vm.onEvent(EditorEvent.SetFullscreen(false))
        e.state.fullscreen shouldBe false
    }

    @Test
    fun `media whose source is missing is flagged and handed to the engine as missing`() = runTest {
        val e = env(missing = setOf("m2"))
        e.state.missingMedia shouldBe setOf("m2")
        e.engine.loads.last().first.media["m2"]?.availability shouldBe Availability.Missing
    }

    @Test
    fun `removing a missing clip deletes its clips and clears the flag`() = runTest {
        val e = env(missing = setOf("m2"))
        e.vm.onEvent(EditorEvent.RemoveMissing("m2"))
        e.state.timeline?.videoLane?.map { it.id } shouldContainExactly listOf("c1", "c3")
        e.state.missingMedia shouldBe emptySet()
    }

    @Test
    fun `relinking rebinds the clips, keeps the trim, and clears the missing flag`() = runTest {
        val e = env(missing = setOf("m3"))
        e.availability.missing = emptySet()
        e.vm.onEvent(EditorEvent.Relink("m3", media("fresh", durationMs = 20_000)))
        val clip = e.state.timeline!!.videoLane.first { it.id == "c3" }
        (clip.sourceStartMs to clip.sourceEndMs) shouldBe (1_000L to 6_000L)
        e.state.missingMedia shouldBe emptySet()
    }

    @Test
    fun `requesting a relink asks the screen to pick a file and the add button opens Add media`() = runTest {
        val e = env()
        e.vm.effects.test {
            e.vm.onEvent(EditorEvent.RequestRelink("m1"))
            awaitItem() shouldBe EditorEffect.PickRelinkFile("m1")
            e.vm.onEvent(EditorEvent.AddClip)
            awaitItem() shouldBe EditorEffect.OpenAddMedia
        }
    }

    @Test
    fun `the project thumbnail is refreshed when the project opens`() = runTest {
        val e = env()
        e.thumbnails.refreshed.firstOrNull()?.first shouldBe "p"
    }

    @Test
    fun `memory pressure lowers quality with a message and critical pressure pauses playback`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.Play)
        e.pressure.onTrimMemory(android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW)
        advanceUntilIdle()
        e.state.message shouldBe "Preview quality lowered to keep playing"
        e.engine.loads.last().second shouldBe PreviewQuality.Reduced

        e.pressure.onThermalStatus(android.os.PowerManager.THERMAL_STATUS_SEVERE)
        advanceUntilIdle()
        e.engine.calls.last() shouldBe "pause"
        e.state.message shouldBe "Playback paused to cool down"
    }
}
