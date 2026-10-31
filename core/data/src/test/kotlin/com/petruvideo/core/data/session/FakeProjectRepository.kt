package com.petruvideo.core.data.session

import com.petruvideo.core.data.repo.LoadedProject
import com.petruvideo.core.data.repo.ProjectFilter
import com.petruvideo.core.data.repo.ProjectRepository
import com.petruvideo.core.data.repo.ProjectSummary
import com.petruvideo.core.model.Project
import com.petruvideo.core.model.ProjectStatus
import com.petruvideo.core.model.Timeline
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeProjectRepository(private val projects: Map<String, LoadedProject> = emptyMap()) : ProjectRepository {
    val saves = mutableListOf<Pair<String, Timeline>>()

    override fun observeProjects(filter: ProjectFilter, query: String): Flow<List<ProjectSummary>> = flowOf(emptyList())

    override suspend fun loadProject(id: String) = projects[id]

    override suspend fun createProject(): Project = error("not used")

    override suspend fun saveTimeline(projectId: String, timeline: Timeline) {
        saves += projectId to timeline
    }

    override suspend fun duplicateProject(id: String): Project? = null

    override suspend fun deleteProject(id: String) = Unit

    override suspend fun rename(id: String, name: String) = Unit

    override suspend fun setStatus(id: String, status: ProjectStatus) = Unit

    override suspend fun setThumbnail(id: String, ref: String?) = Unit
}
