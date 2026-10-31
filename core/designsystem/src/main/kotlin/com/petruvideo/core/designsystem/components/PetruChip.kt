package com.petruvideo.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruShapes
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.designsystem.Spacing

/** Album chip (outlined) and filter chip (filled when selected). */
@Composable
fun PetruChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
) {
    val border = BorderStroke(1.dp, if (selected) PetruColors.Accent else PetruColors.Outline)
    val background = when {
        filled && selected -> PetruColors.OnBackground
        filled -> PetruColors.Surface
        else -> PetruColors.Background
    }
    val content = when {
        filled && selected -> PetruColors.Background
        selected -> PetruColors.Accent
        else -> PetruColors.OnSurfaceMuted
    }
    // The visible chip is 32 to 36 dp; the clickable box is padded up to the 48 dp touch target.
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = Spacing.MinTouchTarget, minHeight = Spacing.MinTouchTarget)
            .clip(PetruShapes.Chip)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = PetruType.BodySmall,
            color = content,
            modifier = Modifier
                .clip(PetruShapes.Chip)
                .background(background)
                .then(if (filled) Modifier else Modifier.border(border, PetruShapes.Chip))
                .padding(horizontal = 14.dp, vertical = 7.dp),
        )
    }
}
