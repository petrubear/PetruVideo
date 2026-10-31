package com.petruvideo.feature.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.icons.PetruIcons

/** Small speaker-off marker on a muted audio clip. */
@Composable
fun MutedBadge(modifier: Modifier = Modifier) {
    Icon(
        PetruIcons.Mute,
        contentDescription = "Muted",
        tint = PetruColors.OnBackground,
        modifier = modifier
            .padding(2.dp)
            .background(PetruColors.Background, CircleShape)
            .padding(2.dp)
            .size(14.dp),
    )
}
