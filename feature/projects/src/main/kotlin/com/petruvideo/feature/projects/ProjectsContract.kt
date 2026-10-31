package com.petruvideo.feature.projects

import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.data.repo.ProjectFilter
import com.petruvideo.core.data.repo.ProjectSummary

data class ProjectCounts(val all: Int, val drafts: Int, val exported: Int)

data class ProjectsState(
    val isLoading: Boolean = true,
    val continueEditing: ProjectSummary? = null,
    val projects: List<ProjectSummary> = emptyList(),
    val filter: ProjectFilter = ProjectFilter.All,
    val counts: ProjectCounts = ProjectCounts(0, 0, 0),
    val query: String = "",
    val searchOpen: Boolean = false,
    /** Selected project in the two-pane (expanded) layout. */
    val selectedProjectId: String? = null,
    /** Media of the project shown in the detail pane (the selected one, else the latest). */
    val selectedMedia: List<MediaItem> = emptyList(),
    val pendingDeleteId: String? = null,
    /** True when a query or filter hides every project that exists. */
    val noResults: Boolean = false,
    /** Name of the inactive entry that was tapped, shown as a "coming soon" message. */
    val comingSoon: String? = null,
)

sealed interface ProjectsEvent {
    data object NewProject : ProjectsEvent
    data class OpenProject(val id: String) : ProjectsEvent
    data class ExportProject(val id: String) : ProjectsEvent
    data class SelectProject(val id: String) : ProjectsEvent
    data class SetFilter(val filter: ProjectFilter) : ProjectsEvent
    data class SetQuery(val query: String) : ProjectsEvent
    data class Rename(val id: String, val name: String) : ProjectsEvent
    data class Duplicate(val id: String) : ProjectsEvent
    data class RequestDelete(val id: String) : ProjectsEvent
    data object ConfirmDelete : ProjectsEvent
    data object CancelDelete : ProjectsEvent
    data object OpenSearch : ProjectsEvent
    data object CloseSearch : ProjectsEvent
    data class TapInactive(val name: String) : ProjectsEvent
    data object DismissComingSoon : ProjectsEvent
}

sealed interface ProjectsEffect {
    data class OpenEditor(val projectId: String) : ProjectsEffect
    data class OpenExport(val projectId: String) : ProjectsEffect
}
