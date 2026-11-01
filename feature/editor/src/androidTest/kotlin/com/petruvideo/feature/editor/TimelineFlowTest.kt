package com.petruvideo.feature.editor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.down
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.up
import com.petruvideo.core.data.autosave.SaveStatus
import com.petruvideo.core.designsystem.PetruTheme
import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.AudioClip
import com.petruvideo.core.model.AudioOrigin
import com.petruvideo.core.model.Fingerprint
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VideoClip
import com.petruvideo.feature.editor.ui.EditorScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TimelineFlowTest {
    @get:Rule
    val compose = createComposeRule()

    private fun media(id: String, kind: MediaKind = MediaKind.Video, available: Boolean = true) = MediaItem(
        id = id, kind = kind, uri = "content://x/$id", displayName = "$id.mp4",
        durationMs = if (kind == MediaKind.Photo) null else 10_000,
        width = 100, height = 100, frameRate = 30f, hasAudio = true,
        fingerprint = Fingerprint(1, 1, 10_000, "video/mp4"),
        availability = if (available) Availability.Available else Availability.Missing,
    )

    private fun state(missing: Set<String> = emptySet(), empty: Boolean = false): EditorState {
        val items = listOf(media("a"), media("b", available = "b" !in missing), media("c"), media("p", MediaKind.Photo))
        val timeline = if (empty) {
            Timeline("p")
        } else {
            Timeline(
                projectId = "p",
                media = items.associateBy { it.id },
                videoLane = listOf(
                    VideoClip("c1", "a", 0, 4_000, true),
                    VideoClip("c2", "b", 0, 4_000, true),
                    VideoClip("c3", "p", 0, 5_000, false),
                ),
                audioLane = listOf(AudioClip("a1", "c", 0, 2_000, 1_000, origin = AudioOrigin.Detached)),
            )
        }
        return EditorState(
            header = EditorHeader("Trip", "1080p · 30 fps", SaveStatus.Saved),
            timeline = timeline,
            playback = PlaybackState(positionMs = 0, durationMs = timeline.totalDurationMs),
            canUndo = true,
            missingMedia = missing,
        )
    }

    private fun show(state: EditorState): MutableList<EditorEvent> {
        val events = mutableListOf<EditorEvent>()
        compose.setContent { PetruTheme { EditorScreen(state, player = null, onEvent = { events += it }, onBack = {}) } }
        return events
    }

    @Test
    fun tappingAClipSelectsIt() {
        val events = show(state())
        // At the start the playhead sits on clip 1; later clips are scrolled into view by seeking.
        compose.onNodeWithContentDescription("Clip 1, a.mp4").performClick()
        assertEquals(EditorEvent.Select(ClipSelection.Video("c1")), events.last())
    }

    @Test
    fun tappingADetachedAudioClipSelectsIt() {
        val events = show(state())
        compose.onNodeWithContentDescription("Detached audio, c.mp4").performClick()
        assertEquals(EditorEvent.Select(ClipSelection.Audio("a1")), events.last())
    }

    @Test
    fun theTransportButtonsSendTheirEvents() {
        val events = show(state())
        compose.onNodeWithContentDescription("Play").performClick()
        compose.onNodeWithContentDescription("Undo").performClick()
        compose.onNodeWithContentDescription("Fullscreen preview").performClick()
        assertEquals(
            listOf(EditorEvent.Play, EditorEvent.Undo, EditorEvent.SetFullscreen(true)),
            events.toList(),
        )
    }

    @Test
    fun addClipOpensAddMedia() {
        val events = show(state())
        compose.onNodeWithContentDescription("Add clip").performClick()
        assertEquals(EditorEvent.AddClip, events.last())
    }

    @Test
    fun scrubbingTheTimelineSeeksForward() {
        val events = show(state())
        compose.onNodeWithContentDescription("Clip 1, a.mp4").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        val seeks = events.filterIsInstance<EditorEvent.SeekTo>()
        assertTrue("expected a seek, got $events", seeks.isNotEmpty())
        assertTrue(seeks.last().ms > 0)
    }

    @Test
    fun longPressAndDragMovesAClipLater() {
        val events = show(state())
        compose.onNodeWithContentDescription("Clip 1, a.mp4").performTouchInput {
            down(center)
            advanceEventTime(700)
            moveBy(androidx.compose.ui.geometry.Offset(1_200f, 0f), delayMillis = 100)
            up()
        }
        compose.waitForIdle()
        val move = events.filterIsInstance<EditorEvent.MoveClip>().lastOrNull()
        assertTrue("expected a move, got $events", move != null && move.clipId == "c1" && move.newIndex > 0)
    }

    @Test
    fun missingMediaOffersRelinkAndRemove() {
        val events = show(state(missing = setOf("b")))
        compose.onNodeWithText("b.mp4 is missing").assertIsDisplayed()
        compose.onNodeWithText("Relink").performClick()
        compose.onNodeWithText("Remove").performClick()
        assertEquals(
            listOf(EditorEvent.RequestRelink("b"), EditorEvent.RemoveMissing("b")),
            events.toList(),
        )
    }

    @Test
    fun everyMainToolIsActive() {
        val events = show(state())
        compose.onNodeWithContentDescription("Effects, coming soon").assertDoesNotExist()
        compose.onNodeWithContentDescription("Effects").performClick()
        assertEquals(EditorEvent.SelectTab(ToolbarTab.Effects), events.last())
        compose.onNodeWithContentDescription("Audio").performClick()
        assertEquals(EditorEvent.SelectTab(ToolbarTab.Audio), events.last())
    }

    @Test
    fun anEmptyProjectShowsTheEmptyStateAndStillOffersAddClip() {
        show(state(empty = true))
        compose.onNodeWithText("Add your first clip").assertIsDisplayed()
        compose.onNodeWithContentDescription("Add clip").assertIsDisplayed()
    }
}
