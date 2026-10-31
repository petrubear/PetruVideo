package com.petruvideo.feature.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.designsystem.Spacing

/** Stand-in for a clip whose source file is missing; playback skips it and shows black for its length. */
@Composable
fun MissingClipPlaceholder(name: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier
            .clip(shape)
            .background(PetruColors.SurfaceHigh)
            .border(1.5.dp, PetruColors.Danger, shape)
            .semantics { contentDescription = "$name is missing" },
        contentAlignment = Alignment.Center,
    ) {
        Text("Missing", style = PetruType.Tiny, color = PetruColors.Danger)
    }
}

/** Relink or Remove actions for the first missing file (FR-012). */
@Composable
fun MissingMediaBar(
    name: String,
    count: Int,
    onRelink: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().background(PetruColors.SurfaceHigh).padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            if (count > 1) "$name and ${count - 1} more are missing" else "$name is missing",
            style = PetruType.BodySmall,
            color = PetruColors.OnBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        BarAction("Relink", PetruColors.Accent, onRelink)
        BarAction("Remove", PetruColors.Danger, onRemove)
    }
}

@Composable
private fun BarAction(label: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Box(
        Modifier
            .heightIn(min = Spacing.MinTouchTarget)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = PetruType.Label, color = color)
    }
}
