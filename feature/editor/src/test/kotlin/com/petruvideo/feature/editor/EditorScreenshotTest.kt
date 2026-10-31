package com.petruvideo.feature.editor

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.petruvideo.core.data.autosave.SaveStatus
import com.petruvideo.core.designsystem.PetruTheme
import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.MediaItem
import com.petruvideo.feature.editor.ui.EditorScreen
import com.petruvideo.feature.editor.ui.LocalMediaLoaders
import com.petruvideo.feature.editor.ui.MediaLoaders
import kotlin.math.abs
import kotlin.math.sin
import org.junit.Rule
import org.junit.Test

/** Goldens for the compact editor; reviewed against design/reference/Editor.png before approval. */
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

    private fun state(
        missing: Set<String> = emptySet(),
        selection: ClipSelection? = null,
        edited: Boolean = false,
        sheet: SheetState? = null,
    ): EditorState {
        val loaded = sampleLoaded()
        val timeline = loaded.timeline.copy(
            videoLane = if (edited) {
                loaded.timeline.videoLane.map {
                    if (it.id == "c1") it.copy(speedMilli = 2000, volumePercent = 40) else it
                }
            } else {
                loaded.timeline.videoLane
            },
            media = loaded.timeline.media.mapValues { (id, item) ->
                if (id in missing) item.copy(availability = Availability.Missing) else item
            },
            playheadMs = 6_200,
        )
        return EditorState(
            header = EditorHeader("Coast road, day 3", "1080p · 30 fps", SaveStatus.Saved),
            timeline = timeline,
            selection = selection,
            playback = PlaybackState(isPlaying = false, positionMs = 6_200, durationMs = timeline.totalDurationMs),
            canUndo = true,
            canRedo = false,
            projectMedia = timeline.media.values.toList(),
            missingMedia = missing,
            sheet = sheet,
            clipTools = when (selection) {
                null -> emptyList()
                is ClipSelection.Audio -> listOf(
                    ClipTool.Split,
                    ClipTool.Speed,
                    ClipTool.Volume,
                    ClipTool.Mute,
                    ClipTool.Replace,
                    ClipTool.Duplicate,
                    ClipTool.Delete,
                )
                is ClipSelection.Video -> listOf(
                    ClipTool.Split,
                    ClipTool.Speed,
                    ClipTool.Volume,
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
