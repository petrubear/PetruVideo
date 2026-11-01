package com.petruvideo.feature.export

import androidx.activity.ComponentActivity
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
import com.petruvideo.core.designsystem.PetruTheme
import com.petruvideo.core.model.export.ExportRecord
import com.petruvideo.core.model.export.ExportSettings
import com.petruvideo.core.model.export.FrameRate
import com.petruvideo.core.model.export.Quality
import com.petruvideo.core.model.export.Resolution
import com.petruvideo.feature.export.ui.ExportsScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ExportsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val settings = ExportSettings(Resolution.R1080, FrameRate.F30, Quality.Recommended)

    private fun row(id: String, missing: Boolean = false) = ExportRow(
        ExportRecord(id, "Trip $id", "content://media/$id", 21_000, 32_000_000, settings, 1_793_000_000_000), missing,
    )

    private fun show(state: ExportsUiState): MutableList<ExportsEvent> {
        val events = mutableListOf<ExportsEvent>()
        compose.setContent { PetruTheme { ExportsScreen(state, { events += it }, {}) } }
        return events
    }

    @Test
    fun actionsSendEventsForTheRow() {
        val events = show(ExportsUiState(listOf(row("a")), loaded = true))
        compose.onNodeWithContentDescription("Play Trip a").performClick()
        assertEquals(ExportsEvent.Open("a"), events.last())
        compose.onNodeWithContentDescription("Share Trip a").performClick()
        assertEquals(ExportsEvent.Share("a"), events.last())
        compose.onNodeWithContentDescription("Remove Trip a from the list").performClick()
        assertEquals(ExportsEvent.Remove("a"), events.last())
    }

    @Test
    fun aMissingFileOffersOnlyRemove() {
        show(ExportsUiState(listOf(row("a", missing = true)), loaded = true))
        compose.onNodeWithText("File not found").assertIsDisplayed()
        compose.onNodeWithContentDescription("Play Trip a").assertDoesNotExist()
        compose.onNodeWithContentDescription("Share Trip a").assertDoesNotExist()
        compose.onNodeWithContentDescription("Remove Trip a from the list").assertIsDisplayed()
    }

    @Test
    fun emptyAndUndoStatesShowTheirMessages() {
        val events = show(ExportsUiState(emptyList(), loaded = true, undo = row("z").record))
        compose.onNodeWithText("Videos you export appear here.").assertIsDisplayed()
        compose.onNodeWithText("Undo").performClick()
        assertEquals(ExportsEvent.Undo, events.last())
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun passesAccessibilityChecks() = runAndroidComposeUiTest<ComponentActivity> {
        enableAccessibilityChecks()
        setContent {
            PetruTheme {
                ExportsScreen(ExportsUiState(listOf(row("a"), row("b", missing = true)), loaded = true), {}, {})
            }
        }
        onRoot().tryPerformAccessibilityChecks()
    }
}
