package com.petruvideo.feature.editor

import com.petruvideo.core.data.session.ProjectSession
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

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelOverlayTest {
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
    private val Env.overlays get() = session.timeline.value!!.overlayLane

    private val clip = MediaItem(
        "ov", MediaKind.Video, "content://ov", "pip.mp4", 6_000, 1280, 720, 30f, false,
        Fingerprint(1, 1, 6_000, "video/mp4"),
    )

    @Test
    fun `the Overlay tab adds an overlay at the playhead and selects it with its tools`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.SelectTab(ToolbarTab.Overlay))
        e.state.toolbarTab shouldBe ToolbarTab.Overlay
        e.vm.onEvent(EditorEvent.AddOverlay)
        e.vm.onEvent(EditorEvent.AddOverlayMedia(clip))
        e.overlays.single().durationMs shouldBe 6_000
        e.state.selection shouldBe ClipSelection.Overlay(e.overlays.single().id)
        e.state.selectedOverlay?.mediaItemId shouldBe "ov"
        e.state.clipTools shouldBe listOf(
            ClipTool.OverlayOpacity, ClipTool.Keyframes, ClipTool.Split, ClipTool.Duplicate, ClipTool.Delete,
        )
        e.vm.onEvent(EditorEvent.Undo)
        e.overlays shouldBe emptyList()
    }

    @Test
    fun `placement and opacity commit as separate edits and a preview is only live`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.AddOverlayMedia(clip))
        val first = e.overlays.single()
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.OverlayOpacity))
        e.state.overlayPanel?.clip?.id shouldBe first.id
        e.vm.onEvent(EditorEvent.OverlayPreview(first.copy(xPermille = 100, opacityPercent = 30)))
        e.engine.calls.last() shouldBe "overlay ${first.id} 100 ${first.yPermille} ${first.widthPermille} 30"
        e.overlays.single() shouldBe first
        e.vm.onEvent(EditorEvent.OverlayCommit(first.copy(xPermille = 100)))
        e.overlays.single().xPermille shouldBe 100
        e.vm.onEvent(EditorEvent.OverlayCommit(e.overlays.single().copy(opacityPercent = 30)))
        e.overlays.single().opacityPercent shouldBe 30
        e.vm.onEvent(EditorEvent.Undo)
        e.overlays.single().opacityPercent shouldBe 100
        e.overlays.single().xPermille shouldBe 100
    }

    @Test
    fun `moves and trims are separate events and the panel closes when the selection changes`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.AddOverlayMedia(clip))
        val id = e.overlays.single().id
        e.vm.onEvent(EditorEvent.MoveOverlayClip(id, 10_000))
        e.state.playback.durationMs shouldBe 16_000
        e.vm.onEvent(EditorEvent.TrimOverlayClip(id, 10_000, 12_000))
        e.overlays.single().durationMs shouldBe 2_000
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.OverlayOpacity))
        e.vm.onEvent(EditorEvent.Select(ClipSelection.Video("c1")))
        e.state.overlayPanel shouldBe null
        e.vm.onEvent(EditorEvent.Select(ClipSelection.Overlay(id)))
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.Duplicate))
        e.overlays.size shouldBe 2
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.Delete))
        e.overlays.size shouldBe 1
    }

    @Test
    fun `an audio file is refused with a message`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.AddOverlayMedia(clip.copy(kind = MediaKind.Audio)))
        e.overlays shouldBe emptyList()
        e.state.message shouldBe "This file cannot be used as an overlay"
    }
}
