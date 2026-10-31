package com.petruvideo.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ColumnScope
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruShapes

/** Raised container used for the selection tray and large-screen panels. */
@Composable
fun SheetSurface(
    modifier: Modifier = Modifier,
    shape: Shape = PetruShapes.Sheet,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .background(PetruColors.Surface, shape)
            .border(1.dp, PetruColors.Outline, shape),
        content = content,
    )
}
