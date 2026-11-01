package com.petruvideo.feature.editor

import com.petruvideo.core.data.session.ProjectSession
import com.petruvideo.core.media.pressure.ResourcePressureMonitor
import com.petruvideo.core.model.BlurStrength
import com.petruvideo.core.model.CanvasBackground
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
class EditorViewModelCanvasTest {
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
        vm.onEvent(EditorEvent.SelectTab(ToolbarTab.Effects))
        vm.onEvent(EditorEvent.OpenCanvas)
        return Env(vm, session)
    }

    private val Env.state get() = vm.state.value

    @Test
    fun `the panel opens on black and each choice is one undo step`() = runTest {
        val e = env()
        e.state.canvasPanel shouldBe CanvasBackground.Black
        e.vm.onEvent(EditorEvent.CanvasSet(CanvasBackground.Blur(BlurStrength.Strong)))
        e.session.timeline.value!!.canvas shouldBe CanvasBackground.Blur(BlurStrength.Strong)
        e.state.canvasPanel shouldBe CanvasBackground.Blur(BlurStrength.Strong)
        e.vm.onEvent(EditorEvent.CanvasSet(CanvasBackground.Colour(-65536)))
        e.vm.onEvent(EditorEvent.Undo)
        e.session.timeline.value!!.canvas shouldBe CanvasBackground.Blur(BlurStrength.Strong)
        e.vm.onEvent(EditorEvent.CanvasDismiss)
        e.state.canvasPanel shouldBe null
    }
}
