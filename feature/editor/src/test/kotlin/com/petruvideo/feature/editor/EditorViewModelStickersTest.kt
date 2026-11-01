package com.petruvideo.feature.editor

import com.petruvideo.core.data.session.ProjectSession
import com.petruvideo.core.media.pressure.ResourcePressureMonitor
import com.petruvideo.core.model.StickerCatalog
import com.petruvideo.core.model.StickerCategory
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
class EditorViewModelStickersTest {
    @After
    fun tearDown() = Dispatchers.resetMain()

    private class Env(val vm: EditorViewModel, val session: ProjectSession)

    private fun TestScope.env(): Env {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val session = ProjectSession(FakeRepository(mapOf("p" to sampleLoaded())), backgroundScope)
        val vm = EditorViewModel(
            session, FakePreviewEngine(), FakeAvailability(), FakeThumbnails(), ResourcePressureMonitor(),
            fakeVoiceOver(),
        )
        vm.open("p")
        vm.onEvent(EditorEvent.SelectTab(ToolbarTab.Text))
        vm.onEvent(EditorEvent.OpenStickers)
        return Env(vm, session)
    }

    private val Env.state get() = vm.state.value
    private val Env.texts get() = session.timeline.value!!.textLane

    @Test
    fun `the gallery opens and stays open while stickers are added, each its own selected layer`() = runTest {
        val e = env()
        e.state.stickersOpen shouldBe true
        val star = StickerCatalog.of(StickerCategory.Shapes).first { it.label == "Star" }
        e.vm.onEvent(EditorEvent.AddStickerLayer(star))
        e.texts.single().text shouldBe star.glyph
        (e.state.selection is ClipSelection.Text) shouldBe true
        e.state.stickersOpen shouldBe true
        e.vm.onEvent(EditorEvent.AddStickerLayer(StickerCatalog.of(StickerCategory.Faces).first()))
        e.texts.size shouldBe 2
        e.vm.onEvent(EditorEvent.StickersDismiss)
        e.state.stickersOpen shouldBe false
    }

    @Test
    fun `adding a sticker is one undo step`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.AddStickerLayer(StickerCatalog.all.first()))
        e.vm.onEvent(EditorEvent.Undo)
        e.texts shouldBe emptyList()
    }
}
