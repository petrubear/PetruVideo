package com.petruvideo.feature.projects.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.model.ProjectStatus
import com.petruvideo.core.model.formatDuration
import com.petruvideo.feature.projects.RelativeTime

private val ThumbShape = RoundedCornerShape(16.dp)

/** Project grid card: thumbnail, duration badge, "Exported" badge, name, relative date, options. */
@Composable
fun ProjectCard(
    summary: ProjectSummary,
    nowMs: Long,
    selected: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val project = summary.project
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ProjectThumbnail(
            project = project,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f / 0.82f)
                .clip(ThumbShape)
                .then(if (selected) Modifier.border(2.5.dp, PetruColors.Accent, ThumbShape) else Modifier)
                .clickable(role = Role.Button, onClick = onClick),
        ) {
            ThumbnailBadges(project.status == ProjectStatus.Exported, summary.durationMs)
        }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    project.name,
                    style = PetruType.Body.copy(fontWeight = FontWeight.Bold),
                    color = PetruColors.OnBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    RelativeTime.format(project.modifiedAt, nowMs),
                    style = PetruType.Caption,
                    color = PetruColors.OnSurfaceMuted,
                )
            }
            ProjectOptionsMenu(project.name, onRename, onDuplicate, onDelete)
        }
    }
}

@Composable
private fun BoxScope.ThumbnailBadges(exported: Boolean, durationMs: Long) {
    if (exported) {
        Text(
            "Exported",
            style = PetruType.Tiny.copy(fontWeight = FontWeight.Bold),
            color = PetruColors.OnAccent,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(PetruColors.Accent)
                .padding(horizontal = 7.dp, vertical = 3.dp),
        )
    }
    Text(
        formatDuration(durationMs),
        style = PetruType.MonoSmall,
        color = PetruColors.OnBackground,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(8.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(PetruColors.Background.copy(alpha = 0.6f))
            .padding(horizontal = 6.dp, vertical = 3.dp),
    )
}
