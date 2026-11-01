package com.petruvideo.feature.editor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.petruvideo.core.designsystem.PetruTheme
import com.petruvideo.core.model.AudioFx
import com.petruvideo.core.model.EqPreset
import com.petruvideo.core.model.EqPresets
import com.petruvideo.core.model.Fade
import com.petruvideo.core.model.NoiseReduction
import com.petruvideo.core.model.NoiseStrength
import com.petruvideo.core.model.Reverb
import com.petruvideo.feature.editor.ui.AudioPanel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AudioPanelTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(kind: AudioToolKind, fx: AudioFx = AudioFx(), length: Long = 8_000): MutableList<EditorEvent> {
        val events = mutableListOf<EditorEvent>()
        val panel = AudioPanelState(kind, "c1", "clip.mp4", fx, length, 80, false, Reverb())
        compose.setContent { PetruTheme { AudioPanel(panel, onEvent = { events += it }) } }
        return events
    }

    @Test
    fun theListShowsEveryToolWithItsStateAndOpensThem() {
        val events = show(AudioToolKind.List, AudioFx(noise = NoiseReduction(true, NoiseStrength.High)))
        compose.onNodeWithContentDescription("Volume, 80%").assertIsDisplayed()
        compose.onNodeWithContentDescription("Noise reduction, High").performClick()
        assertEquals(EditorEvent.OpenAudioTool(AudioToolKind.Noise), events.last())
        compose.onNodeWithContentDescription("Fade, Off").performClick()
        assertEquals(EditorEvent.OpenAudioTool(AudioToolKind.Fade), events.last())
        compose.onNodeWithContentDescription("Volume, 80%").performClick()
        assertEquals(EditorEvent.UseTool(ClipTool.Volume), events.last())
        compose.onNodeWithContentDescription("Reverb, Off").performScrollTo().performClick()
        assertEquals(EditorEvent.UseTool(ClipTool.Reverb), events.last())
    }

    @Test
    fun noiseStrengthChipsCommitWithTheStrengthAndTurnItOn() {
        val events = show(AudioToolKind.Noise)
        compose.onNodeWithText("High").performClick()
        assertEquals(
            EditorEvent.AudioFxCommit(AudioFx(noise = NoiseReduction(true, NoiseStrength.High))),
            events.last(),
        )
    }

    @Test
    fun equalizerPresetTapCommitsAllBandsAndResetAsksForNeutral() {
        val events = show(AudioToolKind.Eq)
        compose.onNodeWithText("Bass boost").performClick()
        assertEquals(
            EditorEvent.AudioFxCommit(AudioFx(eq = EqPresets.settings(EqPreset.BassBoost))),
            events.last(),
        )
        compose.onNodeWithContentDescription("Low").assertIsDisplayed()
        compose.onNodeWithText("Reset").performClick()
        assertEquals(EditorEvent.AudioFxReset(AudioToolKind.Eq), events.last())
    }

    @Test
    fun fadeShowsBothSlidersOrExplainsWhenTheClipIsTooShort() {
        show(AudioToolKind.Fade, AudioFx(fade = Fade(500, 0)))
        compose.onNodeWithContentDescription("Fade in").assertIsDisplayed()
        compose.onNodeWithContentDescription("Fade out").assertIsDisplayed()
    }

    @Test
    fun aShortClipHasNoFadeSliders() {
        show(AudioToolKind.Fade, length = 150)
        compose.onNodeWithText("This clip is too short for a fade").assertIsDisplayed()
    }

    @Test
    fun voiceSwitchesCommitAndSlidersAppearWhenOn() {
        val events = show(AudioToolKind.Voice)
        compose.onNodeWithContentDescription("Echo").performClick()
        assertEquals(true, (events.last() as EditorEvent.AudioFxCommit).fx.voice.echo.enabled)
        compose.onNodeWithText("Done").performClick()
        assertEquals(EditorEvent.AudioDismiss, events.last())
    }

    @Test
    fun toolsChipReturnsToTheList() {
        val events = show(AudioToolKind.Eq)
        compose.onNodeWithText("Tools").performClick()
        assertEquals(EditorEvent.OpenAudioTool(AudioToolKind.List), events.last())
    }
}
