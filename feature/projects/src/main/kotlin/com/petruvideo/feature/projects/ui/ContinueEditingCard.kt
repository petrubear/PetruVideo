package com.petruvideo.feature.projects.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petruvideo.core.data.repo.ProjectSummary
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruShapes
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.model.formatDuration
import com.petruvideo.feature.projects.RelativeTime

/** The "Continue editing" card for the most recently edited project. */
@Composable
fun ContinueEditingCard(
    summary: ProjectSummary,
    nowMs: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = if (summary.durationMs > 0) {
        (summary.playheadMs.toFloat() / summary.durationMs).coerceIn(0f, 1f)
    } else {
        0f
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(PetruShapes.CardLarge)
            .background(PetruColors.Surface)
            .border(1.dp, PetruColors.Outline, PetruShapes.CardLarge)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlaceholderThumbnail(
            seed = summary.project.id,
            modifier = Modifier.size(96.dp).clip(RoundedCornerShape(14.dp)),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("CONTINUE EDITING", style = PetruType.Overline, color = PetruColors.Accent)
            Text(
                summary.project.name,
                style = PetruType.TitleSmall,
                color = PetruColors.OnBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${RelativeTime.format(summary.project.modifiedAt, nowMs)} · " +
                    "${summary.clipCount} clips · ${formatDuration(summary.durationMs)}",
                style = PetruType.BodySmall,
                color = PetruColors.OnSurfaceMuted,
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PetruColors.SurfaceHighest),
            ) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(progress).background(PetruColors.Accent))
            }
            Text(
                "Stopped at ${formatDuration(summary.playheadMs)} of ${formatDuration(summary.durationMs)}",
                style = PetruType.MonoSmall.copy(fontSize = 11.sp),
                color = PetruColors.OnSurfaceMuted,
            )
        }
    }
}
