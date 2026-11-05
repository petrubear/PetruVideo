package com.petruvideo.feature.editor

import com.petruvideo.core.data.session.ProjectSession
import com.petruvideo.core.media.pressure.ResourcePressureMonitor
import com.petruvideo.core.model.Fingerprint
import com.petruvideo.core.model.KEYFRAME_MAX
import com.petruvideo.core.model.Keyframe
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
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
import com.petruvideo.core.designsystem.UiText

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelKeyframesTest {
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
    private val Env.overlay get() = session.timeline.value!!.overlayLane.single()

    private val clip = MediaItem(
        "ov", MediaKind.Video, "content://ov", "pip.mp4", 6_000, 1280, 720, 30f, false,
        Fingerprint(1, 1, 6_000, "video/mp4"),
    )

    private fun Env.withOverlay() {
        vm.onEvent(EditorEvent.AddOverlayMedia(clip))
        vm.onEvent(EditorEvent.UseTool(ClipTool.Keyframes))
    }

    @Test
    fun `the Keyframes tool opens the panel and Add keyframe selects the new keyframe`() = runTest {
        val e = env()
        e.withOverlay()
        e.state.keyframePanel?.keyframes shouldBe emptyList()
        e.vm.onEvent(EditorEvent.KeyframeAdd)
        e.overlay.keyframes.size shouldBe 1
        e.state.keyframePanel?.selectedRelMs shouldBe 0L
        e.state.selectedKeyframeMs shouldBe 0L
        e.vm.onEvent(EditorEvent.Undo)
        e.overlay.keyframes shouldBe emptyList()
    }

    @Test
    fun `a gesture on an animated clip makes a keyframe at the playhead and a preview stays live`() = runTest {
        val e = env()
        e.withOverlay()
        e.vm.onEvent(EditorEvent.KeyframeAdd)
        val first = e.overlay
        e.vm.onEvent(EditorEvent.SeekTo(2_000))
        val moved = e.state.selectedOverlay!!.copy(xPermille = 100, yPermille = 900)
        e.vm.onEvent(EditorEvent.OverlayPreview(moved))
        e.overlay shouldBe first
        e.vm.onEvent(EditorEvent.OverlayCommit(moved))
        e.overlay.keyframes.map { it.timeMs } shouldBe listOf(0L, 2_000L)
        e.overlay.keyframes[1].pose.xPermille shouldBe 100
        e.state.selectedOverlay?.xPermille shouldBe 100
        e.vm.onEvent(EditorEvent.SeekTo(1_000))
        e.state.selectedOverlay?.xPermille shouldBe (first.xPermille + 100) / 2
    }

    @Test
    fun `jump, copy, motion and delete work on the selected keyframe`() = runTest {
        val e = env()
        e.withOverlay()
        e.vm.onEvent(EditorEvent.KeyframeAdd)
        e.vm.onEvent(EditorEvent.SeekTo(3_000))
        e.vm.onEvent(EditorEvent.KeyframeCopyHere)
        e.overlay.keyframes.map { it.timeMs } shouldBe listOf(0L, 3_000L)
        e.vm.onEvent(EditorEvent.KeyframeJump(forward = false))
        e.state.selectedKeyframeMs shouldBe 0L
        e.vm.onEvent(EditorEvent.KeyframeMotion(ease = true))
        e.overlay.keyframes[0].ease shouldBe true
        e.vm.onEvent(EditorEvent.KeyframeJump(forward = true))
        e.state.selectedKeyframeMs shouldBe 3_000L
        e.vm.onEvent(EditorEvent.KeyframeDelete)
        e.overlay.keyframes.size shouldBe 1
        e.state.selectedKeyframeMs shouldBe null
    }

    @Test
    fun `a keyframe moves inside the clip and the limit is explained`() = runTest {
        val e = env()
        e.withOverlay()
        e.vm.onEvent(EditorEvent.KeyframeAdd)
        e.vm.onEvent(EditorEvent.KeyframeMove(e.overlay.id, 0, 1_500))
        e.overlay.keyframes.single().timeMs shouldBe 1_500L
        val full = e.overlay.withKeyframes(List(KEYFRAME_MAX) { Keyframe(it * 100L, Pose(500, 500, 350, 100)) })
        e.session.apply(
            com.petruvideo.core.model.engine.Command {
                com.petruvideo.core.model.engine.CommandResult.Applied(it.copy(overlayLane = listOf(full)), "Fill")
            },
        )
        e.vm.onEvent(EditorEvent.SeekTo(950))
        e.vm.onEvent(EditorEvent.KeyframeAdd)
        e.state.message shouldBe UiText.Res(R.string.editor_a_clip_can_have_50)
    }

    @Test
    fun `text clips have keyframes too and the tool bar lists the tool`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.AddText())
        e.state.clipTools.contains(ClipTool.Keyframes) shouldBe true
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.Keyframes))
        e.vm.onEvent(EditorEvent.KeyframeAdd)
        e.session.timeline.value!!.textLane.single().keyframes.size shouldBe 1
    }
}
