package com.petruvideo.feature.export

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import app.cash.paparazzi.DeviceConfig
import com.android.resources.NightMode
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import com.petruvideo.core.designsystem.PetruTheme
import com.petruvideo.core.designsystem.adaptive.Arrangement
import com.petruvideo.core.designsystem.adaptive.HingeBounds
import com.petruvideo.core.designsystem.adaptive.LocalArrangement
import com.petruvideo.core.export.Blocker
import com.petruvideo.core.export.ExportJob
import com.petruvideo.core.export.ExportPlan
import com.petruvideo.core.model.export.ExportCaps
import com.petruvideo.core.model.export.ExportFailure
import com.petruvideo.core.model.export.ExportSettings
import com.petruvideo.core.model.export.ExportState
import com.petruvideo.core.model.export.DeviceLimits
import com.petruvideo.core.model.export.FrameRate
import com.petruvideo.core.model.export.Quality
import com.petruvideo.core.model.export.Resolution
import com.petruvideo.feature.export.ui.ExportScreen
import org.junit.Rule
import org.junit.Test

/** Goldens for the export screen, reviewed against design/reference/Export.png. */
class ExportScreenshotTest {
    @get:Rule
    val phone = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(nightMode = NightMode.NIGHT, screenHeight = 2_300))

    private val settings = ExportSettings(Resolution.R1080, FrameRate.F30, Quality.Recommended)
    private val caps = ExportCaps(Resolution.R1080, FrameRate.F30)
    private val device = DeviceLimits(2160, 60)

    private fun state(job: ExportState? = null, blocker: Blocker? = null, interrupted: Boolean = false) = ExportUiState(
        loaded = true,
        projectName = "Coast road, day 3",
        durationMs = 21_000,
        settings = settings,
        resolutions = caps.resolutionOptions(device),
        frameRates = caps.frameRateOptions(device),
        videoBitrate = 12_000_000,
        estimatedBytes = 32_000_000,
        blocker = blocker,
        interrupted = interrupted,
        job = job?.let {
            ExportJob("j", "p", "Coast road", settings, ExportPlan(12_000_000, 32_000_000, 21_000), 0, it)
        },
    )

    @Composable
    private fun Show(state: ExportUiState) {
        PetruTheme { ExportScreen(state, onEvent = {}, onBack = {}) }
    }

    @Test
    fun idle() = phone.snapshot { Show(state()) }

    @Test
    fun rendering() = phone.snapshot { Show(state(ExportState.Rendering(64))) }

    @Test
    fun finished() = phone.snapshot { Show(state(ExportState.Finished("content://media/1", 31_000_000, 21_000))) }

    @Test
    fun failedWithTheReason() = phone.snapshot { Show(state(ExportState.Failed(ExportFailure.Pressure))) }

    @Test
    fun blockedForStorage() = phone.snapshot { Show(state(blocker = Blocker.NotEnoughStorage(120_000_000))) }

    @Test
    fun interruptedNotice() = phone.snapshot { Show(state(interrupted = true)) }

    @Test
    fun missingMedia() = phone.snapshot { Show(state(blocker = Blocker.MissingMedia("beach.mp4"))) }
}

/** The unfolded and tabletop arrangements (1040 x 780 dp). */
class ExportFoldScreenshotTest {
    @get:Rule
    val fold = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(
            nightMode = NightMode.NIGHT,
            screenWidth = 2_080, screenHeight = 1_560, xdpi = 320, ydpi = 320, density = Density.XHIGH,
            orientation = ScreenOrientation.LANDSCAPE,
        ),
    )

    private val settings = ExportSettings(Resolution.R1080, FrameRate.F30, Quality.Recommended)
    private val caps = ExportCaps(Resolution.R4K, FrameRate.F60)
    private val device = DeviceLimits(2160, 60)
    private val state = ExportUiState(
        loaded = true,
        projectName = "Coast road, day 3",
        durationMs = 21_000,
        settings = settings,
        resolutions = caps.resolutionOptions(device),
        frameRates = caps.frameRateOptions(device),
        videoBitrate = 12_000_000,
        estimatedBytes = 32_000_000,
        job = ExportJob(
            "j", "p", "Coast road", settings, ExportPlan(12_000_000, 32_000_000, 21_000), 0, ExportState.Rendering(64),
        ),
    )

    private fun show(arrangement: Arrangement) = fold.snapshot {
        CompositionLocalProvider(LocalArrangement provides arrangement) {
            PetruTheme { ExportScreen(state, onEvent = {}, onBack = {}) }
        }
    }

    @Test
    fun unfolded() = show(Arrangement.Expanded(hinge = null))

    @Test
    fun unfoldedWithVerticalHinge() =
        show(Arrangement.Expanded(HingeBounds(left = 1_020, top = 0, right = 1_060, bottom = 1_560)))

    @Test
    fun tabletop() = show(Arrangement.Tabletop(HingeBounds(0, 780, 2_080, 800)))
}
