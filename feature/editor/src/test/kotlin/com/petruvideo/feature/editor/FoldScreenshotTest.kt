package com.petruvideo.feature.editor

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import com.petruvideo.core.data.autosave.SaveStatus
import com.petruvideo.core.designsystem.PetruTheme
import com.petruvideo.core.designsystem.adaptive.Arrangement
import com.petruvideo.core.designsystem.adaptive.HingeBounds
import com.petruvideo.core.designsystem.adaptive.LocalArrangement
import com.petruvideo.core.model.MediaItem
import com.petruvideo.feature.editor.ui.EditorScreen
import com.petruvideo.feature.editor.ui.LocalMediaLoaders
import com.petruvideo.feature.editor.ui.MediaLoaders
import kotlin.math.abs
import kotlin.math.sin
import org.junit.Rule
import org.junit.Test

/** Goldens for the unfolded and tabletop editor (1040 x 780 dp), reviewed against the Fold reference images. */
class FoldScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(
            screenWidth = 2_080,
            screenHeight = 1_560,
            xdpi = 320,
            ydpi = 320,
            density = Density.XHIGH,
            orientation = ScreenOrientation.LANDSCAPE,
        ),
    )

    private val loaders = object : MediaLoaders {
        override suspend fun thumbnail(item: MediaItem, timeMs: Long, heightPx: Int): ImageBitmap {
            val colors = listOf(0xFFF08F5A, 0xFF2C8AA0, 0xFFA9D8FF, 0xFF8A4FD0, 0xFF74A96D)
            val bitmap = android.graphics.Bitmap.createBitmap(8, 8, android.graphics.Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(colors[(item.id.hashCode() and Int.MAX_VALUE) % colors.size].toInt())
            return bitmap.asImageBitmap()
        }

        override suspend fun peaks(item: MediaItem): FloatArray =
            FloatArray(400) { abs(sin(it * 0.45f)) * 0.7f + 0.1f }
    }

    private fun state(selection: ClipSelection? = ClipSelection.Video("c2")): EditorState {
        val timeline = sampleLoaded().timeline.copy(playheadMs = 6_200)
        return EditorState(
            header = EditorHeader("Coast road, day 3", "1080p · 30 fps", SaveStatus.Saved),
            timeline = timeline,
            selection = selection,
            playback = PlaybackState(positionMs = 6_200, durationMs = timeline.totalDurationMs),
            canUndo = true,
            projectMedia = timeline.media.values.toList(),
            clipTools = if (selection == null) {
                emptyList()
            } else {
                listOf(ClipTool.Split, ClipTool.Duplicate, ClipTool.Delete)
            },
        )
    }

    private fun show(arrangement: Arrangement, state: EditorState) = paparazzi.snapshot {
        CompositionLocalProvider(LocalMediaLoaders provides loaders, LocalArrangement provides arrangement) {
            PetruTheme { EditorScreen(state, player = null, onEvent = {}, onBack = {}) }
        }
    }

    @Test
    fun unfoldedEditor() = show(Arrangement.Expanded(hinge = null), state())

    @Test
    fun unfoldedEditorWithVerticalHinge() =
        show(Arrangement.Expanded(HingeBounds(left = 1_020, top = 0, right = 1_060, bottom = 1_560)), state())

    @Test
    fun tabletopEditor() = show(Arrangement.Tabletop(HingeBounds(0, 780, 2_080, 800)), state(selection = null))

    /** c1 is 2x at 40% with reverb on, c3 is muted; shown with the inspector and badges. */
    private fun editedState(
        selection: ClipSelection? = ClipSelection.Video("c1"),
        sheet: SheetState? = null,
        panel: ReverbPanelState? = null,
    ): EditorState {
        val hall = com.petruvideo.core.model.ReverbPresets.settings(com.petruvideo.core.model.ReverbPreset.Hall, true)
        val base = sampleLoaded().timeline.copy(playheadMs = 1_000)
        val timeline = base.copy(
            videoLane = base.videoLane.map {
                when (it.id) {
                    "c1" -> it.copy(speedMilli = 2000, volumePercent = 40, reverb = hall)
                    "c3" -> it.copy(muted = true)
                    else -> it
                }
            },
            audioLane = base.audioLane.map { it.copy(reverb = hall, speedMilli = 500) },
        )
        return EditorState(
            header = EditorHeader("Coast road, day 3", "1080p · 30 fps", SaveStatus.Saved),
            timeline = timeline,
            selection = selection,
            playback = PlaybackState(positionMs = 1_000, durationMs = timeline.totalDurationMs),
            canUndo = true,
            projectMedia = timeline.media.values.toList(),
            clipTools = listOf(
                ClipTool.Split, ClipTool.Speed, ClipTool.Volume, ClipTool.Mute, ClipTool.Reverb, ClipTool.Detach,
                ClipTool.Duplicate, ClipTool.Delete,
            ),
            selectedReverb = hall,
            sheet = sheet,
            reverbPanel = panel,
        )
    }

    @Test
    fun unfoldedInspectorWithSpeedVolumeMuteAndReverbAndBadges() =
        show(Arrangement.Expanded(hinge = null), editedState())

    @Test
    fun tabletopWithTheVolumeSheet() = show(
        Arrangement.Tabletop(HingeBounds(0, 780, 2_080, 800)),
        editedState(sheet = SheetState(ClipTool.Volume, "c1", "m1.mp4", 40, 4_000)),
    )

    @Test
    fun tabletopWithTheReverbPanel() {
        val hall = com.petruvideo.core.model.ReverbPresets.settings(com.petruvideo.core.model.ReverbPreset.Hall, true)
        show(
            Arrangement.Tabletop(HingeBounds(0, 780, 2_080, 800)),
            editedState(panel = ReverbPanelState("c1", "m1.mp4", hall)),
        )
    }
}
