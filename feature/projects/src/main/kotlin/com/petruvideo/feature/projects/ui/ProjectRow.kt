package com.petruvideo.feature.projects.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.petruvideo.core.data.repo.ProjectSummary
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruShapes
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.model.formatDuration
import com.petruvideo.feature.projects.RelativeTime

/** A project in the two-pane list: thumbnail left, name and details right. Selecting it fills the detail pane. */
@Composable
fun ProjectRow(
    summary: ProjectSummary,
    nowMs: Long,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val project = summary.project
    Row(
        modifier
            .fillMaxWidth()
            .clip(PetruShapes.Sheet)
            .background(PetruColors.Surface)
            .then(if (selected) Modifier.border(1.5.dp, PetruColors.Accent, PetruShapes.Sheet) else Modifier)
            .clickable(role = Role.Button, onClick = onSelect)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProjectThumbnail(project, Modifier.size(width = 84.dp, height = 60.dp).clip(PetruShapes.Chip))
        Column(Modifier.weight(1f)) {
            Text(
                project.name,
                style = PetruType.Body.copy(fontWeight = FontWeight.Bold),
                color = PetruColors.OnBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${RelativeTime.format(project.modifiedAt, nowMs)} · ${summary.clipCount} clips · " +
                    formatDuration(summary.durationMs),
                style = PetruType.Caption,
                color = PetruColors.OnSurfaceMuted,
            )
        }
    }
}
