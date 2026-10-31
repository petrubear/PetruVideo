package com.petruvideo.feature.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.down
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.up
import com.petruvideo.core.data.autosave.SaveStatus
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Trim handles, the clip tool bars, and the undo/redo buttons, driven through the stateless editor screen. */
class TrimAndAudioFlowTest {
    @get:Rule
    val compose = createComposeRule()

    private fun media(id: String, kind: MediaKind = MediaKind.Video) = MediaItem(
        id = id, kind = kind, uri = "content://x/$id", displayName = "$id.mp4",
        durationMs = if (kind == MediaKind.Photo) null else 10_000,
        width = 100, height = 100, frameRate = 30f, hasAudio = true,
        fingerprint = Fingerprint(1, 1, 10_000, "video/mp4"), availability = Availability.Available,
    )

    private val timeline = Timeline(
        projectId = "p",
        media = listOf(media("a"), media("c"), media("p", MediaKind.Photo)).associateBy { it.id },
        videoLane = listOf(
            VideoClip("c1", "a", 0, 6_000, attachedAudio = true),
            VideoClip("c2", "p", 0, 5_000, attachedAudio = false),
        ),
        audioLane = listOf(AudioClip("a1", "c", 0, 3_000, timelineStartMs = 1_000, origin = AudioOrigin.Detached)),
    )

    private fun show(
        selection: ClipSelection?,
        tools: List<ClipTool> = emptyList(),
        canUndo: Boolean = false,
        canRedo: Boolean = false,
        photoFirst: Boolean = false,
    ): MutableList<EditorEvent> {
        val events = mutableListOf<EditorEvent>()
        val state = EditorState(
            header = EditorHeader("Trip", "1080p · 30 fps", SaveStatus.Saved),
            timeline = if (photoFirst) timeline.copy(videoLane = timeline.videoLane.reversed()) else timeline,
            selection = selection,
            playback = PlaybackState(durationMs = timeline.totalDurationMs),
            canUndo = canUndo,
            canRedo = canRedo,
            clipTools = tools,
        )
        compose.setContent { PetruTheme { EditorScreen(state, player = null, onEvent = { events += it }, onBack = {}) } }
        return events
    }

    private val videoTools = listOf(
        ClipTool.Split, ClipTool.Speed, ClipTool.Volume, ClipTool.Detach, ClipTool.Duplicate, ClipTool.Delete,
    )
    private val audioTools = listOf(ClipTool.Split, ClipTool.Mute, ClipTool.Replace, ClipTool.Duplicate, ClipTool.Delete)

    @Test
    fun draggingTheStartHandleCommitsOneTrim() {
        val events = show(ClipSelection.Video("c1"), videoTools)
        compose.onNodeWithContentDescription("Trim start", useUnmergedTree = true).performTouchInput {
            down(center)
            repeat(6) { moveBy(Offset(20f, 0f), delayMillis = 50) }
            up()
        }
        compose.waitForIdle()
        val trim = events.filterIsInstance<EditorEvent.TrimClip>().single()
        assertEquals("c1", trim.clipId)
        assertEquals(true, trim.start)
        assertTrue("expected a later start, got ${trim.newSourceMs}", trim.newSourceMs > 0)
    }

    @Test
    fun theBadgeShowsTheSelectedClipLength() {
        show(ClipSelection.Video("c1"), videoTools)
        compose.onNodeWithText("0:06.0").assertIsDisplayed()
    }

    @Test
    fun aPhotoStartHandleCanLengthenTheClip() {
        val events = show(ClipSelection.Video("c2"), videoTools, photoFirst = true)
        compose.onNodeWithContentDescription("Trim start", useUnmergedTree = true).performTouchInput {
            down(center)
            repeat(8) { moveBy(Offset(-25f, 0f), delayMillis = 50) }
            up()
        }
        compose.waitForIdle()
        val trim = events.filterIsInstance<EditorEvent.TrimClip>().single()
        assertTrue("expected a longer photo, got ${trim.newSourceMs}", trim.newSourceMs < 0)
    }

    @Test
    fun theVideoToolBarOffersDetachSpeedAndVolume() {
        val events = show(ClipSelection.Video("c1"), videoTools)
        compose.onNodeWithContentDescription("Speed").assertIsDisplayed()
        compose.onNodeWithContentDescription("Volume").assertIsDisplayed()
        compose.onNodeWithContentDescription("Split").performClick()
        compose.onNodeWithContentDescription("Speed").performClick()
        compose.onNodeWithContentDescription("Detach").performClick()
        compose.onNodeWithContentDescription("Duplicate").performClick()
        compose.onNodeWithContentDescription("Delete").performClick()
        assertEquals(
            listOf(ClipTool.Split, ClipTool.Speed, ClipTool.Detach, ClipTool.Duplicate, ClipTool.Delete)
                .map { EditorEvent.UseTool(it) },
            events.toList(),
        )
    }

    @Test
    fun theAudioToolBarHasMuteAndReplaceButNoDetach() {
        val events = show(ClipSelection.Audio("a1"), audioTools)
        compose.onNodeWithContentDescription("Detach").assertDoesNotExist()
        compose.onNodeWithContentDescription("Mute").performClick()
        compose.onNodeWithContentDescription("Replace").performClick()
        assertEquals(listOf(ClipTool.Mute, ClipTool.Replace).map { EditorEvent.UseTool(it) }, events.toList())
    }

    @Test
    fun undoAndRedoButtonsFollowAvailability() {
        val events = show(null, canUndo = true, canRedo = false)
        compose.onNodeWithContentDescription("Undo").performClick()
        compose.onNodeWithContentDescription("Redo").performClick()
        assertEquals(listOf<EditorEvent>(EditorEvent.Undo), events.toList())
    }
}
