package com.petruvideo.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import com.android.resources.NightMode
import app.cash.paparazzi.Paparazzi
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruTheme
import com.petruvideo.core.designsystem.icons.PetruIcons
import org.junit.Rule
import org.junit.Test

/** Golden for the shared components on the dark theme; reviewed by the designer before approval. */
class ComponentsScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(nightMode = NightMode.NIGHT))

    @Composable
    private fun Sample() {
        PetruTheme {
            Column(
                Modifier.background(PetruColors.Background).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SegmentedControl(listOf("Videos", "Photos", "Audio"), selectedIndex = 0, onSelect = {})
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PetruChip("Recent", selected = true, onClick = {})
                    PetruChip("Camera", selected = false, onClick = {})
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconAction(PetruIcons.Undo, "Undo", onClick = {})
                    IconAction(PetruIcons.Play, "Play", onClick = {})
                    IconAction(PetruIcons.Split, "Split", onClick = {}, dimmed = true)
                }
                PillButton("Add to timeline in order", onClick = {}, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }

    @Test
    fun components() {
        paparazzi.snapshot { Sample() }
    }
}
