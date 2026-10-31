package com.petruvideo.feature.projects.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.designsystem.Spacing
import com.petruvideo.core.designsystem.adaptive.HingeBounds
import com.petruvideo.core.designsystem.adaptive.HingeSplit
import com.petruvideo.core.designsystem.components.PillButton
import com.petruvideo.feature.projects.ProjectsEvent
import com.petruvideo.feature.projects.ProjectsState

private const val LIST_WEIGHT = 0.72f

/**
 * Expanded Projects: the list on the left, and the selected project's details and media on the right, with no
 * navigation. With a vertical hinge the two panes sit on either side of it. The latest project is shown until the
 * user selects another.
 */
@Composable
fun ProjectsTwoPane(
    state: ProjectsState,
    onEvent: (ProjectsEvent) -> Unit,
    nowMs: Long,
    hinge: HingeBounds?,
    modifier: Modifier = Modifier,
) {
    val selectedId = state.selectedProjectId ?: state.projects.firstOrNull()?.project?.id
    val selected = state.projects.firstOrNull { it.project.id == selectedId }
    HingeSplit(hinge, modifier, firstWeight = LIST_WEIGHT, first = {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Spacing.ScreenGutter, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Projects",
                    style = PetruType.Display,
                    color = PetruColors.OnBackground,
                    modifier = Modifier.weight(1f),
                )
                PillButton("+ New project", { onEvent(ProjectsEvent.NewProject) })
            }
            FilterChipRow(
                state.filter,
                state.counts,
                { onEvent(ProjectsEvent.SetFilter(it)) },
                Modifier.padding(horizontal = Spacing.ScreenGutter),
            )
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.ScreenGutter),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.projects, key = { it.project.id }) { summary ->
                    ProjectRow(
                        summary = summary,
                        nowMs = nowMs,
                        selected = summary.project.id == selectedId,
                        onSelect = { onEvent(ProjectsEvent.SelectProject(summary.project.id)) },
                    )
                }
                if (state.noResults) item { NoResults() }
            }
        }
    }, second = {
        ProjectDetailPane(
            summary = selected,
            media = state.selectedMedia,
            onOpen = { selectedId?.let { onEvent(ProjectsEvent.OpenProject(it)) } },
            onDuplicate = { selectedId?.let { onEvent(ProjectsEvent.Duplicate(it)) } },
            onDelete = { selectedId?.let { onEvent(ProjectsEvent.RequestDelete(it)) } },
            onExport = { onEvent(ProjectsEvent.TapInactive("Export")) },
            modifier = Modifier.statusBarsPadding(),
        )
    })
}
