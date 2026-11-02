package com.petruvideo.feature.editor

import com.petruvideo.core.data.session.ProjectSession
import com.petruvideo.core.media.pressure.ResourcePressureMonitor
import com.petruvideo.core.media.voicesplit.SplitStems
import com.petruvideo.core.media.voicesplit.VoiceSplitter
import com.petruvideo.core.model.Fingerprint
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
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
class EditorViewModelVoiceSplitTest {
    @After
    fun tearDown() = Dispatchers.resetMain()

    private class Env(
        val vm: EditorViewModel,
        val session: ProjectSession,
        val result: CompletableDeferred<SplitStems?>,
        val progress: MutableList<(Int) -> Unit>,
    )

    private fun stem(id: String) = MediaItem(
        id, MediaKind.Audio, "file:///$id.wav", "$id.wav", 5_000, null, null, null, true,
        Fingerprint(1, 1, 5_000, "audio/wav"),
    )

    private fun TestScope.env(): Env {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val session = ProjectSession(FakeRepository(mapOf("p" to sampleLoaded())), backgroundScope)
        val result = CompletableDeferred<SplitStems?>()
        val reports = mutableListOf<(Int) -> Unit>()
        val splitter = VoiceSplitter { _, _, _, onProgress ->
            reports += onProgress
            result.await()
        }
        val vm = EditorViewModel(
            session, FakePreviewEngine(), FakeAvailability(), FakeThumbnails(), ResourcePressureMonitor(),
            fakeVoiceOver(), voiceSplitter = splitter,
        )
        vm.open("p")
        vm.onEvent(EditorEvent.Select(ClipSelection.Video("c1")))
        return Env(vm, session, result, reports)
    }

    private val Env.state get() = vm.state.value
    /** The audio clips the split added: the sample project already has one of its own. */
    private val Env.audio get() = session.timeline.value!!.audioLane.filter { it.id != "a1" }

    @Test
    fun `it shows progress, then adds Voice and Backing as one undo step`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.SplitVoice))
        e.state.voiceSplitProgress shouldBe 0
        e.progress.single().invoke(40)
        e.state.voiceSplitProgress shouldBe 40
        e.result.complete(SplitStems(stem("voice"), stem("backing")))
        e.state.voiceSplitProgress shouldBe null
        e.audio.map { it.mediaItemId } shouldBe listOf("voice", "backing")
        e.session.timeline.value!!.videoLane.first { it.id == "c1" }.attachedAudio shouldBe false
        e.vm.onEvent(EditorEvent.Undo)
        e.audio shouldBe emptyList()
        e.session.timeline.value!!.videoLane.first { it.id == "c1" }.attachedAudio shouldBe true
    }

    @Test
    fun `cancel stops it and changes nothing`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.SplitVoice))
        e.vm.onEvent(EditorEvent.VoiceSplitCancel)
        e.state.voiceSplitProgress shouldBe null
        e.state.message shouldBe "Split cancelled"
        e.audio shouldBe emptyList()
    }

    @Test
    fun `changing the clip while it runs cancels the split with a message`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.SplitVoice))
        e.vm.onEvent(EditorEvent.SeekTo(2_000))
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.Split))
        e.state.voiceSplitProgress shouldBe null
        e.state.message shouldBe "Split cancelled because the clip changed"
        e.audio shouldBe emptyList()
    }

    @Test
    fun `a failed split says so and leaves the clip alone`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.SplitVoice))
        e.result.complete(null)
        e.state.message shouldBe "Could not split the voice"
        e.audio shouldBe emptyList()
        e.session.timeline.value!!.videoLane.first { it.id == "c1" }.attachedAudio shouldBe true
    }

    @Test
    fun `a second split while one runs is refused`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.SplitVoice))
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.SplitVoice))
        e.state.message shouldBe "A voice split is already running"
        e.progress.size shouldBe 1
    }
}
