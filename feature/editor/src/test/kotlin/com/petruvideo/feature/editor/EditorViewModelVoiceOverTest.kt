package com.petruvideo.feature.editor

import app.cash.turbine.test
import com.petruvideo.core.data.session.ProjectSession
import com.petruvideo.core.media.pressure.ResourcePressureMonitor
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelVoiceOverTest {
    @After
    fun tearDown() = Dispatchers.resetMain()

    private class Env(
        val vm: EditorViewModel,
        val engine: FakePreviewEngine,
        val session: ProjectSession,
        val recorder: FakeVoiceRecorder,
        val controller: VoiceOverController,
    )

    private fun TestScope.env(): Env {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val session = ProjectSession(FakeRepository(mapOf("p" to sampleLoaded())), backgroundScope)
        val engine = FakePreviewEngine()
        val recorder = FakeVoiceRecorder()
        val controller = fakeVoiceOver(recorder)
        val vm = EditorViewModel(
            session, engine, FakeAvailability(), FakeThumbnails(), ResourcePressureMonitor(), controller,
        )
        vm.open("p")
        return Env(vm, engine, session, recorder, controller)
    }

    private val Env.state get() = vm.state.value
    private val Env.audio get() = session.timeline.value!!.audioLane

    /** Taps Record at [at], allows the microphone, and runs the three second count-in. */
    private fun TestScope.record(e: Env, at: Long) {
        e.vm.onEvent(EditorEvent.SeekTo(at))
        e.vm.onEvent(EditorEvent.MicrophoneResult(MicAccess.Granted))
        advanceTimeBy(3_001)
        runCurrent()
    }

    @Test
    fun `the count-in shows 3, 2, 1 and then recording and playback start from the playhead`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.SeekTo(8_000))
        e.vm.onEvent(EditorEvent.MicrophoneResult(MicAccess.Granted))
        e.state.voiceOver shouldBe VoiceOverUi.CountIn(3)
        advanceTimeBy(1_001)
        e.state.voiceOver shouldBe VoiceOverUi.CountIn(2)
        advanceTimeBy(1_000)
        e.state.voiceOver shouldBe VoiceOverUi.CountIn(1)
        e.recorder.files.size shouldBe 0
        advanceTimeBy(1_000)
        runCurrent()
        e.state.voiceOver shouldBe VoiceOverUi.Recording(0, 0f)
        e.recorder.files.size shouldBe 1
        e.engine.calls.takeLast(2) shouldBe listOf("seek 8000", "play")
        e.recorder.tick(2_500, 0.4f)
        e.state.voiceOver shouldBe VoiceOverUi.Recording(2_500, 0.4f)
    }

    @Test
    fun `Stop adds one selected audio clip where the take started, and Undo removes it`() = runTest {
        val e = env()
        record(e, 8_000)
        e.recorder.stopMs = 5_000
        e.vm.onEvent(EditorEvent.StopVoiceOver)
        e.state.voiceOver shouldBe null
        val clip = e.audio.single { it.timelineStartMs == 8_000L }
        clip.lengthMs shouldBe 5_000
        e.state.selection shouldBe ClipSelection.Audio(clip.id)
        e.engine.calls.last() shouldBe "pause"
        e.controller.files.pendingTake() shouldBe null
        e.vm.onEvent(EditorEvent.Undo)
        e.audio.none { it.timelineStartMs == 8_000L } shouldBe true
        e.vm.onEvent(EditorEvent.Redo)
        e.audio.any { it.timelineStartMs == 8_000L } shouldBe true
    }

    @Test
    fun `cancelling the count-in records and adds nothing`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.MicrophoneResult(MicAccess.Granted))
        advanceTimeBy(1_500)
        e.vm.onEvent(EditorEvent.CancelVoiceOver)
        advanceTimeBy(5_000)
        runCurrent()
        e.state.voiceOver shouldBe null
        e.recorder.files shouldBe emptyList()
        e.audio.size shouldBe 1
    }

    @Test
    fun `a take stops at the next audio clip with a message and keeps what was recorded`() = runTest {
        val e = env()
        // The sample has an audio clip at 5 to 7 s, so a take from 3 s has 2 s of room.
        record(e, 3_000)
        e.recorder.stopMs = 2_000
        e.recorder.tick(2_000)
        runCurrent()
        e.state.voiceOver shouldBe null
        e.audio.single { it.timelineStartMs == 3_000L }.lengthMs shouldBe 2_000
        e.state.message shouldBe "Stopped at the next clip"
    }

    @Test
    fun `no room before the next clip means no recording`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.SeekTo(6_000))
        e.vm.onEvent(EditorEvent.MicrophoneResult(MicAccess.Granted))
        e.state.voiceOver shouldBe null
        e.state.message shouldBe "No room here before the next audio clip"
    }

    @Test
    fun `a take under half a second is discarded and the microphone in use is explained`() = runTest {
        val e = env()
        record(e, 8_000)
        e.recorder.stopMs = 300
        e.vm.onEvent(EditorEvent.StopVoiceOver)
        e.state.message shouldBe "Too short"
        e.audio.size shouldBe 1
        e.recorder.files.single().exists() shouldBe false

        e.recorder.canStart = false
        e.vm.onEvent(EditorEvent.DismissMessage)
        record(e, 8_000)
        e.state.voiceOver shouldBe null
        e.state.message shouldBe "The microphone is in use"
    }

    @Test
    fun `editing is locked during a take except Stop`() = runTest {
        val e = env()
        record(e, 8_000)
        e.vm.onEvent(EditorEvent.AddText)
        e.vm.onEvent(EditorEvent.SeekTo(1_000))
        e.session.timeline.value!!.textLane shouldBe emptyList()
        e.engine.calls.contains("seek 1000") shouldBe false
        e.recorder.stopMs = 2_000
        e.vm.onEvent(EditorEvent.StopVoiceOver)
        e.vm.onEvent(EditorEvent.AddText)
        e.session.timeline.value!!.textLane.size shouldBe 1
    }

    @Test
    fun `an interruption keeps the take and a system stop of the recorder does the same`() = runTest {
        val e = env()
        record(e, 8_000)
        e.recorder.stopMs = 1_500
        e.vm.onEvent(EditorEvent.VoiceOverInterrupted)
        e.audio.single { it.timelineStartMs == 8_000L }.lengthMs shouldBe 1_500

        record(e, 9_500)
        e.recorder.onInterrupted(1_000)
        e.audio.single { it.timelineStartMs == 9_500L }.lengthMs shouldBe 1_000
        e.state.voiceOver shouldBe null
    }

    @Test
    fun `permission answers explain a refusal and a blocked microphone opens the settings next time`() = runTest {
        val e = env()
        e.vm.onEvent(EditorEvent.MicrophoneResult(MicAccess.Denied))
        e.state.message shouldBe "A voice-over needs the microphone"
        e.vm.onEvent(EditorEvent.MicrophoneResult(MicAccess.Blocked))
        e.state.message shouldBe "Microphone access is off. Tap Record again to open Settings"
        e.vm.effects.test {
            e.vm.onEvent(EditorEvent.RecordVoiceOver)
            awaitItem() shouldBe EditorEffect.OpenAppSettings
        }
    }

    @Test
    fun `the first tap on Record asks for the microphone`() = runTest {
        val e = env()
        e.vm.effects.test {
            e.vm.onEvent(EditorEvent.RecordVoiceOver)
            awaitItem() shouldBe EditorEffect.RequestMicrophone
        }
    }

    @Test
    fun `a take left by a lost process comes back as a clip when its project opens`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val session = ProjectSession(FakeRepository(mapOf("p" to sampleLoaded())), backgroundScope)
        val controller = fakeVoiceOver()
        val file = controller.files.newFile().also { it.writeBytes(ByteArray(24_000)) }
        controller.files.openTake("p", 8_000, file)
        val vm = EditorViewModel(
            session, FakePreviewEngine(), FakeAvailability(), FakeThumbnails(), ResourcePressureMonitor(), controller,
        )
        vm.open("p")
        val clip = session.timeline.value!!.audioLane.single { it.timelineStartMs == 8_000L }
        clip.lengthMs shouldBe 2_000
        vm.state.value.message shouldBe "Recovered your voice-over"
        controller.files.pendingTake() shouldBe null
    }

    @Test
    fun `a take for another project stays and one that is too short is removed`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val session = ProjectSession(FakeRepository(mapOf("p" to sampleLoaded())), backgroundScope)
        val controller = fakeVoiceOver()
        val other = controller.files.newFile().also { it.writeBytes(ByteArray(24_000)) }
        controller.files.openTake("other", 0, other)
        val vm = EditorViewModel(
            session, FakePreviewEngine(), FakeAvailability(), FakeThumbnails(), ResourcePressureMonitor(), controller,
        )
        vm.open("p")
        controller.files.pendingTake()?.projectId shouldBe "other"
        session.timeline.value!!.audioLane.size shouldBe 1

        val tiny = controller.files.newFile().also { it.writeBytes(ByteArray(100)) }
        controller.files.openTake("p", 8_000, tiny)
        vm.open("p")
        tiny.exists() shouldBe false
        session.timeline.value!!.audioLane.size shouldBe 1
    }
}
