package com.petruvideo.feature.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.down
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.up
import com.petruvideo.core.data.autosave.SaveStatus
import com.petruvideo.core.designsystem.PetruTheme
import com.petruvideo.core.model.ProjectFormat
import com.petruvideo.core.model.TextClip
import com.petruvideo.core.model.Timeline
import com.petruvideo.feature.editor.ui.PreviewWithText
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TextPreviewLayerTest {
    @get:Rule
    val compose = createComposeRule()

    private val clip = TextClip("t1", "Drag me", 0, 4_000)

    @Test
    fun draggingMovesTheClipLiveAndCommitsOnRelease() {
        val events = mutableListOf<EditorEvent>()
        val state = EditorState(
            header = EditorHeader("Trip", "1080p · 30 fps", SaveStatus.Saved),
            timeline = Timeline("p", format = ProjectFormat(1920, 1080, 30), textLane = listOf(clip)),
            selection = ClipSelection.Text("t1"),
            selectedText = clip,
            playback = PlaybackState(positionMs = 1_000, durationMs = 4_000),
        )
        compose.setContent {
            PetruTheme { PreviewWithText(state, null, { events += it }, Modifier.fillMaxWidth().height(300.dp)) }
        }
        compose.onNodeWithContentDescription("Selected text, drag to move, pinch to resize").performTouchInput {
            down(center)
            repeat(6) { moveBy(Offset(-12f, 14f), delayMillis = 30) }
            up()
        }
        compose.waitForIdle()
        val previews = events.filterIsInstance<EditorEvent.TextPreview>()
        val commit = events.filterIsInstance<EditorEvent.TextCommit>().last().clip
        assertTrue("events $events", previews.isNotEmpty())
        assertTrue("commit $commit", commit.xPermille < 500)
        assertTrue("commit $commit", commit.yPermille > 500)
        assertTrue(events.last() is EditorEvent.TextCommit)
    }
}
