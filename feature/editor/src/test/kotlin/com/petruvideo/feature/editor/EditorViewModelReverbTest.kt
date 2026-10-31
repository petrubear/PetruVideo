package com.petruvideo.feature.editor

import com.petruvideo.core.media.pressure.ResourcePressureMonitor
import com.petruvideo.core.data.session.ProjectSession
import com.petruvideo.core.model.ReverbPreset
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
class EditorViewModelReverbTest {
    @After
    fun tearDown() = Dispatchers.resetMain()

    private class Env(val vm: EditorViewModel, val engine: FakePreviewEngine, val session: ProjectSession)

    private fun TestScope.env(): Env {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val repo = FakeRepository(mapOf("p" to sampleLoaded()))
        val session = ProjectSession(repo, backgroundScope)
        val engine = FakePreviewEngine()
        val vm = EditorViewModel(
            session, engine, FakeAvailability(emptySet()), FakeThumbnails(), ResourcePressureMonitor(),
        )
        vm.open("p")
        return Env(vm, engine, session)
    }

    private val Env.state get() = vm.state.value

    private fun Env.reverbOf(id: String) = session.timeline.value!!.let { t ->
        t.videoLane.firstOrNull { it.id == id }?.reverb ?: t.audioLane.first { it.id == id }.reverb
    }

    @Test
    fun `reverb tool opens the panel and a toggle is one undo step`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.Select(ClipSelection.Video("c1")))
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.Reverb))
        e.state.reverbPanel?.clipId shouldBe "c1"
        e.state.reverbPanel?.reverb?.enabled shouldBe false

        e.vm.onEvent(EditorEvent.ReverbToggle(true))
        e.reverbOf("c1").enabled shouldBe true
        e.state.reverbPanel?.reverb?.enabled shouldBe true
        e.vm.onEvent(EditorEvent.Undo)
        e.reverbOf("c1").enabled shouldBe false
    }

    @Test
    fun `picking a preset sets all numbers and turns reverb on`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.Select(ClipSelection.Audio("a1")))
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.Reverb))
        e.vm.onEvent(EditorEvent.ReverbPick(ReverbPreset.Cave))
        e.reverbOf("a1").preset shouldBe ReverbPreset.Cave
        e.reverbOf("a1").enabled shouldBe true
        e.reverbOf("a1").decayMs shouldBe 7_000
    }

    @Test
    fun `a drag is live and a release is one edit, Custom appears, reset returns to the default preset`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.Select(ClipSelection.Video("c1")))
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.Reverb))
        e.vm.onEvent(EditorEvent.ReverbToggle(true))
        val tuned = e.reverbOf("c1").copy(mix = 77)

        e.vm.onEvent(EditorEvent.ReverbPreview(tuned))
        e.engine.calls.last() shouldBe "reverb c1 77 true"
        e.reverbOf("c1").mix shouldBe 30

        e.vm.onEvent(EditorEvent.ReverbCommit(tuned))
        e.reverbOf("c1").mix shouldBe 77
        e.reverbOf("c1").preset shouldBe ReverbPreset.Custom
        e.vm.onEvent(EditorEvent.ReverbReset)
        e.reverbOf("c1").preset shouldBe ReverbPreset.MediumRoom
        e.reverbOf("c1").mix shouldBe 30
    }

    @Test
    fun `dismissing after a drag puts the live reverb back and selecting another clip closes the panel`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.Select(ClipSelection.Video("c1")))
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.Reverb))
        e.vm.onEvent(EditorEvent.ReverbPreview(e.reverbOf("c1").copy(mix = 90)))
        e.vm.onEvent(EditorEvent.ReverbDismiss)
        e.state.reverbPanel shouldBe null
        e.engine.calls.last() shouldBe "reverb c1 30 false"

        e.vm.onEvent(EditorEvent.UseTool(ClipTool.Reverb))
        e.vm.onEvent(EditorEvent.Select(ClipSelection.Video("c3")))
        e.state.reverbPanel shouldBe null
    }

    @Test
    fun `reverb and mute are dimmed with a reason for a clip without sound`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.Select(ClipSelection.Video("c2")))
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.Reverb))
        e.state.reverbPanel shouldBe null
        e.state.message shouldBe "This clip has no sound"
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.Mute))
        e.state.canUndo shouldBe false
    }

    @Test
    fun `mute toggles a video clip and shows Unmute`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.Select(ClipSelection.Video("c1")))
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.Mute))
        e.session.timeline.value!!.videoLane[0].muted shouldBe true
        e.state.selectedAudioMuted shouldBe true
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.Mute))
        e.state.selectedAudioMuted shouldBe false
        e.session.timeline.value!!.videoLane[0].volumePercent shouldBe 100
    }
}
