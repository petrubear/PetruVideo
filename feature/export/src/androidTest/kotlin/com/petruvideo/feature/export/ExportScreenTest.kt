package com.petruvideo.feature.export

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runAndroidComposeUiTest
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.Density
import com.petruvideo.core.designsystem.PetruTheme
import com.petruvideo.core.designsystem.adaptive.Arrangement
import com.petruvideo.core.designsystem.adaptive.HingeBounds
import com.petruvideo.core.designsystem.adaptive.LocalArrangement
import com.petruvideo.core.export.Blocker
import com.petruvideo.core.export.ExportJob
import com.petruvideo.core.export.ExportPlan
import com.petruvideo.core.model.export.DeviceLimits
import com.petruvideo.core.model.export.ExportCaps
import com.petruvideo.core.model.export.ExportSettings
import com.petruvideo.core.model.export.ExportState
import com.petruvideo.core.model.export.FrameRate
import com.petruvideo.core.model.export.Quality
import com.petruvideo.core.model.export.Resolution
import com.petruvideo.feature.export.ui.ExportScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class ExportScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val settings = ExportSettings(Resolution.R1080, FrameRate.F30, Quality.Recommended)
    private val caps = ExportCaps(Resolution.R1080, FrameRate.F30)
    private val device = DeviceLimits(2160, 60)

    private fun state(job: ExportState? = null, blocker: Blocker? = null) = ExportUiState(
        loaded = true,
        projectName = "Trip",
        durationMs = 21_000,
        settings = settings,
        resolutions = caps.resolutionOptions(device),
        frameRates = caps.frameRateOptions(device),
        videoBitrate = 12_000_000,
        estimatedBytes = 32_000_000,
        blocker = blocker,
        job = job?.let { ExportJob("j", "p", "Trip", settings, ExportPlan(12_000_000, 32_000_000, 21_000), 0, it) },
    )

    private fun show(state: ExportUiState): MutableList<ExportEvent> {
        val events = mutableListOf<ExportEvent>()
        compose.setContent {
            PetruTheme { ExportScreen(state, { events += it }, {}) }
        }
        return events
    }

    @Test
    fun twoTapsFlowAndTheFactsAreShown() {
        val events = show(state())
        compose.onNodeWithText("MP4 · H.264").assertIsDisplayed()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("~ 32 MB"))
        compose.onNodeWithText("12.0 Mbps").assertIsDisplayed()
        compose.onNodeWithText("~ 32 MB").assertIsDisplayed()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Save to gallery"))
        compose.onNodeWithText("Save to gallery").performClick()
        assertEquals(ExportEvent.Start, events.last())
    }

    @Test
    fun selectingAnAllowedOptionSendsTheEvent() {
        val events = show(state())
        compose.onNodeWithContentDescription("720p").performClick()
        assertEquals(ExportEvent.SelectResolution(Resolution.R720), events.last())
        compose.onNodeWithContentDescription("Higher").performClick()
        assertEquals(ExportEvent.SelectQuality(Quality.Higher), events.last())
    }

    @Test
    fun anUnavailableOptionExplainsItselfAndIsNotSelected() {
        val events = show(state())
        compose.onNodeWithContentDescription("4K").performClick()
        compose.onNodeWithText("4K: Above your footage").assertIsDisplayed()
        assertEquals(true, events.none { it is ExportEvent.SelectResolution })
    }

    @Test
    fun progressShowsWithCancelAndSaveIsDisabled() {
        val events = show(state(ExportState.Rendering(64)))
        compose.onNodeWithText("64%").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        assertEquals(ExportEvent.Cancel, events.last())
        compose.onNodeWithText("Save to gallery").performClick()
        assertEquals(true, events.none { it == ExportEvent.Start })
    }

    @Test
    fun finishedOffersOpenAndShare() {
        val events = show(state(ExportState.Finished("content://media/1", 31_000_000, 21_000)))
        compose.onNodeWithText("Saved to gallery").assertIsDisplayed()
        compose.onNodeWithText("Share").performClick()
        assertEquals(ExportEvent.Share, events.last())
        compose.onNodeWithText("Open").performClick()
        assertEquals(ExportEvent.Open, events.last())
    }

    @Test
    fun aBlockerIsExplainedAndTheButtonIsOff() {
        val events = show(state(blocker = Blocker.MissingMedia("beach.mp4")))
        compose.onNodeWithText("beach.mp4 is missing. Relink or remove it in the editor first.").assertIsDisplayed()
        compose.onNodeWithText("Save to gallery").performClick()
        assertEquals(true, events.isEmpty())
    }

    @Test
    fun theRenderStateSurvivesEveryArrangement() {
        val arrangements = listOf(
            Arrangement.Compact(false),
            Arrangement.Expanded(null),
            Arrangement.Expanded(HingeBounds(500, 0, 520, 800)),
            Arrangement.Tabletop(HingeBounds(0, 400, 1_000, 420)),
        )
        var arrangement by mutableStateOf<Arrangement>(arrangements.first())
        compose.setContent {
            CompositionLocalProvider(LocalArrangement provides arrangement) {
                PetruTheme { ExportScreen(state(ExportState.Rendering(64)), {}, {}) }
            }
        }
        arrangements.forEach {
            arrangement = it
            compose.waitForIdle()
            compose.onNodeWithText("64%").assertExists()
        }
    }

    @Test
    fun accessibilityChecksPassIncludingALargeFont() {
        for (scale in listOf(1f, 2f)) {
            runAndroidComposeUiTest<ComponentActivity> {
                enableAccessibilityChecks()
                setContent {
                    val d = LocalDensity.current
                    CompositionLocalProvider(LocalDensity provides Density(d.density, scale)) {
                        PetruTheme { ExportScreen(state(ExportState.Rendering(40)), {}, {}) }
                    }
                }
                onRoot().tryPerformAccessibilityChecks()
            }
        }
    }
}
