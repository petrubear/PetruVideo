package com.petruvideo.feature.editor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.petruvideo.core.designsystem.PetruTheme
import com.petruvideo.core.model.ClipFilter
import com.petruvideo.core.model.FilterLook
import com.petruvideo.feature.editor.ui.FilterPanel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FilterPanelTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(kind: FilterToolKind, filter: ClipFilter = ClipFilter(), canApplyAll: Boolean = true): MutableList<EditorEvent> {
        val events = mutableListOf<EditorEvent>()
        val panel = FilterPanelState(kind, "c1", filter, null, 0, canApplyAll)
        compose.setContent { PetruTheme { FilterPanel(panel, onEvent = { events += it }) } }
        return events
    }

    @Test
    fun looksCommitAndIntensityShowsOnlyWithALook() {
        val events = show(FilterToolKind.Looks)
        compose.onNodeWithContentDescription("Intensity").assertDoesNotExist()
        compose.onNodeWithContentDescription("Look Warm").performScrollTo().performClick()
        assertEquals(EditorEvent.FilterCommit(ClipFilter(look = FilterLook.Warm)), events.last())
    }

    @Test
    fun withALookTheIntensitySliderAndActionsAreThere() {
        val events = show(FilterToolKind.Looks, ClipFilter(look = FilterLook.Faded, intensity = 60))
        compose.onNodeWithContentDescription("Intensity").assertIsDisplayed()
        compose.onNodeWithText("Apply to all").performClick()
        assertEquals(EditorEvent.FilterApplyAll, events.last())
        compose.onNodeWithText("Remove filter").performClick()
        assertEquals(EditorEvent.FilterRemove, events.last())
    }

    @Test
    fun applyToAllDoesNothingWithOneClip() {
        val events = show(FilterToolKind.Looks, canApplyAll = false)
        compose.onNodeWithText("Apply to all").performClick()
        assertEquals(emptyList<EditorEvent>(), events)
    }

    @Test
    fun adjustShowsFiveSlidersAndReset() {
        val events = show(FilterToolKind.Adjust, ClipFilter(brightness = 20))
        for (name in listOf("Brightness", "Contrast", "Saturation", "Warmth", "Vignette")) {
            compose.onNodeWithContentDescription(name).performScrollTo().assertIsDisplayed()
        }
        compose.onNodeWithText("Reset").performScrollTo().performClick()
        assertEquals(EditorEvent.FilterResetAdjustments, events.last())
    }
}
