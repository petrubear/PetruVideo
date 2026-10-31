package com.petruvideo.feature.projects.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.petruvideo.core.data.repo.ProjectFilter
import com.petruvideo.core.designsystem.components.PetruChip
import com.petruvideo.feature.projects.ProjectCounts

@Composable
fun FilterChipRow(
    selected: ProjectFilter,
    counts: ProjectCounts,
    onSelect: (ProjectFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PetruChip("All ${counts.all}", selected == ProjectFilter.All, { onSelect(ProjectFilter.All) }, filled = true)
        PetruChip(
            "Drafts ${counts.drafts}",
            selected == ProjectFilter.Drafts,
            { onSelect(ProjectFilter.Drafts) },
            filled = true,
        )
        PetruChip(
            "Exported ${counts.exported}",
            selected == ProjectFilter.Exported,
            { onSelect(ProjectFilter.Exported) },
            filled = true,
        )
    }
}
