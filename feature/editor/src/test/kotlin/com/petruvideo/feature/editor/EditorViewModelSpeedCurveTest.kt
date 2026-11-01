package com.petruvideo.feature.editor

import com.petruvideo.core.data.session.ProjectSession
import com.petruvideo.core.media.pressure.ResourcePressureMonitor
import com.petruvideo.core.model.SpeedCurve
import com.petruvideo.core.model.SpeedPoint
import com.petruvideo.core.model.SpeedPreset
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
class EditorViewModelSpeedCurveTest {
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
        vm.onEvent(EditorEvent.UseTool(ClipTool.Curve))
        return Env(vm, session)
    }

    private val Env.state get() = vm.state.value
    private val Env.clip get() = session.timeline.value!!.videoLane.first { it.id == "c1" }

    @Test
    fun `the tool opens a flat graph at the clip's speed with its length`() = runTest {
        val e = env()
        val panel = e.state.curvePanel!!
        panel.hasCurve shouldBe false
        panel.curve.points.map { it.speedMilli } shouldBe listOf(1_000, 1_000)
        panel.lengthMs shouldBe e.clip.lengthMs
    }

    @Test
    fun `a preset is one undo step that changes the length and Reset goes back to 1x`() = runTest {
        val e = env()
        val before = e.clip.lengthMs
        e.vm.onEvent(EditorEvent.SpeedCurvePreset(SpeedPreset.Bullet))
        (e.clip.lengthMs != before) shouldBe true
        e.state.curvePanel!!.hasCurve shouldBe true
        e.state.disabledTools.containsKey(ClipTool.Speed) shouldBe true
        e.vm.onEvent(EditorEvent.SpeedCurveReset)
        e.clip.speedCurve shouldBe null
        e.clip.lengthMs shouldBe before
        e.vm.onEvent(EditorEvent.Undo)
        (e.clip.speedCurve != null) shouldBe true
    }

    @Test
    fun `a draft only moves the readout and a commit applies the curve`() = runTest {
        val e = env()
        val length = e.clip.sourceLengthMs
        val slow = SpeedCurve(listOf(SpeedPoint(0, 500), SpeedPoint(length, 500)))
        e.vm.onEvent(EditorEvent.SpeedCurveDraft(slow))
        e.clip.speedCurve shouldBe null
        e.state.curvePanel!!.lengthMs shouldBe slow.timelineMs
        e.vm.onEvent(EditorEvent.SpeedCurveCommit(slow))
        e.clip.speedCurve shouldBe slow
        e.clip.lengthMs shouldBe slow.timelineMs
    }

    @Test
    fun `a curve that does not fit the clip says so and changes nothing`() = runTest {
        val e = env()
        val wrong = SpeedCurve(listOf(SpeedPoint(0, 1_000), SpeedPoint(e.clip.sourceLengthMs - 500, 1_000)))
        e.vm.onEvent(EditorEvent.SpeedCurveCommit(wrong))
        e.clip.speedCurve shouldBe null
        e.state.message shouldBe "That speed curve does not fit this clip"
    }
}
