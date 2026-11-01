package com.petruvideo.feature.editor

import com.petruvideo.core.data.session.ProjectSession
import com.petruvideo.core.media.pressure.ResourcePressureMonitor
import com.petruvideo.core.model.ClipTransform
import com.petruvideo.core.model.CropBox
import com.petruvideo.core.model.CropHandle
import com.petruvideo.core.model.CropShape
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
class EditorViewModelTransformTest {
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
        vm.onEvent(EditorEvent.Select(ClipSelection.Video("c1")))
        vm.onEvent(EditorEvent.UseTool(ClipTool.Transform))
        return Env(vm, session)
    }

    private val Env.state get() = vm.state.value
    private val Env.transform get() = session.timeline.value!!.videoLane.first { it.id == "c1" }.transform

    @Test
    fun `the Transform tool opens the panel and rotate flip and reset are one step each`() = runTest {
        val e = env()
        e.state.transformPanel?.transform shouldBe ClipTransform()
        e.vm.onEvent(EditorEvent.TransformRotate)
        e.vm.onEvent(EditorEvent.TransformFlip(horizontal = true))
        e.transform shouldBe ClipTransform(quarterTurns = 1, flipH = true)
        e.state.transformPanel?.transform shouldBe e.transform
        e.vm.onEvent(EditorEvent.Undo)
        e.transform shouldBe ClipTransform(quarterTurns = 1)
        e.vm.onEvent(EditorEvent.TransformReset)
        e.transform shouldBe ClipTransform()
    }

    @Test
    fun `the crop screen edits a box with a kept shape and Done applies it as one step`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.CropOpen)
        e.state.crop?.box shouldBe CropBox()
        e.vm.onEvent(EditorEvent.CropShapePick(CropShape.Square))
        val square = e.state.crop!!.box
        e.state.crop?.shape shouldBe CropShape.Square
        e.vm.onEvent(EditorEvent.CropDrag(null, 100, 0))
        e.state.crop!!.box.widthPermille shouldBe square.widthPermille
        e.vm.onEvent(EditorEvent.CropDrag(CropHandle.BottomRight, -50, -50))
        val after = e.state.crop!!.box
        e.transform.crop shouldBe CropBox()
        e.vm.onEvent(EditorEvent.CropDone)
        e.state.crop shouldBe null
        e.transform.crop shouldBe after
        e.vm.onEvent(EditorEvent.Undo)
        e.transform.crop shouldBe CropBox()
    }

    @Test
    fun `Cancel and Full leave no crop behind`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.CropOpen)
        e.vm.onEvent(EditorEvent.CropShapePick(CropShape.Story))
        e.vm.onEvent(EditorEvent.CropCancel)
        e.state.crop shouldBe null
        e.transform.crop shouldBe CropBox()
        e.vm.onEvent(EditorEvent.CropOpen)
        e.vm.onEvent(EditorEvent.CropShapePick(CropShape.Story))
        e.vm.onEvent(EditorEvent.CropFullPicture)
        e.state.crop?.box shouldBe CropBox()
    }

    @Test
    fun `the crop screen draws the picture as turned and the panel closes with the selection`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.TransformRotate)
        e.vm.onEvent(EditorEvent.CropOpen)
        val crop = e.state.crop!!
        crop.transform.quarterTurns shouldBe 1
        (crop.pictureWidth < crop.pictureHeight) shouldBe true
        e.vm.onEvent(EditorEvent.Select(ClipSelection.Video("c2")))
        e.state.transformPanel shouldBe null
        e.state.crop shouldBe null
    }
}
