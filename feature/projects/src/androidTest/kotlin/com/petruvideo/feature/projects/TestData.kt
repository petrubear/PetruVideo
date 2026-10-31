package com.petruvideo.feature.projects

import com.petruvideo.core.data.repo.ProjectSummary
import com.petruvideo.core.model.Project
import com.petruvideo.core.model.ProjectStatus

fun summary(
    id: String,
    name: String = "Project $id",
    status: ProjectStatus = ProjectStatus.Draft,
    modifiedAt: Long = 0,
) = ProjectSummary(
    project = Project(id, name, createdAt = 0, modifiedAt = modifiedAt, status = status),
    clipCount = 3,
    durationMs = 60_000,
    playheadMs = 0,
)
