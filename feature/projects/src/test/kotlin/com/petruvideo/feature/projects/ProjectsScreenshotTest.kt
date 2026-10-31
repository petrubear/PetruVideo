package com.petruvideo.feature.projects

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.petruvideo.core.data.repo.ProjectFilter
import com.petruvideo.core.designsystem.PetruTheme
import com.petruvideo.core.model.ProjectStatus
import com.petruvideo.feature.projects.ui.ProjectsScreen
import org.junit.Rule
import org.junit.Test

/** Golden for the compact Projects screen; reviewed against design/reference/Main.png before approval. */
class ProjectsScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(screenHeight = 1_900))

    private val now = 10_000_000_000L
    private val latest = summary(
        "coast",
        "Coast road, day 3",
        modifiedAt = now - 120_000,
        clips = 8,
        durationMs = 102_000,
    ).copy(playheadMs = 65_000)

    private val state = ProjectsState(
        isLoading = false,
        continueEditing = latest,
        projects = listOf(
            summary("dive", "Dive reel", modifiedAt = now - 100_000_000, durationMs = 48_000),
            summary(
                "night",
                "Night market",
                ProjectStatus.Exported,
                modifiedAt = now - 500_000_000,
                durationMs = 135_000,
            ),
        ),
        filter = ProjectFilter.All,
        counts = ProjectCounts(all = 14, drafts = 6, exported = 8),
    )

    @Test
    fun compactProjects() {
        paparazzi.snapshot { PetruTheme { ProjectsScreen(state, onEvent = {}, nowMs = now) } }
    }

    @Test
    fun emptyProjects() {
        paparazzi.snapshot {
            PetruTheme { ProjectsScreen(ProjectsState(isLoading = false), onEvent = {}, nowMs = now) }
        }
    }
}
