package com.petruvideo.feature.projects

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runAndroidComposeUiTest
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import com.petruvideo.core.designsystem.PetruTheme
import com.petruvideo.feature.projects.ui.ProjectsScreen
import org.junit.Test

/** Runs the Android accessibility checks (touch target size, labels, contrast) over Projects. */
@OptIn(ExperimentalTestApi::class)
class AccessibilityTest {
    private fun check(state: ProjectsState) = runAndroidComposeUiTest<ComponentActivity> {
        enableAccessibilityChecks()
        setContent { PetruTheme { ProjectsScreen(state, onEvent = {}) } }
        onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun populated() = check(
        ProjectsState(
            isLoading = false,
            continueEditing = summary("a", "Coast road", modifiedAt = 1),
            projects = listOf(summary("a", "Coast road"), summary("b", "Dive reel")),
            counts = ProjectCounts(2, 2, 0),
        ),
    )

    @Test
    fun empty() = check(ProjectsState(isLoading = false))
}
