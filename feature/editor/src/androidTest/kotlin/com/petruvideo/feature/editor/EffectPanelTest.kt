package com.petruvideo.feature.editor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.petruvideo.core.designsystem.PetruTheme
import com.petruvideo.core.model.ClipEffect
import com.petruvideo.core.model.EffectKind
import com.petruvideo.core.model.Transition
import com.petruvideo.core.model.TransitionKind
import com.petruvideo.feature.editor.ui.EffectPanel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class EffectPanelTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(panel: EffectPanelState): MutableList<EditorEvent> {
        val events = mutableListOf<EditorEvent>()
        compose.setContent { PetruTheme { EffectPanel(panel, onEvent = { events += it }) } }
        return events
    }

    @Test
    fun aTransitionTileSetsItWithTheShownLength() {
        val events = show(EffectPanelState(EffectToolKind.Transition, "c1", null, null, 1_500))
        compose.onNodeWithContentDescription("Length").assertDoesNotExist()
        compose.onNodeWithContentDescription("Transition Slide").performScrollTo().performClick()
        assertEquals(EditorEvent.TransitionSet(TransitionKind.Slide, 1_000), events.last())
    }

    @Test
    fun withATransitionTheLengthSliderAndRemoveAreThere() {
        val events = show(
            EffectPanelState(EffectToolKind.Transition, "c1", Transition(TransitionKind.Zoom, 500), null, 1_500),
        )
        compose.onNodeWithContentDescription("Length").assertIsDisplayed()
        compose.onNodeWithText("Remove transition").performClick()
        assertEquals(EditorEvent.TransitionRemove, events.last())
    }

    @Test
    fun anEffectTileCommitsAndStrengthShowsOnlyWithAnEffect() {
        val events = show(EffectPanelState(EffectToolKind.Effect, "c1", null, null, 0))
        compose.onNodeWithContentDescription("Strength").assertDoesNotExist()
        compose.onNodeWithContentDescription("Effect Mirror").performScrollTo().performClick()
        assertEquals(EditorEvent.EffectCommit(ClipEffect(EffectKind.Mirror, 50)), events.last())
    }

    @Test
    fun withAnEffectTheStrengthSliderAndRemoveAreThere() {
        val events = show(
            EffectPanelState(EffectToolKind.Effect, "c1", null, ClipEffect(EffectKind.Blur, 30), 0),
        )
        compose.onNodeWithContentDescription("Strength").assertIsDisplayed()
        compose.onNodeWithText("Remove effect").performClick()
        assertEquals(EditorEvent.EffectRemove, events.last())
    }
}
