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
        textPanel: TextPanelState? = null,
        filterPanel: FilterPanelState? = null,
        effectPanel: EffectPanelState? = null,
        keyframePanel: KeyframePanelState? = null,
        voiceOver: VoiceOverUi? = null,
        overlayPanel: OverlayPanelState? = null,
        transformPanel: TransformPanelState? = null,
        freezePanel: FreezePanelState? = null,
        reverseProgress: Int? = null,
        curvePanel: SpeedCurvePanelState? = null,
        captionsPanel: CaptionsPanelState? = null,
        stickersOpen: Boolean = false,
        tab: ToolbarTab = ToolbarTab.Edit,
        crop: CropSessionState? = null,
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
            textPanel = textPanel,
            filterPanel = filterPanel,
            effectPanel = effectPanel,
            keyframePanel = keyframePanel,
            voiceOver = voiceOver,
            overlayPanel = overlayPanel,
            transformPanel = transformPanel,
            freezePanel = freezePanel,
            reverseProgress = reverseProgress,
            curvePanel = curvePanel,
            captionsPanel = captionsPanel,
            stickersOpen = stickersOpen,
            toolbarTab = tab,
            crop = crop,
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

    private val readyCaptions = CaptionsPanelState(
        com.petruvideo.core.media.captions.RecognitionAvailability.Available, listOf("en-US", "es-ES"),
        "en-US", true, null, "Added 12 captions",
    )

    @Test
    fun stickersGallery() = check(null, emptyList(), stickersOpen = true, tab = ToolbarTab.Text)

    @Test
    fun stickersGalleryWithLargeFont() =
        check(null, emptyList(), stickersOpen = true, tab = ToolbarTab.Text, fontScale = 2f)

    @Test
    fun captionsPanel() = check(null, emptyList(), captionsPanel = readyCaptions, tab = ToolbarTab.Text)

    @Test
    fun captionsPanelWithLargeFont() =
        check(null, emptyList(), captionsPanel = readyCaptions, tab = ToolbarTab.Text, fontScale = 2f)

    @Test
    fun captionsPanelWithALanguageMissing() = check(
        null, emptyList(), tab = ToolbarTab.Text,
        captionsPanel = CaptionsPanelState(
            com.petruvideo.core.media.captions.RecognitionAvailability.LanguageMissing("fr-FR", listOf("en-US")),
            listOf("en-US"), "fr-FR", true, null, null,
        ),
    )

    private val heroCurve = com.petruvideo.core.model.SpeedCurve.preset(com.petruvideo.core.model.SpeedPreset.Hero, 4_000)

    @Test
    fun speedCurvePanel() = check(
        ClipSelection.Video("c1"), videoTools,
        curvePanel = SpeedCurvePanelState("c1", heroCurve, true, heroCurve.timelineMs, 4_000),
    )

    @Test
    fun speedCurvePanelWithLargeFont() = check(
        ClipSelection.Video("c1"), videoTools,
        curvePanel = SpeedCurvePanelState("c1", heroCurve, true, heroCurve.timelineMs, 4_000), fontScale = 2f,
    )

    @Test
    fun reverseProgress() = check(ClipSelection.Video("c1"), videoTools, reverseProgress = 45)

    @Test
    fun reverseProgressWithLargeFont() = check(ClipSelection.Video("c1"), videoTools, reverseProgress = 45, fontScale = 2f)

    @Test
    fun freezePanel() = check(
        ClipSelection.Video("c1"), videoTools, freezePanel = FreezePanelState("c1", 2_000),
    )

    @Test
    fun freezePanelWithLargeFont() = check(
        ClipSelection.Video("c1"), videoTools, freezePanel = FreezePanelState("c1", 2_000), fontScale = 2f,
    )

    @Test
    fun transformPanel() = check(
        ClipSelection.Video("c1"), videoTools,
        transformPanel = TransformPanelState("c1", com.petruvideo.core.model.ClipTransform(quarterTurns = 1)),
    )

    @Test
    fun transformPanelWithLargeFont() = check(
        ClipSelection.Video("c1"), videoTools,
        transformPanel = TransformPanelState("c1", com.petruvideo.core.model.ClipTransform(quarterTurns = 1)),
        fontScale = 2f,
    )

    @Test
    fun cropScreen() = check(
        ClipSelection.Video("c1"), videoTools,
        crop = CropSessionState(
            "c1", timeline.media.values.first(), 0, com.petruvideo.core.model.ClipTransform(),
            com.petruvideo.core.model.CropBox(100, 100, 700, 800), com.petruvideo.core.model.CropShape.Free, 1920, 1080,
        ),
    )

    @Test
    fun duckingPanel() = check(
        ClipSelection.Audio("a1"), listOf(ClipTool.Audio, ClipTool.Delete),
        audioPanel = audio(AudioToolKind.Ducking).copy(
            clipId = "a1", ducking = com.petruvideo.core.model.Ducking(true, 70), duckingIdle = true,
        ),
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

    private val textClip = com.petruvideo.core.model.TextClip("t1", "Hello", 0, 3_000)

    private fun text(kind: TextToolKind) = check(
        ClipSelection.Text("t1"),
        listOf(ClipTool.TextEdit, ClipTool.TextStyle, ClipTool.TextAnim, ClipTool.Split, ClipTool.Duplicate, ClipTool.Delete),
        textPanel = TextPanelState(kind, textClip),
    )

    @Test
    fun textEditPanel() = text(TextToolKind.Edit)

    @Test
    fun textStylePanel() = text(TextToolKind.Style)

    @Test
    fun textAnimationPanel() = text(TextToolKind.Animation)

    @Test
    fun textStylePanelWithLargeFont() = check(
        ClipSelection.Text("t1"), listOf(ClipTool.TextEdit, ClipTool.TextStyle, ClipTool.TextAnim),
        textPanel = TextPanelState(TextToolKind.Style, textClip), fontScale = 2f,
    )

    private fun filter(kind: FilterToolKind) = check(
        ClipSelection.Video("c1"), listOf(ClipTool.FilterLooks, ClipTool.FilterAdjust, ClipTool.Duplicate, ClipTool.Delete),
        filterPanel = FilterPanelState(kind, "c1", com.petruvideo.core.model.ClipFilter(), null, 0, true),
    )

    @Test
    fun filterLooksPanel() = filter(FilterToolKind.Looks)

    @Test
    fun filterAdjustPanel() = filter(FilterToolKind.Adjust)

    @Test
    fun filterAdjustPanelWithLargeFont() = check(
        ClipSelection.Video("c1"), listOf(ClipTool.FilterLooks, ClipTool.FilterAdjust),
        filterPanel = FilterPanelState(FilterToolKind.Adjust, "c1", com.petruvideo.core.model.ClipFilter(), null, 0, true),
        fontScale = 2f,
    )

    private val transition = com.petruvideo.core.model.Transition(com.petruvideo.core.model.TransitionKind.Dissolve, 1_000)

    @Test
    fun effectsTransitionPanel() = check(
        ClipSelection.Video("c1"), listOf(ClipTool.EffectTransition, ClipTool.EffectFx, ClipTool.Duplicate, ClipTool.Delete),
        effectPanel = EffectPanelState(EffectToolKind.Transition, "c1", transition, null, 1_500),
    )

    @Test
    fun effectsEffectPanelWithLargeFont() = check(
        ClipSelection.Video("c1"), listOf(ClipTool.EffectTransition, ClipTool.EffectFx),
        effectPanel = EffectPanelState(
            EffectToolKind.Effect, "c1", null,
            com.petruvideo.core.model.ClipEffect(com.petruvideo.core.model.EffectKind.Blur, 50), 1_500,
        ),
        fontScale = 2f,
    )

    @Test
    fun voiceOverCountIn() = check(null, emptyList(), voiceOver = VoiceOverUi.CountIn(3))

    @Test
    fun voiceOverRecordingWithLargeFont() =
        check(null, emptyList(), voiceOver = VoiceOverUi.Recording(65_000, 0.8f), fontScale = 2f)

    private val keyframes = listOf(
        com.petruvideo.core.model.Keyframe(0, com.petruvideo.core.model.Pose(250, 300, 300, 100)),
        com.petruvideo.core.model.Keyframe(1_500, com.petruvideo.core.model.Pose(750, 700, 400, 50), ease = true),
    )

    @Test
    fun keyframesPanel() = check(
        ClipSelection.Overlay("o1"), listOf(ClipTool.OverlayOpacity, ClipTool.Keyframes, ClipTool.Split),
        keyframePanel = KeyframePanelState("o1", keyframes, 1_500, 1_000, 3_000),
    )

    @Test
    fun keyframesPanelWithLargeFont() = check(
        ClipSelection.Overlay("o1"), listOf(ClipTool.OverlayOpacity, ClipTool.Keyframes),
        keyframePanel = KeyframePanelState("o1", keyframes, 0, 0, 3_000), fontScale = 2f,
    )

    @Test
    fun keyframesPanelOnAVideoClip() = check(
        ClipSelection.Video("c2"), listOf(ClipTool.Split, ClipTool.Speed, ClipTool.Keyframes),
        keyframePanel = KeyframePanelState(
            "c2", keyframes, 1_500, 1_000, 3_000, com.petruvideo.core.model.Pose(550, 500, 1_500, 90),
        ),
    )

    private val overlayClip = com.petruvideo.core.model.OverlayClip("o1", "m", 0, 3_000, 0)

    @Test
    fun overlayPanel() = check(
        ClipSelection.Overlay("o1"), listOf(ClipTool.OverlayOpacity, ClipTool.Split, ClipTool.Duplicate, ClipTool.Delete),
        overlayPanel = OverlayPanelState(overlayClip, "pip.mp4"),
    )

    @Test
    fun overlayPanelWithLargeFont() = check(
        ClipSelection.Overlay("o1"), listOf(ClipTool.OverlayOpacity, ClipTool.Split),
        overlayPanel = OverlayPanelState(overlayClip, "pip.mp4"), fontScale = 2f,
    )
}
