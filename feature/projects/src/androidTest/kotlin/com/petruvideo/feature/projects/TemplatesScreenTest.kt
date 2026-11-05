package com.petruvideo.feature.projects

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runAndroidComposeUiTest
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.Density
import com.petruvideo.core.designsystem.PetruTheme
import com.petruvideo.feature.projects.templates.TemplatesScreen
import com.petruvideo.feature.projects.templates.TemplatesState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import com.petruvideo.core.designsystem.UiText
import com.petruvideo.feature.projects.R

class TemplatesScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theListShowsTemplatesAndUseSendsItsId() {
        val used = mutableListOf<String>()
        compose.setContent { PetruTheme { TemplatesScreen(TemplatesState(), { used += it }, {}, {}) } }
        compose.onNodeWithText("Memories").assertExists()
        compose.onAllNodesWithText("Use")[1].performClick()
        assertEquals(listOf("memories"), used)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun passesAccessibilityChecksAtDoubleTextSize() = runAndroidComposeUiTest<ComponentActivity> {
        enableAccessibilityChecks()
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                PetruTheme { TemplatesScreen(
                    TemplatesState(message = UiText.Res(R.string.projects_only_first_files_used, 4)),
                    {},
                    {},
                    {},
                ) }
            }
        }
        onRoot().tryPerformAccessibilityChecks()
    }
}
