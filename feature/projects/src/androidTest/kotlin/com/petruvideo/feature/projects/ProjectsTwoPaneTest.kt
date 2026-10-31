package com.petruvideo.feature.projects

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.petruvideo.core.designsystem.PetruTheme
import com.petruvideo.feature.projects.ui.ProjectsScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProjectsTwoPaneTest {
    @get:Rule
    val compose = createComposeRule()

    private val state = ProjectsState(
        isLoading = false,
        projects = listOf(summary("a", "Coast road"), summary("b", "Dive reel")),
        counts = ProjectCounts(2, 2, 0),
    )

    private fun show(state: ProjectsState): MutableList<ProjectsEvent> {
        val events = mutableListOf<ProjectsEvent>()
        compose.setContent {
            PetruTheme {
                // The two-pane layout is for expanded windows (700 dp and up), so give it that much room.
                Box(Modifier.requiredWidth(1_040.dp).requiredHeight(780.dp)) {
                    ProjectsScreen(state, onEvent = { events += it }, twoPane = true)
                }
            }
        }
        return events
    }

    @Test
    fun theLatestProjectIsShownInTheDetailPaneWithoutNavigation() {
        show(state)
        compose.onNodeWithText("Open editor").assertExists()
        compose.onNodeWithText("Media in this project").assertExists()
    }

    @Test
    fun selectingAProjectSendsSelectNotOpen() {
        val events = show(state)
        compose.onNodeWithText("Dive reel").performClick()
        assertEquals(listOf<ProjectsEvent>(ProjectsEvent.SelectProject("b")), events)
    }

    @Test
    fun openEditorOpensTheSelectedProject() {
        val events = show(state.copy(selectedProjectId = "b"))
        compose.onNodeWithText("Open editor").performClick()
        assertEquals(ProjectsEvent.OpenProject("b"), events.last())
    }

    @Test
    fun exportIsDimmedAndOnlySaysComingSoon() {
        val events = show(state)
        compose.onNodeWithText("Export").performClick()
        assertEquals(ProjectsEvent.TapInactive("Export"), events.last())
    }
}
