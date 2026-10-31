package com.petruvideo.feature.editor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.petruvideo.core.designsystem.PetruTheme
import com.petruvideo.core.model.Reverb
import com.petruvideo.core.model.ReverbPreset
import com.petruvideo.feature.editor.ui.ReverbPanel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ReverbPanelTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(reverb: Reverb): MutableList<EditorEvent> {
        val events = mutableListOf<EditorEvent>()
        compose.setContent {
            PetruTheme { ReverbPanel(ReverbPanelState("c1", "clip.mp4", reverb), onEvent = { events += it }) }
        }
        return events
    }

    @Test
    fun switchPresetsAndMainSlidersAreThere() {
        val events = show(Reverb())
        compose.onNodeWithContentDescription("Reverb").assertIsDisplayed()
        compose.onNodeWithText("Hall").performClick()
        assertEquals(EditorEvent.ReverbPick(ReverbPreset.Hall), events.last())
        compose.onNodeWithContentDescription("Mix").assertIsDisplayed()
        compose.onNodeWithContentDescription("Decay time").assertIsDisplayed()
        compose.onNodeWithContentDescription("Reverb").performClick()
        assertEquals(EditorEvent.ReverbToggle(true), events.last())
    }

    @Test
    fun moreRevealsTheOtherThreeSettings() {
        show(Reverb(enabled = true))
        compose.onNodeWithText("More").performClick()
        compose.onNodeWithContentDescription("Room size").assertExists()
        compose.onNodeWithContentDescription("Damping").assertExists()
        compose.onNodeWithContentDescription("Pre-delay").assertExists()
        compose.onNodeWithText("Less").assertIsDisplayed()
    }

    @Test
    fun customShowsAndResetAndDoneSendEvents() {
        val events = show(Reverb(true, ReverbPreset.Custom, 40, 2_400, 65, 45, 55))
        compose.onNodeWithText("Custom").assertIsDisplayed()
        compose.onNodeWithText("Reset").performClick()
        assertEquals(EditorEvent.ReverbReset, events.last())
        compose.onNodeWithText("Done").performClick()
        assertEquals(EditorEvent.ReverbDismiss, events.last())
    }
}
