package com.petruvideo.feature.editor

import com.petruvideo.core.data.session.ProjectSession
import com.petruvideo.core.media.pressure.ResourcePressureMonitor
import com.petruvideo.core.model.AnimKind
import com.petruvideo.core.model.TextAnimation
import com.petruvideo.core.model.TextClip
import com.petruvideo.core.model.TextStyle
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
class EditorViewModelTextTest {
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
    private val Env.texts get() = session.timeline.value!!.textLane

    private fun Env.add(): TextClip {
        vm.onEvent(EditorEvent.AddText)
        return texts.single()
    }

    @Test
    fun `the Text tab offers Add text and adding selects the clip with its edit panel open`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.SelectTab(ToolbarTab.Text))
        e.state.toolbarTab shouldBe ToolbarTab.Text
        val clip = e.add()
        clip.text shouldBe "Your text"
        e.state.selection shouldBe ClipSelection.Text(clip.id)
        e.state.textPanel?.kind shouldBe TextToolKind.Edit
        e.state.clipTools shouldBe listOf(
            ClipTool.TextEdit, ClipTool.TextStyle, ClipTool.TextAnim, ClipTool.Keyframes,
            ClipTool.Split, ClipTool.Duplicate, ClipTool.Delete,
        )
        e.vm.onEvent(EditorEvent.Undo)
        e.texts shouldBe emptyList()
    }

    @Test
    fun `typing is live and commits once with the words`() = runTest {
        val e = env()
        val clip = e.add()
        e.vm.onEvent(EditorEvent.TextPreview(clip.copy(text = "Day one")))
        e.engine.calls.last() shouldBe "text ${clip.id} Day one"
        e.texts.single().text shouldBe "Your text"
        e.vm.onEvent(EditorEvent.TextCommit(clip.copy(text = "Day one")))
        e.texts.single().text shouldBe "Day one"
        e.vm.onEvent(EditorEvent.Undo)
        e.texts.single().text shouldBe "Your text"
    }

    @Test
    fun `typing that was only previewed is committed when the panel closes`() = runTest {
        val e = env()
        val clip = e.add()
        e.vm.onEvent(EditorEvent.TextPreview(clip.copy(text = "Unsent")))
        e.vm.onEvent(EditorEvent.TextDismiss)
        e.texts.single().text shouldBe "Unsent"
        e.state.textPanel shouldBe null
    }

    @Test
    fun `style placement and animation commit as separate parts and reset restores defaults`() = runTest {
        val e = env()
        val clip = e.add()
        e.vm.onEvent(EditorEvent.OpenTextTool(TextToolKind.Style))
        e.vm.onEvent(EditorEvent.TextCommit(clip.copy(style = TextStyle(colorArgb = 0xFFFF0000.toInt()))))
        e.texts.single().style.colorArgb shouldBe 0xFFFF0000.toInt()
        e.vm.onEvent(EditorEvent.TextCommit(e.texts.single().copy(xPermille = 200, yPermille = 800, sizePermille = 90)))
        e.texts.single().xPermille shouldBe 200
        e.vm.onEvent(EditorEvent.TextCommit(e.texts.single().copy(animIn = TextAnimation(AnimKind.Fade, 400))))
        e.texts.single().animIn.kind shouldBe AnimKind.Fade
        e.vm.onEvent(EditorEvent.TextReset(TextToolKind.Style))
        e.texts.single().style shouldBe TextStyle()
        e.texts.single().text shouldBe "Your text"
        e.vm.onEvent(EditorEvent.TextReset(TextToolKind.Animation))
        e.texts.single().animIn.kind shouldBe AnimKind.None
    }

    @Test
    fun `timeline moves and trims are separate events and the project length follows`() = runTest {
        val e = env()
        val clip = e.add()
        e.vm.onEvent(EditorEvent.MoveTextClip(clip.id, 10_000))
        e.texts.single().startMs shouldBe 10_000
        e.state.playback.durationMs shouldBe 15_000
        e.vm.onEvent(EditorEvent.TrimTextClip(clip.id, 10_000, 11_000))
        e.texts.single().durationMs shouldBe 1_000
    }

    @Test
    fun `selecting another clip closes the text panel and split duplicate delete work on text`() = runTest {
        val e = env()
        val clip = e.add()
        e.vm.onEvent(EditorEvent.Select(ClipSelection.Video("c1")))
        e.state.textPanel shouldBe null
        e.vm.onEvent(EditorEvent.Select(ClipSelection.Text(clip.id)))
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.Duplicate))
        e.texts.size shouldBe 2
        e.vm.onEvent(EditorEvent.UseTool(ClipTool.Delete))
        e.texts.size shouldBe 1
    }
}
