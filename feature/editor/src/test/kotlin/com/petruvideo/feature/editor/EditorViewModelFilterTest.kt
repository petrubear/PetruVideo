package com.petruvideo.feature.editor

import com.petruvideo.core.data.session.ProjectSession
import com.petruvideo.core.media.pressure.ResourcePressureMonitor
import com.petruvideo.core.model.ClipFilter
import com.petruvideo.core.model.FilterLook
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
class EditorViewModelFilterTest {
    @After
    fun tearDown() = Dispatchers.resetMain()

    private class Env(val vm: EditorViewModel, val engine: FakePreviewEngine, val session: ProjectSession)

    private fun TestScope.env(): Env {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val repo = FakeRepository(mapOf("p" to sampleLoaded()))
        val session = ProjectSession(repo, backgroundScope)
        val engine = FakePreviewEngine()
        val vm = EditorViewModel(
            session, engine, FakeAvailability(), FakeThumbnails(), ResourcePressureMonitor(),
            fakeVoiceOver(),
        )
        vm.open("p")
        return Env(vm, engine, session)
    }

    private val Env.state get() = vm.state.value
    private fun Env.filterOf(id: String) = session.timeline.value!!.videoLane.first { it.id == id }.filter

    private fun Env.open(kind: FilterToolKind) {
        vm.onEvent(EditorEvent.SelectTab(ToolbarTab.Filters))
        vm.onEvent(EditorEvent.Select(ClipSelection.Video("c1")))
        val tool = if (kind == FilterToolKind.Looks) ClipTool.FilterLooks else ClipTool.FilterAdjust
        vm.onEvent(EditorEvent.UseTool(tool))
    }

    @Test
    fun `the Filters tab shows its own tools for a picture clip and opens the panels`() = runTest {
        val e = env()
        e.open(FilterToolKind.Looks)
        e.state.clipTools shouldBe
            listOf(ClipTool.FilterLooks, ClipTool.FilterAdjust, ClipTool.Duplicate, ClipTool.Delete)
        e.state.filterPanel?.kind shouldBe FilterToolKind.Looks
        e.state.filterPanel?.canApplyAll shouldBe true
        e.vm.onEvent(EditorEvent.OpenFilterTool(FilterToolKind.Adjust))
        e.state.filterPanel?.kind shouldBe FilterToolKind.Adjust
        e.vm.onEvent(EditorEvent.FilterDismiss)
        e.state.filterPanel shouldBe null
    }

    @Test
    fun `a look tap is one undo step and the intensity drag is live then one edit`() = runTest {
        val e = env()
        e.open(FilterToolKind.Looks)
        e.vm.onEvent(EditorEvent.FilterCommit(ClipFilter(look = FilterLook.Vintage)))
        e.filterOf("c1").look shouldBe FilterLook.Vintage
        e.vm.onEvent(EditorEvent.FilterPreview(ClipFilter(look = FilterLook.Vintage, intensity = 40)))
        e.engine.calls.last() shouldBe "filter c1 Vintage 40 0"
        e.filterOf("c1").intensity shouldBe 100
        e.vm.onEvent(EditorEvent.FilterCommit(ClipFilter(look = FilterLook.Vintage, intensity = 40)))
        e.filterOf("c1").intensity shouldBe 40
        e.vm.onEvent(EditorEvent.Undo)
        e.filterOf("c1").intensity shouldBe 100
        e.vm.onEvent(EditorEvent.Undo)
        e.filterOf("c1").look shouldBe FilterLook.None
    }

    @Test
    fun `adjust sliders commit once and reset keeps the look while remove clears it`() = runTest {
        val e = env()
        e.open(FilterToolKind.Adjust)
        e.vm.onEvent(EditorEvent.FilterCommit(ClipFilter(look = FilterLook.None, brightness = 30, vignette = 50)))
        e.filterOf("c1").brightness shouldBe 30
        e.vm.onEvent(EditorEvent.FilterCommit(e.filterOf("c1").copy(look = FilterLook.Warm)))
        e.vm.onEvent(EditorEvent.FilterResetAdjustments)
        e.filterOf("c1") shouldBe ClipFilter(look = FilterLook.Warm)
        e.vm.onEvent(EditorEvent.FilterRemove)
        e.filterOf("c1") shouldBe ClipFilter()
    }

    @Test
    fun `apply to all copies the filter and one undo restores every clip`() = runTest {
        val e = env()
        e.open(FilterToolKind.Looks)
        e.vm.onEvent(EditorEvent.FilterCommit(ClipFilter(look = FilterLook.Cool)))
        e.vm.onEvent(EditorEvent.FilterApplyAll)
        e.session.timeline.value!!.videoLane.all { it.filter.look == FilterLook.Cool } shouldBe true
        e.vm.onEvent(EditorEvent.Undo)
        e.filterOf("c2").look shouldBe FilterLook.None
        e.filterOf("c1").look shouldBe FilterLook.Cool
    }

    @Test
    fun `dismissing after a drag puts the live filter back and selecting another clip closes the panel`() = runTest {
        val e = env()
        e.open(FilterToolKind.Adjust)
        e.vm.onEvent(EditorEvent.FilterPreview(ClipFilter(contrast = 80)))
        e.vm.onEvent(EditorEvent.FilterDismiss)
        e.engine.calls.last() shouldBe "filter c1 None 100 0"
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.FilterAdjust))
        e.vm.onEvent(EditorEvent.Select(ClipSelection.Video("c3")))
        e.state.filterPanel shouldBe null
    }

    @Test
    fun `audio clips get no filter tools`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.SelectTab(ToolbarTab.Filters))
        e.vm.onEvent(EditorEvent.Select(ClipSelection.Audio("a1")))
        e.state.clipTools.contains(ClipTool.FilterLooks) shouldBe false
        e.vm.onEvent(EditorEvent.OpenFilterTool(FilterToolKind.Looks))
        e.state.filterPanel shouldBe null
    }
}
