package com.petruvideo.feature.projects.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruShapes
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.designsystem.icons.PetruIcons

/** The dashed "New project" tile that starts the grid. */
@Composable
fun NewProjectTile(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val dash = PetruColors.OutlineDashed
    Column(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f / 1.12f)
            .clip(PetruShapes.Card)
            .drawBehind {
                drawRoundRect(
                    color = dash,
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))),
                )
            }
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "New project" },
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(PetruColors.Accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                PetruIcons.Add,
                contentDescription = null,
                tint = PetruColors.OnAccent,
                modifier = Modifier.size(24.dp),
            )
        }
        Text("New project", style = PetruType.Label, color = PetruColors.OnBackground)
    }
}
