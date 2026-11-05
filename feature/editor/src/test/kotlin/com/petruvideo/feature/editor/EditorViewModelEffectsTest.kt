package com.petruvideo.feature.editor

import com.petruvideo.core.data.session.ProjectSession
import com.petruvideo.core.media.pressure.ResourcePressureMonitor
import com.petruvideo.core.model.ClipEffect
import com.petruvideo.core.model.EffectKind
import com.petruvideo.core.model.Fingerprint
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.Transition
import com.petruvideo.core.model.TransitionKind
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
import com.petruvideo.core.designsystem.UiText

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelEffectsTest {
    @After
    fun tearDown() = Dispatchers.resetMain()

    private class Env(val vm: EditorViewModel, val engine: FakePreviewEngine, val session: ProjectSession)

    private fun TestScope.env(): Env {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val session = ProjectSession(FakeRepository(mapOf("p" to sampleLoaded())), backgroundScope)
        val engine = FakePreviewEngine()
        val vm = EditorViewModel(
            session, engine, FakeAvailability(), FakeThumbnails(), ResourcePressureMonitor(),
            fakeVoiceOver(),
        )
        vm.open("p")
        return Env(vm, engine, session)
    }

    private val Env.state get() = vm.state.value
    private val Env.lane get() = session.timeline.value!!.videoLane

    private val song = MediaItem(
        "song", MediaKind.Audio, "content://song", "song.mp3", 5_000, null, null, null, true,
        Fingerprint(1, 1, 5_000, "audio/mpeg"),
    )

    @Test
    fun `the Effects tab offers the transition and effect tools for a selected clip`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.SelectTab(ToolbarTab.Effects))
        e.vm.onEvent(EditorEvent.Select(ClipSelection.Video("c1")))
        e.state.clipTools shouldBe
            listOf(ClipTool.EffectTransition, ClipTool.EffectFx, ClipTool.Duplicate, ClipTool.Delete)
    }

    @Test
    fun `a transition is one edit, shortens the project, and the last clip cannot have one`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.Select(ClipSelection.Video("c1")))
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.EffectTransition))
        e.state.effectPanel?.limitMs shouldBe 1_500
        e.vm.onEvent(EditorEvent.TransitionSet(TransitionKind.Slide, 1_000))
        e.lane[0].transitionOut shouldBe Transition(TransitionKind.Slide, 1_000)
        e.state.playback.durationMs shouldBe 12_000 - 1_000
        e.vm.onEvent(EditorEvent.Undo)
        e.lane[0].transitionOut shouldBe null
        e.vm.onEvent(EditorEvent.Select(ClipSelection.Video("c3")))
        e.state.disabledTools[ClipTool.EffectTransition] shouldBe UiText.Res(R.string.editor_no_clip_follows_this_one)
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.EffectTransition))
        e.state.effectPanel shouldBe null
    }

    @Test
    fun `an effect strength is live until committed and the commit is one undo step`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.Select(ClipSelection.Video("c2")))
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.EffectFx))
        e.vm.onEvent(EditorEvent.EffectCommit(ClipEffect(EffectKind.Blur, 50)))
        e.lane[1].effect shouldBe ClipEffect(EffectKind.Blur, 50)
        e.vm.onEvent(EditorEvent.EffectPreview(ClipEffect(EffectKind.Blur, 80)))
        e.engine.calls.last() shouldBe "effect c2 Blur 80"
        e.lane[1].effect shouldBe ClipEffect(EffectKind.Blur, 50)
        e.vm.onEvent(EditorEvent.EffectCommit(ClipEffect(EffectKind.Blur, 80)))
        e.vm.onEvent(EditorEvent.Undo)
        e.lane[1].effect shouldBe ClipEffect(EffectKind.Blur, 50)
        e.vm.onEvent(EditorEvent.EffectRemove(EffectKind.Blur))
        e.lane[1].effect shouldBe null
    }

    @Test
    fun `Add music puts an audio clip at the playhead and a video file is refused`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.SelectTab(ToolbarTab.Audio))
        e.vm.onEvent(EditorEvent.AddMusic)
        e.vm.onEvent(EditorEvent.AddMusicMedia(song))
        e.session.timeline.value!!.audioLane.count { it.mediaItemId == "song" } shouldBe 1
        val video = song.copy(id = "v", kind = MediaKind.Video, width = 100, height = 100)
        e.vm.onEvent(EditorEvent.AddMusicMedia(video))
        e.state.message shouldBe UiText.Res(R.string.editor_that_file_cant_be_used)
    }

    @Test
    fun `a second and third effect stack and a fourth is refused with a message`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.Select(ClipSelection.Video("c2")))
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.EffectFx))
        for (kind in listOf(EffectKind.Blur, EffectKind.Glow, EffectKind.Grain)) {
            e.vm.onEvent(EditorEvent.EffectCommit(ClipEffect(kind, 40)))
        }
        e.lane[1].effects.map { it.kind } shouldBe listOf(EffectKind.Blur, EffectKind.Glow, EffectKind.Grain)
        e.vm.onEvent(EditorEvent.EffectCommit(ClipEffect(EffectKind.Vhs, 40)))
        e.lane[1].effects.size shouldBe 3
        e.state.message shouldBe UiText.Res(R.string.editor_effect_limit, 3)
        e.vm.onEvent(EditorEvent.EffectRemove(EffectKind.Glow))
        e.lane[1].effects.map { it.kind } shouldBe listOf(EffectKind.Blur, EffectKind.Grain)
    }

    @Test
    fun `flashing effects show a notice and the others do not`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.Select(ClipSelection.Video("c2")))
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.EffectFx))
        e.vm.onEvent(EditorEvent.EffectCommit(ClipEffect(EffectKind.Cartoon, 40)))
        e.state.message shouldBe null
        e.vm.onEvent(EditorEvent.EffectCommit(ClipEffect(EffectKind.Strobe, 40)))
        e.state.message shouldBe UiText.Res(R.string.editor_flashing_lights_notice)
        e.vm.onEvent(EditorEvent.DismissMessage)
        e.vm.onEvent(EditorEvent.EffectCommit(ClipEffect(EffectKind.Flash, 0)))
        e.state.message shouldBe null
        e.vm.onEvent(EditorEvent.EffectCommit(ClipEffect(EffectKind.Flash, 60)))
        e.state.message shouldBe UiText.Res(R.string.editor_flashing_lights_notice)
    }
}
