package com.petruvideo.feature.editor

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.withKeyDown
import com.petruvideo.core.designsystem.PetruTheme
import com.petruvideo.core.model.AudioClip
import com.petruvideo.core.model.AudioOrigin
import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.Fingerprint
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VideoClip
import com.petruvideo.feature.editor.ui.EditorScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Hardware keyboard shortcuts reach the editor through the real key event path. */
@OptIn(ExperimentalTestApi::class)
class KeyboardAndDragDropTest {
    @get:Rule
    val compose = createComposeRule()

    private val media = MediaItem(
        "a", MediaKind.Video, "content://x/a", "a.mp4", 10_000, 100, 100, 30f, true,
        Fingerprint(1, 1, 10_000, "video/mp4"), Availability.Available,
    )

    private val timeline = Timeline(
        projectId = "p",
        media = mapOf("a" to media, "c" to media.copy(id = "c", kind = MediaKind.Audio)),
        videoLane = listOf(VideoClip("c1", "a", 0, 4_000, true)),
        audioLane = listOf(AudioClip("a1", "c", 0, 2_000, 1_000, origin = AudioOrigin.Added)),
    )

    private fun show(selection: ClipSelection? = null): MutableList<EditorEvent> {
        val events = mutableListOf<EditorEvent>()
        val state = EditorState(
            header = EditorHeader("Trip", null, com.petruvideo.core.data.autosave.SaveStatus.Saved),
            timeline = timeline,
            selection = selection,
            playback = PlaybackState(durationMs = timeline.totalDurationMs),
        )
        compose.setContent { PetruTheme { EditorScreen(state, null, { events += it }, {}) } }
        compose.waitForIdle()
        return events
    }

    @Test
    fun spaceTogglesPlayback() {
        val events = show()
        compose.onRoot().performKeyInput { pressKey(Key.Spacebar) }
        assertEquals(EditorEvent.Play, events.last())
    }

    @Test
    fun arrowsStepFrames() {
        val events = show()
        compose.onRoot().performKeyInput { pressKey(Key.DirectionRight); pressKey(Key.DirectionLeft) }
        assertEquals(listOf(EditorEvent.StepFrame(1), EditorEvent.StepFrame(-1)), events.toList())
    }

    @Test
    fun deleteRemovesTheSelectedClip() {
        val events = show(ClipSelection.Video("c1"))
        compose.onRoot().performKeyInput { pressKey(Key.Delete) }
        assertEquals(EditorEvent.UseTool(ClipTool.Delete), events.last())
    }

    @Test
    fun undoAndRedoChords() {
        val events = show()
        compose.onRoot().performKeyInput {
            withKeyDown(Key.CtrlLeft) { pressKey(Key.Z) }
            withKeyDown(Key.CtrlLeft) { withKeyDown(Key.ShiftLeft) { pressKey(Key.Z) } }
        }
        assertEquals(listOf<EditorEvent>(EditorEvent.Undo, EditorEvent.Redo), events.toList())
    }
}
