package com.petruvideo.feature.editor

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/** Hardware keyboard shortcuts for the editor: Space, Left/Right, Delete, and the undo and redo chords. */
object KeyboardShortcuts {
    fun eventFor(key: Key, command: Boolean, shift: Boolean, state: EditorState): EditorEvent? = when {
        command && key == Key.Z -> if (shift) EditorEvent.Redo else EditorEvent.Undo
        command && key == Key.Y -> EditorEvent.Redo
        command -> null
        key == Key.Spacebar -> if (state.playback.isPlaying) EditorEvent.Pause else EditorEvent.Play
        key == Key.DirectionLeft -> EditorEvent.StepFrame(-1)
        key == Key.DirectionRight -> EditorEvent.StepFrame(1)
        key == Key.Delete || key == Key.Backspace ->
            EditorEvent.UseTool(ClipTool.Delete).takeIf { state.selection != null }
        else -> null
    }
}

/** Applies [KeyboardShortcuts] to key presses reaching this subtree; keys with no shortcut pass through. */
fun Modifier.editorShortcuts(state: EditorState, onEvent: (EditorEvent) -> Unit): Modifier = onPreviewKeyEvent {
    if (it.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
    val event = KeyboardShortcuts.eventFor(it.key, it.isCtrlPressed || it.isMetaPressed, it.isShiftPressed, state)
    event?.let(onEvent) != null
}
