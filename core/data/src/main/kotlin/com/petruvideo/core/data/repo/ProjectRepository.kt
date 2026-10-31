package com.petruvideo.core.data.repo

import com.petruvideo.core.model.Project
import com.petruvideo.core.model.ProjectStatus
import com.petruvideo.core.model.Timeline
import kotlinx.coroutines.flow.Flow

enum class ProjectFilter { All, Drafts, Exported }

data class ProjectSummary(
    val project: Project,
    val clipCount: Int,
    val durationMs: Long,
    val playheadMs: Long,
)

data class LoadedProject(val project: Project, val timeline: Timeline)

interface ProjectRepository {
    fun observeProjects(filter: ProjectFilter = ProjectFilter.All, query: String = ""): Flow<List<ProjectSummary>>

    suspend fun loadProject(id: String): LoadedProject?

    suspend fun createProject(): Project

    /** Replaces the clip rows in one transaction; rolls back and rethrows on failure so the last good rows stay. */
    suspend fun saveTimeline(projectId: String, timeline: Timeline)

    suspend fun duplicateProject(id: String): Project?

    /** Removes rows only; original media files are never touched. */
    suspend fun deleteProject(id: String)

    suspend fun rename(id: String, name: String)

    suspend fun setStatus(id: String, status: ProjectStatus)

    suspend fun setThumbnail(id: String, ref: String?)
}
