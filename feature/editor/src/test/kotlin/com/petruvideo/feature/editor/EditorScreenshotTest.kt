package com.petruvideo.feature.editor

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.petruvideo.core.data.autosave.SaveStatus
import com.petruvideo.core.designsystem.PetruTheme
import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.AudioFx
import com.petruvideo.core.model.ClipEffect
import com.petruvideo.core.model.EffectKind
import com.petruvideo.core.model.Transition
import com.petruvideo.core.model.MediaItem
import com.petruvideo.feature.editor.ui.EditorScreen
import com.petruvideo.feature.editor.ui.LocalMediaLoaders
import com.petruvideo.feature.editor.ui.MediaLoaders
import kotlin.math.abs
import kotlin.math.sin
import org.junit.Rule
import org.junit.Test

/** Goldens for the compact editor; reviewed against design/reference/Editor.png before approval. */
@Suppress("LargeClass")
class EditorScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(screenHeight = 2_000))

    /** Gradient tiles and a synthetic waveform stand in for real footage, as in the designer's mock. */
    private val loaders = object : MediaLoaders {
        override suspend fun thumbnail(item: MediaItem, timeMs: Long, heightPx: Int): ImageBitmap {
            val colors = listOf(0xFFF08F5A, 0xFF2C8AA0, 0xFFA9D8FF, 0xFF8A4FD0, 0xFF74A96D)
            val argb = colors[(item.id.hashCode() and Int.MAX_VALUE) % colors.size].toInt()
            val bitmap = android.graphics.Bitmap.createBitmap(8, 8, android.graphics.Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(argb)
            return bitmap.asImageBitmap()
        }

        override suspend fun peaks(item: MediaItem): FloatArray =
            FloatArray(400) { abs(sin(it * 0.45f)) * 0.7f + 0.1f }
    }

    @Suppress("LongMethod")
    private fun state(
        missing: Set<String> = emptySet(),
        selection: ClipSelection? = null,
        edited: Boolean = false,
        sheet: SheetState? = null,
        reverbPanel: ReverbPanelState? = null,
        audioPanel: AudioPanelState? = null,
        playheadMs: Long = 6_200,
        texts: List<com.petruvideo.core.model.TextClip> = emptyList(),
        textPanel: TextPanelState? = null,
        tab: ToolbarTab = ToolbarTab.Edit,
        filterPanel: FilterPanelState? = null,
        effectPanel: EffectPanelState? = null,
        voiceOver: VoiceOverUi? = null,
        keyframePanel: KeyframePanelState? = null,
        selectedKeyframeMs: Long? = null,
        joined: Boolean = false,
        overlays: List<com.petruvideo.core.model.OverlayClip> = emptyList(),
        overlayPanel: OverlayPanelState? = null,
        transformPanel: TransformPanelState? = null,
        freezePanel: FreezePanelState? = null,
        reverseProgress: Int? = null,
        curvePanel: SpeedCurvePanelState? = null,
        crop: CropSessionState? = null,
    ): EditorState {
        val loaded = sampleLoaded()
        val timeline = loaded.timeline.copy(
            videoLane = if (edited) {
                loaded.timeline.videoLane.map {
                    if (it.id == "c1") {
                        val fade = com.petruvideo.core.model.Fade(800, 600)
                        it.copy(speedMilli = 2000, volumePercent = 40, fx = AudioFx(fade = fade))
                    } else {
                        it
                    }
                }
            } else if (joined) {
                loaded.timeline.videoLane.mapIndexed { i, clip ->
                    when (i) {
                        0 -> clip.copy(
                            transitionOut = Transition(com.petruvideo.core.model.TransitionKind.Dissolve, 1_000),
                        )
                        1 -> clip.copy(effect = ClipEffect(EffectKind.Blur, 40))
                        else -> clip
                    }
                }
            } else {
                loaded.timeline.videoLane
            },
            media = loaded.timeline.media.mapValues { (id, item) ->
                if (id in missing) item.copy(availability = Availability.Missing) else item
            },
            playheadMs = playheadMs,
            textLane = texts,
            overlayLane = overlays,
            audioLane = if (edited) {
                loaded.timeline.audioLane.map { it.copy(fx = AudioFx(fade = com.petruvideo.core.model.Fade(500, 500))) }
            } else {
                loaded.timeline.audioLane
            },
        )
        return EditorState(
            header = EditorHeader("Coast road, day 3", "1080p · 30 fps", SaveStatus.Saved),
            timeline = timeline,
            selection = selection,
            playback = PlaybackState(isPlaying = false, positionMs = playheadMs, durationMs = timeline.totalDurationMs),
            canUndo = true,
            canRedo = false,
            projectMedia = timeline.media.values.toList(),
            missingMedia = missing,
            sheet = sheet,
            reverbPanel = reverbPanel,
            audioPanel = audioPanel,
            textPanel = textPanel,
            filterPanel = filterPanel,
            effectPanel = effectPanel,
            voiceOver = voiceOver,
            keyframePanel = keyframePanel,
            selectedKeyframeMs = selectedKeyframeMs,
            overlayPanel = overlayPanel,
            transformPanel = transformPanel,
            freezePanel = freezePanel,
            reverseProgress = reverseProgress,
            curvePanel = curvePanel,
            crop = crop,
            selectedOverlay = overlays.firstOrNull { it.id == selection?.clipId },
            toolbarTab = tab,
            selectedText = texts.firstOrNull { it.id == selection?.clipId },
            clipTools = when (selection) {
                null -> emptyList()
                is ClipSelection.Overlay -> listOf(
                    ClipTool.OverlayOpacity, ClipTool.Split, ClipTool.Duplicate, ClipTool.Delete,
                )
                is ClipSelection.Text -> listOf(
                    ClipTool.TextEdit,
                    ClipTool.TextStyle,
                    ClipTool.TextAnim,
                    ClipTool.Split,
                    ClipTool.Duplicate,
                    ClipTool.Delete,
                )
                is ClipSelection.Audio -> listOf(
                    ClipTool.Split,
                    ClipTool.Speed,
                    ClipTool.Audio,
                    ClipTool.Replace,
                    ClipTool.Duplicate,
                    ClipTool.Delete,
                )
                is ClipSelection.Video -> listOf(
                    ClipTool.Split,
                    ClipTool.Speed,
                    ClipTool.Audio,
                    ClipTool.Detach,
                    ClipTool.Duplicate,
                    ClipTool.Delete,
                )
            },
        )
    }

    @Test
    fun compactEditor() {
        paparazzi.snapshot {
            CompositionLocalProvider(LocalMediaLoaders provides loaders) {
                PetruTheme { EditorScreen(state(), player = null, onEvent = {}, onBack = {}) }
            }
        }
    }

    @Test
    fun selectedVideoClipShowsAnOutline() {
        paparazzi.snapshot {
            CompositionLocalProvider(LocalMediaLoaders provides loaders) {
                PetruTheme {
                    EditorScreen(state(selection = ClipSelection.Video("c2")), player = null, onEvent = {}, onBack = {})
                }
            }
        }
    }

    @Test
    fun selectedDetachedAudioShowsHandlesAndAudioTools() {
        paparazzi.snapshot {
            CompositionLocalProvider(LocalMediaLoaders provides loaders) {
                PetruTheme {
                    EditorScreen(state(selection = ClipSelection.Audio("a1")), player = null, onEvent = {}, onBack = {})
                }
            }
        }
    }

    @Test
    fun volumeSheetAndBadges() {
        val sheet = SheetState(ClipTool.Volume, "c1", "m1.mp4", 40, 4_000)
        paparazzi.snapshot {
            CompositionLocalProvider(LocalMediaLoaders provides loaders) {
                PetruTheme {
                    EditorScreen(
                        state(selection = ClipSelection.Video("c1"), edited = true, sheet = sheet),
                        player = null,
                        onEvent = {},
                        onBack = {},
                    )
                }
            }
        }
    }

    @Test
    fun volumeBoostSheet() {
        val sheet = SheetState(ClipTool.Volume, "a1", "d.mp3", 150, 2_000)
        paparazzi.snapshot {
            CompositionLocalProvider(LocalMediaLoaders provides loaders) {
                PetruTheme {
                    EditorScreen(
                        state(selection = ClipSelection.Audio("a1"), sheet = sheet),
                        player = null,
                        onEvent = {},
                        onBack = {},
                    )
                }
            }
        }
    }

    @Test
    fun speedSheet() {
        val sheet = SheetState(ClipTool.Speed, "c1", "m1.mp4", 2000, 4_000)
        paparazzi.snapshot {
            CompositionLocalProvider(LocalMediaLoaders provides loaders) {
                PetruTheme {
                    EditorScreen(
                        state(selection = ClipSelection.Video("c1"), edited = true, sheet = sheet),
                        player = null,
                        onEvent = {},
                        onBack = {},
                    )
                }
            }
        }
    }

    @Test
    fun reverbPanelWithHallOn() {
        val reverb = com.petruvideo.core.model.ReverbPresets.settings(com.petruvideo.core.model.ReverbPreset.Hall, true)
        paparazzi.snapshot {
            CompositionLocalProvider(LocalMediaLoaders provides loaders) {
                PetruTheme {
                    EditorScreen(
                        state(
                            selection = ClipSelection.Video("c1"),
                            reverbPanel = ReverbPanelState("c1", "m1.mp4", reverb),
                        ),
                        player = null,
                        onEvent = {},
                        onBack = {},
                    )
                }
            }
        }
    }

    @Test
    fun reverbPanelCustomOff() {
        val reverb = com.petruvideo.core.model.Reverb(
            false, com.petruvideo.core.model.ReverbPreset.Custom, 40, 2_400, 65, 45, 55,
        )
        paparazzi.snapshot {
            CompositionLocalProvider(LocalMediaLoaders provides loaders) {
                PetruTheme {
                    EditorScreen(
                        state(
                            selection = ClipSelection.Audio("a1"),
                            reverbPanel = ReverbPanelState("a1", "d.mp3", reverb),
                        ),
                        player = null,
                        onEvent = {},
                        onBack = {},
                    )
                }
            }
        }
    }

    private fun audioPanel(kind: AudioToolKind, fx: AudioFx = AudioFx()) =
        AudioPanelState(kind, "c1", "m1.mp4", fx, 4_000, 80, false, com.petruvideo.core.model.Reverb())

    private val turnedClip = com.petruvideo.core.model.ClipTransform(quarterTurns = 1, flipH = true)

    @Test
    fun transformPanelOnAVideoClip() = paparazzi.snapshot {
        CompositionLocalProvider(LocalMediaLoaders provides loaders) {
            PetruTheme {
                EditorScreen(
                    state(
                        selection = ClipSelection.Video("c1"),
                        transformPanel = TransformPanelState(
                            "c1", turnedClip.copy(crop = com.petruvideo.core.model.CropBox(100, 100, 800, 800)),
                        ),
                    ),
                    player = null, onEvent = {}, onBack = {},
                )
            }
        }
    }

    @Test
    fun speedCurvePanelWithTheHeroPreset() = paparazzi.snapshot {
        val curve = com.petruvideo.core.model.SpeedCurve.preset(com.petruvideo.core.model.SpeedPreset.Hero, 4_000)
        CompositionLocalProvider(LocalMediaLoaders provides loaders) {
            PetruTheme {
                EditorScreen(
                    state(
                        selection = ClipSelection.Video("c1"),
                        curvePanel = SpeedCurvePanelState("c1", curve, true, curve.timelineMs, 4_000),
                    ),
                    player = null, onEvent = {}, onBack = {},
                )
            }
        }
    }

    @Test
    fun reversingShowsProgressWithCancel() = paparazzi.snapshot {
        CompositionLocalProvider(LocalMediaLoaders provides loaders) {
            PetruTheme {
                EditorScreen(
                    state(selection = ClipSelection.Video("c1"), reverseProgress = 45),
                    player = null, onEvent = {}, onBack = {},
                )
            }
        }
    }

    @Test
    fun freezePanelOnAVideoClip() = paparazzi.snapshot {
        CompositionLocalProvider(LocalMediaLoaders provides loaders) {
            PetruTheme {
                EditorScreen(
                    state(selection = ClipSelection.Video("c1"), freezePanel = FreezePanelState("c1", 2_000)),
                    player = null, onEvent = {}, onBack = {},
                )
            }
        }
    }

    @Test
    fun cropScreenWithAShapeAndTheBox() = paparazzi.snapshot {
        val media = sampleLoaded().timeline.media.getValue("m1")
        CompositionLocalProvider(LocalMediaLoaders provides loaders) {
            PetruTheme {
                EditorScreen(
                    state(
                        selection = ClipSelection.Video("c1"),
                        crop = CropSessionState(
                            "c1", media, 0, com.petruvideo.core.model.ClipTransform(),
                            com.petruvideo.core.model.CropBox.forShape(1f, 1920, 1080),
                            com.petruvideo.core.model.CropShape.Square, 1920, 1080,
                        ),
                    ),
                    player = null, onEvent = {}, onBack = {},
                )
            }
        }
    }

    @Test
    fun duckingPanelOnAnAudioClip() = paparazzi.snapshot {
        CompositionLocalProvider(LocalMediaLoaders provides loaders) {
            PetruTheme {
                EditorScreen(
                    state(
                        selection = ClipSelection.Audio("a1"),
                        audioPanel = audioPanel(AudioToolKind.Ducking).copy(
                            clipId = "a1",
                            ducking = com.petruvideo.core.model.Ducking(
                                true, 70, com.petruvideo.core.model.DuckSmoothness.Slow,
                            ),
                            duckingIdle = true,
                        ),
                    ),
                    player = null, onEvent = {}, onBack = {},
                )
            }
        }
    }

    private fun snapshotAudio(kind: AudioToolKind, fx: AudioFx = AudioFx()) {
        paparazzi.snapshot {
            CompositionLocalProvider(LocalMediaLoaders provides loaders) {
                PetruTheme {
                    EditorScreen(
                        state(selection = ClipSelection.Video("c1"), audioPanel = audioPanel(kind, fx)),
                        player = null,
                        onEvent = {},
                        onBack = {},
                    )
                }
            }
        }
    }

    private val busyFx = AudioFx(
        fade = com.petruvideo.core.model.Fade(1_500, 1_000),
        noise = com.petruvideo.core.model.NoiseReduction(true),
        eq = com.petruvideo.core.model.EqPresets.settings(com.petruvideo.core.model.EqPreset.BassBoost),
        voice = com.petruvideo.core.model.VoiceFx(
            echo = com.petruvideo.core.model.Echo(true, 300, 3),
            pitch = com.petruvideo.core.model.Pitch(true, 4),
        ),
    )

    @Test
    fun audioToolList() = snapshotAudio(AudioToolKind.List, busyFx)

    @Test
    fun fadePanel() = snapshotAudio(AudioToolKind.Fade, busyFx)

    @Test
    fun noisePanel() = snapshotAudio(AudioToolKind.Noise, busyFx)

    @Test
    fun equalizerPanel() = snapshotAudio(AudioToolKind.Eq, busyFx)

    @Test
    fun voicePanel() = snapshotAudio(AudioToolKind.Voice, busyFx)

    @Test
    fun fadeRampsAndBadgesOnClips() {
        paparazzi.snapshot {
            CompositionLocalProvider(LocalMediaLoaders provides loaders) {
                PetruTheme {
                    EditorScreen(
                        state(selection = ClipSelection.Video("c2"), edited = true, playheadMs = 1_000),
                        player = null,
                        onEvent = {},
                        onBack = {},
                    )
                }
            }
        }
    }

    private val titles = listOf(
        com.petruvideo.core.model.TextClip("t1", "Day one", 1_000, 4_000),
        com.petruvideo.core.model.TextClip("t2", "Coast road at dawn", 3_000, 5_000, yPermille = 850),
        com.petruvideo.core.model.TextClip("t3", "The end", 9_000, 2_000),
    )

    private fun snapshotText(
        kind: TextToolKind?,
        selected: String? = "t1",
        tab: ToolbarTab = ToolbarTab.Edit,
        playheadMs: Long = 2_000,
    ) {
        val clip = titles.firstOrNull { it.id == selected }
        paparazzi.snapshot {
            CompositionLocalProvider(LocalMediaLoaders provides loaders) {
                PetruTheme {
                    EditorScreen(
                        state(
                            selection = selected?.let { ClipSelection.Text(it) },
                            texts = titles,
                            textPanel = if (kind != null && clip != null) TextPanelState(kind, clip) else null,
                            tab = tab,
                            playheadMs = playheadMs,
                        ),
                        player = null,
                        onEvent = {},
                        onBack = {},
                    )
                }
            }
        }
    }

    @Test
    fun textLaneWithOverlappingClips() = snapshotText(null, selected = "t2", playheadMs = 4_000)

    @Test
    fun textTabOffersAddText() = snapshotText(null, selected = null, tab = ToolbarTab.Text)

    @Test
    fun textEditPanel() = snapshotText(TextToolKind.Edit)

    @Test
    fun textStylePanel() = snapshotText(TextToolKind.Style)

    @Test
    fun textAnimationPanel() = snapshotText(TextToolKind.Animation)

    private fun snapshotFilter(kind: FilterToolKind, filter: com.petruvideo.core.model.ClipFilter) {
        val media = sampleLoaded().timeline.media["m1"]
        paparazzi.snapshot {
            CompositionLocalProvider(LocalMediaLoaders provides loaders) {
                PetruTheme {
                    EditorScreen(
                        state(
                            selection = ClipSelection.Video("c1"),
                            tab = ToolbarTab.Filters,
                            filterPanel = FilterPanelState(kind, "c1", filter, media, 1_000, true),
                        ),
                        player = null,
                        onEvent = {},
                        onBack = {},
                    )
                }
            }
        }
    }

    @Test
    fun filterLooksPanel() = snapshotFilter(
        FilterToolKind.Looks, com.petruvideo.core.model.ClipFilter(com.petruvideo.core.model.FilterLook.Vintage, 70),
    )

    @Test
    fun filterAdjustPanel() = snapshotFilter(
        FilterToolKind.Adjust, com.petruvideo.core.model.ClipFilter(brightness = 25, saturation = -40, vignette = 60),
    )

    @Test
    fun filtersTabWithNothingSelected() = paparazzi.snapshot {
        CompositionLocalProvider(LocalMediaLoaders provides loaders) {
            PetruTheme {
                EditorScreen(state(tab = ToolbarTab.Filters), player = null, onEvent = {}, onBack = {})
            }
        }
    }

    private fun snapshotEffect(kind: EffectToolKind, transition: Transition?, effect: ClipEffect?) {
        paparazzi.snapshot {
            CompositionLocalProvider(LocalMediaLoaders provides loaders) {
                PetruTheme {
                    EditorScreen(
                        state(
                            selection = ClipSelection.Video("c1"),
                            tab = ToolbarTab.Effects,
                            joined = true,
                            effectPanel = EffectPanelState(kind, "c1", transition, effect, 1_500),
                        ),
                        player = null,
                        onEvent = {},
                        onBack = {},
                    )
                }
            }
        }
    }

    @Test
    fun effectsTransitionPanel() = snapshotEffect(
        EffectToolKind.Transition,
        Transition(com.petruvideo.core.model.TransitionKind.Slide, 1_000), null,
    )

    @Test
    fun effectsEffectPanel() = snapshotEffect(
        EffectToolKind.Effect, null, ClipEffect(EffectKind.Pixelate, 60),
    )

    @Test
    fun effectsTabWithNothingSelected() = paparazzi.snapshot {
        CompositionLocalProvider(LocalMediaLoaders provides loaders) {
            PetruTheme {
                EditorScreen(state(tab = ToolbarTab.Effects, joined = true), player = null, onEvent = {}, onBack = {})
            }
        }
    }

    private fun snapshotRecording(take: VoiceOverUi) = paparazzi.snapshot {
        CompositionLocalProvider(LocalMediaLoaders provides loaders) {
            PetruTheme {
                EditorScreen(state(tab = ToolbarTab.Audio, voiceOver = take), player = null, onEvent = {}, onBack = {})
            }
        }
    }

    @Test
    fun voiceOverCountIn() = snapshotRecording(VoiceOverUi.CountIn(2))

    @Test
    fun voiceOverRecording() = snapshotRecording(VoiceOverUi.Recording(7_400, 0.55f))

    @Test
    fun audioTabWithNothingSelected() = paparazzi.snapshot {
        CompositionLocalProvider(LocalMediaLoaders provides loaders) {
            PetruTheme {
                EditorScreen(state(tab = ToolbarTab.Audio), player = null, onEvent = {}, onBack = {})
            }
        }
    }

    private val overlays = listOf(
        com.petruvideo.core.model.OverlayClip("v1", "m1", 0, 4_000, 1_000),
        com.petruvideo.core.model.OverlayClip("v2", "m3", 0, 3_000, 2_500, xPermille = 300, yPermille = 700),
    )

    @Test
    fun overlayLaneWithOverlappingClips() = paparazzi.snapshot {
        CompositionLocalProvider(LocalMediaLoaders provides loaders) {
            PetruTheme {
                EditorScreen(
                    state(selection = ClipSelection.Overlay("v1"), overlays = overlays, playheadMs = 3_000),
                    player = null, onEvent = {}, onBack = {},
                )
            }
        }
    }

    private val animatedOverlays = listOf(
        com.petruvideo.core.model.OverlayClip("v1", "m1", 0, 4_000, 1_000).withKeyframes(
            listOf(
                com.petruvideo.core.model.Keyframe(0, com.petruvideo.core.model.Pose(250, 300, 300, 100), ease = true),
                com.petruvideo.core.model.Keyframe(1_500, com.petruvideo.core.model.Pose(750, 700, 400, 50)),
                com.petruvideo.core.model.Keyframe(3_000, com.petruvideo.core.model.Pose(500, 500, 350, 100)),
            ),
        ),
    )

    @Test
    fun keyframesPanelWithDiamondsOnTheSelectedOverlay() = paparazzi.snapshot {
        CompositionLocalProvider(LocalMediaLoaders provides loaders) {
            PetruTheme {
                EditorScreen(
                    state(
                        selection = ClipSelection.Overlay("v1"), overlays = animatedOverlays, playheadMs = 2_500,
                        keyframePanel = KeyframePanelState("v1", animatedOverlays[0].keyframes, 1_500, 1_500, 4_000),
                        selectedKeyframeMs = 1_500,
                    ),
                    player = null, onEvent = {}, onBack = {},
                )
            }
        }
    }

    @Test
    fun keyframesPanelOnAVideoClip() = paparazzi.snapshot {
        val frames = listOf(
            com.petruvideo.core.model.Keyframe(0, com.petruvideo.core.model.FRAMING_NEUTRAL),
            com.petruvideo.core.model.Keyframe(2_000, com.petruvideo.core.model.Pose(600, 500, 2_000, 80)),
        )
        CompositionLocalProvider(LocalMediaLoaders provides loaders) {
            PetruTheme {
                EditorScreen(
                    state(
                        selection = ClipSelection.Video("c2"), playheadMs = 1_000,
                        keyframePanel = KeyframePanelState(
                            "c2", frames, 2_000, 1_000, 3_000, com.petruvideo.core.model.Pose(550, 500, 1_500, 90),
                        ),
                        selectedKeyframeMs = 2_000,
                    ),
                    player = null, onEvent = {}, onBack = {},
                )
            }
        }
    }

    @Test
    fun overlayPanel() = paparazzi.snapshot {
        CompositionLocalProvider(LocalMediaLoaders provides loaders) {
            PetruTheme {
                EditorScreen(
                    state(
                        selection = ClipSelection.Overlay("v1"), overlays = overlays, playheadMs = 3_000,
                        overlayPanel = OverlayPanelState(overlays[0], "m1.mp4"),
                    ),
                    player = null, onEvent = {}, onBack = {},
                )
            }
        }
    }

    @Test
    fun overlayTabOffersAddOverlay() = paparazzi.snapshot {
        CompositionLocalProvider(LocalMediaLoaders provides loaders) {
            PetruTheme { EditorScreen(state(tab = ToolbarTab.Overlay), player = null, onEvent = {}, onBack = {}) }
        }
    }

    @Test
    fun missingMediaShowsRelinkAndRemove() {
        paparazzi.snapshot {
            CompositionLocalProvider(LocalMediaLoaders provides loaders) {
                PetruTheme { EditorScreen(state(missing = setOf("m2")), player = null, onEvent = {}, onBack = {}) }
            }
        }
    }

    @Test
    fun emptyProject() {
        paparazzi.snapshot {
            PetruTheme {
                EditorScreen(
                    EditorState(
                        header = EditorHeader("Untitled 1", null, SaveStatus.Saved),
                        timeline = com.petruvideo.core.model.Timeline("p"),
                    ),
                    player = null,
                    onEvent = {},
                    onBack = {},
                )
            }
        }
    }
}
