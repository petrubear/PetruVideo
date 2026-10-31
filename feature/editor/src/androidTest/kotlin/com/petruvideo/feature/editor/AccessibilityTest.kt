package com.petruvideo.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runAndroidComposeUiTest
import androidx.compose.ui.test.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
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
import org.junit.Test

/** Runs the Android accessibility checks (touch target size, labels, contrast) over the editor. */
@OptIn(ExperimentalTestApi::class)
class AccessibilityTest {
    private val media = MediaItem(
        "a", MediaKind.Video, "content://x/a", "a.mp4", 10_000, 100, 100, 30f, true,
        Fingerprint(1, 1, 10_000, "video/mp4"), Availability.Available,
    )
    private val timeline = Timeline(
        projectId = "p",
        media = mapOf("a" to media, "c" to media.copy(id = "c", kind = MediaKind.Audio)),
        videoLane = listOf(VideoClip("c1", "a", 0, 4_000, true)),
        audioLane = listOf(AudioClip("a1", "c", 0, 2_000, 1_000, origin = AudioOrigin.Detached)),
    )

    private fun check(selection: ClipSelection?, tools: List<ClipTool>) {
        val state = EditorState(
            header = EditorHeader("Trip", "1080p · 30 fps", SaveStatus.Saved),
            timeline = timeline,
            selection = selection,
            playback = PlaybackState(durationMs = timeline.totalDurationMs),
            canUndo = true,
            clipTools = tools,
        )
        runAndroidComposeUiTest<ComponentActivity> {
            enableAccessibilityChecks()
            setContent { PetruTheme { EditorScreen(state, null, {}, {}) } }
            onRoot().tryPerformAccessibilityChecks()
        }
    }

    @Test
    fun editorWithNothingSelected() = check(null, emptyList())

    @Test
    fun editorWithAVideoClipSelected() = check(
        ClipSelection.Video("c1"),
        listOf(ClipTool.Split, ClipTool.Speed, ClipTool.Volume, ClipTool.Detach, ClipTool.Duplicate, ClipTool.Delete),
    )

    @Test
    fun editorWithDetachedAudioSelected() = check(
        ClipSelection.Audio("a1"),
        listOf(ClipTool.Split, ClipTool.Mute, ClipTool.Replace, ClipTool.Duplicate, ClipTool.Delete),
    )
}
