package com.petruvideo.feature.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruType

/** Small marker on an audio clip that lowers itself while other sound plays. */
@Composable
fun DuckBadge(amountPercent: Int, modifier: Modifier = Modifier) {
    Text(
        "Duck",
        style = PetruType.Tiny,
        color = PetruColors.OnBackground,
        modifier = modifier
            .padding(2.dp)
            .background(PetruColors.Background, RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp)
            .clearAndSetSemantics { contentDescription = "Ducking on, $amountPercent percent" },
    )
}
