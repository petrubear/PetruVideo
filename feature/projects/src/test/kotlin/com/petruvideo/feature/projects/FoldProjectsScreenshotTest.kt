package com.petruvideo.feature.projects

import app.cash.paparazzi.DeviceConfig
import com.android.resources.NightMode
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import com.petruvideo.core.data.repo.ProjectFilter
import com.petruvideo.core.designsystem.PetruTheme
import com.petruvideo.core.model.ProjectStatus
import com.petruvideo.feature.projects.ui.ProjectsScreen
import org.junit.Rule
import org.junit.Test

/** Golden for the two-pane Projects view (1040 x 780 dp), reviewed against design/reference/FoldLibrary.png. */
class FoldProjectsScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(
            nightMode = NightMode.NIGHT, screenWidth = 2_080,
            screenHeight = 1_560,
            xdpi = 320,
            ydpi = 320,
            density = Density.XHIGH,
            orientation = ScreenOrientation.LANDSCAPE,
        ),
    )

    private val now = 10_000_000_000L

    private val state = ProjectsState(
        isLoading = false,
        projects = listOf(
            summary("coast", "Coast road, day 3", modifiedAt = now - 120_000, clips = 8, durationMs = 102_000),
            summary("dive", "Dive reel", modifiedAt = now - 100_000_000, clips = 5, durationMs = 48_000),
            summary("night", "Night market", ProjectStatus.Exported, now - 500_000_000, 11, 135_000),
            summary("ferry", "Ferry crossing", modifiedAt = now - 900_000_000, clips = 3, durationMs = 36_000),
        ),
        filter = ProjectFilter.All,
        counts = ProjectCounts(all = 14, drafts = 6, exported = 8),
    )

    @Test
    fun twoPaneProjects() {
        paparazzi.snapshot { PetruTheme { ProjectsScreen(state, onEvent = {}, nowMs = now, twoPane = true) } }
    }
}
