package com.petruvideo.feature.editor

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.petruvideo.core.data.autosave.SaveStatus
import com.petruvideo.core.designsystem.PetruTheme
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.ReverbPreset
import com.petruvideo.core.model.ReverbPresets
import com.petruvideo.feature.editor.ui.EditorScreen
import com.petruvideo.feature.editor.ui.LocalMediaLoaders
import com.petruvideo.feature.editor.ui.MediaLoaders
import kotlin.math.abs
import kotlin.math.sin
import org.junit.Rule
import org.junit.Test

/** Goldens at the 360 dp cover-screen width: the audio tool bar scrolls, panels fit without clipping. */
class CoverScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(
            screenWidth = 1_080, screenHeight = 2_160, xdpi = 480, ydpi = 480, density = Density.XXHIGH,
        ),
    )

    private val loaders = object : MediaLoaders {
        override suspend fun thumbnail(item: MediaItem, timeMs: Long, heightPx: Int): ImageBitmap {
            val bitmap = android.graphics.Bitmap.createBitmap(8, 8, android.graphics.Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(0xFF2C8AA0.toInt())
            return bitmap.asImageBitmap()
        }

        override suspend fun peaks(item: MediaItem): FloatArray =
            FloatArray(400) { abs(sin(it * 0.45f)) * 0.7f + 0.1f }
    }

    private val audioTools = listOf(
        ClipTool.Split, ClipTool.Speed, ClipTool.Volume, ClipTool.Mute, ClipTool.Reverb, ClipTool.Replace,
        ClipTool.Duplicate, ClipTool.Delete,
    )

    private fun state(panel: ReverbPanelState? = null): EditorState {
        val timeline = sampleLoaded().timeline.copy(playheadMs = 6_200)
        return EditorState(
            header = EditorHeader("Coast road, day 3", "1080p · 30 fps", SaveStatus.Saved),
            timeline = timeline,
            selection = ClipSelection.Audio("a1"),
            playback = PlaybackState(positionMs = 6_200, durationMs = timeline.totalDurationMs),
            projectMedia = timeline.media.values.toList(),
            clipTools = audioTools,
            reverbPanel = panel,
        )
    }

    private fun show(state: EditorState) = paparazzi.snapshot {
        CompositionLocalProvider(LocalMediaLoaders provides loaders) {
            PetruTheme { EditorScreen(state, player = null, onEvent = {}, onBack = {}) }
        }
    }

    @Test
    fun audioToolBarAtCoverWidth() = show(state())

    @Test
    fun reverbPanelAtCoverWidth() =
        show(state(ReverbPanelState("a1", "d.mp3", ReverbPresets.settings(ReverbPreset.Cave, enabled = true))))
}
