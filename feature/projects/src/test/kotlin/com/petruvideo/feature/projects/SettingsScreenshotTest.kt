package com.petruvideo.feature.projects

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.petruvideo.core.designsystem.PetruTheme
import com.petruvideo.core.model.AppSettings
import com.petruvideo.core.model.export.Quality
import com.petruvideo.feature.projects.settings.SettingsScreen
import org.junit.Rule
import org.junit.Test

/** Goldens for the Settings screen. */
class SettingsScreenshotTest {
    @get:Rule
    val phone = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(screenHeight = 1_400))

    @Test
    fun defaults() = phone.snapshot {
        PetruTheme { SettingsScreen(AppSettings(), "0.1.0", {}, {}, {}) }
    }

    @Test
    fun changed() = phone.snapshot {
        PetruTheme { SettingsScreen(AppSettings(Quality.Smaller, true), "0.1.0", {}, {}, {}) }
    }
}
