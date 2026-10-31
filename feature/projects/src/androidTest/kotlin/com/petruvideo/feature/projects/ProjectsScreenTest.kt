package com.petruvideo.feature.projects

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.petruvideo.core.designsystem.PetruTheme
import com.petruvideo.feature.projects.ui.ProjectsScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProjectsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(state: ProjectsState): MutableList<ProjectsEvent> {
        val events = mutableListOf<ProjectsEvent>()
        val current = mutableStateOf(state)
        compose.setContent {
            PetruTheme { ProjectsScreen(current.value, onEvent = { events += it }) }
        }
        return events
    }

    private val populated = ProjectsState(
        isLoading = false,
        continueEditing = summary("a", "Coast road", modifiedAt = 1),
        projects = listOf(summary("a", "Coast road"), summary("b", "Dive reel")),
        counts = ProjectCounts(2, 2, 0),
    )

    @Test
    fun newProjectTileSendsNewProject() {
        val events = show(populated)
        compose.onNodeWithContentDescription("New project").performClick()
        assertEquals(listOf<ProjectsEvent>(ProjectsEvent.NewProject), events)
    }

    @Test
    fun cardOptionsMenuOffersRenameDuplicateDelete() {
        show(populated)
        compose.onNodeWithContentDescription("Project options, Dive reel").performClick()
        compose.onNodeWithText("Rename").assertIsDisplayed()
        compose.onNodeWithText("Duplicate").assertIsDisplayed()
        compose.onNodeWithText("Delete").assertIsDisplayed()
    }

    @Test
    fun deleteAsksForConfirmation() {
        val events = show(populated.copy(pendingDeleteId = "b"))
        compose.onNodeWithText("Delete this project?").assertIsDisplayed()
        compose.onNodeWithText("Delete").performClick()
        assertEquals(listOf<ProjectsEvent>(ProjectsEvent.ConfirmDelete), events)
    }

    @Test
    fun emptyStateShowsOnlyTheNewProjectTile() {
        show(ProjectsState(isLoading = false))
        compose.onNodeWithContentDescription("New project").assertIsDisplayed()
        compose.onNodeWithText("CONTINUE EDITING").assertDoesNotExist()
    }

    @Test
    fun noResultsMessageShowsForAQueryWithNoMatches() {
        show(ProjectsState(isLoading = false, query = "zzz", noResults = true, searchOpen = true))
        compose.onNodeWithText("No projects match").assertIsDisplayed()
    }

    @Test
    fun inactiveNavigationEntriesShowComingSoon() {
        val events = show(populated)
        compose.onNodeWithText("Templates").performClick()
        assertEquals(listOf<ProjectsEvent>(ProjectsEvent.TapInactive("Templates")), events)
    }

    @Test
    fun comingSoonMessageIsShown() {
        show(populated.copy(comingSoon = "Exports"))
        compose.onNodeWithText("Coming soon").assertIsDisplayed()
    }
}



