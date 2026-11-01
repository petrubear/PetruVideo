package com.petruvideo.feature.editor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.petruvideo.core.designsystem.PetruTheme
import com.petruvideo.core.model.Keyframe
import com.petruvideo.core.model.Pose
import com.petruvideo.feature.editor.ui.KeyframePanel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class KeyframePanelTest {
    @get:Rule
    val compose = createComposeRule()

    private val frames = listOf(
        Keyframe(0, Pose(250, 300, 300, 100)),
        Keyframe(1_500, Pose(750, 700, 400, 50)),
    )

    private fun show(selected: Long?): MutableList<EditorEvent> {
        val events = mutableListOf<EditorEvent>()
        val panel = KeyframePanelState("o1", frames, selected, 500, 3_000)
        compose.setContent { PetruTheme { KeyframePanel(panel, onEvent = { events += it }) } }
        return events
    }

    @Test
    fun withNothingSelectedOnlyAddAndJumpAreThere() {
        val events = show(null)
        compose.onNodeWithText("2 keyframes").assertIsDisplayed()
        compose.onNodeWithText("Delete keyframe").assertDoesNotExist()
        compose.onNodeWithText("Add keyframe").performClick()
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("Previous").performClick()
        assertEquals(
            listOf(EditorEvent.KeyframeAdd, EditorEvent.KeyframeJump(true), EditorEvent.KeyframeJump(false)), events,
        )
    }

    @Test
    fun aVideoClipPanelShowsZoomAndOpacityAndSendsFramingChanges() {
        val events = mutableListOf<EditorEvent>()
        val panel = KeyframePanelState("c1", frames, null, 500, 3_000, Pose(500, 500, 1_500, 90))
        compose.setContent { PetruTheme { KeyframePanel(panel, onEvent = { events += it }) } }
        compose.onNodeWithText("Zoom").assertIsDisplayed()
        compose.onNodeWithText("150%").assertIsDisplayed()
        compose.onNodeWithText("Opacity").assertIsDisplayed()
        compose.onNodeWithText("90%").assertIsDisplayed()
    }

    @Test
    fun aSelectedKeyframeOffersMotionCopyAndDelete() {
        val events = show(1_500)
        compose.onNodeWithText("Keyframe 2 of 2").assertIsDisplayed()
        compose.onNodeWithText("Ease").performClick()
        compose.onNodeWithText("Copy here").performClick()
        compose.onNodeWithText("Delete keyframe").performClick()
        compose.onNodeWithText("Done").performClick()
        assertEquals(
            listOf(
                EditorEvent.KeyframeMotion(true), EditorEvent.KeyframeCopyHere, EditorEvent.KeyframeDelete,
                EditorEvent.KeyframeDismiss,
            ),
            events,
        )
    }
}
