package com.petruvideo.feature.editor

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.down
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.up
import com.petruvideo.core.designsystem.PetruTheme
import com.petruvideo.core.model.TextClip
import com.petruvideo.feature.editor.ui.TextLaneView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TextLaneTest {
    @get:Rule
    val compose = createComposeRule()

    private val clips = listOf(
        TextClip("a", "First title", 0, 4_000),
        TextClip("b", "Second overlaps", 2_000, 4_000),
    )

    private val selected = mutableListOf<String>()
    private val trims = mutableListOf<Triple<String, Long, Long>>()

    private fun show(selectedId: String? = null) {
        compose.setContent {
            PetruTheme {
                TextLaneView(
                    clips, TimelineGeometry(), selectedId, { selected += it }, { _, _ -> },
                    { id, s, e -> trims += Triple(id, s, e) },
                )
            }
        }
    }

    @Test
    fun clipsExposeTheirWordsAndTimesAndAreSelectable() {
        show()
        compose.onNodeWithContentDescription("Text, First title, starts at 0 seconds, lasts 4 seconds").performClick()
        compose.onNodeWithContentDescription("Text, Second overlaps, starts at 2 seconds, lasts 4 seconds").performClick()
        assertEquals(listOf("a", "b"), selected)
    }

    @Test
    fun overlappingClipsAreOnSeparateRows() {
        show()
        val first = compose.onNodeWithContentDescription("Text, First title, starts at 0 seconds, lasts 4 seconds")
            .fetchSemanticsNode().boundsInRoot
        val second = compose.onNodeWithContentDescription("Text, Second overlaps, starts at 2 seconds, lasts 4 seconds")
            .fetchSemanticsNode().boundsInRoot
        assertTrue(second.top >= first.bottom)
    }

    @Test
    fun draggingTheEndHandleTrims() {
        show("a")
        compose.onNodeWithContentDescription("Trim text end", useUnmergedTree = true).performTouchInput {
            down(center)
            repeat(6) { moveBy(Offset(30f, 0f), delayMillis = 30) }
            up()
        }
        compose.waitForIdle()
        val trim = trims.single()
        assertEquals("a", trim.first)
        assertEquals(0L, trim.second)
        assertTrue(trim.third > 4_000)
    }
}
