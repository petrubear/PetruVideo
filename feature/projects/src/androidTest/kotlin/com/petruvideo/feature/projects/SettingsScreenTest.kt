package com.petruvideo.feature.projects

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runAndroidComposeUiTest
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.Density
import com.petruvideo.core.designsystem.PetruTheme
import com.petruvideo.core.model.AppSettings
import com.petruvideo.core.model.export.Quality
import com.petruvideo.feature.projects.settings.SettingsScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun choicesAndTheSwitchSendTheirValues() {
        var quality: Quality? = null
        var lower: Boolean? = null
        compose.setContent {
            PetruTheme { SettingsScreen(AppSettings(), "0.1.0", { quality = it }, { lower = it }, {}) }
        }
        compose.onNodeWithText("Smaller").performClick()
        assertEquals(Quality.Smaller, quality)
        compose.onNodeWithContentDescription("Lower preview quality").performClick()
        assertEquals(true, lower)
        compose.onNodeWithContentDescription("PetruVideo version 0.1.0").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun passesAccessibilityChecksAtDoubleTextSize() = runAndroidComposeUiTest<ComponentActivity> {
        enableAccessibilityChecks()
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                PetruTheme { SettingsScreen(AppSettings(Quality.Higher, true), "0.1.0", {}, {}, {}) }
            }
        }
        onRoot().tryPerformAccessibilityChecks()
    }
}
