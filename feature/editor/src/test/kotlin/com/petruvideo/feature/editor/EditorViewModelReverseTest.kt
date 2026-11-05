package com.petruvideo.feature.editor

import com.petruvideo.core.data.session.ProjectSession
import com.petruvideo.core.media.pressure.ResourcePressureMonitor
import com.petruvideo.core.media.reverse.ReverseNeed
import com.petruvideo.core.media.reverse.ReversePreparer
import com.petruvideo.core.media.reverse.ReversedSource
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.Timeline
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Test
import com.petruvideo.core.designsystem.UiText

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelReverseTest {
    @After
    fun tearDown() = Dispatchers.resetMain()

    private class FakePreparer(var room: Boolean = true) : ReversePreparer {
        val mutableProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
        override val progress: StateFlow<Map<String, Int>> = mutableProgress
        var gate = CompletableDeferred<Boolean>()
        val asked = mutableListOf<Triple<String, Long, Long>>()

        override fun hasRoom(startMs: Long, endMs: Long) = room

        override suspend fun prepare(clipId: String, media: MediaItem, startMs: Long, endMs: Long): Boolean {
            asked += Triple(clipId, startMs, endMs)
            mutableProgress.value = mapOf(clipId to 40)
            try {
                return gate.await()
            } finally {
                mutableProgress.value = emptyMap()
            }
        }

        override fun sources(timeline: Timeline): Map<String, ReversedSource> = emptyMap()

        override suspend fun ensure(timeline: Timeline) = true

        override fun sweep(needs: List<ReverseNeed>) = Unit
    }

    private class Env(val vm: EditorViewModel, val session: ProjectSession, val preparer: FakePreparer)

    private fun TestScope.env(preparer: FakePreparer = FakePreparer()): Env {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val session = ProjectSession(FakeRepository(mapOf("p" to sampleLoaded())), backgroundScope)
        val vm = EditorViewModel(
            session, FakePreviewEngine(), FakeAvailability(), FakeThumbnails(), ResourcePressureMonitor(),
            fakeVoiceOver(), reverse = preparer,
        )
        vm.open("p")
        vm.onEvent(EditorEvent.Select(ClipSelection.Video("c1")))
        return Env(vm, session, preparer)
    }

    private val Env.state get() = vm.state.value
    private val Env.clip get() = session.timeline.value!!.videoLane.first { it.id == "c1" }

    @Test
    fun `Reverse shows progress and turns on when the copy is ready, then turns off in one step`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.Reverse))
        e.state.reverseProgress shouldBe 40
        e.clip.reversed shouldBe false
        e.preparer.gate.complete(true)
        e.clip.reversed shouldBe true
        e.state.reverseProgress shouldBe null
        e.state.selectedReversed shouldBe true
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.Reverse))
        e.clip.reversed shouldBe false
        e.vm.onEvent(EditorEvent.Undo)
        e.clip.reversed shouldBe true
    }

    @Test
    fun `Cancel leaves the clip alone`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.Reverse))
        e.vm.onEvent(EditorEvent.ReverseCancel)
        e.clip.reversed shouldBe false
        e.state.reverseProgress shouldBe null
    }

    @Test
    fun `a failed copy says so and changes nothing`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.Reverse))
        e.preparer.gate.complete(false)
        e.clip.reversed shouldBe false
        e.state.message shouldBe UiText.Res(R.string.editor_this_clip_could_not_be)
    }

    @Test
    fun `too little storage is reported before anything starts`() = runTest {
        val e = env(FakePreparer(room = false))
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.Reverse))
        e.preparer.asked shouldBe emptyList()
        e.state.message shouldBe UiText.Res(R.string.editor_not_enough_storage_to_reverse)
    }
}
