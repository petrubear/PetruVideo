package com.petruvideo.feature.editor

import com.petruvideo.core.data.session.ProjectSession
import com.petruvideo.core.media.captions.CaptionMaker
import com.petruvideo.core.media.captions.CaptionResult
import com.petruvideo.core.media.captions.RecognitionAvailability
import com.petruvideo.core.media.pressure.ResourcePressureMonitor
import com.petruvideo.core.model.CaptionLine
import com.petruvideo.core.model.Timeline
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
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
class EditorViewModelCaptionsTest {
    @After
    fun tearDown() = Dispatchers.resetMain()

    private class FakeCaptions(var availability: RecognitionAvailability) : CaptionMaker {
        var gate = CompletableDeferred<CaptionResult>()
        var started = 0
        override suspend fun availability(languageTag: String) = availability
        override suspend fun languages() = listOf("en-US", "es-ES")
        override suspend fun make(timeline: Timeline, languageTag: String, onProgress: (Int) -> Unit): CaptionResult {
            started++
            onProgress(40)
            return gate.await()
        }
    }

    private class Env(val vm: EditorViewModel, val session: ProjectSession, val fake: FakeCaptions)

    private fun TestScope.env(availability: RecognitionAvailability = RecognitionAvailability.Available): Env {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val session = ProjectSession(FakeRepository(mapOf("p" to sampleLoaded())), backgroundScope)
        val fake = FakeCaptions(availability)
        val vm = EditorViewModel(
            session, FakePreviewEngine(), FakeAvailability(), FakeThumbnails(), ResourcePressureMonitor(),
            fakeVoiceOver(), captions = fake,
        )
        vm.open("p")
        vm.onEvent(EditorEvent.SelectTab(ToolbarTab.Text))
        vm.onEvent(EditorEvent.OpenCaptions)
        return Env(vm, session, fake)
    }

    private val Env.state get() = vm.state.value
    private val Env.texts get() = session.timeline.value!!.textLane

    @Test
    fun `the panel checks the phone and lists the installed languages`() = runTest {
        val e = env()
        e.state.captionsPanel!!.availability shouldBe RecognitionAvailability.Available
        e.state.captionsPanel!!.languages shouldBe listOf("en-US", "es-ES")
        e.state.captionsPanel!!.replace shouldBe true
    }

    @Test
    fun `Start shows progress and the result is added as captions in one step`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.CaptionsStart)
        e.state.captionsPanel!!.progress shouldBe 40
        val lines = listOf(CaptionLine(1_000, 2_000, "Hello"), CaptionLine(3_000, 4_000, "World"))
        e.fake.gate.complete(CaptionResult(lines, 1, 0, false))
        e.state.captionsPanel!!.progress shouldBe null
        e.texts.map { it.text to it.caption } shouldBe listOf("Hello" to true, "World" to true)
        e.state.captionsPanel!!.note shouldBe UiText.Joined(
            listOf(
                UiText.Plural(R.plurals.editor_captions_added, 2, listOf(2)),
                UiText.Plural(R.plurals.editor_phrases_not_recognised, 1, listOf(1)),
            ),
        )
        e.vm.onEvent(EditorEvent.Undo)
        e.texts shouldBe emptyList()
    }

    @Test
    fun `Cancel leaves the project alone`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.CaptionsStart)
        e.vm.onEvent(EditorEvent.CaptionsCancel)
        e.texts shouldBe emptyList()
        e.state.captionsPanel!!.progress shouldBe null
    }

    @Test
    fun `without speech nothing is added and the note says so`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.CaptionsStart)
        e.fake.gate.complete(CaptionResult(emptyList(), 0, 0, false))
        e.texts shouldBe emptyList()
        e.state.captionsPanel!!.note shouldBe UiText.Res(R.string.editor_there_was_no_speech_to)
    }

    @Test
    fun `when recognition is missing Start does nothing and the settings button opens the settings`() = runTest {
        val e = env(RecognitionAvailability.LanguageMissing("fr-FR", listOf("en-US")))
        e.vm.onEvent(EditorEvent.CaptionsStart)
        e.fake.started shouldBe 0
        e.state.captionsPanel!!.availability shouldBe RecognitionAvailability.LanguageMissing("fr-FR", listOf("en-US"))
    }

    @Test
    fun `closing the panel hides it`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.CaptionsDismiss)
        e.state.captionsPanel shouldBe null
    }
}
