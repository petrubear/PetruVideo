package com.petruvideo.core.model.engine

import com.petruvideo.core.model.Timeline

/** Snapshot-based undo and redo. Rejected commands never touch state. */
class EditHistory(initial: Timeline, private val capacity: Int = DEFAULT_CAPACITY) {
    private class Entry(val timeline: Timeline, val label: String)

    private val undoStack = ArrayDeque<Entry>()
    private val redoStack = ArrayDeque<Entry>()

    var current: Timeline = initial
        private set

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
    val undoLabel: String? get() = undoStack.lastOrNull()?.label
    val redoLabel: String? get() = redoStack.lastOrNull()?.label

    fun apply(command: Command): CommandResult {
        val result = command.apply(current)
        if (result is CommandResult.Applied) {
            undoStack.addLast(Entry(current, result.label))
            if (undoStack.size > capacity) undoStack.removeFirst()
            redoStack.clear()
            current = result.timeline
        }
        return result
    }

    fun undo(): Boolean {
        val entry = undoStack.removeLastOrNull() ?: return false
        redoStack.addLast(Entry(current, entry.label))
        current = entry.timeline
        return true
    }

    fun redo(): Boolean {
        val entry = redoStack.removeLastOrNull() ?: return false
        undoStack.addLast(Entry(current, entry.label))
        current = entry.timeline
        return true
    }

    /** Replaces the current timeline without recording an undo step (used for playhead moves). */
    fun replaceCurrent(timeline: Timeline) {
        current = timeline
    }

    private companion object {
        const val DEFAULT_CAPACITY = 100
    }
}
