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
import androidx.compose.ui.res.stringResource
import com.petruvideo.feature.editor.R

/** Small marker on an audio clip that lowers itself while other sound plays. */
@Composable
fun DuckBadge(amountPercent: Int, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.editor_ducking_on, amountPercent)
    Text(
        stringResource(R.string.editor_duck),
        style = PetruType.Tiny,
        color = PetruColors.OnBackground,
        modifier = modifier
            .padding(2.dp)
            .background(PetruColors.Background, RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp)
            .clearAndSetSemantics { contentDescription = description },
    )
}
