package com.petruvideo.feature.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petruvideo.core.data.repo.ProjectFilter
import com.petruvideo.core.data.repo.ProjectRepository
import com.petruvideo.core.data.repo.ProjectSummary
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.ProjectStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProjectsViewModel @Inject constructor(
    private val repository: ProjectRepository,
) : ViewModel() {

    /** Screen-only state that is not stored with the projects. */
    private data class Ui(
        val filter: ProjectFilter = ProjectFilter.All,
        val query: String = "",
        val searchOpen: Boolean = false,
        val selectedProjectId: String? = null,
        val pendingDeleteId: String? = null,
        val comingSoon: String? = null,
    )

    private val ui = MutableStateFlow(Ui())
    private val effectChannel = Channel<ProjectsEffect>(Channel.BUFFERED)

    val effects = effectChannel.receiveAsFlow()

    private val projects = repository.observeProjects()

    /** Media of the project in the detail pane; reloaded when the selection or the projects change. */
    private val detailMedia: Flow<List<MediaItem>> = combine(projects, ui) { all, ui ->
        val summary = all.firstOrNull { it.project.id == ui.selectedProjectId } ?: all.firstOrNull()
        // Keyed on the edit time too, so the media list follows the project when it changes.
        summary?.let { it.project.id to it.project.modifiedAt }
    }.distinctUntilChanged().flatMapLatest { key ->
        flow { emit(key?.let { repository.loadProject(it.first)?.timeline?.media?.values?.toList() }.orEmpty()) }
    }

    val state: StateFlow<ProjectsState> = combine(projects, ui, detailMedia.onStart { emit(emptyList()) }, ::toState)
        .stateIn(viewModelScope, SharingStarted.Eagerly, ProjectsState())

    fun onEvent(event: ProjectsEvent) {
        when (event) {
            ProjectsEvent.NewProject -> viewModelScope.launch {
                effectChannel.send(ProjectsEffect.OpenEditor(repository.createProject().id))
            }
            is ProjectsEvent.OpenProject -> effectChannel.trySend(ProjectsEffect.OpenEditor(event.id))
            is ProjectsEvent.ExportProject -> effectChannel.trySend(ProjectsEffect.OpenExport(event.id))
            ProjectsEvent.OpenExports -> effectChannel.trySend(ProjectsEffect.OpenExports)
            ProjectsEvent.OpenSettings -> effectChannel.trySend(ProjectsEffect.OpenSettings)
            ProjectsEvent.OpenTemplates -> effectChannel.trySend(ProjectsEffect.OpenTemplates)
            is ProjectsEvent.SelectProject -> ui.update { it.copy(selectedProjectId = event.id) }
            is ProjectsEvent.SetFilter -> ui.update { it.copy(filter = event.filter) }
            is ProjectsEvent.SetQuery -> ui.update { it.copy(query = event.query) }
            is ProjectsEvent.Rename -> rename(event)
            is ProjectsEvent.Duplicate -> viewModelScope.launch { repository.duplicateProject(event.id) }
            is ProjectsEvent.RequestDelete -> ui.update { it.copy(pendingDeleteId = event.id) }
            ProjectsEvent.CancelDelete -> ui.update { it.copy(pendingDeleteId = null) }
            ProjectsEvent.ConfirmDelete -> confirmDelete()
            ProjectsEvent.OpenSearch -> ui.update { it.copy(searchOpen = true) }
            ProjectsEvent.CloseSearch -> ui.update { it.copy(searchOpen = false, query = "") }
            is ProjectsEvent.TapInactive -> ui.update { it.copy(comingSoon = event.name) }
            ProjectsEvent.DismissComingSoon -> ui.update { it.copy(comingSoon = null) }
        }
    }

    private fun rename(event: ProjectsEvent.Rename) {
        val name = event.name.trim()
        if (name.isEmpty()) return
        viewModelScope.launch { repository.rename(event.id, name) }
    }

    private fun confirmDelete() {
        val id = ui.value.pendingDeleteId ?: return
        ui.update {
            it.copy(
                pendingDeleteId = null,
                selectedProjectId = it.selectedProjectId.takeUnless { selected -> selected == id },
            )
        }
        viewModelScope.launch { repository.deleteProject(id) }
    }

    private fun toState(all: List<ProjectSummary>, ui: Ui, media: List<MediaItem>): ProjectsState {
        val byFilter = when (ui.filter) {
            ProjectFilter.All -> all
            ProjectFilter.Drafts -> all.filter { it.project.status == ProjectStatus.Draft }
            ProjectFilter.Exported -> all.filter { it.project.status == ProjectStatus.Exported }
        }
        val query = ui.query.trim()
        val visible = if (query.isEmpty()) byFilter else byFilter.filter { it.project.name.contains(query, true) }
        return ProjectsState(
            isLoading = false,
            continueEditing = all.firstOrNull(),
            projects = visible,
            filter = ui.filter,
            counts = ProjectCounts(
                all = all.size,
                drafts = all.count { it.project.status == ProjectStatus.Draft },
                exported = all.count { it.project.status == ProjectStatus.Exported },
            ),
            query = ui.query,
            searchOpen = ui.searchOpen,
            selectedProjectId = ui.selectedProjectId,
            selectedMedia = media,
            pendingDeleteId = ui.pendingDeleteId,
            noResults = all.isNotEmpty() && visible.isEmpty(),
            comingSoon = ui.comingSoon,
        )
    }
}
