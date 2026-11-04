package com.petruvideo.feature.importmedia

import app.cash.paparazzi.DeviceConfig
import com.android.resources.NightMode
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import com.petruvideo.core.designsystem.PetruTheme
import com.petruvideo.core.designsystem.adaptive.Arrangement
import com.petruvideo.core.designsystem.adaptive.HingeBounds
import com.petruvideo.core.model.MediaKind
import com.petruvideo.feature.importmedia.ui.AddMediaPanel
import com.petruvideo.feature.importmedia.ui.AddMediaScreen
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test

/** Grid columns per arrangement (FR-027a) and the golden for the expanded Add media panel. */
class AddMediaLayoutTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(
            nightMode = NightMode.NIGHT, screenWidth = 2_080,
            screenHeight = 1_560,
            xdpi = 320,
            ydpi = 320,
            density = Density.XHIGH,
            orientation = ScreenOrientation.LANDSCAPE,
        ),
    )

    @Test
    fun `columns follow the arrangement`() {
        columnsFor(Arrangement.Compact(wideContent = false)) shouldBe 3
        columnsFor(Arrangement.Compact(wideContent = true)) shouldBe 5
        columnsFor(Arrangement.Expanded(hinge = null)) shouldBe 6
        columnsFor(Arrangement.Tabletop(HingeBounds(0, 780, 2_080, 800))) shouldBe 6
    }

    @Test
    fun expandedPanel() {
        val items = listOf(14, 32, 8, 65, 21, 11, 47, 19, 26, 9, 38, 15).mapIndexed { i, s ->
            GridItem("content://lib/$i", MediaKind.Video, "clip$i.mp4", s * 1_000L, null)
        }
        val state = AddMediaState(items = items, totalDurationMs = 0, permission = MediaAccess.Granted)
        paparazzi.snapshot { PetruTheme { AddMediaPanel { AddMediaScreen(state, onEvent = {}, columns = 6) } } }
    }
}
