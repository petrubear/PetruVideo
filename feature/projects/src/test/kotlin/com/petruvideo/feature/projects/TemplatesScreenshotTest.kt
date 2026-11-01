package com.petruvideo.feature.projects

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.petruvideo.core.designsystem.PetruTheme
import com.petruvideo.feature.projects.templates.TemplatesScreen
import com.petruvideo.feature.projects.templates.TemplatesState
import org.junit.Rule
import org.junit.Test

/** Goldens for the Templates screen: one column on a phone, two on a tablet. */
class TemplatesScreenshotTest {
    @get:Rule
    val phone = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(screenHeight = 2_400))

    @Test
    fun phone() = phone.snapshot {
        PetruTheme { TemplatesScreen(TemplatesState(), {}, {}, {}) }
    }
}

/** Goldens for the Templates screen on a tablet, where the cards sit in two columns. */
class TemplatesTabletScreenshotTest {
    @get:Rule
    val tablet = Paparazzi(deviceConfig = DeviceConfig.NEXUS_10)

    @Test
    fun twoColumns() = tablet.snapshot {
        PetruTheme { TemplatesScreen(TemplatesState(), {}, {}, {}) }
    }
}
