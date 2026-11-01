package com.petruvideo.feature.export

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.petruvideo.core.designsystem.PetruTheme
import com.petruvideo.core.model.export.ExportRecord
import com.petruvideo.core.model.export.ExportSettings
import com.petruvideo.core.model.export.FrameRate
import com.petruvideo.core.model.export.Quality
import com.petruvideo.core.model.export.Resolution
import com.petruvideo.feature.export.ui.ExportsScreen
import java.util.TimeZone
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

/** Goldens for the Exports list. */
class ExportsScreenshotTest {
    @get:Rule
    val phone = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(screenHeight = 1_800))

    private fun record(id: String, name: String, at: Long, settings: ExportSettings) =
        ExportRecord(id, name, "content://media/$id", 21_000, 32_000_000, settings, at)

    private fun settings(r: Resolution, f: FrameRate, q: Quality) = ExportSettings(r, f, q)

    private val standard = settings(Resolution.R1080, FrameRate.F30, Quality.Recommended)
    private val big = settings(Resolution.R4K, FrameRate.F60, Quality.Higher)

    private val rows = listOf(
        ExportRow(record("a", "Coast road, day 3", 1_793_000_000_000, standard)),
        ExportRow(record("b", "Birthday", 1_792_000_000_000, big), true),
        ExportRow(record("c", "Hike", 1_790_000_000_000, settings(Resolution.R720, FrameRate.F24, Quality.Smaller))),
    )

    companion object {
        /** The dates in the list follow the time zone, so the goldens are made in one on every machine. */
        @JvmStatic
        @BeforeClass
        fun fixTimeZone() = TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @Composable
    private fun Show(state: ExportsUiState) {
        PetruTheme { ExportsScreen(state, onEvent = {}, onBack = {}) }
    }

    @Test
    fun withExports() = phone.snapshot { Show(ExportsUiState(rows, loaded = true)) }

    @Test
    fun empty() = phone.snapshot { Show(ExportsUiState(emptyList(), loaded = true)) }

    @Test
    fun withUndoBar() = phone.snapshot { Show(ExportsUiState(rows.drop(1), loaded = true, undo = rows[0].record)) }
}
