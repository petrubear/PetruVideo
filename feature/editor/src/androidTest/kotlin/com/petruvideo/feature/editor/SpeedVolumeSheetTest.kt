package com.petruvideo.feature.editor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.petruvideo.core.designsystem.PetruTheme
import com.petruvideo.feature.editor.ui.SpeedVolumeSheet
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SpeedVolumeSheetTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(sheet: SheetState): MutableList<EditorEvent> {
        val events = mutableListOf<EditorEvent>()
        compose.setContent { PetruTheme { SpeedVolumeSheet(sheet, onEvent = { events += it }) } }
        return events
    }

    @Test
    fun speedSheetShowsReadoutQuickChoicesAndCommitsOnTap() {
        val events = show(SheetState(ClipTool.Speed, "c1", "clip.mp4", 1000, 10_000))
        compose.onNodeWithText("1.00x").assertIsDisplayed()
        compose.onNodeWithContentDescription("Speed").assertIsDisplayed()
        compose.onNodeWithText("2x").performClick()
        assertEquals(EditorEvent.SheetCommit(ClipTool.Speed, 2000), events.last())
        compose.onNodeWithText("0.25x").assertIsDisplayed()
        compose.onNodeWithText("4x").assertIsDisplayed()
    }

    @Test
    fun volumeSheetShowsBoostAboveOneHundredPercent() {
        show(SheetState(ClipTool.Volume, "a1", "song.mp3", 150, 2_000))
        compose.onNodeWithText("150%").assertIsDisplayed()
        compose.onNodeWithText("Boost").assertIsDisplayed()
        compose.onNodeWithContentDescription("Volume").assertIsDisplayed()
    }

    @Test
    fun resetAndDoneSendTheirEvents() {
        val events = show(SheetState(ClipTool.Volume, "a1", "song.mp3", 40, 2_000))
        compose.onNodeWithText("Reset").performClick()
        assertEquals(EditorEvent.SheetReset(ClipTool.Volume), events.last())
        compose.onNodeWithText("Done").performClick()
        assertEquals(EditorEvent.SheetDismiss, events.last())
    }
}
