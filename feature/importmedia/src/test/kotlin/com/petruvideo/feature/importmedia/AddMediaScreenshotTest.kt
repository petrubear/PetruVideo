package com.petruvideo.feature.importmedia

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.petruvideo.core.data.importing.ImportProgress
import com.petruvideo.core.designsystem.PetruTheme
import com.petruvideo.core.model.MediaKind
import com.petruvideo.feature.importmedia.ui.AddMediaScreen
import org.junit.Rule
import org.junit.Test

/** Golden for the compact Add media screen; reviewed against design/reference/Import.png before approval. */
class AddMediaScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    private val durations = listOf(14, 32, 8, 65, 21, 11, 47, 19, 26, 9, 38, 15)
    private val selectedOrder = mapOf(0 to 1, 2 to 2, 3 to 3, 6 to 4, 8 to 5)

    private val items = durations.mapIndexed { i, seconds ->
        GridItem("content://lib/$i", MediaKind.Video, "clip$i.mp4", seconds * 1_000L, selectedOrder[i])
    }

    private val selection = selectedOrder.keys.sorted().map {
        SelectedItem("content://lib/$it", MediaKind.Video, "clip$it.mp4", durations[it] * 1_000L)
    }

    private val state = AddMediaState(
        items = items,
        selection = selection,
        totalDurationMs = 147_000,
        permission = MediaAccess.Granted,
    )

    @Test
    fun compactAddMedia() {
        paparazzi.snapshot { PetruTheme { AddMediaScreen(state, onEvent = {}) } }
    }

    @Test
    fun importingShowsProgressAndCancel() {
        paparazzi.snapshot {
            PetruTheme { AddMediaScreen(state.copy(importProgress = ImportProgress(2, 5)), onEvent = {}) }
        }
    }

    @Test
    fun accessPromptExplainsWhyAndOffersThePicker() {
        paparazzi.snapshot {
            PetruTheme { AddMediaScreen(AddMediaState(permission = MediaAccess.NotAsked), onEvent = {}) }
        }
    }
}
