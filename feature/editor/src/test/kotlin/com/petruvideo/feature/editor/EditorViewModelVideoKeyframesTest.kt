package com.petruvideo.feature.editor

import com.petruvideo.core.data.session.ProjectSession
import com.petruvideo.core.media.pressure.ResourcePressureMonitor
import com.petruvideo.core.model.FRAMING_NEUTRAL
import com.petruvideo.core.model.Pose
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
class EditorViewModelVideoKeyframesTest {
    @After
    fun tearDown() = Dispatchers.resetMain()

    private class Env(val vm: EditorViewModel, val engine: FakePreviewEngine, val session: ProjectSession)

    private fun TestScope.env(): Env {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val session = ProjectSession(FakeRepository(mapOf("p" to sampleLoaded())), backgroundScope)
        val engine = FakePreviewEngine()
        val vm = EditorViewModel(
            session, engine, FakeAvailability(), FakeThumbnails(), ResourcePressureMonitor(), fakeVoiceOver(),
        )
        vm.open("p")
        vm.onEvent(EditorEvent.Select(ClipSelection.Video("c2")))
        vm.onEvent(EditorEvent.UseTool(ClipTool.Keyframes))
        return Env(vm, engine, session)
    }

    private val Env.state get() = vm.state.value
    private val Env.clip get() = session.timeline.value!!.videoLane.first { it.id == "c2" }

    @Test
    fun `the Keyframes tool opens the panel with the neutral framing and arms the preview`() = runTest {
        val e = env()
        e.state.keyframePanel?.framing shouldBe FRAMING_NEUTRAL
        e.engine.calls.contains("arm c2") shouldBe true
    }

    @Test
    fun `Add keyframe on a video clip makes one at the playhead in clip time`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.SeekTo(5_000))
        e.vm.onEvent(EditorEvent.KeyframeAdd)
        // c2 starts at 4 000 ms.
        e.clip.keyframes.map { it.timeMs } shouldBe listOf(1_000L)
        e.state.selectedKeyframeMs shouldBe 1_000L
    }

    @Test
    fun `a zoom previews live and commits one undo step with the look before it unchanged`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.SeekTo(6_000))
        val zoomed = Pose(600, 500, 2_000, 100)
        e.vm.onEvent(EditorEvent.FramingPreview(zoomed))
        e.clip.keyframes shouldBe emptyList()
        e.engine.calls.last() shouldBe "framing c2 2"
        e.vm.onEvent(EditorEvent.FramingCommit(zoomed))
        e.clip.keyframes.map { it.timeMs } shouldBe listOf(0L, 2_000L)
        e.clip.poseAt(0) shouldBe FRAMING_NEUTRAL
        e.vm.onEvent(EditorEvent.Undo)
        e.clip.keyframes shouldBe emptyList()
    }
}
