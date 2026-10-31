package com.petruvideo.core.model.engine

import com.petruvideo.core.model.Timeline
import io.kotest.matchers.shouldBe
import org.junit.Test

class EditHistoryTest {
    private fun start() = Timeline(projectId = "p")

    private fun setPlayhead(ms: Long, label: String = "move $ms") = Command { t ->
        CommandResult.Applied(t.copy(playheadMs = ms), label)
    }

    private val failing = Command { CommandResult.Rejected(RejectReason.NotFound) }

    @Test
    fun `apply changes the current timeline and enables undo only`() {
        val h = EditHistory(start())
        h.canUndo shouldBe false
        h.canRedo shouldBe false

        h.apply(setPlayhead(10)) shouldBe CommandResult.Applied(start().copy(playheadMs = 10), "move 10")

        h.current.playheadMs shouldBe 10
        h.canUndo shouldBe true
        h.canRedo shouldBe false
    }

    @Test
    fun `undo and redo swap snapshots`() {
        val h = EditHistory(start())
        h.apply(setPlayhead(10))
        h.apply(setPlayhead(20))

        h.undo() shouldBe true
        h.current.playheadMs shouldBe 10
        h.canRedo shouldBe true

        h.redo() shouldBe true
        h.current.playheadMs shouldBe 20
        h.canRedo shouldBe false
    }

    @Test
    fun `undo and redo on empty stacks are no-ops`() {
        val h = EditHistory(start())
        h.undo() shouldBe false
        h.redo() shouldBe false
        h.current shouldBe start()
    }

    @Test
    fun `a new edit clears the redo stack`() {
        val h = EditHistory(start())
        h.apply(setPlayhead(10))
        h.undo()
        h.canRedo shouldBe true

        h.apply(setPlayhead(30))

        h.canRedo shouldBe false
        h.current.playheadMs shouldBe 30
    }

    @Test
    fun `a rejected command leaves state and stacks unchanged`() {
        val h = EditHistory(start())
        h.apply(setPlayhead(10))
        h.undo()

        h.apply(failing) shouldBe CommandResult.Rejected(RejectReason.NotFound)

        h.current shouldBe start()
        h.canUndo shouldBe false
        h.canRedo shouldBe true
    }

    @Test
    fun `capacity is 100 and the oldest snapshot is dropped first`() {
        val h = EditHistory(start())
        for (i in 1..150) h.apply(setPlayhead(i.toLong()))

        var undone = 0
        while (h.undo()) undone++

        undone shouldBe 100
        h.current.playheadMs shouldBe 50
    }

    @Test
    fun `labels follow the stacks`() {
        val h = EditHistory(start())
        h.apply(setPlayhead(10, "first"))
        h.apply(setPlayhead(20, "second"))
        h.undoLabel shouldBe "second"
        h.undo()
        h.redoLabel shouldBe "second"
        h.undoLabel shouldBe "first"
    }
}
