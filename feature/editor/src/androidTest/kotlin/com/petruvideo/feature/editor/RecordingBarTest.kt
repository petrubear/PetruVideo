package com.petruvideo.feature.editor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.petruvideo.core.designsystem.PetruTheme
import com.petruvideo.feature.editor.ui.RecordingBar
import com.petruvideo.feature.editor.ui.timerText
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RecordingBarTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(take: VoiceOverUi): MutableList<EditorEvent> {
        val events = mutableListOf<EditorEvent>()
        compose.setContent { PetruTheme { RecordingBar(take, onEvent = { events += it }) } }
        return events
    }

    @Test
    fun theCountInShowsTheDigitTheHintAndCancel() {
        val events = show(VoiceOverUi.CountIn(3))
        compose.onNodeWithContentDescription("Recording starts in 3").assertIsDisplayed()
        compose.onNodeWithText("Use headphones for a clean voice").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        assertEquals(listOf<EditorEvent>(EditorEvent.CancelVoiceOver), events)
    }

    @Test
    fun theRunningTakeShowsTheTimerTheMeterAndStop() {
        val events = show(VoiceOverUi.Recording(7_400, 0.5f))
        compose.onNodeWithText("0:07").assertIsDisplayed()
        compose.onNodeWithContentDescription("Voice level").assertIsDisplayed()
        compose.onNodeWithContentDescription("Stop recording").performClick()
        assertEquals(listOf<EditorEvent>(EditorEvent.StopVoiceOver), events)
    }

    @Test
    fun theTimerCountsMinutesAndSeconds() {
        assertEquals("0:00", timerText(0))
        assertEquals("1:05", timerText(65_900))
        assertEquals("30:00", timerText(1_800_000))
    }
}
