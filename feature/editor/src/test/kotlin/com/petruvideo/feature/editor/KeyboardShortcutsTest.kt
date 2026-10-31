package com.petruvideo.feature.editor

import androidx.compose.ui.input.key.Key
import io.kotest.matchers.shouldBe
import org.junit.Test

class KeyboardShortcutsTest {
    private val idle = EditorState()
    private val playing = EditorState(playback = PlaybackState(isPlaying = true))
    private val selected = EditorState(selection = ClipSelection.Video("c1"))

    private fun key(key: Key, command: Boolean = false, shift: Boolean = false, state: EditorState = idle) =
        KeyboardShortcuts.eventFor(key, command, shift, state)

    @Test
    fun `space toggles play and pause`() {
        key(Key.Spacebar) shouldBe EditorEvent.Play
        key(Key.Spacebar, state = playing) shouldBe EditorEvent.Pause
    }

    @Test
    fun `arrows step one frame`() {
        key(Key.DirectionLeft) shouldBe EditorEvent.StepFrame(-1)
        key(Key.DirectionRight) shouldBe EditorEvent.StepFrame(1)
    }

    @Test
    fun `delete removes the selection and does nothing without one`() {
        key(Key.Delete, state = selected) shouldBe EditorEvent.UseTool(ClipTool.Delete)
        key(Key.Backspace, state = selected) shouldBe EditorEvent.UseTool(ClipTool.Delete)
        key(Key.Delete) shouldBe null
    }

    @Test
    fun `undo and redo chords`() {
        key(Key.Z, command = true) shouldBe EditorEvent.Undo
        key(Key.Z, command = true, shift = true) shouldBe EditorEvent.Redo
        key(Key.Y, command = true) shouldBe EditorEvent.Redo
    }

    @Test
    fun `other chords and keys pass through`() {
        key(Key.Spacebar, command = true) shouldBe null
        key(Key.A) shouldBe null
    }
}
