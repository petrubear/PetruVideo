package com.petruvideo.feature.projects

import com.petruvideo.core.data.repo.LoadedProject
import com.petruvideo.core.data.repo.ProjectFilter
import com.petruvideo.core.data.repo.ProjectRepository
import com.petruvideo.core.data.repo.ProjectSummary
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.Project
import com.petruvideo.core.model.ProjectStatus
import com.petruvideo.core.model.Timeline
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeProjectsRepository(
    initial: List<ProjectSummary> = emptyList(),
    private val media: Map<String, List<MediaItem>> = emptyMap(),
) : ProjectRepository {
    val projects = MutableStateFlow(initial)
    val renamed = mutableListOf<Pair<String, String>>()
    val duplicated = mutableListOf<String>()
    val deleted = mutableListOf<String>()
    val saved = mutableListOf<Pair<String, Timeline>>()
    var created = 0

    override fun observeProjects(filter: ProjectFilter, query: String): Flow<List<ProjectSummary>> = projects

    override suspend fun loadProject(id: String): LoadedProject? {
        val items = media[id] ?: return null
        val summary = projects.value.firstOrNull { it.project.id == id } ?: return null
        return LoadedProject(summary.project, Timeline(id, media = items.associateBy { it.id }))
    }

    override suspend fun createProject(): Project {
        created++
        return Project("new-$created", "Untitled $created", 0, 0)
    }

    override suspend fun saveTimeline(projectId: String, timeline: Timeline) {
        saved += projectId to timeline
    }

    override suspend fun duplicateProject(id: String): Project? {
        duplicated += id
        return null
    }

    override suspend fun deleteProject(id: String) {
        deleted += id
    }

    override suspend fun rename(id: String, name: String) {
        renamed += id to name
    }

    override suspend fun setStatus(id: String, status: ProjectStatus) = Unit

    override suspend fun setThumbnail(id: String, ref: String?) = Unit
}

fun summary(
    id: String,
    name: String = "Project $id",
    status: ProjectStatus = ProjectStatus.Draft,
    modifiedAt: Long = 0,
    clips: Int = 3,
    durationMs: Long = 60_000,
) = ProjectSummary(
    project = Project(id, name, createdAt = 0, modifiedAt = modifiedAt, status = status),
    clipCount = clips,
    durationMs = durationMs,
    playheadMs = 0,
)
