package com.petruvideo.feature.editor

import com.petruvideo.core.data.session.ProjectSession
import com.petruvideo.core.media.freeze.FreezeStillMaker
import com.petruvideo.core.media.pressure.ResourcePressureMonitor
import com.petruvideo.core.model.Fingerprint
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
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
class EditorViewModelFreezeTest {
    @After
    fun tearDown() = Dispatchers.resetMain()

    private val still = MediaItem(
        "still", MediaKind.Photo, "file://still.jpg", "still.jpg", null, 1920, 1080, null, false,
        Fingerprint(1, 1, 0, "image/jpeg"),
    )

    private class Env(val vm: EditorViewModel, val session: ProjectSession, val asked: MutableList<Long>)

    private fun TestScope.env(maker: (MutableList<Long>) -> FreezeStillMaker): Env {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val session = ProjectSession(FakeRepository(mapOf("p" to sampleLoaded())), backgroundScope)
        val asked = mutableListOf<Long>()
        val vm = EditorViewModel(
            session, FakePreviewEngine(), FakeAvailability(), FakeThumbnails(), ResourcePressureMonitor(),
            fakeVoiceOver(), maker(asked),
        )
        vm.open("p")
        vm.onEvent(EditorEvent.Select(ClipSelection.Video("c1")))
        vm.onEvent(EditorEvent.UseTool(ClipTool.Freeze))
        return Env(vm, session, asked)
    }

    private val Env.state get() = vm.state.value
    private val Env.lane get() = session.timeline.value!!.videoLane

    @Test
    fun `the Freeze tool opens the panel and Insert cuts the clip and adds the still`() = runTest {
        val e = env { asked -> FreezeStillMaker { _, time -> asked += time; still } }
        e.state.freezePanel?.lengthMs shouldBe 2_000L
        e.vm.onEvent(EditorEvent.SeekTo(1_500))
        e.vm.onEvent(EditorEvent.FreezeInsert(3_000))
        e.asked shouldBe listOf(1_500L)
        e.lane.map { it.id }.size shouldBe 5
        e.lane[1].mediaItemId shouldBe "still"
        e.lane[1].lengthMs shouldBe 3_000
        e.state.freezePanel shouldBe null
        e.vm.onEvent(EditorEvent.Undo)
        e.lane.map { it.id } shouldBe listOf("c1", "c2", "c3")
    }

    @Test
    fun `an unreadable frame leaves the project alone and says so`() = runTest {
        val e = env { FreezeStillMaker { _, _ -> null } }
        e.vm.onEvent(EditorEvent.SeekTo(1_500))
        e.vm.onEvent(EditorEvent.FreezeInsert(2_000))
        e.lane.size shouldBe 3
        e.state.message shouldBe UiText.Res(R.string.editor_that_frame_could_not_be)
    }

    @Test
    fun `a playhead outside the clip asks the user to move it`() = runTest {
        val e = env { FreezeStillMaker { _, _ -> still } }
        e.vm.onEvent(EditorEvent.SeekTo(9_000))
        e.vm.onEvent(EditorEvent.FreezeInsert(2_000))
        e.lane.size shouldBe 3
        e.state.message shouldBe UiText.Res(R.string.editor_move_the_playhead_onto_this)
    }
}
