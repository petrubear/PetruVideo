package com.petruvideo.feature.editor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import com.petruvideo.core.designsystem.PetruTheme
import com.petruvideo.core.model.AnimKind
import com.petruvideo.core.model.TextAlign
import com.petruvideo.core.model.TextClip
import com.petruvideo.core.model.TextFont
import com.petruvideo.feature.editor.ui.TextPanel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TextPanelTest {
    @get:Rule
    val compose = createComposeRule()

    private val clip = TextClip("t1", "Hello", 1_000, 4_000)

    private fun show(kind: TextToolKind): MutableList<EditorEvent> {
        val events = mutableListOf<EditorEvent>()
        compose.setContent { PetruTheme { TextPanel(TextPanelState(kind, clip), onEvent = { events += it }) } }
        return events
    }

    @Test
    fun typingIsPreviewedLive() {
        val events = show(TextToolKind.Edit)
        compose.onNodeWithContentDescription("Text").performTextInput(" world")
        val preview = events.filterIsInstance<EditorEvent.TextPreview>().last()
        assertTrue(preview.clip.text, preview.clip.text.contains("world"))
        compose.onNodeWithText("Done").performClick()
        assertEquals(EditorEvent.TextDismiss, events.last())
    }

    @Test
    fun styleChipsCommitFontAndAlignment() {
        val events = show(TextToolKind.Style)
        compose.onNodeWithText("Mono").performClick()
        assertEquals(TextFont.Mono, (events.last() as EditorEvent.TextCommit).clip.style.font)
        compose.onNodeWithText("Right").performClick()
        assertEquals(TextAlign.Right, (events.last() as EditorEvent.TextCommit).clip.style.align)
    }

    @Test
    fun colorSwatchesAndSwitchesCommit() {
        val events = show(TextToolKind.Style)
        compose.onNodeWithContentDescription("Text color Red").performScrollTo().performClick()
        assertEquals(0xFFE53935.toInt(), (events.last() as EditorEvent.TextCommit).clip.style.colorArgb)
        compose.onNodeWithContentDescription("Background").performScrollTo().performClick()
        assertTrue((events.last() as EditorEvent.TextCommit).clip.style.background.enabled)
        compose.onNodeWithContentDescription("Outline").performScrollTo().performClick()
        assertTrue((events.last() as EditorEvent.TextCommit).clip.style.outline.enabled)
        compose.onNodeWithText("Reset style").performScrollTo().performClick()
        assertEquals(EditorEvent.TextReset(TextToolKind.Style), events.last())
    }

    @Test
    fun animationChipsCommitKindsAndShowALengthSlider() {
        val events = show(TextToolKind.Animation)
        compose.onNodeWithContentDescription("In length").assertDoesNotExist()
        compose.onNodeWithText("Slide up").performClick()
        assertEquals(AnimKind.SlideUp, (events.last() as EditorEvent.TextCommit).clip.animIn.kind)
        compose.onNodeWithText("Slide down").performClick()
        assertEquals(AnimKind.SlideDown, (events.last() as EditorEvent.TextCommit).clip.animOut.kind)
        compose.onNodeWithText("Reset animation").performScrollTo().assertIsDisplayed()
    }
}
