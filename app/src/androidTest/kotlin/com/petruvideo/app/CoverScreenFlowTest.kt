package com.petruvideo.app

import android.app.UiAutomation
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Rule
import org.junit.Test

/**
 * The compact (cover screen) flow: create a project, land in the editor, and keep editing across a configuration
 * change, which is the same path the unfold takes (FR-026). Run on the cover-screen profile (360x840 dp).
 */
class CoverScreenFlowTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val automation = InstrumentationRegistry.getInstrumentation().uiAutomation

    @After
    fun unfreezeRotation() {
        automation.setRotation(UiAutomation.ROTATION_UNFREEZE)
    }

    @Test
    fun newProjectOpensTheEditorAndSurvivesRotation() {
        compose.onAllNodes(hasContentDescription("New project") or hasText("New project", substring = true))
            .onFirst()
            .performClick()
        compose.waitUntil(TIMEOUT_MS) {
            compose.onAllNodesWithContentDescription("Undo").fetchSemanticsNodes().isNotEmpty()
        }
        val activity = compose.activity

        automation.setRotation(UiAutomation.ROTATION_FREEZE_90)
        compose.waitForIdle()

        check(compose.activity === activity) { "activity was recreated" }
        compose.onAllNodesWithContentDescription("Undo").onFirst().assertExists()
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
    }
}
