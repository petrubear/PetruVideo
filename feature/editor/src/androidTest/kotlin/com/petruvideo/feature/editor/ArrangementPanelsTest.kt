package com.petruvideo.feature.editor

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.petruvideo.core.data.autosave.SaveStatus
import com.petruvideo.core.designsystem.PetruTheme
import com.petruvideo.core.designsystem.adaptive.Arrangement
import com.petruvideo.core.designsystem.adaptive.HingeBounds
import com.petruvideo.core.designsystem.adaptive.LocalArrangement
import com.petruvideo.core.model.AudioClip
import com.petruvideo.core.model.AudioOrigin
import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.Fingerprint
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.Reverb
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VideoClip
import com.petruvideo.feature.editor.ui.EditorScreen
import org.junit.Rule
import org.junit.Test

/** An open Speed, Volume, or Reverb panel keeps its values as the arrangement changes (fold, unfold, rotate). */
class ArrangementPanelsTest {
    @get:Rule
    val compose = createComposeRule()

    private val media = MediaItem(
        "a", MediaKind.Video, "content://x/a", "a.mp4", 10_000, 100, 100, 30f, true,
        Fingerprint(1, 1, 10_000, "video/mp4"), Availability.Available,
    )
    private val timeline = Timeline(
        projectId = "p",
        media = mapOf("a" to media, "c" to media.copy(id = "c", kind = MediaKind.Audio)),
        videoLane = listOf(VideoClip("c1", "a", 0, 4_000, true, speedMilli = 1500)),
        audioLane = listOf(AudioClip("a1", "c", 0, 2_000, 1_000, origin = AudioOrigin.Detached)),
    )
    private val tools = listOf(
        ClipTool.Split, ClipTool.Speed, ClipTool.Volume, ClipTool.Mute, ClipTool.Reverb, ClipTool.Detach,
        ClipTool.Duplicate, ClipTool.Delete,
    )

    private fun state(sheet: SheetState? = null, panel: ReverbPanelState? = null) = EditorState(
        header = EditorHeader("Trip", "1080p · 30 fps", SaveStatus.Saved),
        timeline = timeline,
        selection = ClipSelection.Video("c1"),
        playback = PlaybackState(durationMs = timeline.totalDurationMs),
        clipTools = tools,
        sheet = sheet,
        reverbPanel = panel,
        selectedReverb = Reverb(enabled = true),
        projectMedia = timeline.media.values.toList(),
    )

    private val arrangements = listOf(
        Arrangement.Compact(wideContent = false),
        Arrangement.Compact(wideContent = true),
        Arrangement.Expanded(hinge = null),
        Arrangement.Tabletop(HingeBounds(0, 400, 1_000, 420)),
    )

    private fun throughEveryArrangement(state: EditorState, visible: String) {
        var arrangement by mutableStateOf<Arrangement>(arrangements.first())
        compose.setContent {
            CompositionLocalProvider(LocalArrangement provides arrangement) {
                PetruTheme { EditorScreen(state, null, {}, {}) }
            }
        }
        arrangements.forEach {
            arrangement = it
            compose.waitForIdle()
            compose.onNodeWithText(visible).assertIsDisplayed()
        }
    }

    @Test
    fun speedSheetStaysOpen() =
        throughEveryArrangement(state(sheet = SheetState(ClipTool.Speed, "c1", "a.mp4", 1500, 4_000)), "1.50x")

    @Test
    fun reverbPanelStaysOpen() =
        throughEveryArrangement(state(panel = ReverbPanelState("c1", "a.mp4", Reverb(enabled = true))), "Decay time")
}
