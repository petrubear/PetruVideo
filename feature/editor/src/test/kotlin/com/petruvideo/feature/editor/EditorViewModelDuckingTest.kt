package com.petruvideo.feature.editor

import com.petruvideo.core.data.session.ProjectSession
import com.petruvideo.core.media.pressure.ResourcePressureMonitor
import com.petruvideo.core.model.DuckSmoothness
import com.petruvideo.core.model.Ducking
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelDuckingTest {
    @After
    fun tearDown() = Dispatchers.resetMain()

    private class Env(val vm: EditorViewModel, val session: ProjectSession)

    private fun TestScope.env(): Env {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val session = ProjectSession(FakeRepository(mapOf("p" to sampleLoaded())), backgroundScope)
        val vm = EditorViewModel(
            session, FakePreviewEngine(), FakeAvailability(), FakeThumbnails(), ResourcePressureMonitor(),
            fakeVoiceOver(),
        )
        vm.open("p")
        return Env(vm, session)
    }

    private val Env.state get() = vm.state.value
    private val Env.clip get() = session.timeline.value!!.audioLane.single()

    @Test
    fun `an audio clip offers ducking in its Audio list and a change is one undo step`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.Select(ClipSelection.Audio("a1")))
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.Audio))
        e.state.audioPanel?.ducking shouldBe Ducking()
        e.vm.onEvent(EditorEvent.OpenAudioTool(AudioToolKind.Ducking))
        e.vm.onEvent(EditorEvent.DuckingChange(Ducking(true, 80, DuckSmoothness.Slow)))
        e.clip.ducking shouldBe Ducking(true, 80, DuckSmoothness.Slow)
        e.vm.onEvent(EditorEvent.Undo)
        e.clip.ducking shouldBe Ducking()
    }

    @Test
    fun `the panel says so when nothing plays over the clip`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.Select(ClipSelection.Audio("a1")))
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.Audio))
        // a1 sits over the second video clip, which has no sound.
        e.state.audioPanel?.duckingIdle shouldBe true
    }

    @Test
    fun `a video clip does not offer ducking`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.Select(ClipSelection.Video("c1")))
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.Audio))
        e.state.audioPanel?.ducking shouldBe null
    }
}
