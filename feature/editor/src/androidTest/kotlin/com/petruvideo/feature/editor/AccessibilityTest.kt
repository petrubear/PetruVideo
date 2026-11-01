package com.petruvideo.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.unit.Density
import com.petruvideo.core.model.AudioFx
import com.petruvideo.core.model.Fade
import com.petruvideo.core.model.Reverb
import com.petruvideo.core.model.ReverbPreset
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

    private fun check(
        selection: ClipSelection?,
        tools: List<ClipTool>,
        sheet: SheetState? = null,
        reverbPanel: ReverbPanelState? = null,
        audioPanel: AudioPanelState? = null,
        fontScale: Float = 1f,
    ) {
        val state = EditorState(
            header = EditorHeader("Trip", "1080p · 30 fps", SaveStatus.Saved),
            timeline = timeline,
            selection = selection,
            playback = PlaybackState(durationMs = timeline.totalDurationMs),
            canUndo = true,
            clipTools = tools,
            sheet = sheet,
            reverbPanel = reverbPanel,
            audioPanel = audioPanel,
            selectedReverb = reverbPanel?.reverb,
        )
        runAndroidComposeUiTest<ComponentActivity> {
            enableAccessibilityChecks()
            setContent {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                    PetruTheme { EditorScreen(state, null, {}, {}) }
                }
            }
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

    private val videoTools = listOf(
        ClipTool.Split, ClipTool.Speed, ClipTool.Audio, ClipTool.Detach,
        ClipTool.Duplicate, ClipTool.Delete,
    )

    @Test
    fun speedSheet() = check(
        ClipSelection.Video("c1"), videoTools,
        sheet = SheetState(ClipTool.Speed, "c1", "a.mp4", 1500, 4_000),
    )

    @Test
    fun volumeSheetWithBoost() = check(
        ClipSelection.Audio("a1"), videoTools,
        sheet = SheetState(ClipTool.Volume, "a1", "c.mp3", 150, 2_000),
    )

    @Test
    fun reverbPanel() = check(
        ClipSelection.Video("c1"), videoTools,
        reverbPanel = ReverbPanelState("c1", "a.mp4", Reverb(true, ReverbPreset.Custom, 40, 2_400, 65, 45, 55)),
    )

    @Test
    fun speedSheetWithLargeFont() = check(
        ClipSelection.Video("c1"), videoTools,
        sheet = SheetState(ClipTool.Speed, "c1", "a.mp4", 1500, 4_000), fontScale = 2f,
    )

    @Test
    fun reverbPanelWithLargeFont() = check(
        ClipSelection.Video("c1"), videoTools,
        reverbPanel = ReverbPanelState("c1", "a.mp4", Reverb()), fontScale = 2f,
    )

    private fun audio(kind: AudioToolKind) = AudioPanelState(
        kind, "c1", "a.mp4", AudioFx(fade = Fade(500, 500)), 4_000, 100, false, Reverb(),
    )

    @Test
    fun audioToolList() = check(ClipSelection.Video("c1"), videoTools, audioPanel = audio(AudioToolKind.List))

    @Test
    fun fadePanel() = check(ClipSelection.Video("c1"), videoTools, audioPanel = audio(AudioToolKind.Fade))

    @Test
    fun noisePanel() = check(ClipSelection.Video("c1"), videoTools, audioPanel = audio(AudioToolKind.Noise))

    @Test
    fun equalizerPanel() = check(ClipSelection.Video("c1"), videoTools, audioPanel = audio(AudioToolKind.Eq))

    @Test
    fun voicePanel() = check(ClipSelection.Video("c1"), videoTools, audioPanel = audio(AudioToolKind.Voice))

    @Test
    fun audioListWithLargeFont() = check(
        ClipSelection.Video("c1"), videoTools, audioPanel = audio(AudioToolKind.List), fontScale = 2f,
    )

    @Test
    fun equalizerPanelWithLargeFont() = check(
        ClipSelection.Video("c1"), videoTools, audioPanel = audio(AudioToolKind.Eq), fontScale = 2f,
    )
}
