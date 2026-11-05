package com.petruvideo.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.NightMode
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruTheme
import com.petruvideo.core.model.TextClip
import com.petruvideo.core.model.TextShadow
import com.petruvideo.core.model.TextStyle
import com.petruvideo.feature.editor.ui.TextStyleControls
import org.junit.Rule
import org.junit.Test

/** The whole Style tool of a text, tall enough to show the controls added in feature 036. */
class TextStyleControlsScreenshotTest {
    @get:Rule
    val paparazzi =
        Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(nightMode = NightMode.NIGHT, screenHeight = 3_000))

    private val clip = TextClip(
        "t", "Hello there", 0, 3_000,
        style = TextStyle(
            shadow = TextShadow(enabled = true, opacityPercent = 70),
            rotationDegrees = 15,
            letterSpacingPercent = 20,
            lineSpacingPercent = 120,
        ),
    )

    @Test
    fun shadowRotationAndSpacing() = paparazzi.snapshot {
        PetruTheme(darkTheme = true) {
            Box(Modifier.fillMaxSize().background(PetruColors.Background).padding(16.dp)) {
                TextStyleControls(clip, onEvent = {})
            }
        }
    }

    @Test
    fun shadowRotationAndSpacingLight() = paparazzi.snapshot {
        PetruTheme(darkTheme = false) {
            Box(Modifier.fillMaxSize().background(PetruColors.Background).padding(16.dp)) {
                TextStyleControls(clip, onEvent = {})
            }
        }
    }
}
