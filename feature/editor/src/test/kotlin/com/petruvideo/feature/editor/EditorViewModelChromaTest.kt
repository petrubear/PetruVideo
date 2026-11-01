package com.petruvideo.feature.editor

import com.petruvideo.core.data.session.ProjectSession
import com.petruvideo.core.media.chroma.ChromaSampler
import com.petruvideo.core.media.pressure.ResourcePressureMonitor
import com.petruvideo.core.model.CHROMA_BLUE
import com.petruvideo.core.model.ChromaKey
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
class EditorViewModelChromaTest {
    @After
    fun tearDown() = Dispatchers.resetMain()

    private class Env(val vm: EditorViewModel, val engine: FakePreviewEngine, val session: ProjectSession)

    private val sampled = mutableListOf<List<Float>>()

    private fun TestScope.env(): Env {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val session = ProjectSession(FakeRepository(mapOf("p" to sampleLoaded())), backgroundScope)
        val engine = FakePreviewEngine()
        val sampler = ChromaSampler { _, _, u, v ->
            sampled += listOf(u, v)
            CHROMA_BLUE
        }
        val vm = EditorViewModel(
            session, engine, FakeAvailability(), FakeThumbnails(), ResourcePressureMonitor(), fakeVoiceOver(),
            chromaSampler = sampler,
        )
        vm.open("p")
        vm.onEvent(EditorEvent.AddOverlayMedia(clip))
        vm.onEvent(EditorEvent.UseTool(ClipTool.OverlayOpacity))
        return Env(vm, engine, session)
    }

    private val Env.overlay get() = session.timeline.value!!.overlayLane.single()

    private val clip = MediaItem(
        "ov", MediaKind.Video, "content://ov", "pip.mp4", 6_000, 1280, 720, 30f, false,
        Fingerprint(1, 1, 6_000, "video/mp4"),
    )

    @Test
    fun `a preview is only live and a commit is one undo step`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.ChromaPreview(ChromaKey(true, strength = 80)))
        e.overlay.chroma shouldBe ChromaKey()
        e.engine.calls.last().startsWith("overlay ") shouldBe true
        e.vm.onEvent(EditorEvent.ChromaCommit(ChromaKey(true, strength = 80)))
        e.overlay.chroma shouldBe ChromaKey(true, strength = 80)
        e.vm.onEvent(EditorEvent.Undo)
        e.overlay.chroma shouldBe ChromaKey()
    }

    @Test
    fun `the checkerboard is local to the preview and never stored`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.ChromaChecker(true))
        e.engine.calls.last() shouldBe "checker true"
        e.vm.state.value.chromaChecker shouldBe true
        e.overlay.chroma shouldBe ChromaKey()
    }

    @Test
    fun `picking samples the overlay under the tap and turns the key on with that colour`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.ChromaPicking(true))
        e.vm.state.value.chromaPicking shouldBe true
        // The overlay is centered at (750, 250) and 400 permille wide, so its center is a tap at (750, 250).
        e.vm.onEvent(EditorEvent.ChromaPick(750, 250))
        e.vm.state.value.chromaPicking shouldBe false
        e.overlay.chroma shouldBe ChromaKey(true, CHROMA_BLUE)
        sampled.single().map { Math.round(it * 100) / 100f } shouldBe listOf(0.5f, 0.5f)
    }
}
