package com.petruvideo.feature.editor

import com.petruvideo.core.data.session.ProjectSession
import com.petruvideo.core.media.pressure.ResourcePressureMonitor
import com.petruvideo.core.model.AudioFx
import com.petruvideo.core.model.EqPreset
import com.petruvideo.core.model.EqPresets
import com.petruvideo.core.model.Fade
import com.petruvideo.core.model.NoiseReduction
import com.petruvideo.core.model.Pitch
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
class EditorViewModelAudioFxTest {
    @After
    fun tearDown() = Dispatchers.resetMain()

    private class Env(val vm: EditorViewModel, val engine: FakePreviewEngine, val session: ProjectSession)

    private fun TestScope.env(): Env {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val repo = FakeRepository(mapOf("p" to sampleLoaded()))
        val session = ProjectSession(repo, backgroundScope)
        val engine = FakePreviewEngine()
        val vm = EditorViewModel(session, engine, FakeAvailability(), FakeThumbnails(), ResourcePressureMonitor())
        vm.open("p")
        return Env(vm, engine, session)
    }

    private val Env.state get() = vm.state.value

    private fun Env.fxOf(id: String): AudioFx = session.timeline.value!!.let { t ->
        t.videoLane.firstOrNull { it.id == id }?.fx ?: t.audioLane.first { it.id == id }.fx
    }

    @Test
    fun `the Audio tool opens the list and a tool opens its panel`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.Select(ClipSelection.Video("c1")))
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.Audio))
        e.state.audioPanel?.kind shouldBe AudioToolKind.List
        e.state.audioPanel?.clipId shouldBe "c1"
        e.vm.onEvent(EditorEvent.OpenAudioTool(AudioToolKind.Fade))
        e.state.audioPanel?.kind shouldBe AudioToolKind.Fade
        e.vm.onEvent(EditorEvent.AudioDismiss)
        e.state.audioPanel shouldBe null
    }

    @Test
    fun `a fade drag is live and a release is one edit and one undo step`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.Select(ClipSelection.Video("c1")))
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.Audio))
        e.vm.onEvent(EditorEvent.OpenAudioTool(AudioToolKind.Fade))
        val fx = AudioFx(fade = Fade(1_000, 500))

        e.vm.onEvent(EditorEvent.AudioFxPreview(fx))
        e.engine.calls.last() shouldBe "fx c1 1000 500"
        e.fxOf("c1").fade shouldBe Fade()

        e.vm.onEvent(EditorEvent.AudioFxCommit(fx))
        e.fxOf("c1").fade shouldBe Fade(1_000, 500)
        e.vm.onEvent(EditorEvent.Undo)
        e.fxOf("c1").fade shouldBe Fade()
    }

    @Test
    fun `a fade longer than half the clip is limited with a message`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.Select(ClipSelection.Video("c1")))
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.Audio))
        e.vm.onEvent(EditorEvent.AudioFxCommit(AudioFx(fade = Fade(3_900, 0))))
        e.fxOf("c1").fade shouldBe Fade(2_000, 0)
        e.state.message shouldBe "Fade limited to half the clip"
    }

    @Test
    fun `noise equalizer and voice changes apply to audio clips and are separate undo steps`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.Select(ClipSelection.Audio("a1")))
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.Audio))
        e.vm.onEvent(EditorEvent.AudioFxCommit(e.fxOf("a1").copy(noise = NoiseReduction(true))))
        e.vm.onEvent(EditorEvent.AudioFxCommit(e.fxOf("a1").copy(eq = EqPresets.settings(EqPreset.BassBoost))))
        val voice = e.fxOf("a1").voice
        e.vm.onEvent(EditorEvent.AudioFxCommit(e.fxOf("a1").copy(voice = voice.copy(pitch = Pitch(true, 7)))))
        e.fxOf("a1").noise.enabled shouldBe true
        e.fxOf("a1").eq.preset shouldBe EqPreset.BassBoost
        e.fxOf("a1").voice.pitch.semitones shouldBe 7
        e.vm.onEvent(EditorEvent.Undo)
        e.fxOf("a1").voice.pitch.enabled shouldBe false
        e.fxOf("a1").eq.preset shouldBe EqPreset.BassBoost
    }

    @Test
    fun `reset returns one tool to neutral`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.Select(ClipSelection.Video("c1")))
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.Audio))
        e.vm.onEvent(EditorEvent.AudioFxCommit(e.fxOf("c1").copy(eq = EqPresets.settings(EqPreset.Warm))))
        e.vm.onEvent(EditorEvent.AudioFxReset(AudioToolKind.Eq))
        e.fxOf("c1").eq.isFlat shouldBe true
    }

    @Test
    fun `dismissing after a drag restores the live values and selecting another clip closes the panel`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.Select(ClipSelection.Video("c1")))
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.Audio))
        e.vm.onEvent(EditorEvent.AudioFxPreview(AudioFx(fade = Fade(800, 0))))
        e.vm.onEvent(EditorEvent.AudioDismiss)
        e.engine.calls.last() shouldBe "fx c1 0 0"
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.Audio))
        e.vm.onEvent(EditorEvent.Select(ClipSelection.Video("c3")))
        e.state.audioPanel shouldBe null
    }

    @Test
    fun `the Audio tool is dimmed with a reason for a clip without sound`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.Select(ClipSelection.Video("c2")))
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.Audio))
        e.state.audioPanel shouldBe null
        e.state.message shouldBe "This clip has no sound"
    }

    @Test
    fun `volume and reverb open from the list and close it`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.Select(ClipSelection.Video("c1")))
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.Audio))
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.Volume))
        e.state.audioPanel shouldBe null
        e.state.sheet?.tool shouldBe ClipTool.Volume
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.Audio))
        e.state.sheet shouldBe null
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.Reverb))
        e.state.audioPanel shouldBe null
        e.state.reverbPanel?.clipId shouldBe "c1"
    }
}
