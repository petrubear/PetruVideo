package com.petruvideo.feature.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.down
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.up
import com.petruvideo.core.designsystem.PetruTheme
import com.petruvideo.core.model.Fingerprint
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.OverlayClip
import com.petruvideo.feature.editor.ui.OverlayLaneView
import com.petruvideo.feature.editor.ui.OverlayPanel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OverlayUiTest {
    @get:Rule
    val compose = createComposeRule()

    private val media = mapOf(
        "m" to MediaItem("m", MediaKind.Video, "content://m", "pip.mp4", 9_000, 1280, 720, 30f, false, Fingerprint(1, 1, 9_000, "video/mp4")),
    )
    private val clips = listOf(OverlayClip("a", "m", 0, 4_000, 0), OverlayClip("b", "m", 0, 4_000, 2_000))

    @Test
    fun panelShowsOpacityAndDoneDismisses() {
        val events = mutableListOf<EditorEvent>()
        compose.setContent { PetruTheme { OverlayPanel(OverlayPanelState(clips[0], "pip.mp4"), { events += it }) } }
        compose.onNodeWithContentDescription("Opacity").assertIsDisplayed()
        compose.onNodeWithText("Done").performClick()
        assertEquals(EditorEvent.OverlayDismiss, events.last())
    }

    @Test
    fun chromaKeyTabPicksAGreenKeyAndStartsPicking() {
        val events = mutableListOf<EditorEvent>()
        compose.setContent { PetruTheme { OverlayPanel(OverlayPanelState(clips[0], "pip.mp4"), { events += it }) } }
        compose.onNodeWithText("Chroma key").performClick()
        compose.onNodeWithText("Green").performClick()
        assertEquals(
            EditorEvent.ChromaCommit(com.petruvideo.core.model.ChromaKey(enabled = true)),
            events.last(),
        )
        compose.onNodeWithText("Pick colour").performClick()
        assertEquals(EditorEvent.ChromaPicking(true), events.last())
        compose.onNodeWithText("Checkerboard").performClick()
        assertEquals(EditorEvent.ChromaChecker(true), events.last())
    }

    @Test
    fun maskTabChoosesAShapeAndOffersSizeFeatherAndInvert() {
        val events = mutableListOf<EditorEvent>()
        val masked = clips[0].copy(
            mask = com.petruvideo.core.model.OverlayMask(com.petruvideo.core.model.MaskKind.Circle),
        )
        compose.setContent { PetruTheme { OverlayPanel(OverlayPanelState(masked, "pip.mp4"), { events += it }) } }
        compose.onNodeWithText("Mask").performClick()
        compose.onNodeWithContentDescription("Size").assertIsDisplayed()
        compose.onNodeWithContentDescription("Feather").assertIsDisplayed()
        compose.onNodeWithText("Invert").performClick()
        assertEquals(
            EditorEvent.MaskCommit(masked.mask.copy(invert = true)),
            events.last(),
        )
        compose.onNodeWithText("Rectangle").performClick()
        assertEquals(
            EditorEvent.MaskCommit(masked.mask.copy(kind = com.petruvideo.core.model.MaskKind.Rectangle)),
            events.last(),
        )
    }

    @Test
    fun laneClipsAreSelectableOnSeparateRowsAndTheEndHandleTrims() {
        val selected = mutableListOf<String>()
        val trims = mutableListOf<Triple<String, Long, Long>>()
        compose.setContent {
            PetruTheme {
                OverlayLaneView(clips, media, TimelineGeometry(), "a", { selected += it }, { _, _ -> }, { id, s, e -> trims += Triple(id, s, e) })
            }
        }
        val a = compose.onNodeWithContentDescription("Overlay, pip.mp4, starts at 0 seconds, lasts 4 seconds").fetchSemanticsNode().boundsInRoot
        val b = compose.onNodeWithContentDescription("Overlay, pip.mp4, starts at 2 seconds, lasts 4 seconds").fetchSemanticsNode().boundsInRoot
        assertTrue(b.top >= a.bottom)
        compose.onNodeWithContentDescription("Overlay, pip.mp4, starts at 2 seconds, lasts 4 seconds").performClick()
        assertEquals(listOf("b"), selected)
        compose.onNodeWithContentDescription("Trim overlay end", useUnmergedTree = true).performTouchInput {
            down(center)
            repeat(5) { moveBy(Offset(-30f, 0f), delayMillis = 30) }
            up()
        }
        compose.waitForIdle()
        assertEquals("a", trims.single().first)
        assertTrue(trims.single().third < 4_000)
    }
}
