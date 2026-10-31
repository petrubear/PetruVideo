package com.petruvideo.feature.projects.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruShapes
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.designsystem.Spacing
import com.petruvideo.core.designsystem.adaptive.HingeBounds
import com.petruvideo.core.designsystem.components.IconAction
import com.petruvideo.core.designsystem.icons.PetruIcons
import com.petruvideo.feature.projects.ProjectsEvent
import com.petruvideo.feature.projects.ProjectsState
import kotlinx.coroutines.delay

private const val COMING_SOON_MS = 2_000L
private const val COMPACT_COLUMNS = 2
private const val WIDE_COLUMNS = 3

/**
 * The phone Projects screen as designed. [wideContent] (medium width) keeps the same layout with more grid
 * columns. Stateless: all behavior flows through [onEvent].
 */
@Composable
fun ProjectsScreen(
    state: ProjectsState,
    onEvent: (ProjectsEvent) -> Unit,
    modifier: Modifier = Modifier,
    wideContent: Boolean = false,
    nowMs: Long = System.currentTimeMillis(),
    twoPane: Boolean = false,
    hinge: HingeBounds? = null,
) {
    var renamingId by rememberSaveable { mutableStateOf<String?>(null) }

    Box(modifier.fillMaxSize().background(PetruColors.Background)) {
        if (twoPane) {
            ProjectsTwoPane(state, onEvent, nowMs, hinge)
        } else {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                Header(state, onEvent)
                ProjectsGrid(
                    state = state,
                    onEvent = onEvent,
                    nowMs = nowMs,
                    wideContent = wideContent,
                    onRename = { renamingId = it },
                    modifier = Modifier.weight(1f),
                )
            }
            BottomNavigation(onEvent, Modifier.align(Alignment.BottomCenter))
        }
        state.comingSoon?.let { ComingSoonMessage(it, onEvent, Modifier.align(Alignment.BottomCenter)) }
    }

    renamingId?.let { id ->
        val current = (state.projects + listOfNotNull(state.continueEditing)).firstOrNull { it.project.id == id }
        RenameDialog(
            currentName = current?.project?.name.orEmpty(),
            onConfirm = { onEvent(ProjectsEvent.Rename(id, it)); renamingId = null },
            onDismiss = { renamingId = null },
        )
    }
    if (state.pendingDeleteId != null) {
        DeleteConfirmationDialog(
            onConfirm = { onEvent(ProjectsEvent.ConfirmDelete) },
            onDismiss = { onEvent(ProjectsEvent.CancelDelete) },
        )
    }
}

@Composable
private fun Header(state: ProjectsState, onEvent: (ProjectsEvent) -> Unit) {
    Column(Modifier.padding(horizontal = Spacing.ScreenGutter).padding(top = 12.dp, bottom = 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("PETRUVIDEO", style = PetruType.BodySmall, color = PetruColors.OnSurfaceMuted)
                Text("Projects", style = PetruType.Display, color = PetruColors.OnBackground)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeaderButton(PetruIcons.Search, "Search") { onEvent(ProjectsEvent.OpenSearch) }
                HeaderButton(PetruIcons.Settings, "Settings") { onEvent(ProjectsEvent.TapInactive("Settings")) }
            }
        }
        if (state.searchOpen) {
            SearchField(state.query, onEvent, Modifier.padding(top = 12.dp))
        }
    }
}

@Composable
private fun HeaderButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconAction(icon, description, onClick, Modifier.clip(PetruShapes.Button).background(PetruColors.Surface))
}

@Composable
private fun SearchField(query: String, onEvent: (ProjectsEvent) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .height(Spacing.MinTouchTarget)
            .clip(PetruShapes.Button)
            .background(PetruColors.Surface)
            .padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = query,
            onValueChange = { onEvent(ProjectsEvent.SetQuery(it)) },
            singleLine = true,
            textStyle = PetruType.Body.copy(color = PetruColors.OnBackground),
            cursorBrush = SolidColor(PetruColors.Accent),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (query.isEmpty()) Text("Search projects", style = PetruType.Body, color = PetruColors.OnSurfaceMuted)
                inner()
            },
        )
        IconAction(PetruIcons.Close, "Close search", { onEvent(ProjectsEvent.CloseSearch) })
    }
}

@Composable
internal fun NoResults() {
    Text(
        "No projects match",
        style = PetruType.Body,
        color = PetruColors.OnSurfaceMuted,
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
    )
}

@Composable
private fun BottomNavigation(onEvent: (ProjectsEvent) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 18.dp)
            .fillMaxWidth()
            .height(68.dp)
            .clip(NavShape)
            .background(PetruColors.SurfaceHigh)
            .border(1.dp, PetruColors.Outline, NavShape),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavEntry("Projects", PetruIcons.NavProjects, active = true) {}
        NavEntry("Templates", PetruIcons.NavTemplates, active = false) {
            onEvent(ProjectsEvent.TapInactive("Templates"))
        }
        NavEntry("Exports", PetruIcons.NavExports, active = false) { onEvent(ProjectsEvent.TapInactive("Exports")) }
    }
}

private val NavShape = RoundedCornerShape(26.dp)

@Composable
private fun NavEntry(label: String, icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    val tint = if (active) PetruColors.Accent else PetruColors.OnSurfaceMuted
    Column(
        Modifier
            .height(Spacing.MinTouchTarget + 8.dp)
            .clip(PetruShapes.Button)
            .alpha(if (active) 1f else 0.85f)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.height(22.dp))
        Text(label, style = PetruType.Tiny, color = tint)
    }
}

@Composable
private fun ComingSoonMessage(name: String, onEvent: (ProjectsEvent) -> Unit, modifier: Modifier = Modifier) {
    LaunchedEffect(name) {
        delay(COMING_SOON_MS)
        onEvent(ProjectsEvent.DismissComingSoon)
    }
    Text(
        "Coming soon",
        style = PetruType.Body,
        color = PetruColors.OnBackground,
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = 104.dp)
            .clip(PetruShapes.Chip)
            .background(PetruColors.SurfaceHighest)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
private fun ProjectsGrid(
    state: ProjectsState,
    onEvent: (ProjectsEvent) -> Unit,
    nowMs: Long,
    wideContent: Boolean,
    onRename: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(if (wideContent) WIDE_COLUMNS else COMPACT_COLUMNS),
        modifier = modifier,
        contentPadding = PaddingValues(
            start = Spacing.ScreenGutter,
            end = Spacing.ScreenGutter,
            bottom = 112.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        state.continueEditing?.takeIf { !state.searchOpen }?.let { latest ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                ContinueEditingCard(
                    latest,
                    nowMs,
                    onClick = { onEvent(ProjectsEvent.OpenProject(latest.project.id)) },
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            FilterChipRow(state.filter, state.counts, { onEvent(ProjectsEvent.SetFilter(it)) })
        }
        item { NewProjectTile(onClick = { onEvent(ProjectsEvent.NewProject) }) }
        items(state.projects, key = { it.project.id }) { summary ->
            ProjectCard(
                summary = summary,
                nowMs = nowMs,
                selected = summary.project.id == state.selectedProjectId,
                onClick = { onEvent(ProjectsEvent.OpenProject(summary.project.id)) },
                onRename = { onRename(summary.project.id) },
                onDuplicate = { onEvent(ProjectsEvent.Duplicate(summary.project.id)) },
                onDelete = { onEvent(ProjectsEvent.RequestDelete(summary.project.id)) },
            )
        }
        if (state.noResults) {
            item(span = { GridItemSpan(maxLineSpan) }) { NoResults() }
        }
    }
}
