package com.petruvideo.feature.importmedia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruShapes

private val PANEL_MAX_WIDTH = 720.dp
private val PANEL_MAX_HEIGHT = 640.dp

/**
 * Add media on an expanded window: a large centered panel on a dimmed backdrop (FR-027a). The same screen content
 * is used, so the selection and import progress behave exactly as on a phone.
 */
@Composable
fun AddMediaPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxSize().background(Color.Black.copy(alpha = SCRIM_ALPHA)), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .padding(24.dp)
                .widthIn(max = PANEL_MAX_WIDTH)
                .heightIn(max = PANEL_MAX_HEIGHT)
                .clip(PetruShapes.Sheet)
                .background(PetruColors.Surface)
                .border(1.dp, PetruColors.Outline, PetruShapes.Sheet),
        ) { content() }
    }
}

private const val SCRIM_ALPHA = 0.6f
