package com.petruvideo.feature.projects

import app.cash.paparazzi.DeviceConfig
import com.android.resources.NightMode
import app.cash.paparazzi.Paparazzi
import com.petruvideo.core.designsystem.PetruTheme
import com.petruvideo.feature.projects.templates.TemplatesScreen
import com.petruvideo.feature.projects.templates.TemplatesState
import org.junit.Rule
import org.junit.Test

/** Goldens for the Templates screen: one column on a phone, two on a tablet. */
class TemplatesScreenshotTest {
    @get:Rule
    val phone = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(nightMode = NightMode.NIGHT, screenHeight = 2_400))

    @Test
    fun phone() = phone.snapshot {
        PetruTheme { TemplatesScreen(TemplatesState(), {}, {}, {}) }
    }
}

/** Goldens for the Templates screen on a tablet, where the cards sit in two columns. */
class TemplatesTabletScreenshotTest {
    @get:Rule
    val tablet = Paparazzi(deviceConfig = DeviceConfig.NEXUS_10.copy(nightMode = NightMode.NIGHT))

    @Test
    fun twoColumns() = tablet.snapshot {
        PetruTheme { TemplatesScreen(TemplatesState(), {}, {}, {}) }
    }

    @Test
    fun lightTwoColumns() = tablet.snapshot {
        PetruTheme(darkTheme = false) { TemplatesScreen(TemplatesState(), {}, {}, {}) }
    }

    @Test
    fun spanishTwoColumns() = spanishTablet().snapshot {
        PetruTheme { TemplatesScreen(TemplatesState(), {}, {}, {}) }
    }

    /** The same device with the phone set to Spanish. */
    private fun spanishTablet(): Paparazzi {
        tablet.unsafeUpdateConfig(deviceConfig = DeviceConfig.NEXUS_10.copy(locale = "es", nightMode = NightMode.NIGHT))
        return tablet
    }
}
