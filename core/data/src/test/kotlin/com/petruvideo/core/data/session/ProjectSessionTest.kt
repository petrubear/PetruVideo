package com.petruvideo.core.data.session

import app.cash.turbine.test
import com.petruvideo.core.data.repo.LoadedProject
import com.petruvideo.core.model.Project
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.engine.Command
import com.petruvideo.core.model.engine.CommandResult
import com.petruvideo.core.model.engine.RejectReason
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectSessionTest {
    private val project = Project(id = "p", name = "Untitled 1", createdAt = 0, modifiedAt = 0)
    private val loaded = LoadedProject(project, Timeline(projectId = "p"))

    private fun movePlayhead(ms: Long) = Command { t -> CommandResult.Applied(t.copy(playheadMs = ms), "move $ms") }
    private val rejecting = Command { CommandResult.Rejected(RejectReason.OutOfRange) }

    @Test
    fun `open loads the project and exposes its timeline`() = runTest {
        val session = ProjectSession(FakeProjectRepository(mapOf("p" to loaded)), this)

        session.open("p") shouldBe true

        session.project.value shouldBe project
        session.timeline.value shouldBe loaded.timeline
    }

    @Test
    fun `open returns false for an unknown project`() = runTest {
        val session = ProjectSession(FakeProjectRepository(), this)
        session.open("nope") shouldBe false
        session.timeline.value shouldBe null
    }

    @Test
    fun `apply updates the timeline and enqueues one autosave`() = runTest {
        val repo = FakeProjectRepository(mapOf("p" to loaded))
        val session = ProjectSession(repo, this)
        session.open("p")

        session.apply(movePlayhead(10))
        session.apply(movePlayhead(20))

        session.timeline.value?.playheadMs shouldBe 20
        advanceTimeBy(1_000)
        repo.saves.size shouldBe 0
        advanceUntilIdle()
        repo.saves.single().second.playheadMs shouldBe 20
    }

    @Test
    fun `undo and redo move through history and report availability`() = runTest {
        val session = ProjectSession(FakeProjectRepository(mapOf("p" to loaded)), this)
        session.open("p")
        session.history.value.canUndo shouldBe false

        session.apply(movePlayhead(10))
        session.history.value.canUndo shouldBe true

        session.undo()
        session.timeline.value?.playheadMs shouldBe 0
        session.history.value.canRedo shouldBe true

        session.redo()
        session.timeline.value?.playheadMs shouldBe 10
        advanceUntilIdle()
    }

    @Test
    fun `a rejected command leaves the state unchanged and does not save`() = runTest {
        val repo = FakeProjectRepository(mapOf("p" to loaded))
        val session = ProjectSession(repo, this)
        session.open("p")

        session.apply(rejecting) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)

        session.timeline.value shouldBe loaded.timeline
        session.history.value.canUndo shouldBe false
        advanceUntilIdle()
        repo.saves.size shouldBe 0
    }

    @Test
    fun `the playhead moves without creating an undo step but is saved`() = runTest {
        val repo = FakeProjectRepository(mapOf("p" to loaded))
        val session = ProjectSession(repo, this)
        session.open("p")

        session.setPlayhead(500)

        session.timeline.value?.playheadMs shouldBe 500
        session.history.value.canUndo shouldBe false
        advanceUntilIdle()
        repo.saves.single().second.playheadMs shouldBe 500
    }

    @Test
    fun `state survives collectors being cancelled and re-attached`() = runTest {
        val session = ProjectSession(FakeProjectRepository(mapOf("p" to loaded)), this)
        session.open("p")
        session.apply(movePlayhead(42))

        session.timeline.test { awaitItem()?.playheadMs shouldBe 42 }
        session.timeline.test { awaitItem()?.playheadMs shouldBe 42 }
        advanceUntilIdle()
    }

    @Test
    fun `flush writes pending changes immediately`() = runTest {
        val repo = FakeProjectRepository(mapOf("p" to loaded))
        val session = ProjectSession(repo, this)
        session.open("p")
        session.apply(movePlayhead(5))

        session.flush()

        repo.saves.single().second.playheadMs shouldBe 5
        advanceUntilIdle()
    }
}
